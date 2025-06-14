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
package gg.essential.gui.screenshot.components

import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.IconButton
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.universal.USound
import gg.essential.vigilance.utils.onLeftClick

fun LayoutScope.backButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(EssentialPalette.ARROW_LEFT_4X7, "Back")
        .invoke(modifier)
        .onLeftClick {
            USound.playButtonPress()
            onClick()
        }
}
