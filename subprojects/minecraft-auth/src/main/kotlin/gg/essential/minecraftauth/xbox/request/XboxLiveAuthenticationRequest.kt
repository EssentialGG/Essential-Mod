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
package gg.essential.minecraftauth.xbox.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

@Serializable
data class XboxLiveAuthenticationRequest(
    @SerialName("Properties")
    val properties: JsonObject,

    @SerialName("RelyingParty")
    val relyingParty: String,

    @SerialName("TokenType")
    val tokenType: String,
) {
    companion object {
        /**
         * Creates a new Xbox Live authentication request which uses a Microsoft access token.
         * The data for this request was derived from [minecraft.wiki's Microsoft Authentication Documentation](https://minecraft.wiki/w/Microsoft_authentication#Authenticate_with_Xbox_Live).
         */
        fun xboxLiveUser(accessToken: String): XboxLiveAuthenticationRequest {
            @Suppress("HttpUrlsUsage")
            return XboxLiveAuthenticationRequest(
                buildJsonObject {
                    put("AuthMethod", "RPS")
                    put("SiteName", "user.auth.xboxlive.com")
                    put("RpsTicket", "d=$accessToken")
                },
                "http://auth.xboxlive.com",
                "JWT",
            )
        }

        /**
         * Creates a new Xbox Services Security Token request from an Xbox Live token.
         * The data for this request was derived from [minecraft.wiki's Microsoft Authentication Documentation](https://minecraft.wiki/w/Microsoft_authentication#Obtain_XSTS_token_for_Minecraft).
         */
        fun xsts(xboxLiveToken: String) : XboxLiveAuthenticationRequest {
            return XboxLiveAuthenticationRequest(
                buildJsonObject {
                    put("SandboxId", "RETAIL")
                    putJsonArray("UserTokens") { add(xboxLiveToken) }
                },
                "rp://api.minecraftservices.com/",
                "JWT"
            )
        }
    }
}