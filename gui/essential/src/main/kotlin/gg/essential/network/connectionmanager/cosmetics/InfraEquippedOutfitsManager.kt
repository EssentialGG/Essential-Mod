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

import com.google.common.collect.MapMaker
import gg.essential.config.EssentialConfig
import gg.essential.connectionmanager.common.packet.cosmetic.ServerCosmeticPlayerSettingsPacket
import gg.essential.connectionmanager.common.packet.cosmetic.ServerCosmeticsSkinTexturePacket
import gg.essential.connectionmanager.common.packet.cosmetic.ServerCosmeticsUserEquippedPacket
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ClientCosmeticOutfitSelectedRequestPacket
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ServerCosmeticOutfitSelectedResponsePacket
import gg.essential.cosmetics.CosmeticId
import gg.essential.cosmetics.EquippedCosmetic
import gg.essential.cosmetics.EquippedCosmeticId
import gg.essential.elementa.state.v2.ReferenceHolder
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.mod.Skin
import gg.essential.mod.cosmetics.CAPE_DISABLED_COSMETIC_ID
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.settings.CosmeticSettings
import gg.essential.network.CMConnection
import gg.essential.network.connectionmanager.NetworkedManager
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager.Outfit
import gg.essential.network.connectionmanager.subscription.SubscriptionManager
import gg.essential.network.cosmetics.toMod
import gg.essential.network.cosmetics.toModSetting
import gg.essential.network.registerPacketHandler
import gg.essential.util.USession
import java.util.*

class InfraEquippedOutfitsManager(
    private val connectionManager: CMConnection,
    private val subscriptionManager: SubscriptionManager,
    private val queueOwnMojangCape: (String) -> Unit,
    cosmeticsData: CosmeticsData,
    private val infraCosmeticsData: InfraCosmeticsData,
    private val applyCapeModelPartEnabled: (Boolean) -> Unit,
) : EquippedOutfitsManager, NetworkedManager, SubscriptionManager.Listener {
    private val refHolder: ReferenceHolder = ReferenceHolderImpl()
    private val infraOutfits: MutableMap<UUID, InfraOutfit> = mutableMapOf()
    private val infraOutfitStates: MutableMap<UUID, MutableState<InfraOutfit>> = MapMaker().weakValues().makeMap()

    private val equippedCosmeticsStateCache: MutableMap<UUID, State<Outfit>> = MapMaker().weakValues().makeMap()

    private val managerImpl = StateBasedEquippedOutfitsManager(
        cosmeticsData,
        subscriptionManager.subscriptionsAndSelf,
        ::getEquippedCosmeticsState,
    )

    private val ownUuid: UUID
        get() = USession.activeNow().uuid

    init {
        subscriptionManager.addListener(this)

        EssentialConfig.disableCosmeticsState.onSetValue(refHolder) { cosmeticsDisabled: Boolean ->
            if (cosmeticsDisabled) {
                return@onSetValue
            }
            val capeHash = infraOutfits[ownUuid]?.cosmetics?.get(CosmeticSlot.CAPE)
            // Configure MC's cape visibility setting accordingly
            applyCapeModelPartEnabled(CAPE_DISABLED_COSMETIC_ID != capeHash)
        }

        connectionManager.registerPacketHandler<ServerCosmeticsUserEquippedPacket> { packet ->
            val oldOutfit = infraOutfits[packet.uuid] ?: InfraOutfit.EMPTY
            update(packet.uuid, oldOutfit.copy(cosmetics = packet.equipped.toMod()))
        }
        connectionManager.registerPacketHandler<ServerCosmeticPlayerSettingsPacket> { packet ->
            val oldOutfit = infraOutfits[packet.uuid] ?: InfraOutfit.EMPTY
            update(packet.uuid, oldOutfit.copy(settings = packet.settings.toModSetting()))
        }
        connectionManager.registerPacketHandler<ServerCosmeticsSkinTexturePacket> { packet ->
            val oldOutfit = infraOutfits[packet.uuid] ?: InfraOutfit.EMPTY
            val skin = packet.skinTexture?.let { Skin.fromInfra(it) }
            update(packet.uuid, oldOutfit.copy(skin = skin))
        }
        connectionManager.registerPacketHandler<ServerCosmeticOutfitSelectedResponsePacket> { packet ->
            val equippedCosmetics = packet.equippedCosmetics ?: emptyMap()
            val cosmeticSettings = packet.cosmeticSettings ?: emptyMap()
            val skin = packet.skinTexture?.let { Skin.fromInfra(it) }
            update(packet.uuid, InfraOutfit(equippedCosmetics.toMod(), cosmeticSettings.toModSetting(), skin))
        }
    }

    fun getEquippedCosmetics(): Outfit {
        return getEquippedCosmetics(ownUuid)
    }

    fun getEquippedCosmetics(playerId: UUID): Outfit {
        return getEquippedCosmeticsState(playerId).getUntracked()
    }

    override fun getEquippedCosmeticsState(playerId: UUID): State<Outfit> {
        return equippedCosmeticsStateCache.getOrPut(playerId) {
            infraOutfitStates.getOrPut(playerId) { mutableStateOf(InfraOutfit.EMPTY) }.map { infraOutfit ->
                Outfit(
                    infraOutfit.cosmetics.mapValues { (_, id) ->
                        EquippedCosmeticId(id, infraOutfit.settings[id] ?: emptyList())
                    },
                    infraOutfit.skin,
                )
            }
        }
    }

    override fun getVisibleCosmeticsState(playerId: UUID): State<Map<CosmeticSlot, EquippedCosmetic>> =
        managerImpl.getVisibleCosmeticsState(playerId)

    override fun getSkin(playerId: UUID): Skin? =
        managerImpl.getSkin(playerId)

    override fun getCapeHash(playerId: UUID): String?=
        managerImpl.getCapeHash(playerId)

    fun update(playerId: UUID, outfit: InfraOutfit) {
        if (subscriptionManager.isSubscribedOrSelf(playerId)) {
            infraCosmeticsData.requestCosmeticsIfMissing(outfit.cosmetics.values)

            infraOutfits[playerId] = outfit
            infraOutfitStates[playerId]?.set(outfit)

            if (playerId == ownUuid) {
                val capeHash = outfit.cosmetics[CosmeticSlot.CAPE]
                val capeDisabled = CAPE_DISABLED_COSMETIC_ID == capeHash
                if (!EssentialConfig.disableCosmetics) {
                    // Configure MC's cape visibility setting accordingly
                    applyCapeModelPartEnabled(!capeDisabled)
                }

                // And queue the cape to be updated at Mojang
                if (!capeDisabled && capeHash != null) {
                    queueOwnMojangCape(capeHash)
                }
            }
        }
    }

    override fun resetState() {
        // handled via onSubscriptionRemoved
    }

    override fun onSubscriptionAdded(uuids: Set<UUID>) {
        for (uuid in uuids) {
            connectionManager.call(ClientCosmeticOutfitSelectedRequestPacket(uuid))
                .fireAndForget()
        }
    }

    override fun onSubscriptionRemoved(uuids: Set<UUID>) {
        for (uuid in uuids) {
            infraOutfits.remove(uuid)
            // Note: The entry MUST NOT be removed from the infraOutfitStates map because downstream states won't
            //       necessarily be re-created on reconnect but should still continue to receive future updates.
            //       It'll be cleaned up automatically as its entries become unused by virtue of having weak values.
            infraOutfitStates[uuid]?.set(InfraOutfit.EMPTY)
        }
    }

    data class InfraOutfit(
        val cosmetics: Map<CosmeticSlot, CosmeticId>,
        val settings: Map<CosmeticId, CosmeticSettings>, // may include settings for cosmetics not presently equipped
        val skin: Skin?,
    ) {
        companion object {
            val EMPTY = InfraOutfit(emptyMap(), emptyMap(), null)
        }
    }
}
