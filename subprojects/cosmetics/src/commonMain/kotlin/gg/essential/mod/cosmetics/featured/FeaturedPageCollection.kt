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
@file:UseSerializers(InstantAsIso8601Serializer::class)

package gg.essential.mod.cosmetics.featured

import gg.essential.cosmetics.FeaturedPageCollectionId
import gg.essential.cosmetics.FeaturedPageWidth
import gg.essential.model.util.Instant
import gg.essential.model.util.InstantAsIso8601Serializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class FeaturedPageCollection(
    val id: FeaturedPageCollectionId,
    val availability: Availability? = null,
    val pages: Map<FeaturedPageWidth, FeaturedPage>
) {

    /**
     * This is used by the featured category page and the Wardrobe width modifier to get the most appropriate layout for the available columns.
     *
     * It gets the layout entry whose width is closest to the desired one, prioritizing smaller ones, otherwise picking at most one bigger
     */
    fun getClosestLayoutOrNull(desiredColumns: Int): Map.Entry<FeaturedPageWidth, FeaturedPage>? =
        pages.entries
            .filter { it.key <= desiredColumns + 1 }
            .minByOrNull {
                when {
                    it.key == desiredColumns -> 0
                    it.key < desiredColumns -> desiredColumns - it.key
                    else -> it.key - desiredColumns + 100 // Make sure pages with smaller width get picked first
                }
            }

    @Serializable
    data class Availability(val after: Instant, val until: Instant, @SerialName("show_timer_after") val showTimerAfter: Instant? = null)
}
