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
package gg.essential.mod.cosmetics

import gg.essential.cosmetics.CosmeticId
import gg.essential.mod.Model
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import kotlinx.serialization.Serializable

@Serializable
data class CosmeticBundle(
    val id: String,
    val name: String,
    val tier: CosmeticTier,
    val discountPercent: Float,
    val rotateOnPreview: Boolean = false,
    var skin: Skin? = null,
    val cosmetics: Map<CosmeticSlot, CosmeticId>,
    val settings: Map<CosmeticId, List<CosmeticSetting>>,
) {
    @Serializable
    data class Skin(
        val hash: String,
        val model: Model,
        val name: String? = null
    ) {

        constructor(skin: gg.essential.mod.Skin, name: String? = null) : this(skin.hash, skin.model, name)

        fun toMod() = gg.essential.mod.Skin(hash, model)

    }

}
