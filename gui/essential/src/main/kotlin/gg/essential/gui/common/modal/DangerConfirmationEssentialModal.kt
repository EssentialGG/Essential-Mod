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
import gg.essential.gui.common.*
import gg.essential.gui.overlay.ModalManager

/**
 * Confirm / deny modal indicating that the action is dangerous (like deleting an account from the account manager or discarding unsaved screenshot edits)
 */
open class DangerConfirmationEssentialModal(
    modalManager: ModalManager,
    confirmText: String,
    requiresButtonPress: Boolean,
) : ConfirmDenyModal(modalManager, requiresButtonPress) {

    init {
        primaryButtonText = confirmText
        contentTextColor = EssentialPalette.TEXT_HIGHLIGHT
        configure {
            primaryButtonStyle = OutlineButtonStyle.RED.defaultStyle
            primaryButtonHoverStyle = OutlineButtonStyle.RED.hoveredStyle
        }
    }
}
