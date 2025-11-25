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
package gg.essential.network.connectionmanager.profile

import gg.essential.Essential
import gg.essential.event.gui.GuiOpenEvent
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.modals.DisconnectModal
import gg.essential.mixins.ext.server.dispatcher
import gg.essential.mixins.transformers.client.gui.GuiDisconnectedAccessor
import gg.essential.network.connectionmanager.ConnectionManager
import gg.essential.universal.UMinecraft
import gg.essential.util.GuiUtil.pushModal
import gg.essential.util.ServerType
import gg.essential.util.UUIDUtil
import gg.essential.util.textLiteral
import me.kbrewster.eventbus.Subscribe
import net.minecraft.client.gui.GuiDisconnected
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.entity.player.EntityPlayerMP
import java.util.*
import kotlin.coroutines.EmptyCoroutineContext

object SuspensionDisconnectHandler {

    private val referenceHolder = ReferenceHolderImpl()

    private const val SELF_SUSPENSION_STRING = "essential:self_suspension"
    private val selfSuspensionComponent = textLiteral(SELF_SUSPENSION_STRING)

    private const val HOST_SUSPENSION_STRING = "essential:host_suspension"
    private val hostSuspensionComponent = textLiteral(HOST_SUSPENSION_STRING)

    fun setupEffects(connectionManager: ConnectionManager) {
        Essential.EVENT_BUS.register(this)

        effect(referenceHolder) {
            val suspension = connectionManager.suspensionManager.activeSuspension()
            if (suspension != null && suspension.isActiveNow()) {
                handleSelfSuspension()
            }
        }

        effect(referenceHolder) {
            val suspensions = connectionManager.profileManager.suspensions()
            for (suspension in suspensions) {
                if (suspension.isActiveNow()) {
                    handleSuspension(suspension.user)
                }
            }
        }
    }

    @Subscribe fun onGuiOpened(event: GuiOpenEvent) {
        if (event.gui is GuiDisconnected) {
            val reason: String = getDisconnectReason(event.gui as GuiDisconnectedAccessor)
            val hostSuspension = HOST_SUSPENSION_STRING == reason
            val selfSuspension = SELF_SUSPENSION_STRING == reason

            if (hostSuspension || selfSuspension) {
                event.isCancelled = true
                UMinecraft.getMinecraft().displayGuiScreen(GuiMainMenu())

                if (hostSuspension) {
                    pushModal {
                        DisconnectModal(it, DisconnectModal.HOST_SUSPENDED)
                    }
                }
            }
        }
    }

    private fun handleSelfSuspension() {
        handleSuspension(UUIDUtil.getClientUUID())
    }

    private fun handleSuspension(uuid: UUID) {
        val self = UUIDUtil.getClientUUID() == uuid
        when (val serverType = ServerType.current()) {
            is ServerType.SPS.Host -> {
                val server = UMinecraft.getMinecraft().integratedServer
                server?.dispatcher?.dispatch(EmptyCoroutineContext) {
                    if (self) {
                        var selfPlayer: EntityPlayerMP? = null
                        server.playerList.players.toList().forEach { player ->
                            if (player.uniqueID == uuid) {
                                selfPlayer = player
                            } else {
                                kickPlayer(player, false)
                            }
                        }
                        selfPlayer?.let { kickPlayer(it, true) }
                    } else {
                        server.playerList.players.find { it.uniqueID == uuid }?.let { kickPlayer(it, true) }
                    }
                }
            }
            is ServerType.SPS.Guest -> {
                val localPlayer = UMinecraft.getPlayer()
                if (localPlayer != null) {
                    if (self) {
                        localPlayer.connection.networkManager.closeChannel(selfSuspensionComponent)
                    } else if (serverType.hostUuid == uuid) {
                        localPlayer.connection.networkManager.closeChannel(hostSuspensionComponent)
                    }
                }
            }
            else -> {}
        }
    }

    private fun kickPlayer(player: EntityPlayerMP, self: Boolean) {
        val connection = player.connection
        //#if MC<=10809
        //$$ connection.kickPlayerFromServer(if (self) SELF_SUSPENSION_STRING else HOST_SUSPENSION_STRING)
        //#else
        connection.disconnect(if (self) selfSuspensionComponent else hostSuspensionComponent)
        //#endif
    }

    private fun getDisconnectReason(accessor: GuiDisconnectedAccessor): String =
        //#if MC>=12100
        //$$ accessor.info.reason.string
        //#elseif MC>=11600
        //$$ accessor.message.string
        //#else
        accessor.message.unformattedText
        //#endif
}