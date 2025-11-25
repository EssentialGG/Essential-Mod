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

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.ContextOptionMenu
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.or
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.overlay.launchModalFlow
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.RemoteScreenshot
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.copyScreenshotToClipboard
import gg.essential.gui.util.hoveredState
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.vigilance.utils.onLeftClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * The share button inside the ScreenshotBrowser that contains a dropdown on click
 */
class ShareButton(
    private val stateManager: ScreenshotStateManager,
    private val focusing: State<ScreenshotId?>,
    private val editComponent: EditViewComponent? = null,
) : UIContainer() {


    private val shareHovered = hoveredState()
    private val shouldMenuExist = BasicState(false)

    private val image by IconButton(EssentialPalette.UPLOAD_9X, tooltipText = "Share")
        .rebindIconColor(EssentialPalette.getTextColor(shareHovered or shouldMenuExist))
        .setColor(EssentialPalette.getButtonColor(shareHovered or shouldMenuExist).toConstraint()) childOf this

    init {
        constrain {
            width = ChildBasedSizeConstraint()
            height = ChildBasedSizeConstraint()
        }
        image.onLeftClick {
            shouldMenuExist.set { !it }
        }
        shouldMenuExist.onSetValue {
            if (it) {
                openMenu()
            }
        }
    }

    private fun openMenu() {
        val options = mutableListOf<ContextOptionMenu.Item>()
        val screenshotManager = platform.screenshotManager

        if (platform.cmConnection.isOpen) {
            options.add(ContextOptionMenu.Option("Send to Friends", image = EssentialPalette.SOCIAL_10X) {
                checkForUnsavedEditsAndRun(
                    withUnsavedEdits = { file, metadata ->
                        val future = CompletableFuture<Unit>()

                        launchModalFlow(platform.createModalManager()) {
                            try {
                                shareScreenshotModal(LocalScreenshot(file.toPath()), metadata)
                            } finally {
                                future.complete(Unit)
                            }
                        }

                        return@checkForUnsavedEditsAndRun future
                    },
                    withoutUnsavedEdits = { id ->
                        launchModalFlow(platform.createModalManager()) { shareScreenshotModal(id) }
                    }
                )
            })
        }

        options.add(ContextOptionMenu.Option("Copy Picture", image = EssentialPalette.COPY_10X7) {

            if (editComponent != null && editComponent.hasEdits()) {
                editComponent.exportEditImageToTempFile()?.thenAcceptAsync({
                    copyScreenshotToClipboard(it.toPath())

                    // Cleanup temp file
                    it.delete()
                }, Dispatchers.Client.asExecutor()) ?: Notifications.error("Picture export failed", "")
            } else {
                val id = focusing.getUntracked() ?: return@Option
                copyScreenshotToClipboard(id)
            }
        })
        if (platform.cmConnection.isOpen) {
            options.add(ContextOptionMenu.Option("Copy Link", image = EssentialPalette.LINK_10X7) {
                checkForUnsavedEditsAndRun(
                    withUnsavedEdits = { file, metadata -> screenshotManager.uploadAndCopyLinkToClipboard(file.toPath(), metadata) },
                    withoutUnsavedEdits = { id ->
                        when (id) {
                            is LocalScreenshot -> screenshotManager.uploadAndCopyLinkToClipboard(id.path)
                            is RemoteScreenshot -> screenshotManager.copyLinkToClipboard(id.media)
                        }
                    }
                )
            })
        }

        val menu = ContextOptionMenu(
            0f,
            0f,
            *options.toTypedArray(),
        )

        menu.onClose {
            shouldMenuExist.set(false)
        }

        Window.enqueueRenderOperation {
            // Align to left when in edit mode
            if (editComponent != null) {
                val position = ContextOptionMenu.Position(this, true)
                menu.reposition(position.xConstraint, position.yConstraint)
            } else {
                menu.reposition(
                    CopyConstraintFloat() boundTo this@ShareButton,
                    SiblingConstraint(2f) boundTo this@ShareButton
                )
            }

            menu childOf Window.of(this@ShareButton)
            menu.init()
        }
    }

    fun setDimension(dimension: IconButton.Dimension): ShareButton {
        (image as IconButton).setDimension(dimension)
        return this
    }

    private fun checkForUnsavedEditsAndRun(
        withUnsavedEdits: (File, ClientScreenshotMetadata) -> CompletableFuture<*>,
        withoutUnsavedEdits: (ScreenshotId) -> Unit
    ) {
        val focus = focusing.getUntracked()
        if (focus != null) {
            if (editComponent != null && editComponent.hasEdits()) {
                editComponent.exportEditImageToTempFile()?.thenAcceptAsync({
                    val checksum = DigestUtils.md5Hex(it.readBytes())
                    val metadata = stateManager.metadata(focus).getUntracked()?.cloneWithNewChecksum(checksum)
                        ?: ClientScreenshotMetadata.createUnknown(focus, checksum)

                    withUnsavedEdits(it, metadata).thenAcceptAsync({ _ ->
                        // Cleanup the temp file
                        it.delete()
                    }, Dispatchers.Client.asExecutor())


                }, Dispatchers.Client.asExecutor()) ?: Notifications.error("Picture export failed", "")

            } else {
                withoutUnsavedEdits(focus)
            }
        }
    }
}
