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
package gg.essential.gui.friends.title

import gg.essential.elementa.components.UIContainer
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialCollapsibleSearchbar
import gg.essential.gui.common.modal.UsernameInputModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.friends.SocialMenu
import gg.essential.gui.friends.modal.MakeGroupModal
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.iconAndMarkdownBody
import gg.essential.network.connectionmanager.relationship.FriendRequestState
import gg.essential.network.connectionmanager.relationship.RelationshipErrorResponse
import gg.essential.network.connectionmanager.relationship.RelationshipResponse
import gg.essential.network.connectionmanager.relationship.message
import gg.essential.util.GuiUtil
import gg.essential.util.colored
import gg.essential.util.thenAcceptOnMainThread
import java.util.concurrent.CompletableFuture

abstract class TitleManagementActions(private val gui: SocialMenu) : UIContainer() {
    private val makeGroupModal = MakeGroupModal(gui)

    abstract val search: EssentialCollapsibleSearchbar

    protected fun addFriend() {
        GuiUtil.pushModal { manager ->
            UsernameInputModal(manager, "") { uuid, username, modal ->
                val future = gui.socialStateManager.relationshipStates.addFriend(uuid, false)
                consumeRelationshipFutureFromModal(
                    modal, future
                ) {
                    Notifications.push("", "") {
                        iconAndMarkdownBody(
                            EssentialPalette.ENVELOPE_9X7.create(),
                            "Friend request sent to ${username.colored(EssentialPalette.TEXT_HIGHLIGHT)}"
                        )
                    }
                }
            }.configure {
                primaryButtonText = "Add"
                titleText = "Add Friend"
                contentText = "Enter a Minecraft username\nto add them as a friend."
            }
        }
    }

    protected fun makeGroup() {
        GuiUtil.pushModal { makeGroupModal.create(it) }
    }

    protected fun blockPlayer() {
        GuiUtil.pushModal { manager ->
            UsernameInputModal(manager, "") { uuid, username, modal ->
                val future = gui.socialStateManager.relationshipStates.blockPlayer(uuid, false)
                consumeRelationshipFutureFromModal(
                    modal, future
                ) {
                    Notifications.push("", "") {
                        iconAndMarkdownBody(
                            EssentialPalette.BLOCK_7X7.create(),
                            "${username.colored(EssentialPalette.TEXT_HIGHLIGHT)} has been blocked"
                        )
                    }
                }
            }.configure {
                primaryButtonText = "Block"
                titleText = "Block Player"
                contentText = "Enter a Minecraft username\nto block them."
            }
        }
    }

    // Adapted from RelationshipStateManagerImpl consumeRelationshipFuture
    private fun consumeRelationshipFutureFromModal(
        modal: UsernameInputModal,
        future: CompletableFuture<RelationshipResponse>,
        onSuccess: () -> Unit
    ) {
        future.thenAcceptOnMainThread {
            modal.primaryButtonEnableStateOverride.set(true)
            when (it.friendRequestState) {
                FriendRequestState.SENT -> {
                    onSuccess()
                    modal.replaceWith(null)
                }

                FriendRequestState.ERROR_HANDLED, FriendRequestState.ERROR_UNHANDLED -> {
                    modal.errorOverride.set(
                        if (it.relationshipErrorResponse == RelationshipErrorResponse.TARGET_NOT_EXIST) {
                            "Not an Essential user"
                        } else {
                            it.message
                        }
                    )
                }
            }
        }.whenComplete { _, _ ->
            // Always re-enable the button when we complete the future
            modal.primaryButtonEnableStateOverride.set(true)
        }
    }

}
