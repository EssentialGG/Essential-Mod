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

import com.sparkuniverse.toolbox.chat.model.Channel
import com.sparkuniverse.toolbox.chat.model.Message
import gg.essential.connectionmanager.common.packet.Packet
import gg.essential.elementa.state.State
import gg.essential.elementa.utils.ObservableList
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.stateUsingSystemTime
import gg.essential.gui.friends.message.v2.ClientMessage
import gg.essential.gui.elementa.state.v2.MutableState as MutableStateV2
import gg.essential.gui.elementa.state.v2.State as StateV2
import gg.essential.network.connectionmanager.relationship.RelationshipResponse
import gg.essential.network.connectionmanager.social.ProfileSuspension
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

interface SocialStates {
    val relationships: IRelationshipStates
    val messages: IMessengerStates
    val activity: IStatusStates
    val suspensions: ListState<ProfileSuspension>

    fun getSuspension(uuid: UUID): StateV2<ProfileSuspension?> = stateUsingSystemTime { now ->
        suspensions().find { it.user == uuid }
    }

    fun isSuspended(uuid: UUID): StateV2<Boolean> = getSuspension(uuid).map { it != null }
}

interface IRelationshipStates {

    fun isFriend(uuid: UUID): Boolean = uuid in getObservableFriendList()

    fun isBlocked(uuid: UUID): Boolean = uuid in getObservableBlockedList()

    fun getObservableFriendList(): ObservableList<UUID>

    fun getObservableBlockedList(): ObservableList<UUID>

    fun getObservableIncomingRequests(): ObservableList<UUID>

    fun getObservableOutgoingRequests(): ObservableList<UUID>

    fun addFriend(uuid: UUID, notification: Boolean): CompletableFuture<RelationshipResponse>

    fun removeFriend(uuid: UUID, notification: Boolean)

    fun acceptIncomingFriendRequest(uuid: UUID, notification: Boolean)

    fun declineIncomingFriendRequest(uuid: UUID, notification: Boolean)

    fun cancelOutgoingFriendRequest(uuid: UUID, notification: Boolean)

    fun getPendingRequestTime(uuid: UUID): Instant?

    fun blockPlayer(uuid: UUID, notification: Boolean): CompletableFuture<RelationshipResponse>

    fun unblockPlayer(uuid: UUID, notification: Boolean)

}

interface IRelationshipManager {

    fun friendAdded(uuid: UUID)

    fun friendRemoved(uuid: UUID)

    fun clearFriends()

    fun playerBlocked(uuid: UUID)

    fun playerUnblocked(uuid: UUID)

    fun clearBlocked()

    fun newIncomingFriendRequest(uuid: UUID)

    fun clearIncomingFriendRequest(uuid: UUID)

    fun clearAllIncomingRequests()

    fun newOutgoingFriendRequest(uuid: UUID)

    fun clearOutgoingFriendRequest(uuid: UUID)

    fun clearAllOutgoingRequests()
}

interface IMessengerStates {

    /**
     * Number of unread messages in this channel
     */
    fun getNumUnread(channelId: Long): StateV2<Int>

    /**
     * State whether there are any unread messages in this channel
     */
    fun getUnreadChannelState(channelId: Long): StateV2<Boolean>

    /**
     * State that records whether this message is unread or not.
     * Calling [State.set] will propagate update to CM
     */
    @Deprecated("Not used in protocol 9 or later")
    fun getUnreadMessageState(channelId: Long, messageId: Long): StateV2<Boolean>
    @Deprecated("Not used in protocol 9 or later")
    fun getUnreadMessageState(message: ClientMessage): StateV2<Boolean> = getUnreadMessageState(message.channel.id, message.id)
    @Deprecated("Not used in protocol 9 or later")
    fun getUnreadMessageState(message: Message): StateV2<Boolean> = getUnreadMessageState(message.channelId, message.id)

    /**
     * State of the last read message in a channel
     */
    fun getLastReadMessageId(channelId: Long): StateV2<Long?>

    /**
     * Sets the message as the last read message in the channel
     */
    fun setLastReadMessage(message: Message) = setLastReadMessage(message.channelId, message.id)

    /**
     * Sets the message as the last read message in the channel
     */
    fun setLastReadMessage(message: ClientMessage) = setLastReadMessage(message.channel.id, message.id)

    /**
     * Sets the message id as the last read message in the channel
     */
    fun setLastReadMessage(channelId: Long, messageId: Long?)

    /**
     * Title of this channel.
     * If announcement -> "Announcements"
     * If DM -> Name of other person
     * If group -> Group name
     */
    fun getTitle(channelId: Long): StateV2<String>

    /**
     * State that records whether this channel is muted or not.
     * Calling [State.set] will propagate update to CM
     */
    fun getMuted(channelId: Long): MutableStateV2<Boolean>

    /**
     * State reflecting the most recent message sent in this channel
     */
    fun getLatestMessage(channelId: Long): StateV2<ClientMessage?>

    /**
     * A List State that has the initial value of all loaded message in this channel.
     * Messages sent by others or deleted will be reflected in this list.
     */
    fun getMessageListState(channelId: Long): ListState<ClientMessage>

    // Maybe make this one read only from the caller's point of view?
    fun getObservableMemberList(channelId: Long): ObservableList<UUID>


    /**
     * List of channels that the current user can access. New channels and deletions will be reflected
     */
    fun getObservableChannelList(): ObservableList<Channel>

    @Deprecated("Not used in protocol 9 or later")
    fun setUnreadState(channelId: Long, messageId: Long, unread: Boolean)
    @Deprecated("Not used in protocol 9 or later")
    fun setUnreadState(message: ClientMessage, unread: Boolean) = setUnreadState(message.channel.id, message.id, unread)
    @Deprecated("Not used in protocol 9 or later")
    fun setUnreadState(message: Message, unread: Boolean) = setUnreadState(message.channelId, message.id, unread)

    /**
     * Sets the title of a channel. Will throw an exception if this channel
     * is not a group DM
     */
    fun setTitle(channelId: Long, title: String)

    /**
     * Requests more messages for this channel from the connection manager
     */
    fun requestMoreMessages(channelId: Long, messageLimit: Int, beforeId: Long? = null): Boolean

    /**
     * Sends the given [message] to the given [channelId], optionally as a [replyTo] another message
     */
    fun sendMessage(channelId: Long, message: String, replyTo: Long? = null, callback: ((Optional<Packet>) -> Unit)? = null)

    /**
     * Edits the given [messageId]
     */
    fun editMessage(channelId: Long, messageId: Long, content: String, callback: (Boolean) -> Unit)
    fun editMessage(message: ClientMessage, content: String, callback: (Boolean) -> Unit) = editMessage(message.channel.id, message.id, content, callback)

    /**
     * Deletes [messageId] from the given [channelId] it is in
     */
    fun deleteMessage(channelId: Long, messageId: Long)

    /**
     * Deletes [message] from the channel it is in
     */
    fun deleteMessage(message: ClientMessage) = deleteMessage(message.channel.id, message.id)

    /**
     * Deletes [message] from the channel it is in
     */
    fun deleteMessage(message: Message) = deleteMessage(message.channelId, message.id)

    fun leaveGroup(channelId: Long)

    fun removeUser(channelId: Long, user: UUID)

    fun createGroup(members: Set<UUID>, groupName: String): CompletableFuture<Channel>

    fun addMembers(channelId: Long, members: Set<UUID>)

    fun onChannelStateChange(callback: (Channel) -> Unit)

    /**
     * Registers a listener that is activated when the state is reset.
     * This is called before states are cleared so that an implementation
     * can store its state before it is cleared
     */
    fun registerResetListener(callback: () -> Unit)

    // FIXME these could probably be cleaned up, though their behavior wrt announcement channels doesn't match existing
    //  methods, so not entirely sure
    fun getMessagesRaw(channelId: Long): Map<Long, Message>?
    fun retrieveMessageHistoryRaw(channelId: Long, before: Long? = null, after: Long? = null, messageLimit: Int = 50, callback: ((Optional<Packet>) -> Unit)? = null)
}

// For callbacks from CM. Will update associated states
interface IMessengerManager {

    /**
     * Called when the CM confirms a delete request we've made or a message from another user
     * is deleted
     */
    fun messageDeleted(message: Message)

    /**
     * Called when the CM receives a new message in this channel
     */
    fun messageReceived(channel: Channel, message: Message)

    /**
     * Called when a message is unread or read
     */
    @Deprecated("Not used in protocol 9 or later")
    fun messageReadStateUpdated(message: Message, read: Boolean)

    /**
     * Called when some property about this channel was updated (such as title or members)
     */
    fun channelUpdated(channel: Channel)

    /**
     * Called when a new channel has been created
     */
    fun newChannel(channel: Channel)

    /**
     * Called when this channel has been deleted or the user was removed from it
     */
    fun channelDeleted(channel: Channel)

    /**
     * Called when the [gg.essential.network.connectionmanager.chat.ChatManager] resets its state and clear any cache data
     * that could become duplicated or outdated.
     */
    fun reset()

}

interface IStatusStates {

    fun getActivityState(uuid: UUID): StateV2<PlayerActivity>

    fun getActivity(uuid: UUID): PlayerActivity

    fun joinSession(uuid: UUID): Boolean
}

interface IStatusManager {

    fun refreshActivity(uuid: UUID)

}

sealed class PlayerActivity {

    data class Offline(val lastOnline: Long?) : PlayerActivity()

    object Online : PlayerActivity()

    data class OnlineWithDescription(val description: String) : PlayerActivity()

    data class SPSSession(val host: UUID, val invited: Boolean) : PlayerActivity()

    data class Multiplayer(val serverAddress: String) : PlayerActivity()

    fun isJoinable(): Boolean {
        return this is Multiplayer || (this is SPSSession && invited)
    }

}
