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
package gg.essential.gui.friends.message

import gg.essential.elementa.components.UIContainer
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.friends.message.v2.ClientMessage
import gg.essential.gui.friends.message.v2.MessageWrapper
import gg.essential.gui.friends.previews.ChannelPreview

/**
 * A message screen is a component in the social menu that displays the messages of a channel.
 */
abstract class MessageScreen : UIContainer(){

    val replyingTo = mutableStateOf<ClientMessage?>(null)

    val editingMessage = mutableStateOf<ClientMessage?>(null)

    /**
     * The channel preview of the channel that this message screen is displaying.
     */
    abstract val preview: ChannelPreview

    /**
     * Called when this screen is opened.
     */
    abstract fun onOpen()

    /**
     * Called when this screen is closed.
     */
    abstract fun onClose()

    /**
     * Called when the user has clicked on the context of a reply
     * and the message screen should scroll to the message and briefly highlight it
     */
    abstract fun scrollToMessage(message: ClientMessage)

    /**
     * Retries to resend the contents of the message
     */
    abstract fun retrySend(message: ClientMessage)

    /**
     * Removes a given unsent message
     */
    abstract fun removeUnsent(message: ClientMessage)

    /**
     * Called when [messageWrapper] has marked itself as manually unread
     */
    @Deprecated("Not used in protocol 9 or later")
    abstract fun markedManuallyUnread(messageWrapper: MessageWrapper)

    /**
     * Called when all messages of this screen should be marked as read at once
     */
    @Deprecated("Not used in protocol 9 or later")
    abstract fun markAllAsRead()

    /**
     * Called when [messageWrapper] has marked itself as unread
     */
    abstract fun markMessageAsUnread(messageWrapper: MessageWrapper)
}