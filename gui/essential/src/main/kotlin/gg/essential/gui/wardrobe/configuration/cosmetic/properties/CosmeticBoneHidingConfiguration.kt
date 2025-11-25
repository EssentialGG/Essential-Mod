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
package gg.essential.gui.wardrobe.configuration.cosmetic.properties

import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.Checkbox
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.input.StateTextInput
import gg.essential.gui.common.input.UITextInput
import gg.essential.gui.common.input.essentialStateTextInput
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.NoticeEssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.addAutoCompleteMenu
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.divider
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledInputRow
import gg.essential.mod.cosmetics.settings.CosmeticProperty
import gg.essential.network.connectionmanager.cosmetics.*
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.vigilance.utils.onLeftClick

class CosmeticBoneHidingConfiguration(
    private val cosmeticsDataWithChanges: CosmeticsDataWithChanges,
    private val cosmetic: Cosmetic,
) : LayoutDslComponent {

    private val boneHidingProperties = cosmetic.properties<CosmeticProperty.CosmeticBoneHiding>()

    override fun LayoutScope.layout(modifier: Modifier) {
        column(Modifier.fillWidth().then(modifier), Arrangement.spacedBy(10f)) {
            row(Modifier.fillWidth(), Arrangement.SpaceBetween) {
                idColumn(boneHidingProperties)
                checkboxColumn("Head", { head }, { copy(head = it) })
                checkboxColumn("Body", { body }, { copy(body = it) })
                checkboxColumn("Arms", { arms }, { copy(arms = it) })
                checkboxColumn("Legs", { legs }, { copy(legs = it) })
                removeColumn(boneHidingProperties)
            }
            column(Modifier.fillWidth(), Arrangement.spacedBy(5f)) {
                text("Add new cosmetic bone hiding setting")
                newSettingInput(boneHidingProperties)
            }
            divider()
            labeledInputRow("Copy from:") {
                val input = essentialStateTextInput(
                    mutableStateOf(null),
                    { "" }, // Since we update when we get a valid result, we don't need this
                    { input ->
                        if (input.isBlank())
                            null
                        else (cosmeticsDataWithChanges.getCosmetic(input)?.properties<CosmeticProperty.CosmeticBoneHiding>() ?: throw StateTextInput.ParseException())
                    }
                )
                addAutoCompleteMenu(input, cosmeticsDataWithChanges.cosmetics.mapEach { it.id to it.displayName }.toListState())
                input
            }.state.onSetValue(stateScope) { propertyList ->
                if (propertyList != null) {
                    val newPropertyList = cosmetic.allProperties.toMutableList()
                    newPropertyList.removeAll(boneHidingProperties)
                    newPropertyList.addAll(propertyList)
                    cosmeticsDataWithChanges.updateCosmetic(cosmetic.id, cosmetic.copy(allProperties = newPropertyList))
                }
            }
        }
    }

    private fun LayoutScope.checkboxColumn(
        name: String,
        initialState: CosmeticProperty.CosmeticBoneHiding.Data.() -> Boolean,
        update: CosmeticProperty.CosmeticBoneHiding.Data.(Boolean) -> CosmeticProperty.CosmeticBoneHiding.Data
    ) {
        column(Arrangement.spacedBy(5f)) {
            text(name)
            for (property in boneHidingProperties) {
                checkbox(property.data.initialState()) {
                    cosmeticsDataWithChanges.updateCosmeticProperty(
                        cosmetic.id,
                        property,
                        property.copy(data = property.data.update(it))
                    )
                }
            }
        }
    }

    private fun LayoutScope.newSettingInput(properties: List<CosmeticProperty.CosmeticBoneHiding>) {

        val input: UITextInput
        row(Modifier.fillWidth(), Arrangement.SpaceAround) {
            box(Modifier.width(75f).childBasedHeight(2f).color(EssentialPalette.BUTTON).hoverColor(EssentialPalette.BUTTON_HIGHLIGHT).hoverScope()) {
                input = UITextInput("Cosmetic ID")(Modifier.fillWidth(padding = 2f))
            }.onLeftClick { input.grabWindowFocus() }

            val headCheckbox = checkbox(false)
            val bodyCheckbox = checkbox(false)
            val armsCheckbox = checkbox(false)
            val legsCheckbox = checkbox(false)

            addButton(input, properties, headCheckbox, bodyCheckbox, armsCheckbox, legsCheckbox)
        }
    }

    private fun LayoutScope.addButton(
        input: UITextInput,
        properties: List<CosmeticProperty.CosmeticBoneHiding>,
        headCheckbox: Checkbox,
        bodyCheckbox: Checkbox,
        armsCheckbox: Checkbox,
        legsCheckbox: Checkbox,
    ) {
        IconButton(EssentialPalette.PLUS_5X, tooltipText = "Add")(Modifier.width(9f).heightAspect(1f)).onLeftClick {
            USound.playButtonPress()
            val cosmeticId = input.getText()
            if (cosmeticsDataWithChanges.getCosmetic(cosmeticId.uppercase()) == null) {
                platform.pushModal { manager ->
                    NoticeEssentialModal(manager, false).configure {
                        titleText = "Invalid Cosmetic ID"
                        contentText = "The ID you entered is not a valid cosmetic ID"
                    }
                }
                return@onLeftClick
            }

            if (properties.any { it.id == cosmeticId }) {
                platform.pushModal { manager ->
                    NoticeEssentialModal(manager, false).configure {
                        titleText = "Duplicate Cosmetic ID"
                        contentText = "The target cosmetic already has a bone hiding setting. Update it instead of adding a new one."
                    }
                }
                return@onLeftClick
            }

            cosmeticsDataWithChanges.addCosmeticProperty(
                cosmetic.id,
                CosmeticProperty.CosmeticBoneHiding(
                    cosmeticId,
                    true,
                    CosmeticProperty.CosmeticBoneHiding.Data(
                        head = headCheckbox.isChecked.get(),
                        body = bodyCheckbox.isChecked.get(),
                        arms = armsCheckbox.isChecked.get(),
                        legs = legsCheckbox.isChecked.get(),
                    )
                )
            )

            input.setText("")
            headCheckbox.isChecked.set(false)
            bodyCheckbox.isChecked.set(false)
            armsCheckbox.isChecked.set(false)
            legsCheckbox.isChecked.set(false)
        }

    }

    private fun LayoutScope.removeColumn(properties: List<CosmeticProperty.CosmeticBoneHiding>) {
        column(Arrangement.spacedBy(7f)) {
            text("Remove")
            for (property in properties) {
                icon(EssentialPalette.CANCEL_7X).apply {
                    bindHoverEssentialTooltip(BasicState("Remove"))
                    onLeftClick {
                        platform.pushModal { manager ->
                            ConfirmDenyModal(manager, false).configure {
                                titleText = "Are you sure you want to remove the setting for ${property.id}?"
                            }.onPrimaryAction {
                                cosmeticsDataWithChanges.removeCosmeticProperty(cosmetic.id, property)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun LayoutScope.idColumn(properties: List<CosmeticProperty.CosmeticBoneHiding>) {
        column(Arrangement.spacedBy(6f)) {
            text("ID")
            for (property in properties) {
                text(property.id)
            }
        }
    }
}
