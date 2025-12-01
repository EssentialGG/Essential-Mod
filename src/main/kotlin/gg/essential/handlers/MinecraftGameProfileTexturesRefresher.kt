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
import gg.essential.network.mojang.ManagedMojangProfileApi
import gg.essential.util.USession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory

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

    private val lock = Mutex()

    suspend fun updateTextures() = lock.withLock {
        val api = ManagedMojangProfileApi.forUser(USession.activeNow().uuid)
        val property = api.obtainSignedTextures()
            ?.let { Property(it.name, it.value, it.signature) }
        if (property != null) {
            updateClientTextures(property)
            updateIntegratedServerTextures(property)
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