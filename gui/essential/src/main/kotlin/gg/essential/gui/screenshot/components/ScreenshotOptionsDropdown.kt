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

import gg.essential.elementa.components.Window
import gg.essential.elementa.dsl.*
import gg.essential.elementa.events.UIClickEvent
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.ContextOptionMenu
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.overlay.launchModalFlow
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.RemoteScreenshot
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.copyScreenshotToClipboard
import gg.essential.gui.screenshot.modal.deleteScreenshot
import gg.essential.gui.screenshot.openScreenshotPropertiesModal
import gg.essential.network.connectionmanager.media.IScreenshotManager
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform

class ScreenshotOptionsDropdown(
    private val screenshotManager: IScreenshotManager,
    private val stateManager: ScreenshotStateManager,
    private val view: MutableState<View>,
) {

    val menuDialogOwner = mutableStateOf<ScreenshotId?>(null)

    /**
     * Opens a right click options menu
     */
    fun handleRightClick(
        id: ScreenshotId,
        event: UIClickEvent,
        errored: Boolean,
    ) {
        if (menuDialogOwner.get() != null) {
            return
        }
        USound.playButtonPress()
        menuDialogOwner.set { id }
        val posX = event.absoluteX
        val posY = event.absoluteY
        val options = mutableListOf<ContextOptionMenu.Item>()

        val metadata = stateManager.metadata(id).getUntracked()

        if (!errored) {
            options.add(ContextOptionMenu.Option("Edit", image = EssentialPalette.EDIT_SHORT_10X7) {
                view.set { View.Edit(id, it) }
            })

            options.add(ContextOptionMenu.Divider)

            options.add(ContextOptionMenu.Option("Send to Friends", image = EssentialPalette.SOCIAL_10X) {
                launchModalFlow(platform.createModalManager()) {
                    shareScreenshotModal(id)
                }
            })

            options.add(ContextOptionMenu.Option("Copy Picture", image = EssentialPalette.COPY_10X7) {
                copyScreenshotToClipboard(id)
            })

            if (platform.cmConnection.isOpen) {
                options.add(ContextOptionMenu.Option("Copy Link", image = EssentialPalette.LINK_10X7) {
                    when (id) {
                        is LocalScreenshot -> screenshotManager.uploadAndCopyLinkToClipboard(id.path)
                        is RemoteScreenshot -> screenshotManager.copyLinkToClipboard(id.media)
                    }
                })
            }

            options.add(ContextOptionMenu.Divider)
        }

        options.add(ContextOptionMenu.Option("Properties", image = EssentialPalette.PROPERTIES_10X5) {
            openScreenshotPropertiesModal(ScreenshotProperties(id, metadata))
        })

        if (id is LocalScreenshot) {
            options.add(ContextOptionMenu.Option("File Location", image = EssentialPalette.FOLDER_10X7) {
                openFileInDirectory(id.path)
            })
        }

        options.add(ContextOptionMenu.Divider)

        val mediaId = metadata?.ownedMediaId
        if (mediaId != null) {
            options.add(
                ContextOptionMenu.Option(
                    "Remove Upload",
                    image = EssentialPalette.CANCEL_5X,
                    hoveredColor = EssentialPalette.TEXT_WARNING,
                    // New default is text, so remove entirely when removing feature flag
                    hoveredShadowColor = EssentialPalette.BLACK,
                ) {
                    launchModalFlow(platform.createModalManager()) {
                        deleteScreenshot(screenshotManager, id, uploadedOnly = true)
                    }
                })
        }

        options.add(
            ContextOptionMenu.Option(
                "Delete",
                image = EssentialPalette.TRASH_9X,
                hoveredColor = EssentialPalette.TEXT_WARNING,
                // New default is text, so remove entirely when removing feature flag
                hoveredShadowColor = EssentialPalette.BLACK,
            ) {
                launchModalFlow(platform.createModalManager()) {
                    deleteScreenshot(screenshotManager, id)
                }
            })
        val menu = ContextOptionMenu(
            posX,
            posY,
            *options.toTypedArray(),
        ) childOf Window.of(event.target)
        menu.init()

        menu.onClose {
            menuDialogOwner.set(null)
        }
    }
}
