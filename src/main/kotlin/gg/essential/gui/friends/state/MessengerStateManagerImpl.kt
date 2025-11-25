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
package gg.essential.gui.friends.state

import com.sparkuniverse.toolbox.chat.enums.ChannelType
import com.sparkuniverse.toolbox.chat.model.Channel
import com.sparkuniverse.toolbox.chat.model.Message
import gg.essential.Essential
import gg.essential.connectionmanager.common.packet.Packet
import gg.essential.elementa.utils.ObservableList
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.MutableListState
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.Observer
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.add
import gg.essential.gui.elementa.state.v2.clear
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableListStateOf
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.removeAll
import gg.essential.gui.elementa.state.v2.set
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.gui.friends.message.v2.ClientMessage
import gg.essential.gui.friends.message.v2.infraInstanceToClient
import gg.essential.network.connectionmanager.chat.ChatManager
import gg.essential.universal.UMinecraft
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import java.util.*
import java.util.concurrent.CompletableFuture


class MessengerStateManagerImpl(private val chatManager: ChatManager) : IMessengerManager, IMessengerStates {


    /** IMessengerStates Fields*/
    private val channelStates = mutableMapOf<Long, ChannelStates>()
    @Deprecated("Not used in protocol 9 or later")
    private val messageUnreadMap = mutableMapOf<Pair<Long, Long>, MutableState<Boolean>>()
    private val observableMessageList = mutableMapOf<Long, Pair<MutableListState<ClientMessage>, ListState<ClientMessage>>>()

    /** IMessengerActions Fields **/
    private val messageRequests = mutableSetOf<Long>()

    private val observableChannelList = ObservableList(chatManager.channels.values.toMutableList())

    private val channelStateChangeCallbacks = mutableListOf<(Channel) -> Unit>()
    private val resetCallbacks = mutableListOf<() -> Unit>()

    init {
        observableChannelList.removeIf {
            it.isAnnouncement() && it.id != chatManager.primaryAnnouncementChannelId
        }
        chatManager.registerStateManager(this)
    }

    override fun getNumUnread(channelId: Long): State<Int> {
        return getOrCreateChannelStates(channelId).numUnreadMessages
    }

    override fun getUnreadChannelState(channelId: Long): State<Boolean> {
        return getOrCreateChannelStates(channelId).numUnreadMessages.map { it > 0 }
    }

    override fun getMuted(channelId: Long): MutableState<Boolean> {
        return getOrCreateChannelStates(channelId).mutedState
    }

    override fun getLatestMessage(channelId: Long): State<ClientMessage?> {
        return getOrCreateChannelStates(channelId).latestMessage
    }

    override fun getMessageListState(channelId: Long): ListState<ClientMessage> {
        return observableMessageList.computeIfAbsent(channelId) {
            val messages = getMessages(channelId)?.map { infraInstanceToClient(it) }?.toTypedArray()
                ?: emptyArray()
            val baseMessages = mutableListStateOf(*messages)
            // Filter messages
            val filteredMessages = memo {
                val unlockedData by lazy { Essential.getInstance().connectionManager.cosmeticsManager.unlockedCosmeticsData() }
                baseMessages().filter { message ->
                    for (part in message.parts) {
                        when (part) {
                            is ClientMessage.Part.Gift -> {
                                if (message.sender == UUIDUtil.getClientUUID()) {
                                    continue
                                }
                                val data = unlockedData[part.id]
                                if (data == null || data.giftedBy != message.sender) {
                                    return@filter false
                                }
                            }
                            else -> {}
                        }
                    }
                    true
                }
            }.toListState()
            Pair(baseMessages, filteredMessages)
        }.second
    }

    override fun getObservableMemberList(channelId: Long): ObservableList<UUID> {
        return getOrCreateChannelStates(channelId).members
    }

    override fun getObservableChannelList(): ObservableList<Channel> {
        return observableChannelList
    }

    @Deprecated("Not used in protocol 9 or later")
    private fun getUnreadMessageStates(channelId: Long, messageId: Long): State<Boolean> {
        return messageUnreadMap.computeIfAbsent(Pair(channelId, messageId)) {
            mutableStateOf(chatManager.getMessageById(channelId, messageId)?.isRead?.not() ?: false)
        }
    }

    @Deprecated("Not used in protocol 9 or later")
    override fun getUnreadMessageState(channelId: Long, messageId: Long): State<Boolean> {
        return getUnreadMessageStates(channelId, messageId)
    }

    override fun getLastReadMessageId(channelId: Long): State<Long?> {
        return getOrCreateChannelStates(channelId).lastReadMessageId
    }

    override fun setLastReadMessage(channelId: Long, messageId: Long?) {
        chatManager.setLastReadMessage(channelId, messageId)
    }

    override fun getTitle(channelId: Long): State<String> {
        return getOrCreateChannelStates(channelId).title
    }

    private fun getOrCreateChannelStates(channelId: Long): ChannelStates {
        return channelStates.computeIfAbsent(channelId) {
            createChannelStates(it)
        }
    }

    private fun getChannel(channelId: Long): Channel {
        return chatManager.getChannel(channelId).orElseThrow {
            IllegalStateException("Channel $channelId not found")
        }
    }

    private fun createChannelStates(channelId: Long): ChannelStates {
        val channel = getChannel(channelId)
        return ChannelStates(
            channel,
            if (platform.cmConnection.usingProtocol >= 9) {
                mutableStateOf(channel.unreadMessages)
            } else {
                memo { getMessageListState(channelId)().count { getUnreadMessageState(it)() } }
            },
            mutableStateOf(channel.isMuted),
            mutableStateOf("Loading..."),
            getMessageListState(channelId).map { list -> list.maxByOrNull { it.id } },
            ObservableList(channel.members.toMutableList()),
            mutableStateOf(channel.lastReadMessageId)
        ).apply {
            updateChannelStates(channel, this)
        }
    }

    private fun updateChannelStates(channel: Channel, states: ChannelStates) {
        states.apply {
            internalMutedState.set(channel.isMuted)
            if (platform.cmConnection.usingProtocol >= 9) {
                if (numUnreadMessages is MutableState) numUnreadMessages.set(channel.unreadMessages)
                lastReadMessageId.set(channel.lastReadMessageId)
            }

            if (channel.isAnnouncement()) {
                states.title.set("Announcements")
            } else if (channel.type == ChannelType.GROUP_DIRECT_MESSAGE) {
                states.title.set(channel.name)
            } else {
                UUIDUtil.getName(channel.getOtherUser())
                    .thenAcceptAsync({ username ->
                        states.title.set(username)
                    }, UMinecraft.getMinecraft().executor)
            }
            states.members.removeAll(states.members - channel.members)
            states.members.addAll(channel.members - states.members)

            channelStateChangeCallbacks.forEach { it.invoke(channel) }
        }
    }

    private fun getMessages(channelId: Long): Collection<Message>? {
        return if (channelId in chatManager.announcementChannelIds) {
            chatManager.announcementChannelIds.mapNotNull {
                chatManager.getMessages(it)?.values
            }.takeIf { it.isNotEmpty() }?.flatten()
        } else {
            chatManager.getMessages(channelId)?.values
        }
    }

    override fun getMessagesRaw(channelId: Long): Map<Long, Message>? {
        return chatManager.getMessages(channelId)
    }

    override fun retrieveMessageHistoryRaw(channelId: Long, before: Long?, after: Long?, messageLimit: Int, callback: ((Optional<Packet>) -> Unit)?) {
        return chatManager.retrieveMessageHistory(channelId, before, after, messageLimit, callback)
    }

    /** IMessengerActions **/
    @Deprecated("Not used in protocol 9 or later")
    override fun setUnreadState(channelId: Long, messageId: Long, unread: Boolean) {
        val message = chatManager.getMessageById(channelId, messageId) ?: return
        chatManager.updateReadState(message, !unread)
    }

    override fun setTitle(channelId: Long, title: String) {
        val channel = getChannel(channelId)
        if (channel.name == title) {
            return
        }
        if (channel.type != ChannelType.GROUP_DIRECT_MESSAGE) {
            throw IllegalStateException("Cannot set the title of a channel that is not a group direct message")
        }
        // Channel name is updated when confirmed by the CM
        chatManager.updateChannelInformation(channelId, title, null)
    }

    override fun requestMoreMessages(channelId: Long, messageLimit: Int, beforeId: Long?): Boolean {
        if (messageRequests.add(channelId)) {
            if (channelId in chatManager.announcementChannelIds) {
                for (announcementChannelId in chatManager.announcementChannelIds) {
                    // No callback because messageReceived will be called when the messages are received
                    chatManager.retrieveMessageHistory(announcementChannelId, beforeId, null, messageLimit, null)
                }
            } else {
                // No callback because messageReceived will be called when the messages are received
                chatManager.retrieveMessageHistory(channelId, beforeId, null, messageLimit, null)
            }
            return true
        }
        return false
    }

    override fun sendMessage(channelId: Long, message: String, replyTo: Long?, callback: ((Optional<Packet>) -> Unit)?) {
        chatManager.sendMessage(channelId, message, replyTo, callback)
    }

    override fun editMessage(channelId: Long, messageId: Long, content: String, callback: (Boolean) -> Unit) {
        chatManager.editMessage(channelId, messageId, content, callback)
    }

    override fun deleteMessage(channelId: Long, messageId: Long) {
        chatManager.deleteMessage(channelId, messageId)
    }

    override fun leaveGroup(channelId: Long) {
        chatManager.removePlayerFromChannel(channelId, UUIDUtil.getClientUUID())
    }

    override fun removeUser(channelId: Long, user: UUID) {
        chatManager.removePlayerFromChannel(channelId, user)
    }

    override fun createGroup(members: Set<UUID>, groupName: String): CompletableFuture<Channel> {
        val future = CompletableFuture<Channel>()
        chatManager.createGroupDM(members.toTypedArray(), groupName) {
            if (it.isPresent) {
                future.complete(it.get())
            } else {
                future.completeExceptionally(RuntimeException("Failed to create group"))
            }
        }
        return future
    }

    override fun addMembers(channelId: Long, members: Set<UUID>) {
        chatManager.addPlayersToChannel(channelId, members.toTypedArray())
    }

    override fun onChannelStateChange(callback: (Channel) -> Unit) {
        channelStateChangeCallbacks.add(callback)
    }

    /** IMessengerConnectionManagerCallbacks **/

    override fun messageDeleted(message: Message) {
        val channelId = chatManager.mergeAnnouncementChannel(message.channelId)
        observableMessageList[channelId]?.first?.removeAll { it.id == message.id }
        messageUnreadMap.remove(Pair(message.channelId, message.id))
        val channelState = channelStates[channelId] ?: return
        updateChannelStates(getChannel(channelId), channelState)

    }

    override fun messageReceived(channel: Channel, message: Message) {
        @Suppress("NAME_SHADOWING")
        val channel = getChannel(chatManager.mergeAnnouncementChannel(channel.id))
        messageRequests.remove(channel.id)
        observableMessageList[channel.id]?.first?.let { messageList ->
            // Prevent duplicates from being added
            val index = messageList.getUntracked().indexOfFirst { it.id == message.id }
            val newMessage = infraInstanceToClient(message)
            if (index != -1) {
                messageList.set(index, newMessage)
            } else {
                messageList.add(newMessage)
            }
        }
        val states = channelStates[channel.id] ?: return
        if (platform.cmConnection.usingProtocol >= 9) {
            updateChannelStates(channel, states)
        } else {
            if (message.sender == UUIDUtil.getClientUUID() && !message.isRead) {
                // Will call updateChannelStates so no need to do it twice
                setUnreadState(message, false)
            } else {
                updateChannelStates(channel, states)
            }
        }
    }

    @Deprecated("Not used in protocol 9 or later")
    override fun messageReadStateUpdated(message: Message, read: Boolean) {
        messageUnreadMap[Pair(message.channelId, message.id)]?.set(!read)
        chatManager.getChannel(chatManager.mergeAnnouncementChannel(message.channelId)).ifPresent {
            channelUpdated(it)
        }
    }

    override fun channelUpdated(channel: Channel) {
        @Suppress("NAME_SHADOWING")
        val channel = getChannel(chatManager.mergeAnnouncementChannel(channel.id))
        val states = channelStates[channel.id] ?: return
        updateChannelStates(channel, states)
    }

    override fun newChannel(channel: Channel) {
        if (channel.isAnnouncement() && channel.id != chatManager.primaryAnnouncementChannelId) {
            return
        }
        if (observableChannelList.none { it.id == channel.id }) {
            observableChannelList.add(channel)
        }
    }

    override fun channelDeleted(channel: Channel) {
        observableChannelList.remove(channel)
    }

    override fun registerResetListener(callback: () -> Unit) {
        resetCallbacks.add(callback)
    }

    override fun reset() {
        // Call callbacks first so state can be saved
        resetCallbacks.forEach { it() }

        observableMessageList.values.forEach {
            it.first.clear()
        }
        observableMessageList.clear()
        messageUnreadMap.clear()
        messageRequests.clear()
        observableChannelList.clear()
        channelStates.clear()
    }

    // Internal class to group all base states together
    private inner class ChannelStates(
        val channel: Channel,
        val numUnreadMessages: State<Int>,
        val internalMutedState: MutableState<Boolean>,
        val title: MutableState<String>,
        val latestMessage: State<ClientMessage?>,
        val members: ObservableList<UUID>,
        val lastReadMessageId: MutableState<Long?>,
    ) {
        val mutedState: MutableState<Boolean> = object : MutableState<Boolean> {
            override fun Observer.get(): Boolean = internalMutedState()
            override fun set(mapper: (Boolean) -> Boolean) {
                val oldMuted = internalMutedState.getUntracked()
                val newMuted = mapper(oldMuted)
                if (newMuted == oldMuted) return
                internalMutedState.set(newMuted)
                chatManager.updateMutedState(channel, newMuted)
            }
        }
    }
}
