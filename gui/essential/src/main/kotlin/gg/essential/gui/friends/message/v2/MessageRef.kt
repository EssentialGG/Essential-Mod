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

import com.sparkuniverse.toolbox.chat.enums.ChannelType
import com.sparkuniverse.toolbox.chat.model.Channel
import com.sparkuniverse.toolbox.chat.model.CreatedInfo
import com.sparkuniverse.toolbox.util.DateTime
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import java.util.*

/**
 * Utility class to manage lazy loading a message identified by [messageId]
 */
data class MessageRef(
    val channelId: Long,
    val messageId: Long,
) : Lazy<ClientMessage> {

    private val privateState: MutableState<ClientMessage?> = mutableStateOf(null)

    val asState: State<ClientMessage?>
        get() = privateState

    override val value: ClientMessage
        get() = privateState.getUntracked() ?: throw IllegalStateException("MessageRef has not been initialized")

    init {
        // When a message is deleted, replies to that message will have their replyTo id set to -1
        if (messageId == -1L) {
            supplyValue(DELETED)
        }
    }

    fun supplyValue(value: ClientMessage) {
        privateState.set(value)
    }

    /**
     * Requests the chat manager to eagerly load all messages in the channel until it retrieves the message
     * this MessageRef is holding
     */
    fun eagerlyLoad() {
        if (messageId != -1L && !isInitialized()) {
            platform.resolveMessageRef(this)
        }
    }

    /**
     * Returns true when the message is resolved
     */
    override fun isInitialized(): Boolean {
        return privateState.getUntracked() != null
    }

    companion object {

        private val deletedMessageFakeChannel = Channel(
            -1,
            ChannelType.GROUP_DIRECT_MESSAGE,
            "",
            null,
            null,
            emptySet(),
            CreatedInfo(DateTime(0), null),
            null,
            false,
            0,
            null,
            0
        )

        /**
         * Special instance of [ClientMessage] that is used to represent
         * all messages that have been deleted.
         */
        val DELETED = ClientMessage(
            -1,
            deletedMessageFakeChannel,
            UUID(0,0),
            "Message deleted",
            SendState.Confirmed,
            null,
            null,
            0
        )
    }
}