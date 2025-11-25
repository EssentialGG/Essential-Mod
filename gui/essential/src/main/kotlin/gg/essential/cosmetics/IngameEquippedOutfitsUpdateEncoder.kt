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

import gg.essential.cosmetics.IngameEquippedOutfitsManager.Update
import gg.essential.gui.elementa.state.v2.collections.TrackedList
import gg.essential.gui.elementa.state.v2.collections.trackedListOf
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager.Outfit
import java.util.*

/**
 * Keeps track of which outfits have previously been sent and computes delta-updates to be delivered to
 * [IngameEquippedOutfitsManager.applyUpdates] to bring it up-to-date with the given current outfits list.
 */
class IngameEquippedOutfitsUpdateEncoder {
    private var sentOutfits = mutableMapOf<UUID, Outfit>()
    private var lastState: TrackedList<Pair<UUID, Outfit>> = trackedListOf()

    fun update(newState: TrackedList<Pair<UUID, Outfit>>): List<Pair<UUID, List<Update>>> {
        if (lastState === newState) return emptyList()

        val changes = newState.getChangesSince(lastState).also { lastState = newState }

        val changedUuids = mutableMapOf<UUID, Outfit>()
        for (change in changes) {
            when (change) {
                is TrackedList.Add -> changedUuids[change.element.value.first] = change.element.value.second
                is TrackedList.Remove -> changedUuids[change.element.value.first] = Outfit.EMPTY
                is TrackedList.Clear -> change.oldElements.forEach { changedUuids[it.first] = Outfit.EMPTY }
            }
        }

        val perPlayerUpdates = mutableListOf<Pair<UUID, List<Update>>>()
        for ((uuid, newOutfit) in changedUuids) {
            val oldOutfit = sentOutfits[uuid] ?: Outfit.EMPTY
            if (newOutfit == Outfit.EMPTY) {
                if (oldOutfit != Outfit.EMPTY) {
                    perPlayerUpdates.add(uuid to listOf(Update.Remove))
                    sentOutfits.remove(uuid)
                }
                continue
            }

            val updates = mutableListOf<Update>()

            for ((slot, value) in newOutfit.cosmetics) {
                if (oldOutfit.cosmetics[slot] != value) {
                    updates.add(Update.Cosmetic(slot, value))
                }
            }
            for (slot in oldOutfit.cosmetics.keys) {
                if (slot !in newOutfit.cosmetics) {
                    updates.add(Update.Cosmetic(slot, null))
                }
            }

            if (oldOutfit.skin != newOutfit.skin) {
                updates.add(Update.Skin(newOutfit.skin))
            }

            perPlayerUpdates.add(uuid to updates)
            sentOutfits[uuid] = newOutfit
        }
        return perPlayerUpdates
    }
}