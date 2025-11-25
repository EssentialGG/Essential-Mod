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

import com.google.common.collect.MapMaker
import gg.essential.gui.elementa.state.v2.MutableSetState
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.add
import gg.essential.gui.elementa.state.v2.mutableSetState
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.remove
import gg.essential.mod.Skin
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.network.connectionmanager.cosmetics.CosmeticsData
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager.Outfit
import gg.essential.network.connectionmanager.cosmetics.StateBasedEquippedOutfitsManager
import java.util.UUID

class IngameEquippedOutfitsManager(
    cosmeticsData: CosmeticsData,
    private val triggerAnimationEvent: (UUID, CosmeticSlot, String) -> Unit,
) : EquippedOutfitsManager {
    private val managedPlayers: MutableSetState<UUID> = mutableSetState()
    private val equippedOutfits: MutableMap<UUID, Outfit> = mutableMapOf()
    private val equippedOutfitStates: MutableMap<UUID, MutableState<Outfit>> = MapMaker().weakValues().makeMap()

    private val managerImpl = StateBasedEquippedOutfitsManager(
        cosmeticsData,
        managedPlayers,
        ::getEquippedCosmeticsState,
    )

    override fun getEquippedCosmeticsState(playerId: UUID): State<Outfit> =
        equippedOutfitStates.getOrPut(playerId) { mutableStateOf(equippedOutfits[playerId] ?: Outfit.EMPTY) }

    override fun getVisibleCosmeticsState(playerId: UUID): State<Map<CosmeticSlot, EquippedCosmetic>> =
        managerImpl.getVisibleCosmeticsState(playerId)

    override fun getSkin(playerId: UUID): Skin? =
        managerImpl.getSkin(playerId)

    override fun getCapeHash(playerId: UUID): String? =
        managerImpl.getCapeHash(playerId)

    fun applyUpdates(list: List<Pair<UUID, List<Update>>>) {
        for ((uuid, updates) in list) {
            applyUpdates(uuid, updates)
        }
    }

    fun applyUpdates(uuid: UUID, updates: List<Update>) {
        var newOutfit = equippedOutfits[uuid] ?: Outfit.EMPTY
        var newCosmetics: MutableMap<CosmeticSlot, EquippedCosmeticId>? = null
        for (update in updates) {
            when (update) {
                is Update.Remove -> {
                    newOutfit = Outfit.EMPTY
                    newCosmetics = null
                }
                is Update.Cosmetic -> {
                    if (newCosmetics == null) newCosmetics = newOutfit.cosmetics.toMutableMap()
                    if (update.value != null) {
                        newCosmetics[update.slot] = update.value
                    } else {
                        newCosmetics.remove(update.slot)
                    }
                }
                is Update.Skin -> {
                    newOutfit = newOutfit.copy(skin = update.skin)
                }
                is Update.AnimationEvent -> {
                    triggerAnimationEvent(uuid, update.slot, update.event)
                }
            }
        }
        if (newCosmetics != null) {
            newOutfit = newOutfit.copy(cosmetics = newCosmetics)
        }
        if (newOutfit != Outfit.EMPTY) {
            managedPlayers.add(uuid)
            equippedOutfits[uuid] = newOutfit
            equippedOutfitStates[uuid]?.set(newOutfit)
        } else {
            managedPlayers.remove(uuid)
            equippedOutfits.remove(uuid)
            // Note: The entry MUST NOT be removed from the states map because downstream states are subscribed to it.
            //       It'll be cleaned up automatically as its entries become unused by virtue of having weak values.
            equippedOutfitStates[uuid]?.set(Outfit.EMPTY)
        }
    }

    sealed interface Update {
        data object Remove : Update

        data class Cosmetic(val slot: CosmeticSlot, val value: EquippedCosmeticId?) : Update
        data class Skin(val skin: gg.essential.mod.Skin?) : Update

        data class AnimationEvent(val slot: CosmeticSlot, val event: String) : Update
    }
}
