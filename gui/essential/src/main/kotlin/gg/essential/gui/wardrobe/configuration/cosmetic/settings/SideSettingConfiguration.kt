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
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledEnumInputRow
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.mod.cosmetics.settings.CosmeticSettingType
import gg.essential.model.Side
import gg.essential.network.connectionmanager.cosmetics.*
import gg.essential.network.cosmetics.Cosmetic

class SideSettingConfiguration(
    cosmeticsData: CosmeticsData,
    modelLoader: ModelLoader,
    cosmeticId: CosmeticId,
    settingsList: MutableListState<CosmeticSetting>,
) : SingletonSettingConfiguration<CosmeticSetting.Side>(
    CosmeticSettingType.SIDE,
    cosmeticsData,
    modelLoader,
    cosmeticId,
    settingsList
) {

    override fun getDefault(cosmetic: Cosmetic, availableSides: Set<Side>): CosmeticSetting.Side? {
        return CosmeticSetting.Side(null, true, CosmeticSetting.Side.Data(cosmetic.defaultSide ?: Side.getDefaultSideOrNull(availableSides) ?: return null))
    }

    override fun LayoutScope.layout(cosmetic: Cosmetic, setting: CosmeticSetting.Side, availableSides: Set<Side>) {
        if(availableSides.isNotEmpty()) {
            labeledEnumInputRow("Side:", setting.data.side, enumFilter = { list -> list.filter { availableSides.contains(it.value) } }) { setting.update(setting.copy(data = setting.data.copy(side = it))) }
        }
    }

}
