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

package gg.essential.mod.cosmetics

import gg.essential.mod.EssentialAsset
import gg.essential.model.util.Instant
import gg.essential.model.util.InstantAsMillisSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class CosmeticCategory(
    val id: String,
    val icon: EssentialAsset,
    val displayNames: Map<String, String>,
    val compactNames: Map<String, String>,
    val descriptions: Map<String, String>,
    val slots: Set<CosmeticSlot>,
    val tags: Set<String>,
    val order: Int,
    val availableAfter: Instant? = null,
    val availableUntil: Instant? = null,
) {
    fun isEmoteCategory() = tags.contains(EMOTE_CATEGORY_TAG)

    fun isHidden() = tags.contains(HIDDEN_CATEGORY_TAG)

    companion object {
        const val EMOTE_CATEGORY_TAG = "EMOTE"
        const val HIDDEN_CATEGORY_TAG = "HIDDEN"
    }

}
