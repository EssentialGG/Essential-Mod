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
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.Response
import java.io.IOException

private val json = Json { ignoreUnknownKeys = true }

inline fun <reified T> decodeMojangResponse(response: Response): T =
    decodeMojangResponse(response, serializer<T>())

fun <T> decodeMojangResponse(response: Response, deserializer: DeserializationStrategy<T>): T {
    when (response.code()) {
        429 -> throw RateLimitException()
        else -> {}
    }

    val body = response.body()!!.string()

    try {
        return json.decodeFromString(deserializer, body)
    } catch (e: Exception) {
        val error = try {
            json.decodeFromString<ErrorResponse>(body)
        } catch (e1: Exception) {
            e.addSuppressed(e1)
            throw IOException("Invalid response (code ${response.code()}): `$body`", e)
        }
        throw MojangApiException(error)
    }
}

@Serializable
data class ErrorResponse(
    val error: String,
    val errorMessage: String,
    val cause: String? = null,
)

class MojangApiException(val response: ErrorResponse) : IOException(response.toString())
