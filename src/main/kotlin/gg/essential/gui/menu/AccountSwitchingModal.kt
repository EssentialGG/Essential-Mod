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
package gg.essential.gui.menu

import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.SequenceAnimatedUIImage
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.childBasedHeight
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.outline
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.overlay.ModalManager
import java.awt.Color
import java.util.concurrent.TimeUnit

class AccountSwitchingModal(
    modalManager: ModalManager,
    private val isSwitching: State<Boolean>
) : Modal(modalManager) {

    override fun LayoutScope.layoutModal() {
        column(
            Modifier
                .childBasedHeight(22f)
                .width(155f)
                .color(Color.BLACK)
                .outline(EssentialPalette.TEXT_MID_GRAY, 1f)
        ) {
            spacer(height = 1f)
            text(
                "Switching account...",
                Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(EssentialPalette.TEXT_SHADOW_LIGHT)
            )
            spacer(height = 10f)
            SequenceAnimatedUIImage(
                "/assets/essential/textures/loading/loading_", ".png",
                12,
                80,
                TimeUnit.MILLISECONDS,
            )(Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(Color.BLACK))
        }
    }

    override fun handleEscapeKeyPress() {
    }

    override fun onOpen() {
        super.onOpen()
        effect(this) {
            if (!isSwitching()) {
                close()
            }
        }
    }

}