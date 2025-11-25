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
package gg.essential.gui.wardrobe.configuration

import gg.essential.cosmetics.CosmeticTypeId
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledListInputRow
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledRow
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledStringInputRow
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.CosmeticType
import gg.essential.mod.cosmetics.SkinLayer
import gg.essential.universal.USound
import gg.essential.vigilance.utils.onLeftClick

class CosmeticTypeConfiguration(
    state: WardrobeState,
) : AbstractConfiguration<CosmeticTypeId, CosmeticType>(
    ConfigurationType.TYPES,
    state
) {

    override fun LayoutScope.columnLayout(type: CosmeticType) {
        labeledStringInputRow("Name:", mutableStateOf(type.displayNames["en_us"] ?: "")).state.onSetValue(stateScope) { type.update(type.copy(displayNames = type.displayNames + ("en_us" to it))) }
        labeledListInputRow("Slot:", type.slot, CosmeticSlot.values(), { it.id }) { type.update(type.copy(slot = it)) }
        for (skinLayer in SkinLayer.values()) {
            labeledRow(skinLayer.name + ":") {
                row(Arrangement.spacedBy(10f)) {
                    checkbox(type.skinLayers[skinLayer] ?: false) { type.update(type.copy(skinLayers = type.skinLayers + (skinLayer to it))) }
                    if (type.skinLayers[skinLayer] != null) {
                        box(Modifier.width(10f).heightAspect(1f).hoverTooltip("Removes setting from config, making it default. (Probably false)", wrapAtWidth = 100f).hoverScope()) {
                            icon(EssentialPalette.CANCEL_5X)
                        }.onLeftClick {
                            USound.playButtonPress()
                            type.update(type.copy(skinLayers = type.skinLayers - skinLayer))
                        }
                    } else {
                        spacer(width = 10f)
                    }
                }
            }
        }
    }

}
