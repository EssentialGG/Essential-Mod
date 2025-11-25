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

import gg.essential.cosmetics.EquippedCosmetic
import gg.essential.cosmetics.EquippedCosmeticId
import gg.essential.gui.elementa.state.v2.State
import gg.essential.mod.Skin
import gg.essential.mod.cosmetics.CosmeticSlot
import java.util.*

interface EquippedOutfitsManager {
    fun getEquippedCosmeticsState(playerId: UUID): State<Outfit>
    fun getVisibleCosmeticsState(playerId: UUID): State<Map<CosmeticSlot, EquippedCosmetic>>
    fun getSkin(playerId: UUID): Skin? // must be thread-safe
    fun getCapeHash(playerId: UUID): String? // must be thread-safe

    data class Outfit(
        val cosmetics: Map<CosmeticSlot, EquippedCosmeticId>,
        val skin: Skin?,
    ) {
        companion object {
            val EMPTY: Outfit = Outfit(emptyMap(), null)
        }
    }
}