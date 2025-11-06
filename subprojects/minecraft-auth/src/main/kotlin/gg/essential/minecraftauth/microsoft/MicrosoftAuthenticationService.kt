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
package gg.essential.minecraftauth.microsoft

import gg.essential.minecraftauth.exception.AuthenticationException
import gg.essential.minecraftauth.exception.MicrosoftAuthenticationException
import gg.essential.minecraftauth.microsoft.response.MicrosoftAccessTokenResponse
import gg.essential.minecraftauth.microsoft.response.MicrosoftAuthorizationErrorResponse
import gg.essential.minecraftauth.util.JSON
import gg.essential.minecraftauth.util.execute
import gg.essential.util.Sha256
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Request
import java.net.URI
import java.util.*

object MicrosoftAuthenticationService {
    private const val CLIENT_ID = "e39cc675-eb52-4475-b5f8-82aaae14eeba"

    // https://minecraft.wiki/w/Microsoft_authentication#Microsoft_OAuth2_flow
    private const val SCOPE = "XboxLive.signin XboxLive.offline_access"

    // https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow#request-an-authorization-code
    private const val OAUTH_AUTHORIZATION_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize"

    // https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow#request-an-access-token-with-a-client_secret
    private const val OAUTH_EXCHANGE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"

    /**
     * Attempts to exchange an authorization code for a Microsoft access token.
     *
     * @param code The code received from the callback URL
     * @param verifier A string used to secure the grant that was provided to [generateAuthorizationURI].
     * @param redirectUri The URI that Microsoft redirects the user to after they have completed authorization.
     *
     * @throws AuthenticationException If the response could not be decoded, or the user is rate-limited.
     * @throws MicrosoftAuthenticationException If Microsoft returns a bad response (4xx).
     * @throws java.io.IOException If an I/O error occurs while sending the request or reading the response.
     */
    @Throws(AuthenticationException::class, MicrosoftAuthenticationException::class)
    fun exchangeCodeForAccessToken(code: String, verifier: String, redirectUri: String): MicrosoftAccessTokenResponse {
        // https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow#request-an-access-token-with-a-client_secret
        return requestAccessToken("authorization_code") {
            add("code", code)
            add("redirect_uri", redirectUri)
            add("code_verifier", verifier)
        }
    }

    /**
     * Attempts to refresh the access token using the provided refresh token.
     *
     * @throws AuthenticationException If the response could not be decoded, or the user is rate-limited.
     * @throws MicrosoftAuthenticationException If Microsoft returns a bad response (4xx).
     * @throws java.io.IOException If an I/O error occurs while sending the request or reading the response.
     */
    @Throws(AuthenticationException::class, MicrosoftAuthenticationException::class)
    fun refreshAccessToken(refreshToken: String): MicrosoftAccessTokenResponse {
        // https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow#refresh-the-access-token
        return requestAccessToken("refresh_token") { add("refresh_token", refreshToken) }
    }

    /**
     * Generates a URI to be opened on the user's browser to authenticate with Microsoft.
     *
     * @param verifier A string used to secure the grant that also must be passed when retrieving the access token.
     * @param redirectUri The URI for Microsoft to redirect the user to after they have completed authorization.
     */
    fun generateAuthorizationURI(verifier: String, redirectUri: String): URI {
        // https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow#request-an-authorization-code
        val parameters = mapOf(
            "client_id" to CLIENT_ID,
            "response_type" to "code",
            "prompt" to "select_account",
            "redirect_uri" to redirectUri,
            "scope" to SCOPE,
            "code_challenge" to Base64.getUrlEncoder().withoutPadding().encodeToString(Sha256.compute(verifier.toByteArray()).bytes),
            "code_challenge_method" to "S256"
        )

        val httpBuilder = HttpUrl.parse(OAUTH_AUTHORIZATION_URL)!!.newBuilder()
        parameters.forEach { httpBuilder.addQueryParameter(it.key, it.value) }
        return httpBuilder.build().uri()
    }


    /**
     * Attempts to request an access token with the data from the provided method body.
     *
     * @throws AuthenticationException If the response could not be decoded, or the user is rate-limited.
     * @throws MicrosoftAuthenticationException If Microsoft returns a bad response (4xx).
     * @throws java.io.IOException If an I/O error occurs while sending the request or reading the response.
     */
    @Throws(AuthenticationException::class, MicrosoftAuthenticationException::class)
    private fun requestAccessToken(
        grantType: String,
        bodyBuilder: FormBody.Builder.() -> Unit
    ): MicrosoftAccessTokenResponse {
        val body = FormBody.Builder()
        bodyBuilder(body)
        val (status, content) = Request.Builder().url(OAUTH_EXCHANGE_URL)
            .header("Accept", "application/json")
            .post(body
                .add("client_id", CLIENT_ID)
                .add("scope", SCOPE)
                .add("grant_type", grantType)
                .build()
            ).build().execute()

        when (status) {
            200 -> {
                return runCatching { JSON.decodeFromString<MicrosoftAccessTokenResponse>(content) }
                    .getOrElse { throw AuthenticationException.InvalidResponse(status, content) }
            }

            // Otherwise, we likely have an error response that we can parse.
            else -> {
                val errorResponse = runCatching { JSON.decodeFromString<MicrosoftAuthorizationErrorResponse>(content) }
                    .getOrElse { throw AuthenticationException.InvalidResponse(status, content) }

                // The authorization code, refresh token, or PKCE code verifier is invalid/has expired.
                if (errorResponse.error == "invalid_grant") {
                    throw AuthenticationException.InvalidCredentials()
                }

                throw MicrosoftAuthenticationException(errorResponse)
            }
        }
    }
}