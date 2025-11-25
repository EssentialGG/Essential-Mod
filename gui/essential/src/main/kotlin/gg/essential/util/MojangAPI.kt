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

import gg.essential.api.utils.JsonHolder
import gg.essential.api.utils.mojang.*
import gg.essential.api.utils.mojang.MojangAPI
import gg.essential.lib.gson.Gson
import okhttp3.*
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture

object MojangAPI : MojangAPI {

    private val LOGGER = LoggerFactory.getLogger(MojangAPI::class.java)
    private val gson = Gson()

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
        val profile = UuidNameLookup.fetchProfileFromUUID(uuid) ?: return null
        return Profile(profile.id, profile.name, profile.properties.map { Property(it.name, it.value) })
    }

    override fun changeSkin(accessToken: String, uuid: UUID, model: Model, url: String): SkinResponse? {
        try {
            val payload = JsonHolder().put("variant", model.variant).put("url", url)
            val MEDIA_TYPE = MediaType.parse("application/json")

            val body = RequestBody.create(MEDIA_TYPE, payload.toString())
            val request = Request.Builder()
                .url("https://api.minecraftservices.com/minecraft/profile/skins")
                .header("Authorization", "Bearer $accessToken")
                .post(body)
                .build()

            val output = httpClient.join().newCall(request).execute().use { it.body()!!.string() }
            return gson.fromJson(output, SkinResponse::class.java)
        } catch (e: Exception) {
            LOGGER.error("An error occurred while updating skin", e)
        }
        return null

    }

    fun uploadSkin(accessToken: String, model: Model, file: File): SkinResponse? {

        try {
            val body: RequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", "skin.png",
                    RequestBody.create(MediaType.parse("image/png"), file)
                )
                .addFormDataPart(
                    "variant", model.variant
                )
                .build()


            val request = Request.Builder()
                .url("https://api.minecraftservices.com/minecraft/profile/skins")
                .header("Authorization", "Bearer $accessToken")
                .post(body)
                .build()

            val output = httpClient.join().newCall(request).execute().use { it.body()!!.string() }
            return gson.fromJson(output, SkinResponse::class.java)
        } catch (e: Exception) {
            LOGGER.error("An error occurred while uploading a skin to Mojang", e)
        }
        return null
    }
}