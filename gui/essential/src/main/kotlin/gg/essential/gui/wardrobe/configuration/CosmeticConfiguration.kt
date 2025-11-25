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

import gg.essential.cosmetics.CosmeticId
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialDropDown
import gg.essential.gui.common.FullEssentialToggle
import gg.essential.gui.common.input.StateTextInput
import gg.essential.gui.common.input.essentialStateTextInput
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledEnumInputRow
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledInputRow
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledIntInputRow
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledListInputRow
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledManagedNullableISODateInputRow
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledRow
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledStringInputRow
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.navButton
import gg.essential.gui.wardrobe.configuration.cosmetic.*
import gg.essential.gui.wardrobe.configuration.cosmetic.properties.*
import gg.essential.mod.cosmetics.settings.CosmeticPropertyType
import gg.essential.network.connectionmanager.cosmetics.*
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.vigilance.utils.onLeftClick

class CosmeticConfiguration(
    state: WardrobeState,
) : AbstractConfiguration<CosmeticId, Cosmetic>(
    ConfigurationType.COSMETICS,
    state
) {

    private val currentView = mutableStateOf(View.HOME)
    private val managingSetting = mutableStateOf<CosmeticPropertyType?>(null)

    init {
        state.currentlyEditingCosmeticTypeId.onSetValue(referenceHolder) {
            currentView.set(View.HOME)
            // Otherwise, clicking settings will bring you into the setting you were previously managing
            // and singleton settings that do not yet exist will not be created
            managingSetting.set(null)
        }
    }

    override fun LayoutScope.columnLayout(cosmetic: Cosmetic) {
        bind(currentView) { view ->
            when (view) {
                View.HOME -> homeView()
                View.PROPERTIES -> propertiesView(cosmetic)
                View.GENERAL -> generalView(cosmetic)
                View.CATEGORIES -> CosmeticCategoriesConfiguration(cosmeticsDataWithChanges, cosmetic)()
            }
            if (view != View.HOME) {
                box(Modifier.fillWidth().height(30f)) {
                    backButton()
                }
            }
        }
    }

    private fun LayoutScope.backButton() {
        navButton("Back to editing selection") {
            when {
                managingSetting.get() != null -> managingSetting.set(null)
                else -> currentView.set(View.HOME)
            }
        }
    }

    private fun LayoutScope.homeView() {
        navButton("Edit general settings") { currentView.set(View.GENERAL) }
        navButton("Edit properties") { currentView.set(View.PROPERTIES) }
        navButton("Edit categories") { currentView.set(View.CATEGORIES) }
    }

    // Settings that can only be applied once (ie Armor Handling)
    private fun LayoutScope.singletonSettingsButton(cosmetic: Cosmetic, type: CosmeticPropertyType) {
        val enabledState = mutableStateOf(cosmetic.properties.any { it.type == type })

        enabledState.onSetValue(stateScope) { cosmeticsDataWithChanges.setCosmeticSingletonPropertyEnabled(cosmetic.id, type, it) }

        row(Modifier.fillWidth(padding = 10f).height(20f)) {
            navButton(type.displayName, Modifier.fillRemainingWidth()) {
                // Create the setting if it does not already exist
                if (cosmetic.allProperties.none { it.type == type }) {
                    cosmeticsDataWithChanges.setCosmeticSingletonPropertyEnabled(cosmetic.id, type, true)
                }
                managingSetting.set(type)
            }
            spacer(width = 10f)
            box(Modifier.width(35f).fillHeight()) {
                FullEssentialToggle(enabledState)()
            }
        }
    }

    // Settings that can be applied multiple times (ie Bone Hiding)
    private fun LayoutScope.variableSettingsButton(type: CosmeticPropertyType) {
        navButton(type.displayName) {
            managingSetting.set(type)
        }
    }

    private fun LayoutScope.generalView(cosmetic: Cosmetic) {
        labeledListInputRow(
            "Type:",
            cosmetic.type,
            cosmeticsDataWithChanges.types.map { types -> types.sortedBy { it.displayNames["en_us"] } }.toListState().mapEach { EssentialDropDown.Option(it.id, it) }) {
            cosmeticsDataWithChanges.setCosmeticType(cosmetic.id, it.id)
        }
        labeledEnumInputRow("Tier:", cosmetic.tier) { cosmeticsDataWithChanges.setCosmeticTier(cosmetic.id, it) }
        labeledStringInputRow("Display Name:", mutableStateOf(cosmetic.displayName)).state.onSetValue(stateScope) { cosmeticsDataWithChanges.setCosmeticDisplayName(cosmetic.id, it) }
        labeledNullableIntInputRow("Price:", mutableStateOf(cosmetic.priceCoinsNullable)).state.onSetValue(stateScope) { cosmeticsDataWithChanges.setCosmeticPriceCoins(cosmetic.id, it) }
        text("Tags:", Modifier.alignHorizontal(Alignment.Start))
        if (cosmetic.tags.isEmpty()) {
            text("No tags...", Modifier.alignHorizontal(Alignment.Start))
        } else {
            for (tag in cosmetic.tags) {
                labeledRow("- $tag") {
                    box(Modifier.width(10f).height(10f)) {
                        icon(EssentialPalette.CANCEL_5X)
                    }.onLeftClick {
                        cosmeticsDataWithChanges.setCosmeticTags(cosmetic.id, cosmetic.tags - tag)
                    }
                }
            }
        }

        labeledStringInputRow("Add tag:", mutableStateOf("")).state.onSetValue(stateScope) { cosmeticsDataWithChanges.setCosmeticTags(cosmetic.id, cosmetic.tags + it) }
        labeledManagedNullableISODateInputRow("Available After:", mutableStateOf(cosmetic.availableAfter)).state.onSetValue(stateScope) { cosmeticsDataWithChanges.setCosmeticAvailable(cosmetic.id, it, cosmetic.availableUntil, cosmetic.showTimerAfter) }
        labeledManagedNullableISODateInputRow("Available Until:", mutableStateOf(cosmetic.availableUntil)).state.onSetValue(stateScope) { cosmeticsDataWithChanges.setCosmeticAvailable(cosmetic.id, cosmetic.availableAfter, it, cosmetic.showTimerAfter) }
        labeledManagedNullableISODateInputRow("Show Timer After:", mutableStateOf(cosmetic.showTimerAfter)).state.onSetValue(stateScope) { cosmeticsDataWithChanges.setCosmeticAvailable(cosmetic.id, cosmetic.availableAfter, cosmetic.availableUntil, it) }
        labeledIntInputRow("Default Sort Weight", mutableStateOf(cosmetic.defaultSortWeight)).state.onSetValue(stateScope) { cosmeticsDataWithChanges.setCosmeticDefaultSortWeight(cosmetic.id, it) }
    }

    private fun LayoutScope.propertiesView(cosmetic: Cosmetic) {
        bind(managingSetting) { settingType ->
            if (settingType == null) {
                CosmeticPropertyType.values().sortedBy { it.singleton }.forEach {
                    if (it.singleton) {
                        singletonSettingsButton(cosmetic, it)
                    } else {
                        variableSettingsButton(it)
                    }
                }
            } else {
                text(settingType.displayName)
                (when (settingType) {
                    CosmeticPropertyType.COSMETIC_BONE_HIDING -> CosmeticBoneHidingConfiguration(cosmeticsDataWithChanges, cosmetic)
                    CosmeticPropertyType.EXTERNAL_HIDDEN_BONE -> ExternalHiddenBoneConfiguration(state, cosmeticsDataWithChanges, cosmetic)
                    CosmeticPropertyType.ARMOR_HANDLING -> ArmorHandlingConfiguration(cosmeticsDataWithChanges, cosmetic)
                    CosmeticPropertyType.ARMOR_HANDLING_V2 -> ArmorHandlingV2Configuration(cosmeticsDataWithChanges, cosmetic)
                    CosmeticPropertyType.POSITION_RANGE -> PlayerPositionAdjustmentPropertyConfiguration(cosmeticsDataWithChanges, cosmetic)
                    CosmeticPropertyType.INTERRUPTS_EMOTE -> InterruptsEmoteConfiguration(cosmeticsDataWithChanges, cosmetic)
                    CosmeticPropertyType.PREVIEW_RESET_TIME -> PreviewResetTimeConfiguration(cosmeticsDataWithChanges, cosmetic)
                    CosmeticPropertyType.LOCALIZATION -> LocalizationConfiguration(cosmeticsDataWithChanges, cosmetic)
                    CosmeticPropertyType.TRANSITION_DELAY -> TransitionDelayConfiguration(cosmeticsDataWithChanges, cosmetic)
                    CosmeticPropertyType.REQUIRES_UNLOCK_ACTION -> RequiresUnlockActionConfiguration(cosmeticsDataWithChanges, cosmetic)
                    CosmeticPropertyType.VARIANTS -> VariantsPropertyConfiguration(cosmeticsDataWithChanges, cosmetic)
                    CosmeticPropertyType.DEFAULT_SIDE -> DefaultSidePropertyConfiguration(cosmeticsDataWithChanges, cosmetic)
                    CosmeticPropertyType.MUTUALLY_EXCLUSIVE -> MutuallyExclusivePropertyConfiguration(cosmeticsDataWithChanges, cosmetic)
                    CosmeticPropertyType.ALL_OTHER_COSMETIC_OR_ITEM_HIDING -> HidesAllOtherCosmeticsOrItemsConfiguration(cosmeticsDataWithChanges, cosmetic)
                    CosmeticPropertyType.LOCKS_PLAYER_ROTATION -> LocksPlayerRotationConfiguration(cosmeticsDataWithChanges, cosmetic)
                }).let {
                    it()
                }
            }
        }
    }

    private enum class View {
        HOME,
        PROPERTIES,
        CATEGORIES,
        GENERAL,
    }

    private fun LayoutScope.labeledNullableIntInputRow(
        label: String,
        state: MutableState<Int?>
    ) = labeledInputRow(label) {
        essentialStateTextInput(
            state,
            { it?.toString() ?: "" },
            {
                try {
                    if (it.isEmpty()) null
                    else it.toInt()
                } catch (e: NumberFormatException) {
                    throw StateTextInput.ParseException()
                }
            },
            Modifier.width(40f),
        )
    }

}
