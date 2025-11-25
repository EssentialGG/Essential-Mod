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
package gg.essential.network.connectionmanager.features

import gg.essential.connectionmanager.common.packet.features.ServerDisabledFeaturesPacket
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.network.CMConnection
import gg.essential.network.connectionmanager.NetworkedManager
import gg.essential.network.registerPacketHandler
import org.slf4j.LoggerFactory

class DisabledFeaturesManager(val connectionManager: CMConnection) : NetworkedManager {

    private val mutableDisabledFeatures = mutableStateOf(listOf<Feature>())
    val disabledFeatures: State<List<Feature>> = mutableDisabledFeatures

    init {
        connectionManager.registerPacketHandler<ServerDisabledFeaturesPacket> { packet ->
            mutableDisabledFeatures.set(packet.disabledFeatures.mapNotNull { featureString ->
                try {
                    Feature.valueOf(featureString)
                } catch (e: IllegalArgumentException) {
                    LOGGER.error("Unknown disabled feature: $featureString")
                    null
                }
            })
        }
    }

    fun isFeatureDisabled(feature: Feature): Boolean {
        return disabledFeatures.getUntracked().contains(feature)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DisabledFeaturesManager::class.java)
    }

}