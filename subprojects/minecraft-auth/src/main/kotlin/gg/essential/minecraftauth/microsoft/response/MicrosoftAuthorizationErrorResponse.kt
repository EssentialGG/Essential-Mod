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
package gg.essential.minecraftauth.microsoft.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Microsoft never says if these can be null or not, but let's just assume that error will always be there, and
// description may or may not. Also, there are other key/value pairs, but we don't need them.
// https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow#error-response-1
@Serializable
data class MicrosoftAuthorizationErrorResponse(
    val error: String,

    @SerialName("error_description")
    val description: String? = null,
)
