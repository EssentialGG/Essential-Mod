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
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.components.noItemsFound
import gg.essential.network.cosmetics.Cosmetic.Diagnostic

fun LayoutScope.diagnosticsCategory(wardrobeState: WardrobeState, modifier: Modifier = Modifier) {
    val byType = Diagnostic.Type.entries.associateWith { type ->
        wardrobeState.visibleCosmeticItems.filter { item ->
            item.cosmetic.diagnostics?.any { it.type == type } == true
        }
    }
    val loading = wardrobeState.visibleCosmeticItems.filter { item ->
        item.cosmetic.diagnostics == null
    }

    column(Modifier.fillWidth().then(modifier)) {
        for ((type, diagnostics) in byType) {
            if_({ diagnostics().isNotEmpty() }) {
                cosmeticGroup(type.name, diagnostics, wardrobeState)
            }
        }
        if_({ loading().isNotEmpty() }) {
            cosmeticGroup("Loading", loading, wardrobeState)
        }
        if_({ byType.all { it.value().isEmpty() } && loading().isEmpty() }) {
            noItemsFound()
        }

        spacer(height = 10f)
    }
}

