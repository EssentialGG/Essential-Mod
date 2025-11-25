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

import gg.essential.elementa.UIComponent
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.Item
import gg.essential.gui.wardrobe.WardrobeCategory
import gg.essential.gui.wardrobe.WardrobeState

fun LayoutScope.cosmeticsGrid(
    category: WardrobeCategory,
    cosmetics: ListState<out Item>,
    wardrobeState: WardrobeState,
    modifier: Modifier = Modifier,
): UIComponent {
    return flowContainer(modifier.fillWidth(), cosmeticXSpacing, cosmeticYSpacing, Arrangement.spacedBy(cosmeticXSpacing, FloatPosition.START)) {
        forEach(cosmetics, cache = true) { cosmetic ->
            cosmeticItem(cosmetic, category, wardrobeState, Modifier)
        }
    }
}
