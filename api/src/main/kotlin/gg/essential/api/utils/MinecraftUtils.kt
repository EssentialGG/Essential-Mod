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
package gg.essential.api.utils

import net.minecraft.util.ResourceLocation
import java.awt.image.BufferedImage

/**
 * Provides a collection of general Minecraft-related utility functions, so you don't have to copy and paste
 * common functions into your mods.
 */
interface MinecraftUtils {
    /**
     * Queues a message component to be displayed to the player in chat, client-side only.
     */
    //#if MC<12105
    @Deprecated("Use `UChat.chat` instead.")
    fun sendMessage(message: gg.essential.universal.wrappers.message.UTextComponent)
    //#endif

    /**
     * Queues a message to be displayed to the player in chat, client-side only. The input message is also
     * translated/formatted with minecraft internationalization utility, I18n.
     */
    fun sendChatMessageAndFormat(message: String)

    /**
     * Queues a message to be displayed to the player in chat, client-side only. The input message is also
     * translated/formatted with minecraft internationalization utility, I18n and the given parameters.
     */
    fun sendChatMessageAndFormat(message: String, vararg parameters: Any)

    /**
     * Queues a message to be displayed to the player in chat, client-side only.
     *
     * NOTE: This message is prefixed with `[Essential]`, and as such, should only be used to display Essential
     * information. If you simply want to send a normal message, use the function here that takes a [UTextComponent].
     */
    fun sendMessage(message: String)

    /**
     * Queues a message to be displayed to the player in chat, client-side only. The message is in the format:
     *  "$prefix&r$message"
     *
     * The input message is also translated/formatted with minecraft internationalization utility, I18n.
     */
    fun sendMessage(prefix: String, message: String)

    /**
     * @return whether the player is currently logged onto the Hypixel server
     */
    fun isHypixel(): Boolean

    /**
     * Loads the given ResourceLocation into memory as a BufferedImage, potentially for use in a DynamicTexture
     *
     * @return the image, or null if it failed to load
     */
    fun getResourceImage(location: ResourceLocation): BufferedImage?

    /**
     * @return true if the game is launched in the development environment rather than production
     */
    fun isDevelopment(): Boolean
}
