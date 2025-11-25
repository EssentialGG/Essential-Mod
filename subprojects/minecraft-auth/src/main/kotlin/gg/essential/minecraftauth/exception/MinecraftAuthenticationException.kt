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
package gg.essential.minecraftauth.exception

import gg.essential.minecraftauth.minecraft.response.MinecraftAuthenticationErrorResponse

sealed class MinecraftAuthenticationException(message: String) : AuthenticationException(message) {
    class Failed(response: MinecraftAuthenticationErrorResponse) :
        MinecraftAuthenticationException("Failed to authenticate with Minecraft Services: ${response.error}")

    class InsufficientPrivileges : MinecraftAuthenticationException("The user is not allowed to perform this operation.")

    class ProfileNotFound : MinecraftAuthenticationException("This profile does not own Minecraft.")
}
