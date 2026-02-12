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

import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.modal.EssentialModal2
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.image
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.underline
import gg.essential.gui.layoutdsl.wrappedText
import gg.essential.gui.overlay.ModalManager
import gg.essential.util.openInBrowser
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color
import java.net.URI

class FeatureDisabledModal private constructor(
    private val featureText: String,
    modalManager: ModalManager,
    private val featureTextIntro: String = "The $featureText is",
) : EssentialModal2(modalManager) {
    override fun LayoutScope.layoutBody() {
        column(Modifier.Companion.fillWidth()) {
            wrappedText(
                "Sorry! $featureTextIntro\n" +
                        "undergoing maintenance.\n" +
                        "We'll be back soon.",
                Modifier.Companion.color(EssentialPalette.MODAL_WARNING).shadow(Color.BLACK),
                centered = true
            )
            spacer(height = 17f)
            text(
                "Check $featureText status here:",
                Modifier.Companion.color(EssentialPalette.TEXT_MID_GRAY).shadow(Color.BLACK)
            )
            spacer(height = 4f)
            row(Modifier.Companion.fillWidth().hoverScope(), Arrangement.Companion.spacedBy(4f)) {
                val linkModifier = Modifier.Companion.color(EssentialPalette.TEXT).hoverColor(EssentialPalette.TEXT_HIGHLIGHT)
                    .shadow(Color.BLACK)
                text("status.essential.gg", linkModifier.underline())
                image(EssentialPalette.ARROW_UP_RIGHT_5X5, linkModifier)
            }.onLeftClick {
                openInBrowser(URI("https://status.essential.gg/"))
            }
        }
    }

    override fun LayoutScope.layoutButtons() {
        cancelButton("Okay")
    }

    companion object {
        fun store(manager: ModalManager) = FeatureDisabledModal("store", manager)
        fun wardrobe(manager: ModalManager) = FeatureDisabledModal("wardrobe", manager)
        fun social(manager: ModalManager) = FeatureDisabledModal("social system", manager)
        fun media(manager: ModalManager) = FeatureDisabledModal("media systems", manager, "Media systems are")
        fun worldHosting(manager: ModalManager) = FeatureDisabledModal("hosting", manager, "Hosting is")
    }
}