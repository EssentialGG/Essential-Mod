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
package gg.essential.gui.account

import com.mojang.authlib.GameProfile
import com.mojang.authlib.exceptions.InvalidCredentialsException
import gg.essential.minecraftauth.exception.AuthenticationException
import gg.essential.minecraftauth.microsoft.MicrosoftAuthenticationService
import gg.essential.minecraftauth.microsoft.response.MicrosoftAccessTokenResponse
import gg.essential.minecraftauth.minecraft.MinecraftAuthenticationService
import gg.essential.minecraftauth.xbox.XboxLiveAuthenticationService
import gg.essential.handlers.account.WebAccountManager
import gg.essential.lib.gson.JsonDeserializationContext
import gg.essential.lib.gson.JsonDeserializer
import gg.essential.lib.gson.JsonElement
import gg.essential.lib.gson.JsonPrimitive
import gg.essential.lib.gson.JsonSerializationContext
import gg.essential.lib.gson.JsonSerializer
import gg.essential.lib.gson.annotations.JsonAdapter
import gg.essential.util.UUIDUtil
import java.lang.reflect.Type
import java.net.URI
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

class MicrosoftUserAuthentication {

    private var redirectUri: String? = null
    private var accessToken: Token? = null

    @field:JsonAdapter(LegacyTokenSerializer::class)
    private var refreshToken: Token? = null
    private var xblToken: Token? = null
    private var xstsToken: Token? = null
    private var uhs: String? = null
    private var mcToken: Token? = null
    private var profile: GameProfile? = null
    val expiryTime: Instant?
        get() = refreshToken?.expires
    var openUri: URI? = null

    fun logIn(future: CompletableFuture<URI>, forceRefresh: Boolean = false): Pair<GameProfile, String> {
        if (forceRefresh) {
            mcToken = null
        }
        val profile = acquireGameProfile(future)
        val token = acquireMCToken(future)
        return Pair(profile, token)
    }

    fun refreshRefreshToken() {
        acquireAccessToken(CompletableFuture.completedFuture(null), true)
    }

    private fun acquireAccessToken(future: CompletableFuture<URI>, forceRefresh: Boolean): String {
        if (!forceRefresh) {
            accessToken.ifValid { return it }
        }

        val refreshToken = this.refreshToken ?: return acquireAccessTokenViaOAuth(future)
        val response = MicrosoftAuthenticationService.refreshAccessToken(refreshToken.value)
        saveMicrosoftTokens(response)

        return response.accessToken
    }

    private fun acquireAccessTokenViaOAuth(future: CompletableFuture<URI>): String {
        val codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
        val code = acquireAuthorizationCode(codeVerifier, future)

        val response = MicrosoftAuthenticationService.exchangeCodeForAccessToken(code, codeVerifier, redirectUri ?: "")
        saveMicrosoftTokens(response)

        return response.accessToken
    }

    private fun saveMicrosoftTokens(response: MicrosoftAccessTokenResponse) {
        // Refresh tokens last 90 days https://docs.microsoft.com/en-us/azure/active-directory/develop/refresh-tokens
        this.refreshToken = Token(response.refreshToken, Instant.now() + Duration.ofDays(90))
        this.accessToken = Token(response.accessToken, Instant.now() + Duration.ofSeconds(response.expiresIn))
    }

    private fun acquireAuthorizationCode(codeVerifier: String, future: CompletableFuture<URI>): String {
        //Get future from account manager instead of ourselves

        redirectUri = WebAccountManager.microsoftRedirectUri

        val uri = MicrosoftAuthenticationService.generateAuthorizationURI(codeVerifier, redirectUri ?: "")
        if (future.isDone) {
            throw InvalidCredentialsException("Re-authentication with Microsoft required")
        }
        future.complete(uri)
        openUri = uri
        return WebAccountManager.authorizationCodeFuture?.join()!!
    }

    // https://wiki.vg/Microsoft_Authentication_Scheme#Authenticate_with_XBL
    private fun acquireXBLToken(future: CompletableFuture<URI>, retry: Boolean = true): String {
        xblToken.ifValid { return it }

        val accessToken = acquireAccessToken(future, false)

        val response = try {
            XboxLiveAuthenticationService.authenticateWithXboxLive(accessToken)
        } catch (e: AuthenticationException.InvalidCredentials) {
            if (retry) {
                this.accessToken = null
                return this.acquireXBLToken(future, false)
            } else {
                throw e
            }
        }

        val expires = Instant.parse(response.notAfter)
        this.xblToken = Token(response.token, expires)

        return response.token
    }

    private fun acquireXSTSToken(future: CompletableFuture<URI>, retry: Boolean = true): Pair<String, String> {
        xstsToken.ifValid { return Pair(it, uhs!!) }

        val xblToken = acquireXBLToken(future)

        val (response, userHash) = try {
            XboxLiveAuthenticationService.authenticateWithXSTS(xblToken)
        } catch (e: AuthenticationException.InvalidCredentials) {
            if (retry) {
                this.xblToken = null
                return this.acquireXSTSToken(future, false)
            } else {
                throw e
            }
        }

        val expires = Instant.parse(response.notAfter)

        this.uhs = userHash
        this.xstsToken = Token(response.token, expires)

        return response.token to userHash
    }

    private fun acquireMCToken(future: CompletableFuture<URI>, retry: Boolean = true): String {
        mcToken.ifValid { return it }

        val (xstsToken, uhs) = acquireXSTSToken(future)

        val response = try {
            MinecraftAuthenticationService.authenticateWithXbox(xstsToken, uhs)
        } catch (e: AuthenticationException.InvalidCredentials) {
            if (retry) {
                this.xstsToken = null
                return this.acquireMCToken(future, false)
            } else {
                throw e
            }
        }

        val token = response.accessToken
        val expiresIn = response.expiresIn

        mcToken = Token(token, Instant.now() + Duration.ofSeconds(expiresIn))

        // Since we have a new access token for Minecraft, the profile might be different.
        profile = null

        return token
    }

    private fun acquireGameProfile(future: CompletableFuture<URI>): GameProfile {
        // We acquire the token before checking the cache so that we regularly refresh the profile.
        val token = acquireMCToken(future)
        profile?.let { return it }

        val response = MinecraftAuthenticationService.getProfile(token)
        val profile = GameProfile(UUIDUtil.formatWithDashes(response.id), response.name)

        this.profile = profile
        return profile
    }

    data class Token(val value: String, val expires: Instant) {
        val expired: Boolean
            get() = expires.isBefore(Instant.now())
    }

    private inline fun Token?.ifValid(block: (token: String) -> Unit) {
        if (this != null && !this.expired) {
            block(value)
        }
    }

    private class LegacyTokenSerializer : JsonSerializer<Token>, JsonDeserializer<Token> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Token {
            return when (json) {
                is JsonPrimitive -> Token(json.asString, Instant.now())
                else -> context.deserialize(json, Token::class.java)
            }
        }

        override fun serialize(src: Token, typeOfSrc: Type, context: JsonSerializationContext): JsonElement =
            context.serialize(src)
    }
}
