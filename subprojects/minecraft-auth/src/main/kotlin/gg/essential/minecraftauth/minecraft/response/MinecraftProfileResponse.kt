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
package gg.essential.minecraftauth.minecraft.response

import kotlinx.serialization.Serializable

// There are more key/value pairs associated with this response, but we don't really need them.
// https://minecraft.wiki/w/Microsoft_authentication#Getting_the_profile
@Serializable
data class MinecraftProfileResponse(
    val id: String,
    val name: String
)
