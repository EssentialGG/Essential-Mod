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
package gg.essential.gui.wardrobe.configuration.cosmetic.settings

import gg.essential.cosmetics.CosmeticId
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledListInputRow
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledRow
import gg.essential.mod.cosmetics.settings.CosmeticProperty
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.mod.cosmetics.settings.CosmeticSettingType
import gg.essential.model.Side
import gg.essential.network.connectionmanager.cosmetics.*
import gg.essential.network.cosmetics.Cosmetic

class VariantSettingConfiguration(
    cosmeticsData: CosmeticsData,
    modelLoader: ModelLoader,
    cosmeticId: CosmeticId,
    settingsList: MutableListState<CosmeticSetting>,
) : SingletonSettingConfiguration<CosmeticSetting.Variant>(
    CosmeticSettingType.VARIANT,
    cosmeticsData,
    modelLoader,
    cosmeticId,
    settingsList
) {

    override fun getDefault(cosmetic: Cosmetic, availableSides: Set<Side>): CosmeticSetting.Variant? {
        val variants = cosmetic.property<CosmeticProperty.Variants>()?.data?.variants
        return if (variants.isNullOrEmpty()) null else CosmeticSetting.Variant(null, true, CosmeticSetting.Variant.Data(variants.first().name))
    }

    override fun LayoutScope.layout(cosmetic: Cosmetic, setting: CosmeticSetting.Variant, availableSides: Set<Side>) {
        val variants = cosmetic.variants
        if (variants.isNullOrEmpty()) {
            labeledRow("Variant:") {
                text("No variants found...")
            }
        } else {
            val variant = variants.firstOrNull { it.name == setting.data.variant } ?: variants.first()
            labeledListInputRow("Variant:", variant, variants, { it.name }) { setting.update(setting.copy(data = setting.data.copy(variant = it.name))) }
        }
    }

}
