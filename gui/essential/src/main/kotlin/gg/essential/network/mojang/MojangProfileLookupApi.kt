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
package gg.essential.network.mojang

import gg.essential.mod.Model
import gg.essential.mod.Skin
import gg.essential.util.RateLimitException
import gg.essential.util.UuidAsDashlessStringSerializer
import gg.essential.util.executeAwait
import gg.essential.util.httpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.Request
import java.io.IOException
import java.util.Base64
import java.util.UUID

/**
 * Provides a Kotlin interface to Mojang's "/session/minecraft/profile/<uuid>" API.
 *
 * All methods are completely unmanaged (no locking) and direct (no local caching or anything).
 * All methods are fully thread-safe.
 */
object MojangProfileLookupApi {
    private val json = Json { ignoreUnknownKeys = true }

    private val UUID_TO_NAME_API = System.getProperty(
        "minecraft.api.session.host",
        "https://sessionserver.mojang.com",
    ) + "/session/minecraft/profile/"

    @Throws(RateLimitException::class, IOException::class)
    suspend fun fetch(uuid: UUID, signed: Boolean = false, bypassProxyCache: Boolean = false): Profile? = withContext(Dispatchers.IO) {
        val url = HttpUrl.parse(UUID_TO_NAME_API)!!.newBuilder().apply {
            addPathSegment(uuid.toString().replace("-", ""))
            if (signed) addQueryParameter("unsigned", "false")
            if (bypassProxyCache) addQueryParameter("cache_buster", UUID.randomUUID().toString())
        }.build()
        val request = Request.Builder().url(url).build()

        httpClient().newCall(request).executeAwait().use { response ->
            if (response.code() == 204) return@use null // user doesn't exist
            decodeMojangResponse<Profile>(response)
        }
    }

    @Deprecated("Use coroutines")
    @Throws(RateLimitException::class, IOException::class)
    fun fetchBlocking(uuid: UUID): Profile? =
        runBlocking { fetch(uuid) }

    @Serializable
    data class Property(
        val name: String,
        val value: String,
        // Only present when explicitly requested
        val signature: String? = null,
    ) {
        val decodedValue: String
            get() = String(Base64.getDecoder().decode(value))

        val decodedAsTextures: Profile.Textures
            get() = json.decodeFromString(decodedValue)
    }

    @Serializable
    data class Profile(
        @Serializable(UuidAsDashlessStringSerializer::class)
        val id: UUID,
        val name: String,
        val properties: List<Property> = emptyList(),
    ) {
        fun property(name: String): Property? = properties.find { it.name == name }

        val texturesProperty: Property?
            get() = property("textures")

        val textures: Textures?
            get() = texturesProperty?.decodedAsTextures

        @Serializable
        data class Textures(val textures: Map<String, Texture> = emptyMap()) {
            val skin: Skin?
                get() = textures["SKIN"]?.let { Skin.fromUrl(it.url, it.metadata.model) }
            val capeHash: String?
                get() = textures["CAPE"]?.hash
        }

        @Serializable
        data class Texture(val url: String, val metadata: TextureMetadata = TextureMetadata(Model.STEVE)) {
            val hash: String
                get() = url.substringAfterLast('/')
        }

        @Serializable
        data class TextureMetadata(val model: Model)
    }
}
