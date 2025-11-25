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
package gg.essential.network.connectionmanager.telemetry

import gg.essential.Essential
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket
import gg.essential.event.network.server.ServerJoinEvent
import gg.essential.event.network.server.ServerLeaveEvent
import gg.essential.event.network.server.SingleplayerJoinEvent
import gg.essential.event.render.RenderTickEvent
import me.kbrewster.eventbus.Subscribe

object FPSTelemetryManager {
    private val telemetryManager = Essential.getInstance().connectionManager.telemetryManager
    private var fpsSessionMonitor: FPSSessionMonitor? = null

    fun initialize() {
        Essential.EVENT_BUS.register(this)
    }

    private fun startSession() {
        fpsSessionMonitor = FPSSessionMonitor()
    }

    private fun endSession() {
        val fps = fpsSessionMonitor?.getAverageFPS()
        if (fps != null) {
            telemetryManager.enqueue(ClientTelemetryPacket("SESSION_FPS", mapOf("averageFPS" to fps)))
        }
        fpsSessionMonitor = null
    }

    @Subscribe
    fun onServerJoinEvent(event: ServerJoinEvent) {
        startSession()
    }

    @Subscribe
    fun onSingleplayerJoinEvent(event: SingleplayerJoinEvent) {
        startSession()
    }

    @Subscribe
    fun onServerLeaveEvent(event: ServerLeaveEvent) {
        endSession()
    }

    @Subscribe
    fun onRenderTickEvent(event: RenderTickEvent) {
        if (event.isPre) {
            return
        }
        fpsSessionMonitor?.frame()
    }

}