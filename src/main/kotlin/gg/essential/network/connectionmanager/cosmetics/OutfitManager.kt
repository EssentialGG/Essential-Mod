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

import gg.essential.Essential
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ClientCosmeticOutfitCreatePacket
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ClientCosmeticOutfitCosmeticSettingsUpdatePacket
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ClientCosmeticOutfitDeletePacket
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ClientCosmeticOutfitEquippedCosmeticsUpdatePacket
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ClientCosmeticOutfitUpdateFavoriteStatePacket
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ClientCosmeticOutfitNameUpdatePacket
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ClientCosmeticOutfitSelectPacket
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ClientCosmeticOutfitSkinUpdatePacket
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ServerCosmeticOutfitPopulatePacket
import gg.essential.cosmetics.SkinId
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.clear
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.zip
import gg.essential.gui.elementa.state.v2.mutableListStateOf
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.onChange
import gg.essential.gui.elementa.state.v2.remove
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.gui.emotes.EmoteWheel
import gg.essential.gui.notification.Notifications
import gg.essential.handlers.GameProfileManager
import gg.essential.mod.Skin
import gg.essential.mod.cosmetics.CosmeticOutfit
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.OutfitSkin
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.network.connectionmanager.ConnectionManager
import gg.essential.network.connectionmanager.NetworkedManager
import gg.essential.network.connectionmanager.handler.cosmetics.ServerCosmeticOutfitPopulatePacketHandler
import gg.essential.network.connectionmanager.queue.PacketQueue
import gg.essential.network.connectionmanager.queue.SequentialPacketQueue
import gg.essential.network.cosmetics.toInfra
import gg.essential.universal.UMinecraft
import gg.essential.util.Multithreading
import gg.essential.util.UUIDUtil
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

class OutfitManager(
    val connectionManager: ConnectionManager,
    val cosmeticsManager: CosmeticsManager,
    val skins: State<Map<SkinId, Skin>>
) : NetworkedManager {

    private val mutableSelectedOutfitId: MutableState<String?> = mutableStateOf(null)
    val selectedOutfitId: State<String?> = mutableSelectedOutfitId
    private var sentOutfitId: String? = null

    private val mutableOutfits = mutableListStateOf<CosmeticOutfit>()
    val outfits = stateBy {
        val skins = skins()
        mutableOutfits().sortedWith(
            compareBy<CosmeticOutfit> { it.favoritedSince?.toEpochMilli() }
                .thenByDescending { it.createdAt.toEpochMilli() }
        ).map { item ->
            val skin = skins[item.skinId]
            item.copy(skin = if (skin == null) null else OutfitSkin(skin, true))
        }
    }.toListState()

    private val equippedOwnedCosmetics = stateBy {
        val selected = selectedOutfitId()
        val unlockedCosmetics = cosmeticsManager.unlockedCosmetics()
        outfits().find { it.id == selected }?.equippedCosmetics?.filter { it.value in unlockedCosmetics } ?: mapOf()
    }
    private val equippedOwnedCosmeticsSettings = stateBy {
        val selected = selectedOutfitId()
        val unlockedCosmetics = cosmeticsManager.unlockedCosmetics()
        val outfit = outfits().find { it.id == selected }
        outfit?.cosmeticSettings?.filter { it.key in unlockedCosmetics && it.key in outfit.equippedCosmetics.values } ?: mapOf()
    }
    private val equippedSkin = stateBy {
        val selected = selectedOutfitId()
        outfits().find { it.id == selected }?.skin?.skin
    }

    private var currentFlushOutfitTaskId = 0

    private val packetQueue: PacketQueue = SequentialPacketQueue.Builder(connectionManager)
        .onTimeoutRetransmit()
        .create()
    private val referenceHolder = ReferenceHolderImpl()

    init {
        connectionManager.registerPacketHandler(
            ServerCosmeticOutfitPopulatePacket::class.java, ServerCosmeticOutfitPopulatePacketHandler()
        )
        Essential.EVENT_BUS.register(this)

        equippedOwnedCosmetics.zip(equippedOwnedCosmeticsSettings).onChange(referenceHolder) { (cosmetics, settings) ->
            cosmeticsManager.equippedCosmeticsManager.update(UUIDUtil.getClientUUID(), cosmetics, settings)
        }
        equippedSkin.onSetValue(referenceHolder) { skin ->
            applyOutfitSkin(skin ?: return@onSetValue, true)
        }
    }

    override fun resetState() {
        super.resetState()
        this.packetQueue.reset()
        this.setSelectedOutfit(null as CosmeticOutfit?)
        this.sentOutfitId = null
        this.mutableOutfits.clear()
    }

    fun getOutfit(id: String): CosmeticOutfit? {
        return outfits.get().find { it.id == id }
    }

    fun getSelectedOutfit(): CosmeticOutfit? {
        return getOutfit(selectedOutfitId.get() ?: return null)
    }

    fun setSelectedOutfit(id: String?) {
        if (id == null) {
            return
        }
        val outfit = getOutfit(id)
        if (outfit == null) {
            Essential.logger.error("Unable to select outfit $id as it doesn't exist")
            return
        }
        setSelectedOutfit(outfit)
    }

    /**
     * Sets the given outfit as the active one.
     * This runs [flushSelectedOutfit] afterwards to save selected outfit to infra with debounce.
     * To save immediately use [flushSelectedOutfit] with debounce false after calling.
     *
     * @return The previously active outfit, if any
     */
    private fun setSelectedOutfit(outfit: CosmeticOutfit?): CosmeticOutfit? {
        val oldOutfit: CosmeticOutfit? = getSelectedOutfit()
        this.mutableSelectedOutfitId.set(outfit?.id)
        if (outfit == null) {
            return null
        }

        if (oldOutfit != null) {
            EmoteWheel.unequipCurrentEmote(oldOutfit)
        }
        flushSelectedOutfit(true)
        return oldOutfit
    }

    fun populateOutfits(outfits: List<CosmeticOutfit>) {
        mutableOutfits.set { list ->
            list.removeAll(list.filter { o -> outfits.any { it.id == o.id } }).addAll(outfits)
        }
    }

    fun populateSelection(selectedOutfitId: String?) {
        setSelectedOutfit(selectedOutfitId)
        sentOutfitId = selectedOutfitId
    }

    fun addOutfit(
        name: String = createNewOutfitName(),
        skinId: String = getSelectedOutfit()?.skinId ?: "",
        equippedCosmetics: Map<CosmeticSlot, String> = mapOf(),
        cosmeticSettings: Map<String, List<CosmeticSetting>> = mapOf(),
        callback: (outfit: CosmeticOutfit?) -> Unit,
    ) {
        if (skinId == "") {
            Essential.logger.error("Unable to get skin id for outfit creation.")
            callback(null)
            return
        }
        this.connectionManager.send(
            ClientCosmeticOutfitCreatePacket(
                name,
                skinId,
                equippedCosmetics.mapKeys { it.key.toInfra() },
                cosmeticSettings.mapValues { it.value.map { it.toInfra() } }
            ),
            { responsePacket ->
                if (!responsePacket.isPresent) {
                    Essential.debug.error("ClientCosmeticOutfitCreatePacket did not give a response")
                    callback(null)
                    return@send
                }
                val response = responsePacket.get()
                if (response !is ServerCosmeticOutfitPopulatePacket) {
                    Essential.debug.error("ClientCosmeticOutfitCreatePacket did not give a successful response")
                    callback(null)
                    return@send
                }

                val infraOutfits = response.outfits
                if (infraOutfits.isEmpty()) {
                    Essential.debug.error("ClientCosmeticOutfitCreatePacket gave an empty list")
                    callback(null)
                    return@send
                }
                callback(outfits.get().find { infraOutfits.first().id == it.id })
            },
            TimeUnit.SECONDS,
            20
        )
    }

    fun renameOutfit(id: String, name: String) {
        val outfit = getOutfit(id)
        if (outfit == null) {
            Essential.logger.error("Unable to rename outfit $id as it doesn't exist")
            return
        }
        editOutfit(outfit.copy(name = name))
        this.packetQueue.enqueue(ClientCosmeticOutfitNameUpdatePacket(outfit.id, name))
    }

    fun deleteOutfit(id: String) {
        val outfit = getOutfit(id)
        if (outfit == null) {
            Essential.logger.error("Unable to delete outfit $id as it doesn't exist")
            return
        }
        if (outfit.id == selectedOutfitId.get()) {
            val toBeSelected = outfits.get().firstOrNull { it.id != outfit.id }
            if (toBeSelected == null) {
                Notifications.push("Error deleting outfit", "You must have one or more outfits.")
                return
            }
            setSelectedOutfit(toBeSelected)
        }
        mutableOutfits.get().find { outfit.id == it.id }?.let { mutableOutfits.remove(it) }
        this.packetQueue.enqueue(ClientCosmeticOutfitDeletePacket(outfit.id))
    }

    fun setFavorite(id: String, favorite: Boolean) {
        val outfit = getOutfit(id) ?: return
        if (favorite == (outfit.favoritedSince != null)) {
            return
        }
        val now = Instant.now()
        editOutfit(outfit.copy(favoritedSince = if (favorite) now else null))
        this.packetQueue.enqueue(ClientCosmeticOutfitUpdateFavoriteStatePacket(outfit.id, favorite))
    }

    fun updateOutfitCosmeticSettings(outfitId: String, cosmeticId: String, settings: List<CosmeticSetting>) {
        val outfit = getOutfit(outfitId)
        if (outfit == null) {
            Essential.logger.error("Unable to update cosmetic settings for $outfitId")
            return
        }

        if ((outfit.cosmeticSettings[cosmeticId] ?: emptyList()) == settings) {
            return // Early return if the cosmetic settings didn't change
        }

        val cosmeticSettings = outfit.cosmeticSettings.toMutableMap()
        if (settings.isNotEmpty()) {
            cosmeticSettings[cosmeticId] = settings
        } else {
            cosmeticSettings.remove(cosmeticId)
        }
        editOutfit(outfit.copy(cosmeticSettings = cosmeticSettings))
        packetQueue.enqueue(ClientCosmeticOutfitCosmeticSettingsUpdatePacket(outfit.id, cosmeticId, settings.toInfra()))
    }

    fun updateEquippedCosmetic(outfitId: String, slot: CosmeticSlot, cosmeticId: String?) {
        val outfit = getOutfit(outfitId)
        if (outfit == null) {
            Essential.logger.error("Unable to update equipped cosmetic for $outfitId")
            return
        }

        val equippedCosmetics = outfit.equippedCosmetics.toMutableMap()
        if (cosmeticId != null) {
            equippedCosmetics[slot] = cosmeticId
        } else {
            equippedCosmetics.remove(slot)
        }

        editOutfit(outfit.copy(equippedCosmetics = equippedCosmetics))
        packetQueue.enqueue(ClientCosmeticOutfitEquippedCosmeticsUpdatePacket(outfit.id, slot.toInfra(), cosmeticId))
    }

    fun cleanUpUnusedSettingsOnOutfits() {
        for (outfit in outfits.get().toList()) {
            outfit.cosmeticSettings.filter { it.key !in outfit.equippedCosmetics.values }.forEach { (cosmeticId, _) ->
                updateOutfitCosmeticSettings(outfit.id, cosmeticId, emptyList())
            }
        }
    }

    fun updateOutfitSkin(outfitId: String, newSkinId: SkinId) {
        val outfit = getOutfit(outfitId) ?: return
        updateOutfitSkin(outfit, newSkinId)
    }

    private fun updateOutfitSkin(outfit: CosmeticOutfit, newSkinId: SkinId) {
        if (outfit.skinId == newSkinId) {
            return
        }
        editOutfit(outfit.copy(skinId = newSkinId))
        packetQueue.enqueue(ClientCosmeticOutfitSkinUpdatePacket(outfit.id, null, newSkinId))
    }

    fun updateOutfitSkin(skinId: SkinId, allProfiles: Boolean) {
        for (outfit in outfits.get()) {
            if (allProfiles || outfit.id == selectedOutfitId.get()) {
                updateOutfitSkin(outfit, skinId)
            }
        }
    }

    fun flushSelectedOutfit(debounce: Boolean) {
        val taskId = ++currentFlushOutfitTaskId
        if (debounce) {
            Multithreading.scheduleOnMainThread({
                if (taskId != currentFlushOutfitTaskId) {
                    return@scheduleOnMainThread
                }
                flushSelectedOutfit(false)
            }, 3, TimeUnit.SECONDS)
            return
        }
        val selectedOutfit = selectedOutfitId.get()
        if (selectedOutfit == null || selectedOutfit == sentOutfitId) {
            return
        }
        sentOutfitId = selectedOutfit
        this.packetQueue.enqueue(ClientCosmeticOutfitSelectPacket(selectedOutfit))
    }

    private fun applyOutfitSkin(skin: Skin, updateWithMojang: Boolean) {
        val gameProfileManager = Essential.getInstance().gameProfileManager
        val skinManager = Essential.getInstance().skinManager
        val model = skin.model
        val hash = skin.hash
        gameProfileManager.updatePlayerSkin(UUIDUtil.getClientUUID(), hash, model.type)
        if (updateWithMojang) {
            skinManager.changeSkin(
                UMinecraft.getMinecraft().session.token,
                model,
                String.format(Locale.ROOT, GameProfileManager.SKIN_URL, hash)
            )
        }
    }

    private fun createNewOutfitName(): String {
        val outfitNames = outfits.get().map { it.name }.toSet()
        return (outfitNames.size + 1..Int.MAX_VALUE).firstNotNullOf { i ->
            "Outfit #$i".takeUnless { it in outfitNames }
        }
    }

    private fun editOutfit(new: CosmeticOutfit) {
        mutableOutfits.set { list ->
            list.removeAll(list.filter { it.id == new.id }).add(new)
        }
    }

}