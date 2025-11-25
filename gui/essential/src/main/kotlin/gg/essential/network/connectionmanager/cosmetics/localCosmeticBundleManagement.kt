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
package gg.essential.network.connectionmanager.cosmetics

import gg.essential.cosmetics.CosmeticBundleId
import gg.essential.cosmetics.CosmeticId
import gg.essential.mod.Skin
import gg.essential.mod.cosmetics.CosmeticBundle
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.CosmeticTier
import gg.essential.mod.cosmetics.settings.CosmeticSetting

fun CosmeticsDataWithChanges.registerBundle(
    id: CosmeticBundleId,
    name: String,
    tier: CosmeticTier,
    discount: Float,
    rotateOnPreview: Boolean,
    skin: CosmeticBundle.Skin?,
    cosmetics: Map<CosmeticSlot, CosmeticId>,
    settings: Map<CosmeticId, List<CosmeticSetting>>,
) {
    if (getCosmeticBundle(id) != null) {
        throw IllegalArgumentException("A bundle with the ID $id already exists")
    }
    updateBundle(
        id,
        CosmeticBundle(
            id,
            name,
            tier,
            discount,
            rotateOnPreview,
            skin,
            cosmetics,
            settings
        )
    )
}

private fun CosmeticsDataWithChanges.updateBundle(bundleId: CosmeticBundleId, func: CosmeticBundle.() -> CosmeticBundle) {
    getCosmeticBundle(bundleId)!!.let { oldBundle ->
        val newBundle = oldBundle.func()
        updateBundle(bundleId, newBundle)
    }
}

fun CosmeticsDataWithChanges.resetBundle(bundleId: CosmeticBundleId) {
    updateBundle(bundleId, inner.getCosmeticBundle(bundleId))
}
