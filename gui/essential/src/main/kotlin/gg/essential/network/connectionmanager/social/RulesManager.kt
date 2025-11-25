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
package gg.essential.network.connectionmanager.social

import gg.essential.connectionmanager.common.packet.response.ResponseActionPacket
import gg.essential.connectionmanager.common.packet.social.ClientCommunityRulesAgreedPacket
import gg.essential.connectionmanager.common.packet.social.ServerCommunityRulesStatePacket
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.letState
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.network.CMConnection
import gg.essential.network.connectionmanager.NetworkedManager
import gg.essential.network.registerPacketHandler

class RulesManager(private val connectionManager: CMConnection) : NetworkedManager {

    private var rules = mutableStateOf<Rules?>(null)
    private val communityRules = rules.letState { it?.rules ?: listOf() }.memo()
    val isLoaded: State<Boolean> = rules.letState { it != null }
    val hasRules: State<Boolean> = communityRules.letState { it.isNotEmpty() }

    var acceptedRules = false
        private set

    init {
        connectionManager.registerPacketHandler<ServerCommunityRulesStatePacket> { packet ->
            rules.set(Rules(packet.rules))
            acceptedRules = packet.accepted
        }
    }

    override fun onConnected() {
        super.onConnected()

        if (connectionManager.usingProtocol < REQUIRED_PROTOCOL) {
            rules.set(Rules())
        }
    }

    suspend fun acceptRules(): Boolean {
        acceptedRules = connectionManager.call(ClientCommunityRulesAgreedPacket()).exponentialBackoff().await<ResponseActionPacket>().isSuccessful
        return acceptedRules
    }

    @Override
    override fun resetState() {
        rules.set(null)
        acceptedRules = false
    }

    fun rules(preferredLocale: String): State<List<String>> = State {
        communityRules().mapNotNull { it[preferredLocale] ?: it["en_us"] }
    }

    private data class Rules(val rules: List<Map<String, String>> = listOf())

    private companion object {

        const val REQUIRED_PROTOCOL = 9
    }
}
