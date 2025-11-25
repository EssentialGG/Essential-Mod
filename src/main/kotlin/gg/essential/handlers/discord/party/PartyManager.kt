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
package gg.essential.handlers.discord.party

import gg.essential.Essential
import gg.essential.api.gui.NotificationType
import gg.essential.api.gui.Slot
import gg.essential.connectionmanager.common.packet.social.SocialDiscordRequestJoinServerPacket
import gg.essential.gui.EssentialPalette
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.content.ConfirmDenyNotificationActionComponent
import gg.essential.gui.notification.markdownBody
import gg.essential.util.Client
import gg.essential.util.MinecraftUtils
import gg.essential.util.UUIDUtil
import gg.essential.util.UuidNameLookup
import gg.essential.util.colored
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PartyManager(private val scope: CoroutineScope) {
    private val hostsToHideNotifications = mutableListOf<UUID>()

    /**
     * Tells the current client to join a party
     *
     * @param joinSecret The join secret received. This is in the following format: "mode|...data"
     *                   - SPS: "sps|hostUUID|key"
     *                   - Multiplayer: "multiplayer|serverAddress"
     */
    suspend fun joinParty(joinSecret: String) {
        val data = joinSecret.split("|")
        when (val mode = data[0]) {
            "sps" -> {
                if (data.size != 3) {
                    Essential.logger.error("Invalid amount of arguments for mode: sps ($joinSecret). Expected 3, got ${data.size}.")
                }

                joinSPS(data[1], joinSecret)
            }

            "multiplayer" -> {
                if (data.size != 2) {
                    Essential.logger.error("Invalid amount of arguments for mode: multiplayer ($joinSecret). Expected 2, got ${data.size}.")
                }

                joinMultiplayer(data[1])
            }

            else -> Essential.logger.error("Unknown party mode: $mode ($joinSecret)")
        }
    }

    fun shouldHideNotificationForHost(uuid: UUID) = hostsToHideNotifications.contains(uuid)

    /**
     * Returns whether the [target] is allowed to join the world or not.
     *
     * If the [target] is blocked by the user, false will be returned.
     * If the [target] is friends with the user, true will be returned.
     *
     * If the [target] is not blocked by the user, but is also not friends with the user, the user will be asked
     * for confirmation on weather or not this user should be allowed to join.
     */
    suspend fun shouldAllowUserToJoin(target: UUID): Boolean {
        val relationshipManager = Essential.getInstance().connectionManager.relationshipManager

        // If the target is blocked by the user...
        if (relationshipManager.isBlockedByMe(target)) {
            // We don't want to do anything, deny the request.
            return false
        }

        // If the target and the user are friends...
        if (relationshipManager.isFriend(target)) {
            // We don't need any further confirmation, accept the request.
            return true
        }

        // If the target is not blocked, but the target isn't friends with the user, let's ask the user for confirmation.
        val username = UuidNameLookup.getName(target).asDeferred().await()
        val accepted = suspendCoroutine { continuation ->
            Notifications.push("Discord Join Request", "") {
                type = NotificationType.DISCORD
                var resumed = false
                onClose = { if (!resumed) continuation.resume(false); resumed = true }
                val component = ConfirmDenyNotificationActionComponent(
                    confirmTooltip = "Accept",
                    denyTooltip = "Decline",
                    confirmAction = { if (!resumed) continuation.resume(true); resumed = true },
                    denyAction = { if (!resumed) continuation.resume(false); resumed = true },
                    dismissNotification = dismissNotification,
                )
                timerEnabled = component.timerEnabledState
                withCustomComponent(Slot.ACTION, component)
                withCustomComponent(Slot.ICON, EssentialPalette.ENVELOPE_9X7.create())
                markdownBody("${username.colored(EssentialPalette.TEXT_HIGHLIGHT)} wants to join your world.")
            }
        }

        return accepted
    }

    private suspend fun joinMultiplayer(serverAddress: String) {
        withContext(Dispatchers.Client) {
            MinecraftUtils.connectToServer(serverAddress, serverAddress)
        }
    }

    private suspend fun joinSPS(hostUUID: String, joinSecret: String) {
        val host = UUID.fromString(hostUUID)
        val username = UUIDUtil.getName(host).await()
        Essential.logger.info("Attempting to join $username's world...")

        withContext(Dispatchers.Client) {
            hostsToHideNotifications.add(host)

            val connectionManager = Essential.getInstance().connectionManager
            connectionManager.send(SocialDiscordRequestJoinServerPacket(host, joinSecret))

            scope.launch {
                delay(7000)
                hostsToHideNotifications.remove(host)
            }
        }
    }
}