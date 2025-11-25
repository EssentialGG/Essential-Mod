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
package gg.essential.gui.wardrobe.something

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.State
import gg.essential.gui.EssentialPalette
import gg.essential.gui.about.components.ColoredDivider
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.mapEach
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.Item
import gg.essential.gui.wardrobe.WardrobeCategory
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.components.cosmeticsGrid

class CosmeticGroup(
    val category: WardrobeCategory,
    val name: String,
    val cosmetics: ListState<out Item.CosmeticOrEmote>,
    wardrobeState: WardrobeState,
    scroller: ScrollComponent,
    scrollerPercentState: State<Float>,
) : UIContainer() {

    val cosmeticsWithSortInfo = cosmetics.mapEach { item ->
        val unlocked = wardrobeState.unlockedCosmetics.map { item.id in it }
        val price = item.getCost(wardrobeState)
        val collection = (category as? WardrobeCategory.InfraCollectionSubcategory)?.category
        stateBy {
            item to WardrobeState.CosmeticWithSortInfo(item.cosmetic, unlocked(), price(), collection)
        }
    }
    val sortedCosmetics = stateBy {
        cosmeticsWithSortInfo()
            .map { it() }
            .sortedWith(compareBy(wardrobeState.filterSort()) { it.second })
            .map { it.first }
    }.toListState()

    // FIXME: Kotlin emits invalid bytecode if this is `val`, see https://youtrack.jetbrains.com/issue/KT-48757
    var cosmeticsContainer: UIComponent

    init {
        layoutAsColumn(Modifier.fillWidth().childBasedHeight()) {
            box(Modifier.fillWidth().height(headerHeight)) {
                ColoredDivider(name, dividerColor = EssentialPalette.BUTTON)()
            }

            cosmeticsContainer = cosmeticsGrid(category, sortedCosmetics, wardrobeState)
        }
    }

    companion object {
        internal const val headerHeight = 28f
    }
}
