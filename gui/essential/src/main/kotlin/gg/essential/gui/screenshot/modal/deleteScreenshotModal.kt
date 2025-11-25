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
package gg.essential.gui.screenshot.modal

import gg.essential.api.gui.Slot
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixel
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.modal.DangerConfirmationEssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.common.shadow.ShadowIcon
import gg.essential.gui.notification.Notifications
import gg.essential.gui.overlay.ModalFlow
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.network.connectionmanager.media.IScreenshotManager
import java.awt.Color


suspend fun ModalFlow.deleteScreenshot(
    screenshotManager: IScreenshotManager,
    id: ScreenshotId,
    uploadedOnly: Boolean = false,
) {
    awaitModal { cont ->
        val modal = DeleteScreenshotConfirmationModal(modalManager, id.name, uploadedOnly)
        modal.onPrimaryAction {
            modal.replaceWith(cont.resumeImmediately(Unit))
        }
        modal
    }

    val metadata = screenshotManager.screenshots.getUntracked().find { it.id == id }?.metadata

    if (id is LocalScreenshot && !uploadedOnly) {
        screenshotManager.deleteFile(id.path)
    }

    val mediaId = metadata?.ownedMediaId
    if (mediaId != null) {
        screenshotManager.deleteMedia(mediaId, (id as? LocalScreenshot)?.path)
    }

    if (uploadedOnly) {
        // FIXME should probably be done by the caller and only if the packet succeeds
        Notifications.push("Upload has been removed.", "") {
            val icon =
                ShadowIcon(EssentialPalette.CHECKMARK_7X5, true)
                    .constrain { y = 1.pixels }
                    .rebindPrimaryColor(BasicState(Color.WHITE))
                    .rebindShadowColor(BasicState(EssentialPalette.MODAL_OUTLINE))
            val container = UIContainer()
                .constrain {
                    width = ChildBasedSizeConstraint() + 1.pixel
                    height = ChildBasedSizeConstraint() + 2.pixels
                }
                .addChild(icon)
            withCustomComponent(Slot.PREVIEW, container)
        }
    }
}

class DeleteScreenshotConfirmationModal(manager: ModalManager, name: String, uploadedOnly: Boolean) : DangerConfirmationEssentialModal(manager, "Delete", requiresButtonPress = false) {
    init {
        configure {
            contentText =
                if (uploadedOnly) {
                    "Are you sure you want to remove the upload of $name?\n" +
                            "This will invalidate all links to the image."
                } else {
                    "Are you sure you want to delete $name?"
                }
        }
    }
}
