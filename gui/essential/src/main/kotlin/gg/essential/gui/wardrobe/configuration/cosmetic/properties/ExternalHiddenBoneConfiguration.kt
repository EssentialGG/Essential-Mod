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

import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.input.StateTextInput
import gg.essential.gui.common.input.essentialStateTextInput
import gg.essential.gui.common.modal.CancelableInputModal
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.EssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.addAutoCompleteMenu
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.divider
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledInputRow
import gg.essential.mod.cosmetics.settings.CosmeticProperty
import gg.essential.network.connectionmanager.cosmetics.*
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.vigilance.utils.onLeftClick

class ExternalHiddenBoneConfiguration(
    private val state: WardrobeState,
    private val cosmeticsDataWithChanges: CosmeticsDataWithChanges,
    private val cosmetic: Cosmetic,
) : LayoutDslComponent {

    override fun LayoutScope.layout(modifier: Modifier) {
        val boneHidingProperties = cosmetic.properties<CosmeticProperty.ExternalHiddenBone>()
        column(Modifier.fillWidth().then(modifier), Arrangement.spacedBy(10f)) {
            column(Modifier.fillWidth(), Arrangement.spacedBy(5f)) {
                for (property in boneHidingProperties) {
                    boneProperty(property)
                }
            }
            box(Modifier.fillWidth().childBasedHeight(5f).color(EssentialPalette.BUTTON).hoverColor(EssentialPalette.BUTTON_HIGHLIGHT).hoverScope()) {
                text("Add new target cosmetic")
            }.onLeftClick { platform.pushModal { NewSettingModal(it) } }
            divider()
            labeledInputRow("Copy from:") {
                val input = essentialStateTextInput(
                    mutableStateOf(null),
                    { "" }, // Since we update when we get a valid result, we don't need this
                    { input ->
                        if (input.isBlank())
                            null
                        else (cosmeticsDataWithChanges.getCosmetic(input)?.properties<CosmeticProperty.ExternalHiddenBone>() ?: throw StateTextInput.ParseException())
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

    private fun LayoutScope.boneProperty(setting: CosmeticProperty.ExternalHiddenBone) {
        val expanded = mutableStateOf(false)
        val collapsedHeight = 20f

        val currentIcon = expanded.map {
            if (it) {
                EssentialPalette.ARROW_UP_7X4
            } else {
                EssentialPalette.ARROW_DOWN_7X4
            }
        }

        column(Modifier.fillWidth().whenTrue(expanded, Modifier.childBasedHeight(), Modifier.height(collapsedHeight)).effect { ScissorEffect() }, Arrangement.spacedBy(0f, FloatPosition.START)) {
            row(Modifier.fillWidth()) {
                row(Modifier.fillRemainingWidth().height(collapsedHeight).color(EssentialPalette.BUTTON).hoverColor(EssentialPalette.BUTTON_HIGHLIGHT).hoverScope(), Arrangement.SpaceAround) {
                    text(setting.id)
                    icon(currentIcon.toV1(stateScope))
                }.onLeftClick { expanded.set { !it } }
                box(Modifier.childBasedWidth(5f)) {
                    removeButton(setting)
                }
            }
            column(Modifier.fillWidth().color(EssentialPalette.COMPONENT_BACKGROUND), Arrangement.spacedBy(5f)) {
                setting.data.hiddenBones.forEach {
                    boneEntry(setting, it)
                }
            }
            box(Modifier.fillWidth().height(15f).color(EssentialPalette.BUTTON).hoverColor(EssentialPalette.BUTTON_HIGHLIGHT).hoverScope()) {
                text("Add new bone")
            }.onLeftClick {
                val cosmetic = cosmeticsDataWithChanges.getCosmetic(setting.id)
                if (cosmetic == null) {
                    platform.pushModal { manager ->
                        EssentialModal(manager, false).configure {
                            titleText = "Cosmetic ${setting.id} not found"
                        }
                    }
                    return@onLeftClick
                }
                platform.pushModal { ExternalBoneInputModal(it, setting, cosmetic) }
            }
        }
    }

    private fun LayoutScope.boneEntry(setting: CosmeticProperty.ExternalHiddenBone, boneName: String) {
        row(Modifier.fillWidth().childBasedHeight(3f), Arrangement.SpaceAround) {
            text(boneName)
            icon(EssentialPalette.CANCEL_7X).apply {
                bindHoverEssentialTooltip(BasicState("Remove"))
                onLeftClick {
                    platform.pushModal { manager ->
                        ConfirmDenyModal(manager, false).configure {
                            titleText = "Are you sure you want to the bone $boneName ${setting.id}?"
                        }.onPrimaryAction {
                            cosmeticsDataWithChanges.updateCosmeticProperty(cosmetic.id, setting, setting.copy(data = setting.data.copy(hiddenBones = setting.data.hiddenBones - boneName)))
                        }
                    }
                }
            }
        }
    }

    private fun LayoutScope.removeButton(setting: CosmeticProperty.ExternalHiddenBone) {
        icon(EssentialPalette.CANCEL_7X).apply {
            bindHoverEssentialTooltip(BasicState("Remove"))
            onLeftClick {
                platform.pushModal { manager ->
                    ConfirmDenyModal(manager, false).configure {
                        titleText = "Are you sure you want to remove the setting for ${setting.id}?"
                    }.onPrimaryAction {
                        cosmeticsDataWithChanges.removeCosmeticProperty(cosmetic.id, setting)
                    }
                }
            }
        }
    }

    /**
     * An input modal that allows the user to input a bone name and adds it to the list of hidden bones.
     * The bone name is validated against the model of the cosmetic.
     */
    inner class ExternalBoneInputModal(
        modalManager: ModalManager,
        setting: CosmeticProperty.ExternalHiddenBone,
        targetCosmetic: Cosmetic,
    ) : CancelableInputModal(modalManager, "Bone name", "") {

        private val model = state.modelLoader.getModel(targetCosmetic, targetCosmetic.defaultVariantName, AssetLoader.Priority.Blocking).get()

        init {

            val validBone = inputTextState.map { it in model.bones }
            bindConfirmAvailable(validBone.toV1(this))

            onPrimaryActionWithValue {
                cosmeticsDataWithChanges.updateCosmeticProperty(cosmetic.id, setting, setting.copy(data = setting.data.copy(hiddenBones = setting.data.hiddenBones + it)))
            }

            configure {
                titleText = "Enter the name of the bone you want to hide"
            }
        }
    }


    /**
     * An input modal that allows the user to input a cosmetic ID and adds it to the list of cosmetics
     */
    inner class NewSettingModal(modalManager: ModalManager) : CancelableInputModal(modalManager, "Cosmetic ID", "") {

        init {
            configure {
                titleText = "Enter cosmetic ID of the cosmetic you want to hide bones of"
            }
            val validCosmetic = inputTextState.map { cosmeticsDataWithChanges.getCosmetic(it.uppercase()) != null }
            bindConfirmAvailable(validCosmetic.toV1(this))

            onPrimaryActionWithValue { inputValue ->
                cosmeticsDataWithChanges.addCosmeticProperty(cosmetic.id, CosmeticProperty.ExternalHiddenBone(inputValue.uppercase(), true, CosmeticProperty.ExternalHiddenBone.Data(emptySet())))
            }
        }
    }


}
