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

import gg.essential.util.RateLimitException
import gg.essential.util.UuidAsDashlessStringSerializer
import gg.essential.util.executeAwait
import gg.essential.util.httpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request
import java.io.IOException
import java.util.UUID

/**
 * Provides a Kotlin interface to Mojang's "/minecraft/profile/lookup/name/<name>" API.
 *
 * All methods are completely unmanaged (no locking) and direct (no local caching or anything).
 * All methods are fully thread-safe.
 */
object MojangNameToUuidApi {
    private val NAME_TO_UUID_API = System.getProperty(
        "minecraft.api.services.host",
        "https://api.minecraftservices.com",
    ) + "/minecraft/profile/lookup/name/"

    @Throws(RateLimitException::class, IOException::class)
    suspend fun fetch(username: String): Profile? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(NAME_TO_UUID_API + username).build()

        httpClient().newCall(request).executeAwait().use { response ->
            if (response.code() == 404) return@use null // user doesn't exist
            decodeMojangResponse<Profile>(response)
        }
    }

    @Deprecated("Use coroutines")
    @Throws(RateLimitException::class, IOException::class)
    fun fetchBlocking(username: String): Profile? =
        runBlocking { fetch(username) }

    @Serializable
    data class Profile(
        @Serializable(UuidAsDashlessStringSerializer::class)
        val id: UUID,
        val name: String,
    )
}
