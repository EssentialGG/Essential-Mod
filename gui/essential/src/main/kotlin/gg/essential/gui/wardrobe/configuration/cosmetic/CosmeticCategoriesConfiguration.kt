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
package gg.essential.gui.wardrobe.configuration.cosmetic

import gg.essential.elementa.components.UIContainer
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialDropDown
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.input.essentialIntInput
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.divider
import gg.essential.mod.cosmetics.CosmeticCategory
import gg.essential.network.connectionmanager.cosmetics.*
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.vigilance.utils.onLeftClick

class CosmeticCategoriesConfiguration(
    private val cosmeticsDataWithChanges: CosmeticsDataWithChanges,
    private val cosmetic: Cosmetic,
) : UIContainer() {

    init {
        layoutAsColumn(Modifier.fillWidth().childBasedHeight(), Arrangement.spacedBy(5f)) {
            val categories = cosmeticsDataWithChanges.categories.filter { cosmetic.categories.containsKey(it.id) }.mapEach { Pair(it, cosmetic.categories[it.id]!!) }

            fun LayoutScope.categoryLine(category: CosmeticCategory, sortWeight: Int) {
                row(Modifier.fillWidth()) {
                    row(Modifier.fillRemainingWidth(), Arrangement.SpaceAround) {
                        text(category.displayNames["en_us"]!!)
                        essentialIntInput(mutableStateOf(sortWeight)).state.onSetValue(stateScope) {
                            cosmeticsDataWithChanges.addToCategory(cosmetic.id, category.id, it)
                        }
                    }
                    IconButton(EssentialPalette.CANCEL_7X, "Remove From Category").onLeftClick {
                        cosmeticsDataWithChanges.removeCosmeticFromCategory(cosmetic.id, category.id)
                    }()
                }

            }

            forEach(categories) {
                categoryLine(it.first, it.second)
            }
            divider()
            addNewCategory(categories)
        }
    }

    private fun LayoutScope.addNewCategory(categories: ListState<Pair<CosmeticCategory, Int>>) {
        val excludedCategories = stateBy {
            cosmeticsDataWithChanges.categories() - categories().map { it.first }.toSet()
        }.toListState()

        if_(excludedCategories.map { it.isNotEmpty() }) {
            val dropDown = EssentialDropDown(excludedCategories.get()[0], excludedCategories.mapEach { EssentialDropDown.Option(it.displayNames["en_us"]!!, it) })
            box(Modifier.fillWidth()) {
                row(Modifier.fillWidth(), Arrangement.SpaceAround) {
                    dropDown()

                    IconButton(EssentialPalette.PLUS_7X, "Add to category").onLeftClick {
                        val category = dropDown.selectedOption.get().value
                        cosmeticsDataWithChanges.addToCategory(
                            cosmetic.id,
                            category.id,
                            0
                        )
                    }()
                }
            }
        }
    }
}
