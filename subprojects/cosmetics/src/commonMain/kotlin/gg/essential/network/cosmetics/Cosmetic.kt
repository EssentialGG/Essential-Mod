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
@file:UseSerializers(InstantAsMillisSerializer::class)

package gg.essential.network.cosmetics

import gg.essential.mod.EssentialAsset
import gg.essential.mod.Model
import gg.essential.mod.cosmetics.CosmeticAssets
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.CosmeticTier
import gg.essential.mod.cosmetics.CosmeticType
import gg.essential.mod.cosmetics.SkinLayer
import gg.essential.mod.cosmetics.database.LOCAL_PATH
import gg.essential.mod.cosmetics.settings.CosmeticProperty
import gg.essential.mod.cosmetics.settings.CosmeticPropertyType
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.mod.cosmetics.settings.CosmeticSettings
import gg.essential.mod.cosmetics.settings.variant
import gg.essential.model.Side
import gg.essential.model.util.Instant
import gg.essential.model.util.InstantAsMillisSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers

@Serializable
data class Cosmetic(
    val id: String,
    val type: CosmeticType,
    val tier: CosmeticTier,
    val displayNames: Map<String, String>,
    val files: Map<String, EssentialAsset>,
    val allProperties: List<CosmeticProperty>,
    val storePackageId: Int,
    val prices: Map<String, Double>,
    val tags: Set<String>,
    val createdAt: Instant,
    val availableAfter: Instant?,
    val availableUntil: Instant?,
    val showTimerAfter: Instant?,
    val skinLayers: Map<SkinLayer, Boolean>,
    val categories: Map<String, Int>,
    val defaultSortWeight: Int,
    val diagnostics: List<Diagnostic>? = null, // null means unknown/loading, empty list means no issues
) {
    val baseAssets: CosmeticAssets
    val assetVariants: Map<String, CosmeticAssets>
    init {
        val base = mutableMapOf<String, EssentialAsset>()
        val variants = mutableMapOf<String, MutableMap<String, EssentialAsset>>()
        for ((path, asset) in files) {
            val prefix = "variants/"
            if (path.startsWith(prefix)) {
                val delim = path.indexOf("/", prefix.length)
                if (delim < 0) continue
                val variantName = path.substring(prefix.length, delim)
                val variantPath = path.substring(delim + 1)
                variants.getOrPut(variantName, ::mutableMapOf)[variantPath] = asset
            } else {
                base[path] = asset
            }
        }
        baseAssets = CosmeticAssets(base)
        assetVariants = variants.mapValues { (_, files) -> CosmeticAssets(base + files) }
    }

    fun assets(variant: String): CosmeticAssets = assetVariants[variant] ?: baseAssets
    fun assets(settings: CosmeticSettings) = assets(settings.variant ?: defaultVariantName)

    val properties: List<CosmeticProperty> = allProperties.filter { @Suppress("DEPRECATION") it.enabled }
    val disabledProperties: List<CosmeticProperty> = allProperties.filterNot { @Suppress("DEPRECATION") it.enabled }

    val displayName: String
        get() = displayNames["en_us"] ?: id

    val localPath: String
        get() = displayNames[LOCAL_PATH] ?: "<$id>"

    val isLegacy: Boolean
        get() = "LEGACY" in tags

    val priceCoinsNullable = prices["coins"]?.toInt()

    val priceCoins = priceCoinsNullable ?: 0

    val isPurchasable = priceCoinsNullable != null && !requiresUnlockAction()

    // Rename to isFree when removing flag
    val isCosmeticFree = priceCoinsNullable == 0

    val defaultSide: Side?
        get() = property<CosmeticProperty.DefaultSide>()?.data?.side

    val mutuallyExclusiveWith: Set<CosmeticSlot>
        get() = property<CosmeticProperty.MutuallyExclusive>()?.data?.slots ?: emptySet()

    // Added some convenient variants values, especially convenient in Java
    val variants: List<CosmeticProperty.Variants.Variant>?
        get() = property<CosmeticProperty.Variants>()?.data?.variants

    val defaultVariant: CosmeticProperty.Variants.Variant?
        get() = variants?.firstOrNull()

    val defaultVariantName: String
        get() = defaultVariant?.name ?: ""

    val defaultVariantSetting: CosmeticSetting.Variant?
        get() = defaultVariant?.let { CosmeticSetting.Variant(id, true, CosmeticSetting.Variant.Data(it.name)) }

    fun getDisplayName(locale: String): String? {
        return displayNames[locale]
    }

    fun getPrice(currency: String): Double? {
        return prices[currency]
    }

    /**
     * Returns true if the cosmetic requires a specific action to unlock separate from the standard
     * purchase flow.
     */
    fun requiresUnlockAction(): Boolean {
        return properties.any { it.type == CosmeticPropertyType.REQUIRES_UNLOCK_ACTION }
    }

    private fun tagValue(key: String) =
        tags.firstNotNullOfOrNull { if (it.startsWith(key)) it.substring(key.length) else null }

    @Transient
    val partnerCreator: Boolean = "PARTNER" in tags

    @Transient
    val partnerMod: String? = tagValue("mod:")

    @Transient
    val partnerEvent: String? = tagValue("event:")

    @Transient
    val isPartnered: Boolean = (partnerCreator || partnerMod != null || partnerEvent != null) && "NON_PARTNER" !in tags

    @Transient
    val partnerName: String? =
        property<CosmeticProperty.Localization>()?.data?.en_US
            ?: partnerMod
            ?: (if (partnerCreator) "cosmetic.$id.partnername" else null)
            ?: partnerEvent?.let { "studio.$it" }

    @Transient
    val emoteInterruptionTriggers: CosmeticProperty.InterruptsEmote.Data =
        property<CosmeticProperty.InterruptsEmote>()?.data
            ?: CosmeticProperty.InterruptsEmote.Data()


    inline fun <reified T : CosmeticProperty> property(): T? {
        return properties.firstNotNullOfOrNull { it as? T }
    }

    inline fun <reified T : CosmeticProperty> properties(): List<T> {
        return properties.filterIsInstance<T>()
    }

    @Serializable
    data class Diagnostic(
        val type: Type,
        val message: String,
        val stacktrace: String? = null,
        val file: String? = null,
        val lineColumn: Pair<Int, Int>? = null,
        val variant: String? = null,
        val skin: Model? = null,
    ) {
        @Serializable
        enum class Type {
            Fatal,
            Error,
            Warning,
        }

        companion object {
            fun fatal(
                message: String,
                stacktrace: String? = null,
                file: String? = null,
                lineColumn: Pair<Int, Int>? = null,
                variant: String? = null,
                skin: Model? = null,
            ) = Diagnostic(Type.Fatal, message, stacktrace, file, lineColumn, variant, skin)

            fun error(
                message: String,
                stacktrace: String? = null,
                file: String? = null,
                lineColumn: Pair<Int, Int>? = null,
                variant: String? = null,
                skin: Model? = null,
            ) = Diagnostic(Type.Error, message, stacktrace, file, lineColumn, variant, skin)

            fun warning(
                message: String,
                stacktrace: String? = null,
                file: String? = null,
                lineColumn: Pair<Int, Int>? = null,
                variant: String? = null,
                skin: Model? = null,
            ) = Diagnostic(Type.Warning, message, stacktrace, file, lineColumn, variant, skin)
        }
    }
}
