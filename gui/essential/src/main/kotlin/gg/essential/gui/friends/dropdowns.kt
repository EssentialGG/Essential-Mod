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
package gg.essential.gui.friends

import com.sparkuniverse.toolbox.chat.enums.ChannelType
import com.sparkuniverse.toolbox.chat.model.Channel
import gg.essential.elementa.components.Window
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.ContextOptionMenu
import gg.essential.gui.elementa.state.v2.filter
import gg.essential.gui.elementa.state.v2.mapList
import gg.essential.gui.friends.message.SocialMenuActions
import gg.essential.gui.friends.message.SocialMenuState
import gg.essential.gui.friends.modals.ConfirmGroupLeaveModal
import gg.essential.gui.friends.modals.RenameGroupModal
import gg.essential.gui.friends.modals.createAddFriendsToGroupModal
import gg.essential.gui.friends.previews.ChannelPreview
import gg.essential.gui.friends.state.IMessengerStates
import gg.essential.gui.util.toStateV2List
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.ServerType
import gg.essential.util.USession
import gg.essential.util.UuidNameLookup
import gg.essential.util.getOtherUser
import gg.essential.util.isAnnouncement
import java.util.UUID

fun showManagementDropdown(
    window: Window,
    socialMenuState: SocialMenuState,
    socialMenuActions: SocialMenuActions,
    preview: ChannelPreview,
    position: ContextOptionMenu.Position,
    extraOptions: List<ContextOptionMenu.Item>,
    onClose: () -> Unit
) {
    val otherUser = preview.otherUser
    when {
        otherUser != null -> {
            showUserDropdown(window, socialMenuState, socialMenuActions, otherUser, position, extraOptions.toMutableList().apply {
                addMarkMessagesReadOption(socialMenuState.messages, preview.channel.id, this)
            }, onClose)
        }
        !preview.channel.isAnnouncement() -> showGroupDropdown(window, socialMenuState, preview.channel, position, extraOptions, onClose)
        else -> onClose.invoke()
    }
}

private fun addMarkMessagesReadOption(
    messages: IMessengerStates,
    channelId: Long,
    options: MutableList<ContextOptionMenu.Item>,
) {
    if (messages.getUnreadChannelState(channelId).getUntracked()) {
        options.add(ContextOptionMenu.Option("Mark as Read", image = EssentialPalette.MARK_UNREAD_10X7) {
            if (platform.cmConnection.usingProtocol >= 9) {
                val latestMessage = messages.getLatestMessage(channelId).getUntracked() ?: return@Option
                messages.setLastReadMessage(latestMessage)
            } else {
                messages.getMessageListState(channelId).getUntracked().forEach {
                    messages.setUnreadState(it, false)
                }
            }
        })
    }
}

fun showUserDropdown(
    window: Window,
    socialMenuState: SocialMenuState,
    socialMenuActions: SocialMenuActions,
    user: UUID,
    position: ContextOptionMenu.Position,
    extraOptions: List<ContextOptionMenu.Item>,
    onClose: () -> Unit
) {
    val options = extraOptions.toMutableList()

    val joinPlayerOption = ContextOptionMenu.Option(
        "Join Game",
        // New default is text, so remove entirely when removing feature flag
        textColor = EssentialPalette.TEXT,
        hoveredColor = EssentialPalette.MESSAGE_SENT,
        // New default is black, so remove entirely when removing feature flag
        shadowColor = EssentialPalette.BLACK,
        image = EssentialPalette.JOIN_ARROW_5X,
    ) {
        socialMenuActions.joinSessionWithConfirmation(user)
    }
    val invitePlayerOption = ContextOptionMenu.Option(
        "Invite to Game",
        // New default is text, so remove entirely when removing feature flag
        textColor = EssentialPalette.TEXT,
        hoveredColor = EssentialPalette.MESSAGE_SENT,
        // New default is black, so remove entirely when removing feature flag
        shadowColor = EssentialPalette.BLACK,
        image = EssentialPalette.ENVELOPE_9X7,
    ) {
        socialMenuActions.invitePlayers(setOf(user), UuidNameLookup.nameState(user).getUntracked())
    }

    val topmostOptions: MutableList<ContextOptionMenu.Item> = mutableListOf()

    if (socialMenuState.activity.getActivity(user).isJoinable()) {
        topmostOptions.add(joinPlayerOption)
    }
    if (ServerType.current()?.supportsInvites == true) {
        topmostOptions.add(invitePlayerOption)
    }

    if (topmostOptions.isNotEmpty()) {
        options.add(0, ContextOptionMenu.Divider)

        for (optionItem in topmostOptions) {
            options.add(0, optionItem)
        }
    }

    // Don't add a divider below is we haven't added anything above here
    var addedDivider = extraOptions.isEmpty()

    if (socialMenuState.tab.getUntracked() == Tab.FRIENDS) {
        options.add(ContextOptionMenu.Option("Send Message", image = EssentialPalette.MESSAGE_10X6) {
            socialMenuActions.openMessageScreen(user)
        })
        // We always add this divider as it's below the option
        options.add(ContextOptionMenu.Divider)
        addedDivider = true
    } else {
        val channel = socialMenuState.messages.getObservableChannelList().firstOrNull {
            it.getOtherUser() == user
        }
        if (channel != null) {
            val muted = socialMenuState.messages.getMuted(channel.id)

            if (!addedDivider) {
                options.add(ContextOptionMenu.Divider)
                addedDivider = true
            }
            options.add(ContextOptionMenu.Option(
                {
                    if (muted()) {
                        "Unmute Friend"
                    } else {
                        "Mute Friend"
                    }
                },
                image = {
                    if (muted()) {
                        EssentialPalette.UNMUTE_8X9
                    } else {
                        EssentialPalette.MUTE_8X9
                    }
                },
            ) {
                muted.set { !it }
            })
        }

    }
    if (!addedDivider) {
        options.add(ContextOptionMenu.Divider)
    }

    val blocked = socialMenuState.relationships.isBlocked(user)
    val friend = socialMenuState.relationships.isFriend(user)
    val isSuspended = socialMenuState.isSuspended(user).getUntracked()

    if (!blocked) {
        if (friend || !isSuspended) {
            options.add(ContextOptionMenu.Option(
                if (friend) {
                    "Remove Friend"
                } else {
                    "Add Friend"
                },
                image = if (friend) EssentialPalette.REMOVE_FRIEND_10X5 else EssentialPalette.INVITE_10X6,
            ) {
                socialMenuActions.addOrRemoveFriend(user)
            })
        }
    }

    options.add(ContextOptionMenu.Option(
        if (blocked) {
            "Unblock"
        } else {
            "Block"
        },
        image = EssentialPalette.BLOCK_10X7,
        hoveredColor = EssentialPalette.TEXT_WARNING
    ) {
        socialMenuActions.blockOrUnblock(user)
    })
    ContextOptionMenu.create(position, window, *options.toTypedArray(), onClose = onClose)
}

private fun showGroupDropdown(
    window: Window,
    socialMenuState: SocialMenuState,
    channel: Channel,
    position: ContextOptionMenu.Position,
    extraOptions: List<ContextOptionMenu.Item>,
    onClose: () -> Unit
) {
    val options = extraOptions.toMutableList()

    addMarkMessagesReadOption(socialMenuState.messages, channel.id, options)

    // Left commented if we re-add in the future
    /* if (ServerType.current()?.supportsInvites == true) {
        options.add(
            ContextOptionMenu.Option(
                "Invite Group",
                // New default is text, so remove entirely when removing feature flag
                textColor = EssentialPalette.TEXT,
                hoveredColor = EssentialPalette.MESSAGE_SENT,
                // New default is black, so remove entirely when removing feature flag
                shadowColor = EssentialPalette.BLACK,
                image = EssentialPalette.INVITE_10X6,
            ) {
                handleInvitePlayers(channel.members, channel.name)
            }
        )

        options.add(ContextOptionMenu.Divider)
    } */

    val mutedState = socialMenuState.messages.getMuted(channel.id)
    if (channel.type == ChannelType.GROUP_DIRECT_MESSAGE && channel.createdInfo.by == USession.activeNow().uuid) {
        options.add(ContextOptionMenu.Option(
            "Invite Friends",
            image = EssentialPalette.MARK_UNREAD_10X7
        ) {
            // We don't want to show anyone currently in the group here
            val potentialFriends = socialMenuState.relationships.getObservableFriendList()
                    .toStateV2List()
                    .mapList { list ->
                        list.filter { !channel.members.contains(it) && !socialMenuState.isSuspended(it)() }
                    }


            platform.pushModal { manager ->
                createAddFriendsToGroupModal(manager, potentialFriends).onPrimaryAction { users ->
                    socialMenuState.messages.addMembers(channel.id, users)
                }
            }
        })
        options.add(ContextOptionMenu.Divider)
        options.add(ContextOptionMenu.Option(
            "Rename Group",
            image = EssentialPalette.PENCIL_7x7
        ) {
            platform.pushModal { manager ->
                RenameGroupModal(manager, channel.name).onPrimaryActionWithValue { it ->
                    socialMenuState.messages.setTitle(channel.id, it)
                }
            }
        })
        options.add(ContextOptionMenu.Divider)
    }

    options.add(ContextOptionMenu.Option({
        if (mutedState()) {
            "Unmute Group"
        } else {
            "Mute Group"
        }
    }, image = {
        if (mutedState()) {
            EssentialPalette.UNMUTE_8X9
        } else {
            EssentialPalette.MUTE_8X9
        }
    }) {
        mutedState.set { !it } // Will be automatically applied properly
    })

    options.add(ContextOptionMenu.Option("Leave Group", image = EssentialPalette.LEAVE_10X7) {
        platform.pushModal { manager ->
            ConfirmGroupLeaveModal(manager).onPrimaryAction {
                socialMenuState.messages.leaveGroup(channel.id)
            }
        }
    })

    ContextOptionMenu.create(position, window, *options.toTypedArray(), onClose = onClose)
}
