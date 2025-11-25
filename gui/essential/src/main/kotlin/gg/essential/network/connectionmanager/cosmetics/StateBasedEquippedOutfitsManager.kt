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

import com.google.common.collect.ImmutableMap
import com.google.common.collect.MapMaker
import gg.essential.config.EssentialConfig
import gg.essential.cosmetics.EquippedCosmetic
import gg.essential.elementa.state.v2.ReferenceHolder
import gg.essential.gui.elementa.state.v2.Observer
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.SetState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.elementa.state.v2.mapEach
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.toList
import gg.essential.mod.Skin
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager.Outfit
import gg.essential.util.associateNotNull
import java.util.*

class StateBasedEquippedOutfitsManager(
    private val cosmeticsData: CosmeticsData,
    managedPlayers: SetState<UUID>,
    private val getEquippedCosmeticsStateFunc: (UUID) -> State<Outfit>,
) : EquippedOutfitsManager {
    private val refHolder: ReferenceHolder = ReferenceHolderImpl()

    override fun getEquippedCosmeticsState(playerId: UUID): State<Outfit> =
        getEquippedCosmeticsStateFunc(playerId)

    private val visibleCosmeticsStateCache: MutableMap<UUID, State<Map<CosmeticSlot, EquippedCosmetic>>> = MapMaker().weakValues().makeMap()
    override fun getVisibleCosmeticsState(playerId: UUID): State<Map<CosmeticSlot, EquippedCosmetic>> {
        return visibleCosmeticsStateCache.getOrPut(playerId) {
            val outfitState = getEquippedCosmeticsState(playerId)
            memo { computeVisibleCosmetics(outfitState()) }
        }
    }

    private fun Observer.computeVisibleCosmetics(outfit: Outfit): Map<CosmeticSlot, EquippedCosmetic> {
        val cosmeticsHidden = EssentialConfig.disableCosmeticsState()
        val emotesHidden = EssentialConfig.disableEmotesState()

        fun isVisible(slot: CosmeticSlot): Boolean {
            if (slot == CosmeticSlot.ICON) {
                return true
            }

            if (cosmeticsHidden && slot != CosmeticSlot.EMOTE) {
                return false
            }

            if (slot == CosmeticSlot.EMOTE && emotesHidden) {
                return false
            }

            return true
        }

        val builder = ImmutableMap.Builder<CosmeticSlot, EquippedCosmetic>()

        for ((slot, value) in outfit.cosmetics) {
            if (!isVisible(slot)) continue
            val cosmetic = cosmeticsData.cosmetics().find { it.id == value.id } ?: continue
            builder.put(slot, EquippedCosmetic(cosmetic, value.settings))
        }

        return builder.build()
    }

    // Note: `getSkin` must be thread-safe, and therefore cannot evaluate State, so we have to
    //       proactively maintain a thread-safe (because immutable) map
    // TODO Would be more efficient to use a ConcurrentHashMap here.
    //      Though we are lacking an efficient way to flatten the nested States in the ListState, so we have to
    //      re-construct the whole flattened list on any change regardless, so may as well just construct a Map.
    private var skins = emptyMap<UUID, Skin>()
    init {
        val skinStates = managedPlayers.toList().mapEach { uuid ->
            uuid to getEquippedCosmeticsState(uuid).map { it.skin }
        }
        effect(refHolder) {
            skins = skinStates().associateNotNull { (uuid, skinState) ->
                val skin = skinState() ?: return@associateNotNull null
                uuid to skin
            }
        }
    }
    override fun getSkin(playerId: UUID): Skin? = skins[playerId]

    // Similar to getSkin, getCapeHash must also be thread safe as it's called from the same place
    private var capes = emptyMap<UUID, String>()
    init {
        val capeStates = managedPlayers.toList().mapEach { uuid ->
            uuid to getEquippedCosmeticsState(uuid).map { it.cosmetics[CosmeticSlot.CAPE]?.id }
        }
        effect(refHolder) {
            capes = capeStates().associateNotNull { (uuid, capeState) ->
                val cape = capeState() ?: return@associateNotNull null
                uuid to cape
            }
        }
    }
    override fun getCapeHash(playerId: UUID): String? = capes[playerId]
}
