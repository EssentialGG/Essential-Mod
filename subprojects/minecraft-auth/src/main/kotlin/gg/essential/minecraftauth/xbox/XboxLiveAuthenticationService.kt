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
package gg.essential.minecraftauth.xbox

import gg.essential.minecraftauth.exception.AuthenticationException
import gg.essential.minecraftauth.exception.XboxLiveAuthenticationException
import gg.essential.minecraftauth.util.JSON
import gg.essential.minecraftauth.util.JSON_MEDIA_TYPE
import gg.essential.minecraftauth.util.execute
import gg.essential.minecraftauth.xbox.request.XboxLiveAuthenticationRequest
import gg.essential.minecraftauth.xbox.response.XboxLiveAuthenticationErrorResponse
import gg.essential.minecraftauth.xbox.response.XboxLiveTokenResponse
import gg.essential.util.har.requestBodyContainsSecrets
import gg.essential.util.har.responseBodyContainsSecrets
import kotlinx.serialization.encodeToString
import okhttp3.Request
import okhttp3.RequestBody

object XboxLiveAuthenticationService {
    private const val XBL_AUTHENTICATION_URI = "https://user.auth.xboxlive.com/user/authenticate"
    private const val XSTS_AUTHENTICATION_URI = "https://xsts.auth.xboxlive.com/xsts/authorize"

    /**
     * Attempts to authenticate with the Xbox Live service via a Microsoft Access Token.
     *
     * @param accessToken A valid Microsoft access token.
     *
     * @throws XboxLiveAuthenticationException If an invalid response is returned from the API.
     * @throws java.io.IOException If an I/O error occurs while sending the request or reading the response.
     */
    @Throws(XboxLiveAuthenticationException::class)
    fun authenticateWithXboxLive(accessToken: String): XboxLiveTokenResponse {
        // https://minecraft.wiki/w/Microsoft_authentication#Authenticate_with_Xbox_Live
        return makeRequest(XBL_AUTHENTICATION_URI, XboxLiveAuthenticationRequest.xboxLiveUser(accessToken))
    }

    /**
     * Attempts to get an Xbox Security Services Token via an Xbox Live access token.
     *
     * @param xblToken A valid Xbox Live token, usually from [authenticateWithXboxLive].
     *
     * @throws XboxLiveAuthenticationException If an invalid response is returned from the API.
     * @throws XboxLiveAuthenticationException.MissingXboxLiveClaims If a user-hash was not found in the response.
     * @throws java.io.IOException If an I/O error occurs while sending the request or reading the response.
     *
     * @return The xbox live token and the user hash.
     */
    @Throws(XboxLiveAuthenticationException::class)
    fun authenticateWithXSTS(xblToken: String): Pair<XboxLiveTokenResponse, String> {
        // https://minecraft.wiki/w/Microsoft_authentication#Obtain_XSTS_token_for_Minecraft
        val response = makeRequest(XSTS_AUTHENTICATION_URI, XboxLiveAuthenticationRequest.xsts(xblToken))
        val userHash = response.displayClaims["xui"]?.firstNotNullOfOrNull { it["uhs"] }
            ?: throw XboxLiveAuthenticationException.MissingXboxLiveClaims()

        return Pair(response, userHash)
    }


    /**
     * Attempts to send a [XboxLiveAuthenticationRequest] in a POST request to [uri].
     *
     * @param uri The URI to send the post request to.
     * @param request The request to serialize as a JSON body for the request.
     *
     * @throws XboxLiveAuthenticationException If an invalid response is returned from the API.
     * @throws java.io.IOException If an I/O error occurs while sending the request or reading the response.
     */
    @Throws(XboxLiveAuthenticationException::class)
    private fun makeRequest(uri: String, request: XboxLiveAuthenticationRequest): XboxLiveTokenResponse {
        val (status, content) = Request.Builder().url(uri)
            .header("Accept", "application/json")
            .requestBodyContainsSecrets()
            .responseBodyContainsSecrets()
            .post(RequestBody.create(JSON_MEDIA_TYPE, JSON.encodeToString(request)))
            .build().execute()

        when (status) {
            200 -> return runCatching { JSON.decodeFromString<XboxLiveTokenResponse>(content) }
                .getOrElse { throw AuthenticationException.InvalidResponse(status, content) }

            else -> {
                if (content.isEmpty()) {
                    // This exception isn't *exactly* correct, but there's no better way to know what caused the error,
                    // it could be because the schema changed - but that's unlikely, so the only other possible thing
                    // is that a credential is invalid somewhere.
                    throw AuthenticationException.InvalidCredentials()
                }

                val response = runCatching { JSON.decodeFromString<XboxLiveAuthenticationErrorResponse>(content) }
                    .getOrElse { throw AuthenticationException.InvalidResponse(status, content) }

                throw XboxLiveAuthenticationException.XboxLiveError(response.errorCode)
            }
        }
    }
}