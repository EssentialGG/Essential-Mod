/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.handlers

import com.mojang.authlib.properties.Property
import gg.essential.mixins.ext.server.dispatcher
import gg.essential.universal.UMinecraft
import gg.essential.util.Client
import gg.essential.util.JsonHolder
import gg.essential.util.USession
import gg.essential.util.httpGetToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.Base64
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.EmptyCoroutineContext

//#if MC>=12002
//$$ import com.google.common.collect.LinkedHashMultimap
//$$ import com.mojang.authlib.yggdrasil.ProfileResult
//$$ import gg.essential.mixins.transformers.feature.skin_overwrites.MinecraftAccessor
//$$ import com.mojang.authlib.GameProfile
//$$ import java.util.concurrent.CompletableFuture
//#endif

//#if MC>=12109
//$$ import com.mojang.authlib.properties.PropertyMap
//$$ import gg.essential.mixins.transformers.feature.skin_overwrites.PlayerEntityAccessor
//#endif

object MinecraftGameProfileTexturesRefresher {
    private val LOGGER = LoggerFactory.getLogger(MinecraftGameProfileTexturesRefresher::class.java)
    const val SESSION_URL: String = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false"

    fun updateTextures(newHash: String?, textureType: String) {
         runBlocking(if (!UMinecraft.isCallingFromMinecraftThread()) Dispatchers.Client else EmptyCoroutineContext) {
             val property = createTextureProperty(USession.activeNow().uuid, newHash ?: "") { base64 -> getTextureValue(base64, textureType) }
             if (property != null) {
                 updateClientTextures(property)
                 updateIntegratedServerTextures(property)
             }
        }
    }

    private fun updateClientTextures(property: Property) {
        //#if MC>=12002
        //$$ val profile = MinecraftClient.getInstance().gameProfile
        //$$ val propertyMap = LinkedHashMultimap.create(profile.properties)
        //$$ propertyMap.removeAll("textures")
        //$$ propertyMap.put("textures", property)
        //#if MC>=12109
        //$$ val newProfile = GameProfile(profile.id, profile.name, PropertyMap(propertyMap))
        //#else
        //$$ val newProfile = GameProfile(profile.id, profile.name)
        //$$ newProfile.properties.putAll(propertyMap)
        //#endif
        //$$ (MinecraftClient.getInstance() as MinecraftAccessor).setGameProfileFuture(
        //$$         CompletableFuture.completedFuture(ProfileResult(newProfile, setOf()))
        //$$ )
        //#else
        val propertyMap = Minecraft.getMinecraft().profileProperties
        propertyMap.removeAll("textures")
        propertyMap.put("textures", property)
        //#endif
    }

    private suspend fun createTextureProperty(uuid: UUID, newHash: String, getHashValue: (String) -> String): Property? {
        var propertyValues = getTextureProperty(uuid)
        val maxDelay = 60000L
        var delay = 500L
        // If the hash of the texture property from Mojang doesn't match, it means the API instance hasn't updated yet
        // See: EM-2640
        while (propertyValues == null || getHashValue(propertyValues.first) != newHash) {
            delay *= 2
            if (delay >= maxDelay) {
                LOGGER.warn("Unable to get new cape property")
                return null
            }
            delay(delay)
            propertyValues = getTextureProperty(uuid)
        }
        return Property("textures", propertyValues.first, propertyValues.second)
    }

    private fun getTextureValue(base64: String, value: String): String {
        val skinHolder = JsonHolder(String(Base64.getDecoder().decode(base64)))
            .optJSONObject("textures")
            .optJSONObject(value)
        return skinHolder.optString("url").substringAfterLast('/')
    }

    private suspend fun getTextureProperty(uuid: UUID): Pair<String, String>? {
        try {
            val jsonHolder = JsonHolder(
                httpGetToString(
                    String.format(
                        Locale.ROOT,
                        SESSION_URL,
                        uuid.toString().replace("-", "")
                    )
                )
            )
            if (jsonHolder.keys.isEmpty()) {
                return null
            }

            val properties = jsonHolder.optJSONArray("properties")[0].asJsonObject
            return Pair(
                properties["value"].asString,
                properties["signature"].asString
            )
        } catch (e: IOException) {
            LOGGER.error("Failed to fetch texture property", e)
            return null
        }
    }

    private suspend fun updateIntegratedServerTextures(property: Property) {
        Minecraft.getMinecraft().integratedServer?.let { integratedServer ->
            val uuid = USession.activeNow().uuid
            withContext(integratedServer.dispatcher) {
                val player = integratedServer.playerList.players.find { it.uniqueID == uuid }
                //#if MC>=12109
                //$$ player?.gameProfile?.let { profile ->
                //$$     val propertyMap = LinkedHashMultimap.create(profile.properties())
                //$$     propertyMap.removeAll("textures")
                //$$     propertyMap.put("textures", property)
                //$$     (player as? PlayerEntityAccessor)?.setGameProfile(GameProfile(profile.id, profile.name, PropertyMap(propertyMap)))
                //$$ }
                //#else
                player?.gameProfile?.properties?.run {
                    removeAll("textures")
                    put("textures", property)
                }
                //#endif
            }
        }
    }
}