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
package gg.essential.network.connectionmanager.skins

import gg.essential.connectionmanager.common.packet.skin.ClientSkinCreatePacket
import gg.essential.connectionmanager.common.packet.skin.ClientSkinDeletePacket
import gg.essential.connectionmanager.common.packet.skin.ClientSkinUpdateDataPacket
import gg.essential.connectionmanager.common.packet.skin.ClientSkinUpdateFavoriteStatePacket
import gg.essential.connectionmanager.common.packet.skin.ClientSkinUpdateLastUsedStatePacket
import gg.essential.connectionmanager.common.packet.skin.ClientSkinUpdateNamePacket
import gg.essential.connectionmanager.common.packet.skin.ServerSkinPopulatePacket
import gg.essential.cosmetics.SkinId
import gg.essential.gui.common.modal.DangerConfirmationEssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.overlay.ModalManager
import gg.essential.mod.Model
import gg.essential.mod.Skin
import gg.essential.network.CMConnection
import gg.essential.network.connectionmanager.NetworkedManager
import gg.essential.network.connectionmanager.cosmetics.OutfitManager
import gg.essential.network.connectionmanager.queue.SequentialPacketQueue
import gg.essential.network.cosmetics.toInfra
import gg.essential.network.cosmetics.toMod
import gg.essential.network.registerPacketHandler
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.CompletableFuture

class SkinsManager(private val connectionManager: CMConnection) : NetworkedManager {

    private val packetQueue = SequentialPacketQueue.Builder(connectionManager).onTimeoutSkip().create()

    // Actual data
    private val mutableSkins = mutableStateOf<Map<SkinId, SkinItem>>(mapOf())

    // Derived data
    val skins: State<Map<SkinId, SkinItem>> = mutableSkins
    val skinsOrdered = mutableSkins.map { skins ->
        skins.values.sortedWith(
            compareBy<SkinItem> { it.favoritedSince?.toEpochMilli() }
                .thenByDescending { (it.lastUsedAt ?: it.createdAt).toEpochMilli() }
        )
    }.toListState()

    private val skinsOrderedByLastUsed = mutableSkins.map { skins ->
        skins.values.sortedByDescending { (it.lastUsedAt ?: it.createdAt).toEpochMilli()  }
    }.toListState()

    init {
        connectionManager.registerPacketHandler<ServerSkinPopulatePacket> { packet ->
            mutableSkins.set { map -> map + packet.skins.associate { it.id to it.toMod() } }
        }
    }


    override fun onConnected() {
        // Infra sends us a packet with all the skins when we connect
        resetState()
    }

    override fun resetState() {
        mutableSkins.set { mapOf() }
    }

    fun getSkin(id: SkinId) = skins.map { it[id] }

    fun getNextIncrementalSkinName(): String {
        val skinNames = skins.get().map { it.value.name }.toSet()
        return (skinNames.size + 1 .. Int.MAX_VALUE).firstNotNullOf { i ->
            "Skin #$i".takeUnless { it in skinNames }
        }
    }

    fun addSkin(name: String, skin: Skin, favorite: Boolean = false): CompletableFuture<SkinItem> {
        return connectionManager.connectionScope.async {
            val request = ClientSkinCreatePacket(name, skin.model.toInfra(), skin.hash, favorite)
            val response = connectionManager.call(request)
                .await<ServerSkinPopulatePacket>()
                ?: throw IOException("Got no reply for $request")
            // We don't add the skin here, since the packet handler adds it already
            response.skins.first().toMod()
        }.asCompletableFuture()
    }

    fun openDeleteSkinModal(skinId: SkinId, outfitManager: OutfitManager) {
        val skin = skins.getUntracked()[skinId] ?: return
        if (outfitManager.outfits.getUntracked().any { it.skinId == skinId }) {
            Notifications.error("Can’t delete skin", "Skin is currently used on one or more outfits.")
            return
        }
        val manager = platform.createModalManager()
        manager.queueModal(
            DeleteSkinModal(manager, skin.name).onPrimaryAction { deleteSkin(skinId) }
        )
    }

    private fun deleteSkin(skinId: SkinId) {
        connectionManager.connectionScope.launch {
            val success = connectionManager.call(ClientSkinDeletePacket(skinId)).awaitResponseActionPacket()
            if (success) {
                mutableSkins.set { it - skinId }
            } else {
                Notifications.push("Error deleting skin", "An unexpected error has occurred. Try again.")
            }
        }
    }

    fun setFavoriteState(skinId: SkinId, favorite: Boolean) {
        packetQueue.enqueue(ClientSkinUpdateFavoriteStatePacket(skinId, favorite)) { maybeResponse ->
            val response = maybeResponse.orElse(null)
            // We don't replace the skin here, since the packet handler already replaces it
            if (response !is ServerSkinPopulatePacket) {
                Notifications.push("Error updating skin", "An unexpected error has occurred. Try again.")
            }
        }
    }

    fun renameSkin(skinId: SkinId, name: String) {
        packetQueue.enqueue(ClientSkinUpdateNamePacket(skinId, name)) { maybeResponse ->
            val response = maybeResponse.orElse(null)
            // We don't replace the skin here, since the packet handler already replaces it
            if (response !is ServerSkinPopulatePacket) {
                Notifications.push("Error updating skin", "An unexpected error has occurred. Try again.")
            }
        }
    }

    fun updateLastUsedAtState(skinId: SkinId) {
        // Skip update if the skin is already the most recently used
        if (skinsOrderedByLastUsed.getUntracked().first().id == skinId) return
        packetQueue.enqueue(ClientSkinUpdateLastUsedStatePacket(skinId)) { maybeResponse ->
            val response = maybeResponse.orElse(null)
            // We don't replace the skin here, since the packet handler already replaces it
            if (response !is ServerSkinPopulatePacket) {
                Notifications.push("Error updating skin", "An unexpected error has occurred. Try again.")
            }
        }
    }

    fun setSkinModel(skinId: SkinId, model: Model) {
        val skin = skins.getUntracked()[skinId] ?: return
        packetQueue.enqueue(ClientSkinUpdateDataPacket(skinId, model.toInfra(), skin.skin.hash)) { maybeResponse ->
            val response = maybeResponse.orElse(null)
            // We don't replace the skin here, since the packet handler already replaces it
            if (response !is ServerSkinPopulatePacket) {
                Notifications.push("Error updating skin", "An unexpected error has occurred. Try again.")
            }
        }
    }

    class DeleteSkinModal(manager: ModalManager, name: String) : DangerConfirmationEssentialModal(manager, "Delete", false) {
        init {
            configure { contentText = "Are you sure you want to delete\n${name}?" }
        }
    }

}
