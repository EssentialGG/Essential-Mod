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
package gg.essential.gui.friends.modals

import com.sparkuniverse.toolbox.chat.model.Channel
import gg.essential.elementa.components.Window
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.modal.CancelableInputModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.friends.state.SocialStates
import gg.essential.gui.modals.select.offlinePlayers
import gg.essential.gui.modals.select.onlinePlayers
import gg.essential.gui.modals.select.selectModal
import gg.essential.gui.overlay.ModalFlow
import kotlinx.coroutines.future.asDeferred
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun ModalFlow.makeGroupModal(socialStates: SocialStates): Channel? {
    while (true) {
        val friends = selectFriendsForGroupModal() ?: return null
        val name = enterGroupNameModal() ?: continue

        val channel = socialStates.messages.createGroup(friends, name)
            .exceptionally { null }.asDeferred().await()

        // Intentionally delay one frame so that the channel preview callback can fire first
        suspendCoroutine { Window.enqueueRenderOperation { it.resume(null) } }

        return channel
    }
}

suspend fun ModalFlow.enterGroupNameModal(): String? {
    return awaitModal { continuation ->
        CancelableInputModal(modalManager, "", "", maxLength = 24).configure {
            titleText = "Make Group"
            contentText = "Enter a name for your group."
            primaryButtonText = "Make Group"
            titleTextColor = EssentialPalette.TEXT_HIGHLIGHT

            cancelButtonText = "Back"

            mapInputToEnabled { it.isNotBlank() }
            onPrimaryActionWithValue { result -> replaceWith(continuation.resumeImmediately(result)) }
            onCancel { button -> if (button) replaceWith(continuation.resumeImmediately(null)) }
        }
    }
}

suspend fun ModalFlow.selectFriendsForGroupModal(): Set<UUID>? {
    return selectModal("Select friends to make group", "SelectFriendsForGroup") {
        requiresSelection = true
        requiresButtonPress = false

        onlinePlayers()
        offlinePlayers()

        modalSettings {
            primaryButtonText = "Continue"
            cancelButtonText = "Cancel"
        }
    }
}
