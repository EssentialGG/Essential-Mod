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
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledRow
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.mod.cosmetics.settings.CosmeticSettingType
import gg.essential.model.Side
import gg.essential.network.connectionmanager.cosmetics.*
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.vigilance.utils.onLeftClick

abstract class SingletonSettingConfiguration<P : CosmeticSetting>(
    private val cosmeticSettingType: CosmeticSettingType,
    private val cosmeticsData: CosmeticsData,
    private val modelLoader: ModelLoader,
    private val cosmeticId: CosmeticId,
    private val settingsList: MutableListState<CosmeticSetting>,
) : LayoutDslComponent {

    @Suppress("UNCHECKED_CAST")
    private val cosmeticAndSetting = stateBy {
        val cosmetic = cosmeticsData.cosmetics().firstOrNull { it.id == cosmeticId }
        val setting = settingsList().firstOrNull { it.type == cosmeticSettingType } as? P
        Pair(cosmetic, setting)
    }

    override fun LayoutScope.layout(modifier: Modifier) {
        box(Modifier.fillWidth().then(modifier)) {
            bind(cosmeticAndSetting) { (cosmetic, setting) ->
                if (cosmetic != null) {
                    val model = modelLoader.getModel(cosmetic, cosmetic.defaultVariantName, AssetLoader.Priority.Blocking).join()
                    val sidesAvaliable = model.sideOptions

                    if (setting != null) {
                        column(Modifier.fillWidth(), Arrangement.spacedBy(5f)) {
                            labeledRow(cosmeticSettingType.displayName) {
                                box(Modifier.width(10f).heightAspect(1f)) {
                                    icon(EssentialPalette.CANCEL_5X)
                                }.onLeftClick {
                                    setting.update(null)
                                }
                            }
                            layout(cosmetic, setting, sidesAvaliable)
                        }
                    } else {
                        val defaultSetting = getDefault(cosmetic, sidesAvaliable)

                        if (defaultSetting != null) {
                            labeledRow("Add " + cosmeticSettingType.displayName + ":") {
                                box(Modifier.width(10f).heightAspect(1f)) {
                                    icon(EssentialPalette.PLUS_5X)
                                }.onLeftClick {
                                    settingsList.add(defaultSetting)
                                }
                            }
                        }/* else {
                            text(cosmeticSettingType.name + " not applicable for "+cosmeticId)
                        }*/
                    }
                } else {
                    text("Cosmetic was not found?")
                }
            }
        }
    }

    abstract fun LayoutScope.layout(cosmetic: Cosmetic, setting: P, availableSides: Set<Side>)

    abstract fun getDefault(cosmetic: Cosmetic, availableSides: Set<Side>): P?

    protected fun CosmeticSetting.update(setting: P?) = if (setting == null) settingsList.remove(this) else settingsList.set { it.set(it.indexOf(this), setting) }

}
