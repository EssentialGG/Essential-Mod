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
package gg.essential.network.connectionmanager.handler.upnp

import gg.essential.config.EssentialConfig
import gg.essential.connectionmanager.common.packet.upnp.ServerUPnPSessionInviteAddPacket
import gg.essential.gui.notification.sendSpsInviteNotification
import gg.essential.handlers.discord.DiscordIntegration
import gg.essential.network.connectionmanager.ConnectionManager
import gg.essential.network.connectionmanager.handler.PacketHandler
import gg.essential.util.Multithreading
import java.util.*
import java.util.concurrent.TimeUnit

class ServerUPnPSessionInviteAddPacketHandler : PacketHandler<ServerUPnPSessionInviteAddPacket>() {
    private val cooldowns = mutableSetOf<UUID>()

    override fun onHandle(connectionManager: ConnectionManager, packet: ServerUPnPSessionInviteAddPacket) {
        val hostUUID = packet.hostUUID
        val spsManager = connectionManager.spsManager

        val session = spsManager.getRemoteSession(hostUUID)
        if (session == null || !EssentialConfig.essentialEnabled) {
            return
        }

        // The host may indirectly invite the user to their server when the user requests to
        // join via the Discord Integration.
        if (DiscordIntegration.partyManager.shouldHideNotificationForHost(hostUUID)) {
            return
        }

        if (cooldowns.contains(hostUUID)) return
        cooldowns.add(hostUUID)
        Multithreading.scheduleOnMainThread({ cooldowns.remove(hostUUID) }, 7, TimeUnit.SECONDS)

        sendSpsInviteNotification(hostUUID)
    }
}