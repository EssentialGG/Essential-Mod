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
package gg.essential.util

import gg.essential.api.utils.mojang.*
import gg.essential.api.utils.mojang.MojangAPI
import gg.essential.mod.Skin
import gg.essential.network.mojang.MojangProfileApi
import gg.essential.network.mojang.MojangProfileLookupApi
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture

object MojangAPI : MojangAPI {

    private val LOGGER = LoggerFactory.getLogger(MojangAPI::class.java)

    override fun getUUID(name: String): CompletableFuture<UUID>? {
        return UuidNameLookup.getUUID(name)
    }

    override fun getName(uuid: UUID): CompletableFuture<String>? {
        return UuidNameLookup.getName(uuid)
    }

    @Deprecated("Name history has been removed from the Mojang API")
    override fun getNameHistory(uuid: UUID?): List<Name?>? {
        return emptyList()
    }

    override fun getProfile(uuid: UUID): Profile? {
        val profile = MojangProfileLookupApi.fetchBlocking(uuid) ?: return null
        return Profile(profile.id.toString().replace("-", ""), profile.name, profile.properties.map { Property(it.name, it.value) })
    }

    override fun changeSkin(accessToken: String, uuid: UUID, model: Model, url: String): SkinResponse? {
        try {
            val api = MojangProfileApi(accessToken)
            return runBlocking { api.putSkin(Skin.fromUrl(url, model.toModModel())).toSkinResponse() }
        } catch (e: Exception) {
            LOGGER.error("An error occurred while updating skin", e)
        }
        return null

    }

    fun uploadSkin(accessToken: String, model: Model, file: File): SkinResponse? {
        try {
            val api = MojangProfileApi(accessToken)
            return runBlocking { api.putSkin(file.readBytes(), model.toModModel()).toSkinResponse() }
        } catch (e: Exception) {
            LOGGER.error("An error occurred while uploading a skin to Mojang", e)
        }
        return null
    }

    private fun MojangProfileApi.Profile.toSkinResponse(): SkinResponse =
        SkinResponse(
            id.toString().replace("-", ""),
            name,
            skins.map { skin ->
                Skin(skin.id, if (skin.active) "ACTIVE" else "INACTIVE", skin.url, skin.variant.variant)
            },
            capes.map { cape ->
                Skin(cape.id, if (cape.active) "ACTIVE" else "INACTIVE", cape.url, "N/A")
            }
        )
}