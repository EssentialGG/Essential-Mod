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

import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillRemainingWidth
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.overlay.ModalManager
import java.awt.Color

class PropertiesModal(
    modalManager: ModalManager,
    private val metadata: Map<String, State<String>>,
) : EssentialModal2(modalManager) {
    override fun LayoutScope.layoutTitle() = title("Image Properties")

    override fun LayoutScope.layoutContent(modifier: Modifier) = layoutContentImpl(modifier.width(233f))

    override fun LayoutScope.layoutBody() {
        row(Modifier.fillWidth(), Arrangement.spacedBy(21f)) {
            textColumn {
                metadata.keys.forEach { key ->
                    text("$key:", Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(Color.BLACK))
                }
            }

            textColumn(Modifier.fillRemainingWidth()) {
                metadata.values.forEach { value ->
                    text(value, Modifier.color(EssentialPalette.TEXT_MID_DARK), truncateIfTooSmall = true)
                }
            }
        }
    }

    override fun LayoutScope.layoutButtons() {
        cancelButton("Close")
    }

    private fun LayoutScope.textColumn(modifier: Modifier = Modifier, block: LayoutScope.() -> Unit) {
        column(modifier, Arrangement.spacedBy(11f), Alignment.Start, block)
    }
}
