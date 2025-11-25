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
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.FloatPosition
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.layoutdsl.image
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.wrappedText

fun LayoutScope.invalidScreenshotView(modifier: Modifier = Modifier) {
    column(Modifier.color(EssentialPalette.COMPONENT_BACKGROUND).fillParent().then(modifier), Arrangement.spacedBy(10f, FloatPosition.CENTER)) {
        val colorModifier = Modifier.color(EssentialPalette.INVALID_SCREENSHOT_TEXT).shadow(EssentialPalette.TEXT_SHADOW)

        image(EssentialPalette.ROUND_WARNING_7X, colorModifier)
        wrappedText("File can not be read", colorModifier, true)
    }
}