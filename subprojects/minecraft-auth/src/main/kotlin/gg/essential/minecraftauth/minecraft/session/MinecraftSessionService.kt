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
package gg.essential.minecraftauth.minecraft.session

import gg.essential.minecraftauth.exception.AuthenticationException
import gg.essential.minecraftauth.exception.MinecraftAuthenticationException
import gg.essential.minecraftauth.minecraft.response.MinecraftAuthenticationErrorResponse
import gg.essential.minecraftauth.minecraft.session.request.JoinServerRequest
import gg.essential.minecraftauth.util.JSON
import gg.essential.minecraftauth.util.JSON_MEDIA_TYPE
import gg.essential.minecraftauth.util.execute
import gg.essential.util.har.requestBodyContainsSecrets
import kotlinx.serialization.encodeToString
import okhttp3.Request
import okhttp3.RequestBody
import java.util.*

object MinecraftSessionService {
    private val JOIN_URI = System.getProperty(
        "minecraft.api.session.host",
        "https://sessionserver.mojang.com"
    ) + "/session/minecraft/join"

    /**
     * Attempts to send a "fake" join request to Mojang's session server.
     *
     * @param accessToken The access token for the user.
     * @param uuid The user's minecraft UUID.
     * @param serverId A unique hash provided by the server.
     *
     * @throws AuthenticationException.Ratelimited If the API returns a 429 status code.
     * @throws MinecraftAuthenticationException.InsufficientPrivileges If the user is not allowed to perform this action.
     * @throws MinecraftAuthenticationException.Failed If authentication with Minecraft Services fails.
     *
     * @returns The response code from the HTTP request.
     */
    fun joinServer(accessToken: String, uuid: UUID, serverId: String) {
        val request = JoinServerRequest(accessToken, uuid.toString().replace("-", ""), serverId)
        val (status, content) = Request.Builder().url(JOIN_URI)
            .requestBodyContainsSecrets()
            .post(RequestBody.create(JSON_MEDIA_TYPE, JSON.encodeToString(request)))
            .build().execute()

        when (status) {
            // If everything goes well, the client will receive a "204 No Content" response.
            204 -> {}

            429 -> throw AuthenticationException.Ratelimited()

            else -> {
                // Otherwise, the server will respond with an error code (providing the request reached the API).
                val body = runCatching { JSON.decodeFromString<MinecraftAuthenticationErrorResponse>(content) }
                    .getOrElse { throw AuthenticationException.InvalidResponse(status, content) }

                when (body.error) {
                    "InsufficientPrivilegesException" -> throw MinecraftAuthenticationException.InsufficientPrivileges()
                    "ForbiddenOperationException" -> throw AuthenticationException.InvalidCredentials()
                    else -> throw MinecraftAuthenticationException.Failed(body)
                }
            }
        }
    }
}