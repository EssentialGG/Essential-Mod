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
package gg.essential.gui.friends.message.v2

import com.sparkuniverse.toolbox.chat.model.Message
import gg.essential.Essential

/**
 * Creates a message from a network type, excluding the replyTo state.
 */
fun infraInstanceToClient(message: Message): ClientMessage {
    val chatManager = Essential.getInstance().connectionManager.chatManager
    return ClientMessage(
        message.id,
        chatManager.getChannel(message.channelId).get(),
        message.sender,
        message.contents,
        SendState.Confirmed,
        message.replyTargetId?.let {
            MessageRef(message.channelId, it)
        },
        message.lastEditTime,
        message.createdAt,
    )
}
