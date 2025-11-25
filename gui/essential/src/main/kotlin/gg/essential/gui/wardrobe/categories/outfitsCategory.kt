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

import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.components.noItemsFound

fun LayoutScope.outfitsCategory(wardrobeState: WardrobeState, modifier: Modifier = Modifier) {
    val outfits = wardrobeState.visibleOutfitItems
    val favorites = outfits.filter { it.isFavorite }
    val nonFavorites = outfits.filter { !it.isFavorite }

    column(Modifier.fillWidth().then(modifier)) {
        if_(favorites.map { it.isNotEmpty() }) {
            cosmeticGroup("Favorites", favorites, wardrobeState)
        }
        if_(nonFavorites.map { it.isNotEmpty() }) {
            cosmeticGroup("Library", nonFavorites, wardrobeState)
        }
        if_(outfits.map { it.isEmpty() }) {
            noItemsFound()
        }

        spacer(height = 10f)
    }
}
