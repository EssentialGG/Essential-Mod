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
package gg.essential.gui.serverdiscovery

import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.OutlineButtonStyle
import gg.essential.gui.common.modal.EssentialModal2
import gg.essential.gui.common.textStyle
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.image
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.tag
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.layoutdsl.wrappedText
import gg.essential.gui.overlay.ModalManager
import gg.essential.util.openInBrowser
import java.net.URI

class VersionDownloadModal(modalManager: ModalManager, private val recommendedVersion: String) : EssentialModal2(modalManager) {
    override fun LayoutScope.layoutTitle() {
        wrappedText(
            "Your Minecraft version is not\nsupported. We recommend\nMinecraft $recommendedVersion to join.",
            Modifier.color(EssentialPalette.MODAL_TITLE_BLUE),
            centered = true
        )
    }

    override fun LayoutScope.layoutBody() {
        description("Easily install the required version\nusing the Essential Installer.")
    }

    override fun LayoutScope.layoutButtons() {
        row(Arrangement.spacedBy(8f)) {
            cancelButton("Cancel")
            outlineButton(
                Modifier.width(91f).tag(PrimaryAction),
                style = OutlineButtonStyle.BLUE,
                action = {
                    openInBrowser(URI.create("https://essential.gg/download"))
                }
            ) { style ->
                row(Arrangement.spacedBy(5f)) {
                    text("Download", Modifier.textStyle(style))
                    image(EssentialPalette.ARROW_UP_RIGHT_5X5, Modifier.textStyle(style))
                }
            }
        }
    }
}
