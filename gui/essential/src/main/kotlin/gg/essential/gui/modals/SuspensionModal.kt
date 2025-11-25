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
package gg.essential.gui.modals

import gg.essential.config.EssentialConfig
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.OutlineButtonStyle
import gg.essential.gui.common.SequenceAnimatedUIImage
import gg.essential.gui.common.modal.EssentialModal2
import gg.essential.gui.common.textStyle
import gg.essential.gui.elementa.state.v2.withSystemTime
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedHeight
import gg.essential.gui.layoutdsl.childBasedWidth
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.image
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.layoutdsl.wrappedText
import gg.essential.gui.overlay.ModalFlow
import gg.essential.gui.overlay.ModalManager
import gg.essential.network.connectionmanager.social.Suspension
import gg.essential.util.openInBrowser
import gg.essential.util.toShortString
import java.net.URI
import java.time.Instant
import java.util.concurrent.TimeUnit

class SuspensionModal(
    modalManager: ModalManager,
    private val suspension: Suspension,
    private val reasons: Map<String, String>,
    private val continuation: ModalFlow.ModalContinuation<Unit>
) : EssentialModal2(modalManager) {

    val title = if (suspension.isTOSSuspension) {
        if (suspension.isPermanent) {
            permanentTOSSuspensionText
        } else {
            temporaryTOSSuspensionText
        }
    } else {
        if (suspension.isPermanent) {
            permanentSuspensionText
        } else {
            temporarySuspensionText
        }
    }

    override fun LayoutScope.layoutTitle() {
        wrappedText(title, Modifier.color(EssentialPalette.RED).shadow(EssentialPalette.BLACK).width(200f), centered = true)
    }

    override fun LayoutScope.layoutBody() {
        if (suspension.isPermanent && suspension.isTOSSuspension) {
            return
        }

        column(Modifier.fillWidth(), Arrangement.spacedBy(13f)) {
            if (!suspension.isTOSSuspension) {
                wrappedText("Reason: ${reasons[suspension.reason]}", Modifier.color(EssentialPalette.TEXT_MID_GRAY).shadow(EssentialPalette.BLACK))
            }
            if (suspension.expiresAt != null) {
                timer(suspension.expiresAt)
            }
        }
    }

    override fun LayoutScope.layoutButtons() {
        row(Arrangement.spacedBy(8f)) {
            outlineButton(
                Modifier.width(91f).shadow(),
                action = {
                    openInBrowser(URI.create("https://essential.gg/wiki/banned-accounts"))
                }
            ) { style ->
                row(Arrangement.spacedBy(5f)) {
                    text("Help", Modifier.textStyle(style))
                    image(EssentialPalette.ARROW_UP_RIGHT_5X5, Modifier.textStyle(style))
                }
            }
            primaryButton("Okay", style = OutlineButtonStyle.BLUE) {
                close()
            }
        }
    }

    override fun onClose() {
        super.onClose()
        if (suspension.isPermanent) {
            EssentialConfig.acknowledgedPermanentSuspension.set(true)
        }
        modalManager.queueModal(continuation.resumeImmediately(Unit))
    }

    private fun LayoutScope.timer(expiresAt: Instant) {
        box(Modifier.childBasedHeight(4f).childBasedWidth(12f).color(EssentialPalette.COMPONENT_BACKGROUND).shadow(EssentialPalette.BLACK)) {
            column {
                spacer(height = 1f)
                row(Modifier.childBasedWidth().alignHorizontal(Alignment.Center(true)), Arrangement.spacedBy(5f)) {
                    SequenceAnimatedUIImage(
                        "/assets/essential/textures/studio/clock_", ".png",
                        4,
                        1000,
                        TimeUnit.MILLISECONDS,
                    )(Modifier.color(EssentialPalette.RED).shadow(EssentialPalette.BLACK))
                    text(
                        { withSystemTime { it.until(expiresAt).toShortString() } },
                        Modifier.color(EssentialPalette.TEXT_MID_GRAY).shadow(EssentialPalette.BLACK)
                    )
                }
            }
        }
    }

    companion object {

        private val permanentSuspensionText = """
            You broke Essential’s community
            guidelines and have been permanently
            suspended from Essential
        """.trimIndent()

        private val temporarySuspensionText = """
            You broke Essential's community
            guidelines and have been temporarily
            suspended from social features
        """.trimIndent()

        private val permanentTOSSuspensionText = """
            You broke Essential’s terms of
            service and have been permanently
            suspended from Essential
        """.trimIndent()

        private val temporaryTOSSuspensionText = """
            You broke Essential’s terms of
            service and have been temporarily
            suspended from Essential
        """.trimIndent()
    }
}