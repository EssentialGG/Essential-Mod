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
package gg.essential.gui.screenshot.components

import com.sparkuniverse.toolbox.chat.model.Channel
import gg.essential.gui.modals.ensurePrerequisites
import gg.essential.gui.modals.select.friendsAndGroups
import gg.essential.gui.modals.select.selectModal
import gg.essential.gui.overlay.ModalFlow
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.RemoteScreenshot
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import kotlinx.coroutines.future.await

suspend fun ModalFlow.shareScreenshotModal(
    screenshot: ScreenshotId,
    metadata: ClientScreenshotMetadata? = null,
) {
    ensurePrerequisites(social = true)

    val selectedChannels = selectScreenshotShareTargetsModal()?.toList() ?: return
    val screenshotManager = platform.screenshotManager
    when (screenshot) {
        is LocalScreenshot -> if (metadata != null) {
            screenshotManager.uploadAndShareLinkToChannels(selectedChannels, screenshot.path, metadata).await()
        } else {
            screenshotManager.uploadAndShareLinkToChannels(selectedChannels, screenshot.path).await()
        }

        is RemoteScreenshot -> screenshotManager.shareLinkToChannels(selectedChannels, screenshot.media)
    }
}

suspend fun ModalFlow.selectScreenshotShareTargetsModal(): Set<Channel>? {
    return selectModal("Share picture", "SelectScreenshotShare") {
        friendsAndGroups()

        modalSettings {
            primaryButtonText = "Share"
            cancelButtonText = "Cancel"
        }

        selectTooltip = "Add"
        deselectTooltip = "Remove"

        requiresSelection = true
        requiresButtonPress = false
    }
}

