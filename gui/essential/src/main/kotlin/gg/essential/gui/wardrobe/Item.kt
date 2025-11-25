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

import gg.essential.cosmetics.CosmeticId
import gg.essential.cosmetics.SkinId
import gg.essential.mod.cosmetics.CosmeticBundle
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.state.Sale
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.CosmeticTier
import gg.essential.mod.cosmetics.featured.FeaturedItem
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.network.cosmetics.Cosmetic
import java.awt.Color
import java.time.Instant
import kotlin.math.roundToInt

sealed interface Item {
    val id: String
    val itemId: ItemId
    val name: String
    val tier: Tier
    val isFavorite: Boolean
    val isPurchasable: Boolean

    fun getPricingInfo(wardrobeState: WardrobeState): State<PricingInfo?> {
        return stateOf(null)
    }

    fun getCost(wardrobeState: WardrobeState): State<Int?> = getPricingInfo(wardrobeState).map { it?.realCost }

    data class CosmeticOrEmote(val cosmetic: Cosmetic, val settingsOverride: List<CosmeticSetting> = emptyList()) : Item {
        override val id: String
            get() = cosmetic.id
        override val itemId: ItemId
            get() = ItemId.CosmeticOrEmote(id)
        override val name: String
            get() = cosmetic.getDisplayName("en_us") ?: cosmetic.id
        override val tier: Tier
            get() = cosmetic.tier.toItemTier()
        override val isFavorite: Boolean
            get() = false
        override val isPurchasable: Boolean
            get() = cosmetic.isPurchasable

        override fun getPricingInfo(wardrobeState: WardrobeState): State<PricingInfo?> {
            return stateBy {
                getPricingInfoInternal(wardrobeState.saleState())
            }
        }

        // Helper method to allow calling from bundle price calculation without wrapping it into a state
        fun getPricingInfoInternal(sales: List<Sale>): PricingInfo? {
            val basePrice: Int = cosmetic.priceCoinsNullable ?: return null
            if (basePrice == 0) return PricingInfo(0, 0, 0, 0)

            val saleDiscountPercent: Int? = sales.filter { cosmetic in it }.maxByOrNull { it.discountPercent }?.discountPercent
            if (saleDiscountPercent == null) {
                return PricingInfo(basePrice, basePrice, basePrice, 0)
            }
            // Verbose, for clarity
            val discountMultiplier: Float = (1f - (saleDiscountPercent / 100f))
            val realPriceExact: Float = basePrice * discountMultiplier
            val realPriceRounded: Int = StrictMath.round(realPriceExact)
            val realPriceFloored: Int = realPriceRounded - (realPriceRounded % 10)
            val realDiscountMultiplier: Float = realPriceFloored.toFloat() / basePrice.toFloat()
            val realDiscountPercent: Int = ((1f - realDiscountMultiplier) * 100f).roundToInt()
            return PricingInfo(basePrice, realPriceFloored, realPriceRounded, realDiscountPercent)
        }
    }

    data class SkinItem(
        override val id: SkinId,
        override val name: String,
        val skin: gg.essential.mod.Skin,
        val createdAt: Instant,
        val lastUsedAt: Instant?,
        val favoritedSince: Instant?,
    ) : Item {
        override val itemId: ItemId
            get() = ItemId.SkinItem(id)
        override val tier: Tier
            get() = Tier.Common
        override val isFavorite: Boolean
            get() = favoritedSince != null
        override val isPurchasable: Boolean
            get() = false
    }

    data class OutfitItem(
        override val id: String,
        override val name: String,
        val skinId: SkinId,
        val skin: gg.essential.mod.Skin,
        val cosmetics: Map<CosmeticSlot, CosmeticId>,
        val settings: Map<CosmeticId, List<CosmeticSetting>>,
        val createdAt: Instant,
        val favoritedSince: Instant?,
    ) : Item {
        override val itemId: ItemId
            get() = ItemId.OutfitItem(id)
        override val tier: Tier
            get() = Tier.Common
        override val isFavorite: Boolean
            get() = favoritedSince != null
        override val isPurchasable: Boolean
            get() = false
    }

    data class Bundle(
        override val id: String,
        override val name: String,
        override val tier: Tier,
        val discountPercent: Float,
        val rotateOnPreview: Boolean,
        val skin: CosmeticBundle.Skin?,
        val cosmetics: Map<CosmeticSlot, CosmeticId>,
        val settings: Map<CosmeticId, List<CosmeticSetting>>,
    ) : Item {
        override val itemId: ItemId
            get() = ItemId.Bundle(id)
        override val isFavorite: Boolean
            get() = false
        override val isPurchasable: Boolean
            get() = true

        override fun getPricingInfo(wardrobeState: WardrobeState): State<PricingInfo?> {
            return stateBy {
                val sales = wardrobeState.saleState()
                val allCosmetics = wardrobeState.rawCosmetics()
                val unlockedCosmetics = wardrobeState.unlockedCosmetics()
                val nonUnlockedCosmeticPricingInfos = cosmetics.values.filter { it !in unlockedCosmetics }.mapNotNull { cosmeticId ->
                    allCosmetics.find { it.id == cosmeticId }?.let { CosmeticOrEmote(it).getPricingInfoInternal(sales) }
                }

                val basePrice: Int = nonUnlockedCosmeticPricingInfos.sumOf { it.baseCost }
                if (basePrice == 0) return@stateBy PricingInfo(0, 0, 0, 0)

                val baseRealPrice: Int = nonUnlockedCosmeticPricingInfos.sumOf { it.realCostNonFloored } // We don't double floor

                // Verbose, for clarity
                val bundleDiscountPercent: Float = discountPercent
                val discountMultiplier: Float = (1f - (bundleDiscountPercent / 100f))
                val realPriceExact: Float = baseRealPrice * discountMultiplier
                val realPriceRounded: Int = StrictMath.round(realPriceExact)
                val realPriceFloored: Int = realPriceRounded - (realPriceRounded % 50)
                val totalDiscountMultiplier: Float = realPriceFloored.toFloat() / basePrice.toFloat()
                val totalDiscountPercent: Int = ((1f - totalDiscountMultiplier) * 100f).roundToInt()
                return@stateBy PricingInfo(basePrice, realPriceFloored, realPriceRounded, totalDiscountPercent)
            }
        }
    }

    enum class Tier(
        val barColor: Color,
        val saveTagColor: Color,
    ) {
        Common(Color(0x757575), Color(0x757575)),
        Uncommon(Color(0x1C7C35), Color(0x20953F)),
        Rare(Color(0x185695), Color(0x0C76E3)),
        Epic(Color(0x7B25A4), Color(0xA115E3)),
        Legendary(Color(0xA46823), Color(0xD78100)),
    }

    data class PricingInfo(val baseCost: Int, val realCost: Int, val realCostNonFloored: Int, val discountPercentage: Int)

    companion object {
        fun CosmeticTier.toItemTier(): Tier {
            return when (this) {
                CosmeticTier.COMMON -> Tier.Common
                CosmeticTier.UNCOMMON -> Tier.Uncommon
                CosmeticTier.RARE -> Tier.Rare
                CosmeticTier.EPIC -> Tier.Epic
                CosmeticTier.LEGENDARY -> Tier.Legendary
                else -> Tier.Common
            }
        }

        fun CosmeticBundle.toItem(): Bundle {
            return Bundle(
                id,
                name,
                tier.toItemTier(),
                discountPercent,
                rotateOnPreview,
                skin,
                cosmetics,
                settings
            )
        }

        fun FeaturedItem.toModItem(state: WardrobeState): State<Item?> {
            return when (this) {
                is FeaturedItem.Cosmetic -> state.rawCosmetics.map { cosmetics -> cosmetics.firstOrNull { it.id == cosmetic }?.let { CosmeticOrEmote(it, settings) } }
                is FeaturedItem.Bundle -> state.rawBundles.map { bundleItem -> bundleItem.firstOrNull { it.id == bundle }?.toItem() }
                is FeaturedItem.Empty -> stateOf(null)
            }
        }

    }

}

sealed interface ItemId {
    val id: String

    data class CosmeticOrEmote(override val id: String): ItemId
    data class SkinItem(override val id: String): ItemId
    data class Bundle(override val id: String): ItemId
    data class OutfitItem(override val id: String): ItemId
}
