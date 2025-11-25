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
import gg.essential.connectionmanager.common.packet.Packet
import gg.essential.connectionmanager.common.packet.cosmetic.ClientCosmeticCheckoutPacket
import gg.essential.connectionmanager.common.packet.cosmetic.ServerCosmeticsUserUnlockedPacket
import gg.essential.cosmetics.isAvailable
import gg.essential.gui.common.sendCosmeticUnlockedToast
import gg.essential.gui.elementa.state.v2.ObservedInstant
import gg.essential.mod.Model
import gg.essential.mod.cosmetics.settings.CosmeticProperty
import gg.essential.network.connectionmanager.ConnectionManager
import gg.essential.network.connectionmanager.handler.PacketHandler
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.util.BuildInfo
import java.time.Instant

inline fun <reified T : Packet> ConnectionManager.registerPacketHandler(handler: PacketHandler<T>) =
    registerPacketHandler(T::class.java, handler)

fun primeCache(modelLoader: ModelLoader, assetLoader: AssetLoader, cosmetic: Cosmetic) {
    val variants = cosmetic.property<CosmeticProperty.Variants>()?.data?.variants?.map { it.name } ?: listOf("")
    var priority = AssetLoader.Priority.Background
    for (variant in variants) {
        val variantAssets = cosmetic.assets(variant)

        modelLoader.getModel(cosmetic, variant, Model.STEVE, priority)
        priority = AssetLoader.Priority.BackgroundUnlikely // by default wardrobe only shows initial variant+skinType
        modelLoader.getModel(cosmetic, variant, Model.ALEX, priority)

        variantAssets.soundDefinitions
            ?.let { assetLoader.getAsset(it, priority, AssetLoader.AssetType.SoundDefinitions) }
            ?.parsed
            ?.thenAccept { data ->
                for (definition in data.definitions.values) {
                    for (sound in definition.sounds) {
                        val asset = variantAssets.allFiles[sound.name + ".ogg"] ?: continue
                        assetLoader.getAssetBytes(asset, AssetLoader.Priority.BackgroundUnlikely)
                    }
                }
            }
    }
}

fun ConnectionManager.unlockSpsCosmetics() {
    // Current version of Minecraft (e.g. 1.21.4) as list of ints, [1, 21, 4]
    val currentVersionInts = listOf(
        BuildInfo.TARGET_MC_VERSION / 10000,
        (BuildInfo.TARGET_MC_VERSION % 10000) / 100,
        BuildInfo.TARGET_MC_VERSION % 100,
    )

    unlock<CosmeticProperty.RequiresUnlockAction.Data.JoinSps> { joinSPSProperty ->
        val requiredVersionInts = joinSPSProperty.requiredVersion?.split(".")?.map {
            it.toIntOrNull() ?: run {
                Essential.logger.warn("When unlocking SPS cosmetic the required version could not be parsed from {}", joinSPSProperty)
                return@unlock false
            }
        }?.take(3) ?: return@unlock true
        // Major version must match
        if (currentVersionInts.take(2) != requiredVersionInts.take(2)) return@unlock false
        // If no required minor version, that's it
        if (requiredVersionInts.size <= 2) return@unlock true
        // Otherwise, check minor version
        return@unlock (currentVersionInts.getOrNull(2) ?: 0) >= requiredVersionInts[2]
    }
}

fun ConnectionManager.unlockServerCosmetics(address: String) {
    unlock<CosmeticProperty.RequiresUnlockAction.Data.JoinServer> {  it.serverAddress.equals(address, ignoreCase = true)  }
}

private inline fun <reified T : CosmeticProperty.RequiresUnlockAction.Data> ConnectionManager.unlock(filter: (T) -> Boolean) {
    val now = ObservedInstant(Instant.now()) {}
    val toUnlock = cosmeticsManager.cosmetics.get()
        .filter { it.id !in cosmeticsManager.unlockedCosmetics.get() && it.isAvailable(now) }
        .filter { cosmetic -> cosmetic.properties<CosmeticProperty.RequiresUnlockAction>().mapNotNull { it.data as? T }.any(filter) }

    if (toUnlock.isEmpty()) return

    send(ClientCosmeticCheckoutPacket(toUnlock.map { it.id }.toSet())) { packetOptional ->
        val packet = packetOptional.orElse(null)
        if (packet !is ServerCosmeticsUserUnlockedPacket) {
            Essential.logger.error("Failed to unlock cosmetics: $packet")
        } else {
            for (unlockedCosmeticId in packet.unlockedCosmetics.keys) {
                sendCosmeticUnlockedToast(toUnlock.find { it.id == unlockedCosmeticId } ?: continue)
            }
        }
    }
}
