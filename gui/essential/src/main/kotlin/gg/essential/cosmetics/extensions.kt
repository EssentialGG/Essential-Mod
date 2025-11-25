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
package gg.essential.cosmetics

import gg.essential.gui.elementa.state.v2.ObservedInstant
import gg.essential.mod.cosmetics.featured.FeaturedPageCollection
import gg.essential.network.cosmetics.Cosmetic

fun Cosmetic.isAvailable(now: ObservedInstant) =
    (availableAfter?.let { now.isAfter(it) } ?: false) && (availableUntil?.let { now.isBefore(it) } ?: true)

fun FeaturedPageCollection.isAvailable(now: ObservedInstant) =
    availability?.let { now.isAfter(it.after) && now.isBefore(it.until) } ?: true
