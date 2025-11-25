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
package gg.essential.network.connectionmanager.relationship

import gg.essential.gui.EssentialPalette
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.notification.markdownBody
import gg.essential.gui.notification.warning
import gg.essential.network.connectionmanager.relationship.RelationshipErrorResponse.*
import gg.essential.universal.UI18n
import gg.essential.util.colored
import java.util.UUID

val RelationshipErrorResponse.message: String
    get() = UI18n.i18n("connectionmanager.friends." + name)

fun RelationshipErrorResponse.showToast(uuid: UUID, name: String) = when (this) {
    EXISTING_REQUEST_IS_PENDING ->
        Notifications.error("Friend request failed", "") {
            markdownBody("Friend request already sent to ${name.colored(EssentialPalette.TEXT_HIGHLIGHT)}.")
        }
    SENDER_TYPE_VERIFIED_SLOT_LIMIT ->
        Notifications.error(
            "Friend request failed",
            "Your friends list is full. Remove friends before adding new ones."
        )
    TARGET_TYPE_VERIFIED_SLOT_LIMIT ->
        Notifications.error("Friend request failed", "") {
            markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)}'s friends list is full.")
        }
    NO_PENDING_REQUEST_TO_VERIFY ->
        Notifications.error("Error", "No pending friend request to accept.")
    VERIFIED_RELATIONSHIP_ALREADY_EXISTS ->
        Notifications.error("Friend request failed", "") {
            markdownBody("You are already friends with ${name.colored(EssentialPalette.TEXT_HIGHLIGHT)}.")
        }
    NO_BLOCKED_RELATIONSHIP_TO_CONVERT ->
        Notifications.error("Error", "") {
            markdownBody("Failed to block ${name.colored(EssentialPalette.TEXT_HIGHLIGHT)}.")
        }
    SENDER_BLOCKED_TARGET ->
        Notifications.error("Friend request failed", "") {
            markdownBody("You have blocked ${name.colored(EssentialPalette.TEXT_HIGHLIGHT)}. " +
                    "Unblock them before sending them a friend request.")
        }
    TARGET_BLOCKED_SENDER ->
        Notifications.warning("Friend request declined", "") {
            markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} has blocked you.")
        }
    SENDER_TYPE_OUTGOING_SLOT_LIMIT ->
        Notifications.error(
            "Friend request failed",
            "You have too many pending friend requests. Remove some to make space for new ones."
        )
    TARGET_TYPE_INCOMING_SLOT_LIMIT ->
        Notifications.error("Friend request failed", "") {
            markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} has too many pending friend requests.")
        }
    TARGET_PRIVACY_SETTING_FRIEND_OF_FRIENDS ->
        Notifications.warning("Friend request declined", "") {
            markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} is only accepting friend requests from friends of friends.")
        }
    TARGET_PRIVACY_SETTING_NO_ONE ->
        Notifications.warning("Friend request declined", "") {
            markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} is not accepting any friend requests.")
        }
    TARGET_NOT_EXIST ->
        Notifications.error("Error", "") {
            markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} does not exist.")
        }
    USER_IS_PERMANENTLY_SUSPENDED ->
        Notifications.error("Friend request failed", "") {
            markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} is suspended.")
        }
    USER_IS_TEMPORARILY_SUSPENDED ->
        Notifications.error("Friend request failed", "") {
            markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} is temporarily suspended.")
        }
    else -> throw AssertionError() // FIXME: Workaround for compiler bug fixed in Kotlin 2.0
}
