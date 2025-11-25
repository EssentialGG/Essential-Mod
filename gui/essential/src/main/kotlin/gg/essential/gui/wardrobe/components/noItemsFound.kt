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
package gg.essential.gui.wardrobe.components

import gg.essential.gui.EssentialPalette
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text

fun LayoutScope.noItemsFound(modifier: Modifier = Modifier.fillWidth()) =
    noItemsFound("No items found.", modifier)

private fun LayoutScope.noItemsFound(text: String, modifier: Modifier) {
    column(modifier) {
        spacer(height = 20f)
        text(text, Modifier.color(EssentialPalette.TEXT))
    }
}
