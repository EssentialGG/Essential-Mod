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
package gg.essential.gui.friends.modals

import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.overlay.ModalManager

class ConfirmJoinModal(modalManager: ModalManager, user: String, isSps: Boolean) : ConfirmDenyModal(modalManager, false) {
    init {
        val title = buildString {
            append("Are you sure you want to join $user's ")
            if (isSps) {
                append("world")
            } else {
                append("server")
            }
            append("?")
        }
        configure {
            titleText = title
        }
    }
}
