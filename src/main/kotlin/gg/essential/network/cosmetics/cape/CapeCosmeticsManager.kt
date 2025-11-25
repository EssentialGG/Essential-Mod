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
package gg.essential.network.cosmetics.cape

import gg.essential.Essential
import gg.essential.connectionmanager.common.packet.cosmetic.ClientCosmeticCheckoutPacket
import gg.essential.connectionmanager.common.packet.cosmetic.ServerCosmeticsUserUnlockedPacket
import gg.essential.connectionmanager.common.packet.cosmetic.capes.ClientCosmeticCapesUnlockedPacket
import gg.essential.connectionmanager.common.packet.response.ResponseActionPacket
import gg.essential.handlers.MinecraftGameProfileTexturesRefresher
import gg.essential.mod.cosmetics.CAPE_DISABLED_COSMETIC_ID
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.network.connectionmanager.ConnectionManager
import gg.essential.network.connectionmanager.cosmetics.CosmeticsManager
import gg.essential.util.Multithreading
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EnumPlayerModelParts
import java.util.concurrent.Semaphore


class CapeCosmeticsManager(
    private val connectionManager: ConnectionManager,
    private val cosmeticsManager: CosmeticsManager,
) {

    /**
     * Mutex which must be acquired while talking to Mojang to avoid two threads getting into a data race.
     * If synchronization on `this` is also required, this mutex must be acquired first.
     */
    private val mojangLock = Semaphore(1)

    private var cachedCapes: List<MojangCapeApi.Cape>? = null

    private var activeCape: String? = null
    private var queued: Upload? = null

    private data class Upload(val hash: String?)

    private fun fetchCapes(allowCache: Boolean) = (if (allowCache) cachedCapes else null)
        ?: MojangCapeApi.fetchCapes().also { capes ->
            synchronized(this) {
                cachedCapes = capes
                activeCape = capes.find { it.active }?.hash
            }
        }

    fun queueCape(hash: String?) = synchronized(this) {
        queued = Upload(hash)
    }

    fun flushCapeUpdates() {
        var cape: MojangCapeApi.Cape? = null

        mojangLock.acquire()
        try {
            val capes = fetchCapes(true)

            synchronized(this) {
                val upload = queued.also { queued = null } ?: return

                if (upload.hash == activeCape) {
                    return
                }

                cape = capes.find { it.hash == upload.hash }

                activeCape = cape?.hash
            }

            MojangCapeApi.putCape(cape?.id)
            Essential.logger.info("Updated Mojang cape to \"${cape?.name ?: "<none>"}\"")
            MinecraftGameProfileTexturesRefresher.updateTextures(activeCape, "CAPE")
        } catch (e: Throwable) {
            Essential.logger.error("Error enabling cape $cape at Mojang:", e)
        } finally {
            mojangLock.release()
        }
    }

    fun unlockMissingCapesAsync() {
        Multithreading.scheduledPool.execute {
            mojangLock.acquire()
            try {
                this.unlockMissingCapes()
            } catch (e: Throwable) {
                Essential.logger.error("Error loading capes from Mojang:", e)
            } finally {
                mojangLock.release()
            }
        }
        if (CAPE_DISABLED_COSMETIC_ID !in cosmeticsManager.unlockedCosmetics.get()) {
            val capesVisible = Minecraft.getMinecraft().gameSettings.isPlayerModelPartEnabled(EnumPlayerModelParts.CAPE)
            connectionManager.send(ClientCosmeticCheckoutPacket(setOf(CAPE_DISABLED_COSMETIC_ID))) { maybeReply ->
                val reply = maybeReply.orElse(null)
                if (reply !is ServerCosmeticsUserUnlockedPacket && !(reply is ResponseActionPacket && reply.isSuccessful)) {
                    Essential.logger.error("Failed to unlock $CAPE_DISABLED_COSMETIC_ID ($maybeReply).")
                    return@send
                }

                // This is either a new user, or they're migrating from the two-state cape system
                // FIXME: isNewInstallation can be unreliable (e.g. it'll be false if the user only accepts the tos on
                //        second boot, or crashes on first boot) but that's fine because we can forcefully migrate
                //        everyone after a day or so (when no more old clients are in use), this is just for the time
                //        until then
                val outfitManager = connectionManager.outfitManager
                if (Essential.getInstance().isNewInstallation) {
                    // If they are new and currently have capes disabled, then we'll equip the Cape Disabled
                    // cosmetic in their active outfit to keep it that way.
                    if (!capesVisible) {
                        val selectedOutfit = outfitManager.getSelectedOutfit()
                        if (selectedOutfit != null) {
                            outfitManager.updateEquippedCosmetic(selectedOutfit.id, CosmeticSlot.CAPE, CAPE_DISABLED_COSMETIC_ID)
                        }
                    }
                } else {
                    // Otherwise, they're likely migrating from the two-state cape system
                    // In this case, we'll reset all their capes (and thereby implicitly their cape-visibility).
                    // We can't really guess what they really want it to be set to and have determined that resetting
                    // will likely result in the least amount of confusion/annoyance over all.
                    for (outfit in outfitManager.outfits.get()) {
                        outfitManager.updateEquippedCosmetic(outfit.id, CosmeticSlot.CAPE, null)
                    }
                }
            }
        }
    }

    private fun unlockMissingCapes() {
        val capes = fetchCapes(false)
        val missing = capes.filter { it.hash !in cosmeticsManager.unlockedCosmetics.get() }
        if (missing.isEmpty()) {
            return // no capes yet to unlock, nothing to do
        }

        // Fetching signatures requires changing the active cape, we need to revert that later
        val originallyActive = capes.find { it.active }
        var active = originallyActive

        val signatures = mutableListOf<Pair<String, String>>()
        for (cape in missing) {
            // If the cape is not currently active, we need to activate it cause that's the only way to get a signature
            if (cape != active) {
                MojangCapeApi.putCape(cape.id)
                // Wait a second just in case it needs to propagate on Mojang's end (and in case there's a rate limit)
                Thread.sleep(1000)
                active = cape
            }
            // Fetch a signature for this cape
            signatures.add(MojangCapeApi.fetchCurrentTextures())
        }

        // If we had to change the active cape on Mojang's end, revert it to what the user expects
        if (originallyActive != active) {
            MojangCapeApi.putCape(originallyActive?.id)
        }

        // Finally, send an unlock request to the backend
        connectionManager.send(ClientCosmeticCapesUnlockedPacket(signatures.toMap())) { reply ->
            if ((reply.orElse(null) as ResponseActionPacket?)?.isSuccessful != true) {
                Essential.logger.warn("Backend failed to unlock capes ($reply):")
                for ((cape, proof) in missing.zip(signatures)) {
                    Essential.logger.warn("  - ${cape.name}:")
                    Essential.logger.warn("      Id: ${cape.id}")
                    Essential.logger.warn("      Url: ${cape.url}")
                    Essential.logger.warn("      Proof of ownership: ${proof.first}")
                    Essential.logger.warn("      Signature: ${proof.second}")
                }
            }
        }
    }

    //#if MC<11700
    private fun net.minecraft.client.settings.GameSettings.isPlayerModelPartEnabled(part: EnumPlayerModelParts): Boolean =
        part in modelParts
    //#endif
}