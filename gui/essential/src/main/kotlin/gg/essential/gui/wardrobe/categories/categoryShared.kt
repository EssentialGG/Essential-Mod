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
package gg.essential.gui.wardrobe.categories

import gg.essential.gui.EssentialPalette
import gg.essential.gui.about.components.*
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.Item
import gg.essential.gui.wardrobe.WardrobeCategory
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.components.cosmeticsGrid
import gg.essential.gui.wardrobe.something.CosmeticGroup

fun LayoutScope.cosmeticGroup(name: String, items: ListState<out Item>, wardrobeState: WardrobeState) {
    column(Modifier.fillWidth()) {
        box(Modifier.fillWidth().height(CosmeticGroup.headerHeight)) {
            ColoredDivider(name, dividerColor = EssentialPalette.BUTTON)()
        }

        cosmeticsGrid(WardrobeCategory.Outfits, items, wardrobeState)
    }
}
