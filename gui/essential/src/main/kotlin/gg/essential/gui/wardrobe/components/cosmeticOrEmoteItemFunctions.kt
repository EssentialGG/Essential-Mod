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
package gg.essential.gui.wardrobe.components

import gg.essential.api.gui.NotificationType
import gg.essential.api.gui.Slot
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket
import gg.essential.elementa.components.Window
import gg.essential.elementa.events.UIClickEvent
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.ContextOptionMenu
import gg.essential.gui.common.sendCosmeticUnlockedToast
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.notification.warning
import gg.essential.gui.wardrobe.Item
import gg.essential.gui.wardrobe.WardrobeCategory
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.hasEnoughCoins
import gg.essential.gui.wardrobe.modals.CoinsPurchaseModal
import gg.essential.gui.wardrobe.modals.StoreDisabledModal
import gg.essential.gui.wardrobe.openPurchaseItemModal
import gg.essential.gui.wardrobe.purchaseAndCreateOutfitForBundle
import gg.essential.gui.wardrobe.purchaseCosmeticOrEmote
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.settings.CosmeticProperty
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.network.connectionmanager.features.Feature
import gg.essential.universal.UDesktop
import gg.essential.universal.USound
import gg.essential.util.Client
import gg.essential.util.EssentialSounds
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

fun handleCosmeticOrEmoteLeftClick(item: Item.CosmeticOrEmote, category: WardrobeCategory, wardrobeState: WardrobeState) {

    USound.playButtonPress()

    val cosmetic = item.cosmetic
    val outfitManager = wardrobeState.outfitManager
    val selectedOutfitId = outfitManager.selectedOutfitId.getUntracked() ?: return
    val cosmeticsManager = wardrobeState.cosmeticsManager
    val slot = item.cosmetic.type.slot
    val isOwned = cosmeticsManager.unlockedCosmetics.get().contains(cosmetic.id)

    wardrobeState.itemIdToCategoryMap[item.id] = category

    val isFree = cosmetic.isCosmeticFree
    val claimFreeItem = isFree && !isOwned && !cosmetic.requiresUnlockAction()

    val bundleWasSelected = wardrobeState.selectedBundle.get() != null
    val emoteWasSelected = wardrobeState.selectedEmote.getUntracked() != null

    val previouslySelectedItem = wardrobeState.selectedItem.getUntracked()
    wardrobeState.selectedItem.set(item)

    wardrobeState.cosmeticNotices.clearNewState(cosmetic.id)

    if (slot == CosmeticSlot.EMOTE && !isOwned) {
        if (previouslySelectedItem == item && !wardrobeState.inEmoteWheel.getUntracked()) {
            wardrobeState.selectedItem.set(null)
            wardrobeState.inEmoteWheel.set(category.superCategory == WardrobeCategory.Emotes)
        } else {
            wardrobeState.inEmoteWheel.set(false)
            sendItemPreviewTelemetry(item, category, wardrobeState)
        }
        return
    }

    val startedInEmoteWheel = wardrobeState.inEmoteWheel.getUntracked()
    wardrobeState.inEmoteWheel.set(slot == CosmeticSlot.EMOTE)

    if (item != wardrobeState.editingCosmetic.get()) {
        wardrobeState.editingCosmetic.set(null)
    }

    if (wardrobeState.inEmoteWheel.get()) {
        val selectedEmoteWheelId = wardrobeState.emoteWheelManager.selectedEmoteWheelId.getUntracked() ?: return
        val selectedEmoteWheelSlots = wardrobeState.emoteWheelManager.selectedEmoteWheelSlots
        val existingIndex = selectedEmoteWheelSlots.getUntracked().indexOf(cosmetic.id)
        if (existingIndex != -1) {
            if (startedInEmoteWheel && !bundleWasSelected && !emoteWasSelected) {
                // Only remove the emote if the emote wheel preview was open when the emote was clicked and a bundle was not selected
                wardrobeState.emoteWheelManager.setEmote(selectedEmoteWheelId, existingIndex, null)
                // Remove duplicates as well
                while (selectedEmoteWheelSlots.getUntracked().indexOf(cosmetic.id) != -1) {
                    wardrobeState.emoteWheelManager.setEmote(selectedEmoteWheelId, selectedEmoteWheelSlots.getUntracked().indexOf(cosmetic.id), null)
                }
            }
        } else {
            val emptyIndex = selectedEmoteWheelSlots.getUntracked().indexOfFirst { it == null }
            if (emptyIndex != -1) {
                wardrobeState.emoteWheelManager.setEmote(selectedEmoteWheelId, emptyIndex, cosmetic.id)
            } else {
                Notifications.warning("Emote wheel is full.", "")
            }
        }
        return
    }

    if (wardrobeState.equippedCosmeticsState.get()[slot] == cosmetic.id) {
        if (!(slot == CosmeticSlot.ICON) && (!startedInEmoteWheel && !bundleWasSelected && !emoteWasSelected)) {
            // Only unequip the cosmetic if the emote wheel preview was not open when the cosmetic was clicked and a bundle was not selected
            outfitManager.updateEquippedCosmetic(selectedOutfitId, slot, null)
            wardrobeState.selectedItem.set(null)
        }
    } else {
        outfitManager.updateEquippedCosmetic(selectedOutfitId, slot, cosmetic.id)
        updateSettingsToOverriddenIfNotSet(item, wardrobeState)

        if (!isOwned) {
            sendItemPreviewTelemetry(item, category, wardrobeState)
        }
    }

    // After above logic so that the item gets previewed. claimNow() will update
    // the preview to be an equipped state if the user is still previewing the item
    // when the CM confirms the purchase.
    if (claimFreeItem) {
        claimFreeItemNow(item, wardrobeState)
    }
}

private fun updateSettingsToOverriddenIfNotSet(item: Item.CosmeticOrEmote, wardrobeState: WardrobeState) {
    // If we select an item with overridden settings, we override the player's settings too, if they don't have them already set
    // Used by the featured page, since it initially show the item with overridden settings, so those should apply those when first selected
    for (setting in item.settingsOverride) {
        when {
            setting is CosmeticSetting.Variant -> {
                if (wardrobeState.getVariant(item).get() == null) wardrobeState.setVariant(item, setting.data.variant)
            }

            setting is CosmeticSetting.PlayerPositionAdjustment -> {
                if (wardrobeState.getSelectedPosition(item).get() == null) wardrobeState.setSelectedPosition(item, Triple(setting.data.x, setting.data.y, setting.data.z))
            }

            setting is CosmeticSetting.Side -> {
                if (wardrobeState.getSelectedSide(item).get() == null) wardrobeState.setSelectedSide(item, setting.data.side)
            }
        }
    }
}

fun handleCosmeticOrEmoteRightClick(item: Item.CosmeticOrEmote, category: WardrobeCategory, wardrobeState: WardrobeState, event: UIClickEvent) {
    wardrobeState.itemIdToCategoryMap[item.id] = category

    val options = getRightClickOptions(item, wardrobeState).get()

    if (options.isNotEmpty()) {
        ContextOptionMenu.create(
            ContextOptionMenu.Position(event.absoluteX, event.absoluteY),
            Window.of(event.currentTarget),
            *options.map { it() }.toTypedArray()
        )
    }
}

fun handleBundleLeftClick(
    item: Item.Bundle,
    category: WardrobeCategory,
    wardrobeState: WardrobeState,
) {
    if (item.id !in wardrobeState.unlockedBundles.get()) {
        USound.playButtonPress()

        if (wardrobeState.selectedItem.getUntracked() == item) {
            wardrobeState.selectedItem.set(null)
        } else {
            wardrobeState.selectedItem.set(item)
        }

        sendItemPreviewTelemetry(item, category, wardrobeState)
    }
}

fun handleBundleRightClick(item: Item.Bundle, wardrobeState: WardrobeState, event: UIClickEvent) {
    val options = getBundleRightClickOptions(item, wardrobeState).get()

    if (options.isNotEmpty()) {
        ContextOptionMenu.create(
            ContextOptionMenu.Position(event.absoluteX, event.absoluteY),
            Window.of(event.currentTarget),
            *options.map { it() }.toTypedArray()
        )
    }
}

fun hasCosmeticOrEmoteOptionsButton(item: Item.CosmeticOrEmote, wardrobeState: WardrobeState): State<Boolean> {
    return getRightClickOptions(item, wardrobeState).map { it.isNotEmpty() }
}

fun hasBundleOptionsButton(item: Item.Bundle, wardrobeState: WardrobeState): State<Boolean> {
    return getBundleRightClickOptions(item, wardrobeState).map { it.isNotEmpty() }
}

private fun getBundleRightClickOptions(item: Item.Bundle, wardrobeState: WardrobeState): State<List<() -> ContextOptionMenu.Item>> {
    val cost = item.getCost(wardrobeState).map { it ?: 0 }
    val unlocked = wardrobeState.unlockedBundles.map { item.id in it }

    fun purchaseOrClaim() {
        if (platform.disabledFeaturesManager.isFeatureDisabled(Feature.COSMETIC_PURCHASE)) {
            platform.pushModal { StoreDisabledModal(it) }
            return
        }

        wardrobeState.purchaseAndCreateOutfitForBundle(item, false) { success ->
            if (success) {
                EssentialSounds.playPurchaseConfirmationSound()
            } else {
                Notifications.push("Purchase failed", "Please try again later or contact our support.") {
                    type = NotificationType.ERROR
                    withCustomComponent(Slot.ICON, EssentialPalette.REPORT_10X7.create())
                }
            }
        }
    }

    return stateBy {
        val options = mutableListOf<() -> ContextOptionMenu.Item>()

        if (!unlocked()) {
            options.add {
                if (cost() > 0) {
                    ContextOptionMenu.Option("Purchase", EssentialPalette.SHOPPING_CART_8X7) {
                        if (!wardrobeState.hasEnoughCoins(item)) {
                            CoinsPurchaseModal.open(wardrobeState, item.getCost(wardrobeState).getUntracked())
                            return@Option
                        }

                        wardrobeState.openPurchaseItemModal(item) {
                            purchaseOrClaim()
                        }
                    }
                } else {
                    ContextOptionMenu.Option("Claim", EssentialPalette.PLUS_5X) {
                        purchaseOrClaim()
                    }
                }
            }
        }

        if (wardrobeState.cosmeticsManager.cosmeticsDataWithChanges != null) {
            options.add {
                ContextOptionMenu.Option("Edit", image = EssentialPalette.EDIT_10X7) {
                    wardrobeState.currentlyEditingCosmeticBundleId.set(item.id)
                }
            }

            options.add {
                ContextOptionMenu.Option("Copy ID", image = EssentialPalette.COPY_10X7) {
                    UDesktop.setClipboardString(item.id)
                }
            }
        }

        options
    }
}

private fun getRightClickOptions(item: Item.CosmeticOrEmote, wardrobeState: WardrobeState): State<List<() -> ContextOptionMenu.Item>> {
    val notOwned = wardrobeState.unlockedCosmetics.map { item.cosmetic.id !in it }
    val isPurchasable = item.isPurchasable
    val hasCost = item.cosmetic.priceCoins > 0
    val hasPurchaseOrClaimOption = notOwned.map { it && isPurchasable }
    val hasGiftOption = hasCost && isPurchasable
    return stateBy {
        val options = mutableListOf<() -> ContextOptionMenu.Item>()
        val showClaimOption = hasPurchaseOrClaimOption() && !hasCost
        val showPurchaseOption = hasPurchaseOrClaimOption() && hasCost

        if (showPurchaseOption) {
            options.add {
                ContextOptionMenu.Option("Purchase", image = EssentialPalette.SHOPPING_CART_8X7) {
                    if (!wardrobeState.hasEnoughCoins(item)) {
                        CoinsPurchaseModal.open(wardrobeState, item.getCost(wardrobeState).getUntracked())
                        return@Option
                    }
                    wardrobeState.openPurchaseItemModal(item) {
                        wardrobeState.purchaseCosmeticOrEmote(item) { success ->
                            if (success) {
                                EssentialSounds.playPurchaseConfirmationSound()
                            } else {
                                Notifications.push("Purchase failed", "Please try again later or contact our support.") {
                                    type = NotificationType.ERROR
                                    withCustomComponent(Slot.ICON, EssentialPalette.REPORT_10X7.create())
                                }
                            }
                        }
                    }
                }
            }
        } else if (showClaimOption) {
            options.add {
                ContextOptionMenu.Option("Claim", image = EssentialPalette.PLUS_5X) {
                    claimFreeItemNow(item, wardrobeState)
                }
            }
        }

        if ((showPurchaseOption || showClaimOption) && hasGiftOption) {
            options.add {
                ContextOptionMenu.Divider
            }
        }

        if (hasGiftOption) {
            options.add {
                ContextOptionMenu.Option("Gift to Friend", image = EssentialPalette.WARDROBE_GIFT_7X) {
                    openGiftModal(item, wardrobeState)
                }
            }
        }

        if (wardrobeState.cosmeticsManager.cosmeticsDataWithChanges != null) {
            options.add {
                ContextOptionMenu.Option("Edit", image = EssentialPalette.EDIT_10X7) {
                    wardrobeState.currentlyEditingCosmeticId.set(item.cosmetic.id)
                }
            }
            options.add {
                ContextOptionMenu.Option("Copy ID", image = EssentialPalette.COPY_10X7) {
                    UDesktop.setClipboardString(item.cosmetic.id)
                }
            }
        }
        options
    }
}

private fun sendItemPreviewTelemetry(item: Item, category: WardrobeCategory, wardrobeState: WardrobeState) {
    val type = if (item is Item.Bundle) "BUNDLE" else "COSMETIC"
    val featuredPageId = wardrobeState.featuredPageCollection.getUntracked()?.id ?: "DEFAULT"

    val (sourceType, source) = when (category) {
        is WardrobeCategory.FeaturedRefresh -> ("FEATURED" to "FEATURED")
        else -> ("CATEGORY" to category.fullName)
    }

    platform.enqueueTelemetry(
        ClientTelemetryPacket(
            "ITEM_PREVIEW_2",
            mapOf(
                "id" to item.id,
                "type" to type,
                "columnCount" to wardrobeState.getColumnCount(category).getUntracked(),
                "featuredPageId" to featuredPageId,
                "sourceType" to sourceType,
                "source" to source
            )
        )
    )
}

fun claimFreeItemNow(item: Item.CosmeticOrEmote, wardrobeState: WardrobeState) {
    if (platform.disabledFeaturesManager.isFeatureDisabled(Feature.COSMETIC_PURCHASE)) {
        platform.pushModal { StoreDisabledModal(it) }
        return
    }

    val cosmetic = item.cosmetic
    val cosmeticsManager = wardrobeState.cosmeticsManager
    wardrobeState.unlockedCosmetics.set { it + cosmetic.id }
    cosmeticsManager.claimFreeItems(setOf(cosmetic.id)).whenCompleteAsync({ success, throwable ->
        if (!success || throwable != null) {
            throwable?.printStackTrace()
            wardrobeState.unlockedCosmetics.set { it - cosmetic.id }
            Notifications.error("Error", "Failed to claim item.")
        } else {
            sendCosmeticUnlockedToast(cosmetic, wardrobeState.selectedPreviewingEquippedSettings.map { it[item.id] ?: listOf() })
            EssentialSounds.playPurchaseConfirmationSound()
        }
    }, Dispatchers.Client.asExecutor())
}

fun handleVariantHover(variant: CosmeticProperty.Variants.Variant, item: Item.CosmeticOrEmote, state: WardrobeState, hovered: Boolean) {
    val setting = CosmeticSetting.Variant(item.cosmetic.id, true, CosmeticSetting.Variant.Data(variant.name))
    state.previewingSetting.set { map ->
        if (hovered) map + (item.cosmetic.id to setting)
        else if (map[item.cosmetic.id] == setting) map - item.cosmetic.id
        else map
    }
}
