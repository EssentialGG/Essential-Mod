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

import gg.essential.elementa.utils.ObservableAddEvent
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.network.connectionmanager.ConnectionManager
import gg.essential.network.connectionmanager.social.ProfileSuspension
import gg.essential.util.UUIDUtil
import gg.essential.util.getOtherUser
import gg.essential.util.thenAcceptOnMainThread
import java.util.*

class SocialStateManager(connectionManager: ConnectionManager) : SocialStates {

    private val messageStateImpl = MessengerStateManagerImpl(connectionManager.chatManager)
    private val relationshipStateImpl = RelationshipStateManagerImpl(connectionManager.relationshipManager)
    private val statusStateManagerImpl = StatusStateManagerImpl(connectionManager.profileManager, connectionManager.spsManager)

    val messengerStates: IMessengerStates = messageStateImpl

    val relationshipStates: IRelationshipStates = relationshipStateImpl

    val statusStates: IStatusStates = statusStateManagerImpl

    override val relationships: IRelationshipStates
        get() = relationshipStates
    override val messages: IMessengerStates
        get() = messengerStates
    override val activity: IStatusStates
        get() = statusStates

    override val suspensions: ListState<ProfileSuspension> = connectionManager.profileManager.suspensions

    init {
        val observableFriendList = relationshipStates.getObservableFriendList()
        val observableChannelList = messengerStates.getObservableChannelList()
        val directMessages = observableChannelList.mapNotNull {
            it.getOtherUser()
        }

        observableFriendList.filter {
            it !in directMessages
        }.forEach {
            UUIDUtil.getName(it).thenAcceptOnMainThread { name ->
                connectionManager.chatManager.createDM(it, name, null) // Callback will trigger a change in observableChannelList
            }
        }

        observableFriendList.addObserver { _, event ->
            if (event is ObservableAddEvent<*>) {
                val newFriend = event.element.value as UUID
                UUIDUtil.getName(newFriend).thenAcceptOnMainThread { name ->
                    connectionManager.chatManager.createDM(newFriend, name, null) // Callback will trigger a change in observableChannelList
                }
            }
        }
    }
}