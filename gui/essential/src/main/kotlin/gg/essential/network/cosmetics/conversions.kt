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
package gg.essential.network.cosmetics

import gg.essential.cosmetics.model.CosmeticStoreBundle
import gg.essential.cosmetics.model.CosmeticStoreBundleSkin
import gg.essential.lib.gson.Gson
import gg.essential.mod.EssentialAsset
import gg.essential.mod.Model
import gg.essential.mod.Skin
import gg.essential.mod.cosmetics.*
import gg.essential.mod.cosmetics.settings.CosmeticProperty
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.mod.cosmetics.settings.UntypedCosmeticSetting
import gg.essential.network.connectionmanager.coins.CoinBundle
import gg.essential.network.connectionmanager.skins.SkinItem
import gg.essential.skins.SkinModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Currency
import gg.essential.coins.model.CoinBundle as InfraCoinBundle
import gg.essential.cosmetics.CosmeticSlot as InfraCosmeticSlot
import gg.essential.cosmetics.SkinLayer as InfraSkinLayer
import gg.essential.cosmetics.model.Cosmetic as InfraCosmetic
import gg.essential.cosmetics.model.CosmeticCategory as InfraCosmeticCategory
import gg.essential.cosmetics.model.CosmeticOutfit as InfraCosmeticOutfit
import gg.essential.cosmetics.model.CosmeticSetting as InfraCosmeticSetting
import gg.essential.cosmetics.model.CosmeticTier as InfraCosmeticTier
import gg.essential.cosmetics.model.CosmeticType as InfraCosmeticType
import gg.essential.cosmetics.model.EmoteWheel as InfraEmoteWheel
import gg.essential.model.EssentialAsset as InfraEssentialAsset
import gg.essential.skins.model.Skin as InfraSkin

//
// Fundamental conversion methods between infra and mod types
//

fun CosmeticSlot.toInfra() = InfraCosmeticSlot.of(id)
fun InfraEmoteWheel.toMod() = EmoteWheelPage(id(), createdAt().toInstant(), slots().let {slots ->
    mutableListOf<String?>(*arrayOfNulls(8)).also {
        slots.forEach { (t, u) -> it[t] = u }
    }
})

fun InfraCosmeticSlot.toMod() = CosmeticSlot.of(id)

fun SkinLayer.toInfra() = InfraSkinLayer.valueOf(name)
fun InfraSkinLayer.toMod() = SkinLayer.valueOf(name)

fun UntypedCosmeticSetting.toInfra() = InfraCosmeticSetting(id, type, isEnabled, data)

fun CosmeticProperty.toInfra() = gson.fromJson(CosmeticProperty.json.encodeToString(this), InfraCosmeticSetting::class.java)

fun CosmeticSetting.toInfra() = gson.fromJson(CosmeticSetting.json.encodeToString(this), InfraCosmeticSetting::class.java)

fun InfraCosmeticSetting.toModSetting() = CosmeticSetting.fromJson(Json.encodeToString(toMod()))

fun InfraCosmeticSetting.toMod() = UntypedCosmeticSetting(id, type, isEnabled, data)

fun CosmeticType.toInfra() = InfraCosmeticType(id, slot.toInfra(), displayNames, skinLayers.toInfra())
fun InfraCosmeticType.toMod() = CosmeticType(id, slot.toMod(), displayNames, skinLayers?.toMod() ?: mapOf())

fun CosmeticStoreBundleSkin.toMod() = CosmeticBundle.Skin(Skin(hash, model.toMod()), name)

fun CosmeticStoreBundle.toMod() = CosmeticBundle(id, name, tier.toMod(), discount, rotateOnPreview, skin?.toMod(), cosmetics.toMod(), settings.toModSetting())

fun Model.toInfra() = when (this) {
    Model.STEVE -> SkinModel.CLASSIC
    Model.ALEX -> SkinModel.SLIM
}

fun SkinModel.toMod() = when (this) {
    SkinModel.CLASSIC -> Model.STEVE
    SkinModel.SLIM -> Model.ALEX
}

fun InfraSkin.toMod() = SkinItem(id, name, Skin(hash, model.toMod()), createdAt.toInstant(), lastUsedAt?.toInstant(), favoritedAt?.toInstant())

fun InfraCosmeticTier?.toMod() = when (this) {
    InfraCosmeticTier.COMMON -> CosmeticTier.COMMON
    InfraCosmeticTier.UNCOMMON -> CosmeticTier.UNCOMMON
    InfraCosmeticTier.RARE -> CosmeticTier.RARE
    InfraCosmeticTier.EPIC -> CosmeticTier.EPIC
    InfraCosmeticTier.LEGENDARY -> CosmeticTier.LEGENDARY
    else -> CosmeticTier.COMMON // Remove null option when removing flag
}

fun InfraCosmeticCategory.toMod() = CosmeticCategory(
    id,
    icon.toMod(),
    displayNames,
    compactNames,
    descriptions,
    slots?.map { it.toMod() }?.toSet() ?: emptySet(),
    tags ?: emptySet(),
    order,
    availableAfter?.toInstant(),
    availableUntil?.toInstant(),
)

fun InfraEssentialAsset.toMod() = EssentialAsset(url, checksum)

fun InfraCosmeticOutfit.toMod() = CosmeticOutfit(
    id,
    name,
    skinTexture?.let { Skin.fromInfra(it) },
    skinId,
    equippedCosmetics.mapKeys { it.key.toMod() }.toMutableMap(),
    cosmeticSettings.toModSetting().toMutableMap(),
    favoritedAt?.toInstant(),
    createdAt.toInstant(),
)

fun InfraCosmetic.toMod(type: CosmeticType, settings: List<CosmeticProperty>): Cosmetic {
    return Cosmetic(
        id,
        type,
        tier.toMod(),
        displayNames,
        assetsMap.mapValues { it.value.toMod() },
        settings,
        storePackageId,
        priceCoins?.let { mapOf("coins" to it.toDouble()) } ?: mapOf(),
        tags,
        createdAt.toInstant(),
        availableAfter?.toInstant(),
        availableUntil?.toInstant(),
        showTimerAfter?.toInstant(),
        skinLayers.toMod(),
        categories,
        defaultSortWeight ?: 20,
    )
}

fun InfraCoinBundle.toMod(currency: Currency) = CoinBundle(
    id,
    coins,
    currency,
    price,
    (extraPercent * 100).toInt(),
    icon.toMod(),
    isHighlighted,
    isExchangeBundle
)

//
// Utilities for converting common container types (added an demand)
//

@JvmName("propertiesToInfra") fun List<CosmeticProperty>.toInfra() = map { it.toInfra() }
@JvmName("settingsToInfra") fun List<CosmeticSetting>.toInfra() = map { it.toInfra() }
@JvmName("settingsToModSetting") fun List<InfraCosmeticSetting>.toModSetting() = map { it.toModSetting() }
@JvmName("settingsToModSetting") fun Map<String, List<InfraCosmeticSetting>>.toModSetting() = mapValues { it.value.toModSetting() }
@JvmName("skinLayersToMod") fun Map<InfraSkinLayer, Boolean>.toMod() = mapKeys { it.key.toMod() }
@JvmName("skinLayersToInfra") fun Map<SkinLayer, Boolean>.toInfra() = mapKeys { it.key.toInfra() }
@JvmName("outfitsToMod") fun List<InfraCosmeticOutfit>.toMod() = map { it.toMod() }
@JvmName("slotsToMod") fun Map<InfraCosmeticSlot, String>.toMod() = mapKeys { it.key.toMod() }

private val gson = Gson()
