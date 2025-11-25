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
package gg.essential.gui.wardrobe

import gg.essential.api.gui.Slot
import gg.essential.connectionmanager.common.packet.checkout.ClientCheckoutCosmeticsPacket
import gg.essential.connectionmanager.common.packet.checkout.ClientCheckoutStoreBundlePacket
import gg.essential.connectionmanager.common.packet.cosmetic.ServerCosmeticsUserUnlockedPacket
import gg.essential.connectionmanager.common.packet.response.ResponseActionPacket
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.fullBodyRenderPreview
import gg.essential.gui.common.sendCosmeticUnlockedToast
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.wardrobe.modals.PurchaseConfirmationModal
import gg.essential.gui.wardrobe.modals.StoreDisabledModal
import gg.essential.mod.cosmetics.CosmeticOutfit
import gg.essential.network.connectionmanager.features.Feature
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import org.slf4j.LoggerFactory
import java.util.UUID

fun WardrobeState.hasEnoughCoins(item: Item): Boolean {
    return (item.getCost(this).get() ?: 0) <= coins.get()
}

fun WardrobeState.hasEnoughCoins(items: Set<Item>): Boolean {
    return getTotalCost(stateOf(items.toList()).toListState()).get() <= coins.get()
}

fun WardrobeState.purchaseCosmeticOrEmote(item: Item.CosmeticOrEmote, callback: (success: Boolean) -> Unit) {
    if (item.cosmetic.id in unlockedCosmetics.get()) {
        callback(false)
        return
    }
    purchaseCosmeticOrEmote(setOf(item), null, callback)
}

fun WardrobeState.purchaseAndCreateOutfitForBundle(
    item: Item.Bundle,
    changeSelectedOutfit: Boolean = true,
    callback: (success: Boolean) -> Unit
) {
    if (item.id in unlockedBundles.get()) {
        callback(false)
        return
    }
    if (!hasEnoughCoins(item)) {
        callback(false)
        return
    }

    if (outfitManager.outfits.get().size >= settings.outfitsLimit.get()) {
        Notifications.error(
            "Your outfit library is full!",
            "Delete an outfit to make space for purchasing this bundle."
        )
    }

    if (skinsManager.skins.get().size >= settings.skinsLimit.get()) {
        Notifications.error(
            "Your skin library is full!",
            "Delete a skin to make space for purchasing this bundle."
        )
    }

    fun purchaseStateUpdateCallback(success: Boolean) {
        if (bundlePurchaseInProgress.get() == item.id) {
            bundlePurchaseInProgress.set(null)
        }

        callback(success)
    }

    // Only send the bundle unlock packet if the user does not own all the cosmetics already
    if (!unlockedCosmetics.get().containsAll(item.cosmetics.values)) {
        ServerCosmeticsUserUnlockedPacket.suppressNotifications.addAll(item.cosmetics.values)
        bundlePurchaseInProgress.set(item.id)

        sendPurchaseBundlePacket(item) { success ->
            if (success) {
                platform.enqueueTelemetry(
                    ClientTelemetryPacket(
                        "COSMETIC_PURCHASE_2",
                        mapOf(
                            "source_type" to "FEATURED",
                            "source" to "FEATURED",
                            "item_id" to item.id,
                            "item_type" to "BUNDLE",
                            "columnCount" to getColumnCount(WardrobeCategory.FeaturedRefresh).getUntracked(),
                            "featuredPageId" to (featuredPageCollection.getUntracked()?.id ?: "DEFAULT")
                        )
                    )
                )

                createOutfitForBundle(item, changeSelectedOutfit) { purchaseStateUpdateCallback(it) }
            } else {
                ServerCosmeticsUserUnlockedPacket.suppressNotifications.removeAll(item.cosmetics.values.toSet())
                purchaseStateUpdateCallback(false)
            }
        }
    } else {
        createOutfitForBundle(item, changeSelectedOutfit) { purchaseStateUpdateCallback(it) }
    }
}

fun WardrobeState.purchaseSelectedBundle(callback: (success: Boolean) -> Unit) {
    selectedBundle.get()?.let { purchaseAndCreateOutfitForBundle(it, callback = callback) }
}

fun WardrobeState.purchaseEquippedCosmetics(callback: (success: Boolean) -> Unit) {
    purchaseCosmeticOrEmote(equippedCosmeticsPurchasable.get().toSet(), null, callback)
}

fun WardrobeState.purchaseSelectedEmote(callback: (success: Boolean) -> Unit) {
    selectedEmote.getUntracked()?.let { purchaseCosmeticOrEmote(it, callback) }
}

fun WardrobeState.giftCosmeticOrEmote(item: Item.CosmeticOrEmote, giftTo: UUID, callback: (success: Boolean, errorCode: String?) -> Unit) {
    purchaseCosmeticOrEmote(setOf(item), giftTo, callback)
}

fun WardrobeState.openPurchaseItemModal(item: Item, primaryAction: () -> Unit) {
    if (platform.disabledFeaturesManager.isFeatureDisabled(Feature.COSMETIC_PURCHASE)) {
        platform.pushModal { StoreDisabledModal(it) }
        return
    }

    if (item is Item.Bundle) {
        platform.pushModal { PurchaseConfirmationModal.forBundle(it, item, this, primaryAction) }
    } else if (item is Item.CosmeticOrEmote) {
        platform.pushModal { PurchaseConfirmationModal.forItem(it, item, this, primaryAction) }
    }
}

private fun WardrobeState.purchaseCosmeticOrEmote(items: Set<Item.CosmeticOrEmote>, giftTo: UUID?, callback: (success: Boolean) -> Unit) =
    purchaseCosmeticOrEmote(items, giftTo) { success, _ -> callback(success) }

private fun WardrobeState.purchaseCosmeticOrEmote(items: Set<Item.CosmeticOrEmote>, giftTo: UUID?, callback: (success: Boolean, errorCode: String?) -> Unit) {
    if (!hasEnoughCoins(items)) {
        callback(false, null)
        return
    }

    sendPurchaseCosmeticOrEmotePacket(items, giftTo) { success, errorCode ->
        if (success) {
            items.forEach { item ->
                if (giftTo == null) {
                    sendCosmeticUnlockedToast(item.cosmetic, selectedPreviewingEquippedSettings.map { it[item.id] ?: listOf() })
                }

                // FIXME: Albin has explicitly said that cosmetics will only have one category for now.
                //        https://discord.com/channels/887304453500325900/887708890127536128/1184453340982153246
                //        Also: This map is also not an ideal solution, but there's no point in doing a whole
                //        bunch of re-architecting for the sake of telemetry.
                val category = this.itemIdToCategoryMap.remove(item.id)
                val (sourceType, source) = when (category) {
                    is WardrobeCategory.FeaturedRefresh -> ("FEATURED" to "FEATURED")
                    null -> ("UNKNOWN" to "UNKNOWN")
                    else -> ("CATEGORY" to category.fullName)
                }

                platform.enqueueTelemetry(
                    ClientTelemetryPacket(
                        "COSMETIC_PURCHASE_2",
                        mapOf(
                            "source_type" to sourceType,
                            "source" to source,
                            "item_id" to item.id,
                            "item_type" to "COSMETIC",
                            "columnCount" to getColumnCount(category).getUntracked(),
                            "featuredPageId" to (featuredPageCollection.getUntracked()?.id ?: "DEFAULT")
                        )
                    )
                )
            }
        }

        callback(success, errorCode)
    }
}

private fun sendPurchaseCosmeticOrEmotePacket(items: Set<Item.CosmeticOrEmote>, giftTo: UUID?, callback: (success: Boolean, errorCode: String?) -> Unit) {
    val isGift = giftTo != null
    platform.cmConnection.send(
        ClientCheckoutCosmeticsPacket(items.map { it.id }.toSet(), giftTo)
    ) { packet ->
        if (!packet.isPresent) {
            LOGGER.error("ClientCheckoutCosmeticsPacket did not give a response")
            callback(false, null)
            return@send
        }
        val response = packet.get()
        if (!isGift) {
            if (response is ServerCosmeticsUserUnlockedPacket) {
                callback(true, null)
                return@send
            }
        } else if (response is ResponseActionPacket) {
            if (response.isSuccessful) {
                callback(true, null)
                return@send
            } else if (response.errorMessage != null) {
                callback(false, response.errorMessage)
                return@send
            }
        }
        LOGGER.error("ClientCheckoutCosmeticsPacket did not give a successful response")
        callback(false, null)
    }
}

private fun sendPurchaseBundlePacket(item: Item.Bundle, callback: (success: Boolean) -> Unit) {
    platform.cmConnection.send(ClientCheckoutStoreBundlePacket(item.id)) { packet ->
        if (!packet.isPresent) {
            LOGGER.error("ClientCheckoutStoreBundlePacket did not give a response")
            callback(false)
            return@send
        }
        val response = packet.get()
        if (response !is ServerCosmeticsUserUnlockedPacket) {
            LOGGER.error("ClientCheckoutStoreBundlePacket did not give a successful response")
            callback(false)
            return@send
        }
        callback(true)
    }
}

private fun WardrobeState.createOutfitForBundle(item: Item.Bundle, changeSelectedOutfit: Boolean = true, callback: (success: Boolean) -> Unit) {
    val bundleSkin = item.skin

    fun outfitCallback(outfit: CosmeticOutfit?) {
        if (outfit == null) {
            callback(false)
            return
        }

        Notifications.push("", "§f${item.name}§r bundle has been unlocked.") {
            val component = UIBlock(EssentialPalette.BUTTON).constrain {
                width = 28.pixels
                height = AspectConstraint()
            }

            component.layout {
                fullBodyRenderPreview(this@createOutfitForBundle, bundleSkin?.toMod(), item.cosmetics, item.settings, true)
            }

            withCustomComponent(Slot.SMALL_PREVIEW, component)
        }

        Notifications.push("Outfit created", "") {
            withCustomComponent(Slot.ICON, EssentialPalette.COSMETICS_10X7.create())
        }

        if (changeSelectedOutfit) {
            outfitManager.setSelectedOutfit(outfit.id)
        }

        callback(true)
    }
    // If there's no skin, setup outfit immediately, otherwise add the skin first
    if (bundleSkin == null) {
        outfitManager.addOutfit(item.name, equippedCosmetics = item.cosmetics, cosmeticSettings = item.settings, callback = ::outfitCallback)
    } else {
        skinsManager.addSkin(bundleSkin.name ?: skinsManager.getNextIncrementalSkinName(), bundleSkin.toMod())
            .whenComplete { skin, _ ->
                if (skin == null) {
                    callback(false)
                    return@whenComplete
                }

                outfitManager.addOutfit(item.name, skin.id, item.cosmetics, item.settings, ::outfitCallback)
            }
    }
}

private val LOGGER = LoggerFactory.getLogger("Essential Logger")
