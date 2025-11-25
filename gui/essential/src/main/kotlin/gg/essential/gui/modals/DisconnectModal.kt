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
import gg.essential.gui.common.OutlineButtonStyle
import gg.essential.gui.common.modal.EssentialModal2
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.overlay.ModalManager

class DisconnectModal(modalManager: ModalManager, private val reason: String) : EssentialModal2(modalManager) {

    override fun LayoutScope.layoutTitle() {
        title("Connection Lost", Modifier.color(EssentialPalette.MODAL_WARNING).shadow(EssentialPalette.BLACK))
    }

    override fun LayoutScope.layoutBody() {
        text(reason, Modifier.color(EssentialPalette.TEXT).shadow(EssentialPalette.BLACK))
    }

    override fun LayoutScope.layoutButtons() {
        primaryButton("Understood", style = OutlineButtonStyle.BLUE) {
            close()
        }
    }

    companion object {

        const val HOST_SUSPENDED = "The host has been suspended."
    }
}