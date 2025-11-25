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

import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.gui.overlay.ModalManager

/**
 * Confirm / deny modal where cancel button is positioned above primary button
 */
open class VerticalConfirmDenyModal(
    modalManager: ModalManager,
    requiresButtonPress: Boolean,
    buttonPadding: Float = 12f
) : ConfirmDenyModal(modalManager, requiresButtonPress) {

    init {
        cancelButton.setX(CenterConstraint())

        buttonContainer.constrain {
            x = CenterConstraint()
            y = SiblingConstraint(buttonPadding)
            width = ChildBasedMaxSizeConstraint()
            height = ChildBasedSizeConstraint()
        }

        primaryActionButton.constrain {
            y = SiblingConstraint(3f)
            x = CenterConstraint()
        }
    }
}
