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
package gg.essential.commands.impl

import gg.essential.Essential
import gg.essential.api.commands.Command
import gg.essential.api.commands.DefaultHandler
import gg.essential.api.commands.DisplayName
import gg.essential.api.commands.SubCommand
import gg.essential.commands.engine.EssentialFriend
import gg.essential.commands.engine.EssentialUser
import gg.essential.handlers.PauseMenuDisplay
import gg.essential.network.connectionmanager.sps.SPSSessionSource
import gg.essential.universal.ChatColor
import gg.essential.universal.UMinecraft
import gg.essential.upnp.model.UPnPSession
import gg.essential.util.*
import kotlinx.coroutines.future.await
import net.minecraft.client.Minecraft
import java.time.Duration
import java.time.Instant
import java.util.*

abstract class CommandOpBase(name: String) : Command(name) {

    val spsManager = Essential.getInstance().connectionManager.spsManager

    @DefaultHandler
    suspend fun handle(@DisplayName("player") user: EssentialUser) {
        if (!spsManager.isAllowCheats) {
            MinecraftUtils.sendMessage("Cheats must be enabled to use the op command.")
            return
        }

        if (user.uuid !in spsManager.invitedUsers) {
            if (this is CommandOp) {
                MinecraftUtils.sendMessage("Cannot op ${user.name} because they are not invited to your world")
            } else {
                MinecraftUtils.sendMessage("Cannot deop ${user.name} because they are not invited to your world")
            }
            return
        }

        apply(user.uuid, user.name)
    }

    abstract suspend fun apply(uuid: UUID, username: String)
}

object CommandDeOp : CommandOpBase("deop") {

    override suspend fun apply(uuid: UUID, username: String) {
        if (uuid in spsManager.oppedPlayers) {
            spsManager.updateOppedPlayers(spsManager.oppedPlayers - uuid)
            MinecraftUtils.sendMessage("Removed op from $username.")
        } else {
            MinecraftUtils.sendMessage("$username is not opped.")
        }
    }
}

object CommandOp : CommandOpBase("op") {

    override suspend fun apply(uuid: UUID, username: String) {
        if (uuid in spsManager.oppedPlayers) {
            MinecraftUtils.sendMessage("$username is already opped.")
        } else {
            spsManager.updateOppedPlayers(spsManager.oppedPlayers + uuid)
            MinecraftUtils.sendMessage("$username is now opped.")
        }
    }
}

object CommandInvite : Command("einvite") {

    @DefaultHandler
    suspend fun handle(@DisplayName("friend") friend: EssentialFriend) {

        if (friend.uuid == UUIDUtil.getClientUUID()) {
            MinecraftUtils.sendMessage("You cannot invite yourself.")
            return
        }

        val username = friend.ign
        val connectionManager = Essential.getInstance().connectionManager
        val socialManager = connectionManager.socialManager
        val uuid = friend.uuid
        val serverType = ServerType.current()

        if (serverType !is ServerType.SPS.Host) {
            when (serverType) {
                is ServerType.Singleplayer -> {
                    PauseMenuDisplay.showInviteOrHostModal(prepopulatedInvites = setOf(uuid), source = SPSSessionSource.COMMAND)
                }
                is ServerType.Multiplayer -> {
                    // Reinvite in case they're already invited, so they receive the notification again
                    socialManager.reinviteFriendsOnServer(serverType.address, setOf(uuid))
                    MinecraftUtils.sendMessage("Invited $username.")
                }
                else -> MinecraftUtils.sendMessage("You cannot invite players to this server.")
            }
            return
        }

        val spsManager = connectionManager.spsManager

        if (uuid in spsManager.invitedUsers) {
            MinecraftUtils.sendMessage("$username is already invited to your world.")
            return
        }

        spsManager.updateInvitedUsers(spsManager.invitedUsers + uuid)
        MinecraftUtils.sendMessage("Invited $username to your world.")
    }

    @SubCommand("cancel", description = "Cancel invite to player")
    suspend fun handleCancelInvite(@DisplayName("friend") friend: EssentialUser) {
        if (friend.uuid == UUIDUtil.getClientUUID()) {
            MinecraftUtils.sendMessage("You cannot remove an invite from yourself.")
            return
        }
        cancelInviteAndKick(friend.uuid, friend.name, false)
    }
}

private suspend fun cancelInviteAndKick(uuid: UUID, username: String, kick: Boolean) {
    val connectionManager = Essential.getInstance().connectionManager
    val serverType = ServerType.current()

    if (serverType !is ServerType.SPS.Host) {
        if (serverType !is ServerType.Multiplayer) {
            MinecraftUtils.sendMessage("Cannot cancel invite because you are not currently on a session that supports invites")
            return
        }
        val invites = connectionManager.socialManager.getInvitesOnServer(serverType.address)
        if (uuid !in invites) {
            MinecraftUtils.sendMessage("Cannot cancel invite because $username is not invited to your current session")
            return
        }

        connectionManager.socialManager.setInvitedFriendsOnServer(
            serverType.address, invites - uuid,
        )
        MinecraftUtils.sendMessage("Cancelled invite to $username")
        return
    }

    val spsManager = connectionManager.spsManager

    if (uuid !in spsManager.invitedUsers) {
        MinecraftUtils.sendMessage("$username is not invited to your world.")
        return
    }

    spsManager.updateInvitedUsers(spsManager.invitedUsers - uuid)

    if (kick) {
        MinecraftUtils.sendMessage("Kicked $username")
    } else {
        MinecraftUtils.sendMessage("Canceled invite to $username")
    }
}

object CommandKick : Command("kick") {

    @DefaultHandler
    suspend fun handle(@DisplayName("player") player: EssentialUser) {
        if (player.uuid == UUIDUtil.getClientUUID()) {
            MinecraftUtils.sendMessage("You cannot kick yourself.")
            return
        }

        cancelInviteAndKick(player.uuid, player.name, true)
    }
}

object CommandSession : Command("esession") {

    private val connectionManager = Essential.getInstance().connectionManager
    private val socialManager = connectionManager.socialManager

    @SubCommand("open", description = "Start a world share session")
    fun handleOpen() {
        when (ServerType.current()) {
            is ServerType.SPS.Host -> MinecraftUtils.sendMessage("Cannot start session, one is already running.")
            is ServerType.Singleplayer, is ServerType.SupportsInvites -> PauseMenuDisplay.showInviteOrHostModal(SPSSessionSource.COMMAND)
            else -> MinecraftUtils.sendMessage("Cannot start session, your current world does not support invites")
        }
    }

    @SubCommand("close", description = "Close your world share session")
    fun handleClose() {
        val currentServerData = UMinecraft.getMinecraft().currentServerData

        val spsManager = connectionManager.spsManager

        when {

            // Hosting a single player world
            Minecraft.getMinecraft().isIntegratedServerRunning && spsManager.localSession != null -> {
                spsManager.closeLocalSession()
                MinecraftUtils.sendMessage("Closed session")
            }

            // On a multiplayer server with friends invited
            currentServerData != null && socialManager.getInvitesOnServer(currentServerData.serverIP).isNotEmpty() -> {
                socialManager.setInvitedFriendsOnServer(currentServerData.serverIP, emptySet())
                MinecraftUtils.sendMessage("Closed session")
            }

            // Other cases do not have a session running to close
            else -> MinecraftUtils.sendMessage("No session running")
        }

    }

    @SubCommand("info", description = "Info about your world share session")
    suspend fun handleInfo() {

        val localSession = connectionManager.spsManager.localSession
        if (localSession == null) {
            handleInfoNoSps()
            return
        }
        handleInfoSpsOld(localSession)
    }

    suspend fun handleInfoNoSps() {
        val currentServerData = UMinecraft.getMinecraft().currentServerData
        if (currentServerData != null) {
            val invitesOnServer = socialManager.getInvitesOnServer(currentServerData.serverIP)
            if (invitesOnServer.isNotEmpty()) {
                MinecraftUtils.sendMessage("Invited Players: ")
                invitesOnServer.forEach {
                    MinecraftUtils.sendMessage(" - ${UUIDUtil.getName(it).await()}")
                }
                return
            }
        }
        MinecraftUtils.sendMessage("No session running")
    }

    suspend fun handleInfoSpsOld(localSession: UPnPSession) {
        val spsManager = connectionManager.spsManager
        MinecraftUtils.sendMessage("Privacy setting: ${localSession.privacy}")
        MinecraftUtils.sendMessage("Cheats for all: ${spsManager.isAllowCheats}")
        MinecraftUtils.sendMessage("Default gamemode: ${spsManager.currentGameMode}")
        MinecraftUtils.sendMessage("Difficulty: ${spsManager.difficulty}")
        MinecraftUtils.sendMessage(
            "World uptime: ${
                Duration.between(spsManager.sessionStartTime, Instant.now()).toShortString()
            }"
        )
        MinecraftUtils.sendMessage("Invited Players: ")
        (spsManager.invitedUsers + UUIDUtil.getClientUUID()).forEach { invitedUser ->
            val username = UUIDUtil.getName(invitedUser).await()
            val colorPrefix = when {
                invitedUser == UUIDUtil.getClientUUID() -> ChatColor.AQUA
                spsManager.getOnlineState(invitedUser).getUntracked() -> ChatColor.GREEN
                else -> ChatColor.GRAY
            }
            val suffix = when(invitedUser) {
                UUIDUtil.getClientUUID() -> " (Host)"
                in spsManager.oppedPlayers -> " (OP)"
                else -> ""
            }
            MinecraftUtils.sendMessage("$colorPrefix - $username$suffix")
        }
    }

}