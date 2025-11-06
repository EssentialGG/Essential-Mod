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
package gg.essential.minecraftauth.minecraft

import gg.essential.minecraftauth.exception.AuthenticationException
import gg.essential.minecraftauth.exception.MinecraftAuthenticationException
import gg.essential.minecraftauth.minecraft.request.MinecraftAuthenticationRequest
import gg.essential.minecraftauth.minecraft.response.MinecraftAuthenticationErrorResponse
import gg.essential.minecraftauth.minecraft.response.MinecraftProfileResponse
import gg.essential.minecraftauth.minecraft.response.MinecraftTokenResponse
import gg.essential.minecraftauth.util.JSON
import gg.essential.minecraftauth.util.JSON_MEDIA_TYPE
import gg.essential.minecraftauth.util.execute
import kotlinx.serialization.encodeToString
import okhttp3.Request
import okhttp3.RequestBody

object MinecraftAuthenticationService {
    private const val AUTHENTICATION_URI = "https://api.minecraftservices.com/authentication/login_with_xbox"
    private const val PROFILE_URI = "https://api.minecraftservices.com/minecraft/profile"

    /**
     * Attempts to authenticate with Minecraft Services via an Xbox Live token.
     *
     * @param token An xbox services security token.
     * @param userHash The user's hash from their Xbox Live claims.
     *
     * @see gg.essential.minecraftauth.xbox.XboxLiveAuthenticationService
     *
     * @throws MinecraftAuthenticationException When the authentication fails.
     * @throws AuthenticationException.Ratelimited When the API returns a 429 status code.
     */
    @Throws(MinecraftAuthenticationException::class)
    fun authenticateWithXbox(token: String, userHash: String): MinecraftTokenResponse {
        val request = MinecraftAuthenticationRequest("XBL3.0 x=$userHash;$token")
        val (status, content) = Request.Builder().url(AUTHENTICATION_URI)
            .header("Accept", "application/json")
            .post(RequestBody.create(JSON_MEDIA_TYPE, JSON.encodeToString(request)))
            .build().execute()

        return when (status) {
            200 -> runCatching { JSON.decodeFromString<MinecraftTokenResponse>(content) }
                .getOrElse { throw AuthenticationException.InvalidResponse(status, content) }

            401 -> throw AuthenticationException.InvalidCredentials()

            else -> {
                val body = runCatching { JSON.decodeFromString<MinecraftAuthenticationErrorResponse>(content) }
                    .getOrElse { throw AuthenticationException.InvalidResponse(status, content) }

                throw MinecraftAuthenticationException.Failed(body)
            }
        }
    }

    /**
     * Attempts to fetch the profile for the user that owns the provided access token.
     *
     * @param accessToken The minecraft services access token for the user.
     *
     * @throws MinecraftAuthenticationException When the request fails.
     * @throws MinecraftAuthenticationException.ProfileNotFound When the user does not own Minecraft.
     * @throws AuthenticationException.Ratelimited When the API returns a 429 status code.
     */
    @Throws(MinecraftAuthenticationException::class)
    fun getProfile(accessToken: String): MinecraftProfileResponse {
        val (status, content) = Request.Builder().url(PROFILE_URI)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Accept", "application/json").get().build().execute()

        return when (status) {
            200 -> runCatching { JSON.decodeFromString<MinecraftProfileResponse>(content) }
                .getOrElse { throw AuthenticationException.InvalidResponse(status, content) }

            401 -> throw AuthenticationException.InvalidCredentials()

            else -> {
                val body = runCatching { JSON.decodeFromString<MinecraftAuthenticationErrorResponse>(content) }
                    .getOrElse { throw AuthenticationException.InvalidResponse(status, content) }

                if (body.error == "NOT_FOUND") {
                    // In this case, it is fine to assume that this means that the user does not own Minecraft,
                    // as it is expected for the accessToken provided to be valid.
                    throw MinecraftAuthenticationException.ProfileNotFound()
                }

                throw MinecraftAuthenticationException.Failed(body)
            }
        }
    }
}