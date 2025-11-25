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
package gg.essential.network.connectionmanager.social.handler

import gg.essential.connectionmanager.common.packet.social.ClientSocialDiscordRequestJoinServerResponsePacket
import gg.essential.connectionmanager.common.packet.social.SocialDiscordRequestJoinServerPacket
import gg.essential.handlers.discord.DiscordIntegration
import gg.essential.network.connectionmanager.ConnectionManager
import gg.essential.network.connectionmanager.handler.PacketHandler
import kotlinx.coroutines.launch

class SocialDiscordRequestJoinServerPacketHandler : PacketHandler<SocialDiscordRequestJoinServerPacket?>() {
    override fun onHandle(connectionManager: ConnectionManager, packet: SocialDiscordRequestJoinServerPacket) {
        connectionManager.connectionScope.launch {
            handle(connectionManager, packet)
        }
    }

    private suspend fun handle(connectionManager: ConnectionManager, packet: SocialDiscordRequestJoinServerPacket) {
        val target = packet.targetUUID
        val address = DiscordIntegration.getAddress(packet.secret) ?: return

        val accepted = DiscordIntegration.partyManager.shouldAllowUserToJoin(target)

        if (!accepted) return

        // If the target was accepted, let's add them to the list of invited users...
        val spsManager = connectionManager.spsManager
        spsManager.updateInvitedUsers(spsManager.getInvitedUsers() + target)

        connectionManager.call(ClientSocialDiscordRequestJoinServerResponsePacket(address))
            .inResponseTo(packet)
            .fireAndForget()
    }
}
