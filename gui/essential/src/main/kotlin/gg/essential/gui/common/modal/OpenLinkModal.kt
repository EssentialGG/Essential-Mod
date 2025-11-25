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
package gg.essential.gui.common.modal

import gg.essential.config.EssentialConfig
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.OutlineButtonStyle
import gg.essential.gui.common.outlineButton
import gg.essential.gui.common.textStyle
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.hoverTooltip
import gg.essential.gui.layoutdsl.image
import gg.essential.gui.layoutdsl.onLeftClick
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.overlay.ModalManager
import gg.essential.universal.USound
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.openInBrowser
import java.awt.Color
import java.net.URI

class OpenLinkModal(
    modalManager: ModalManager,
    private val uri: URI,
) : EssentialModal2(modalManager) {
    override fun LayoutScope.layoutBody() {
        // FIXME: We could use `wrappedText` (with the placeholder) here, but there's currently no way to
        //        specify each `row` as `fillWidth`.
        //        I haven't figured out a nice API for this, and would like to consider it a bit more
        //        without rushing something together for this.
        column(Modifier.fillWidth(), Arrangement.spacedBy(4f)) {
            text("You are about to visit:", Modifier.color(EssentialPalette.TEXT).shadow(Color.BLACK))
            text(
                uri.host,
                Modifier
                    .color(EssentialPalette.TEXT_HIGHLIGHT)
                    .shadow(Color.BLACK)
                    .hoverScope()
                    .hoverTooltip(uri.toString(), wrapAtWidth = 300f),
                truncateIfTooSmall = true,
                showTooltipForTruncatedText = false,
            )
        }
    }

    override fun LayoutScope.layoutButtons() {
        row(Arrangement.spacedBy(8f)) {
            cancelButton("Cancel")

            outlineButton(
                Modifier.width(91f).shadow().onLeftClick {
                    USound.playButtonPress()

                    openInBrowser(uri)
                    close()
                },
                style = OutlineButtonStyle.BLUE,
            ) { style ->
                row(Arrangement.spacedBy(5f)) {
                    text("Open", Modifier.textStyle(style))
                    image(EssentialPalette.ARROW_UP_RIGHT_5X5, Modifier.textStyle(style))
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun openUrl(uri: URI) {
            val isTrusted = uri.host in platform.trustedHosts

            if (isTrusted) {
                openInBrowser(uri)
            } else if (EssentialConfig.linkWarning) {
                platform.pushModal { OpenLinkModal(it, uri) }
            }
        }
    }
}
