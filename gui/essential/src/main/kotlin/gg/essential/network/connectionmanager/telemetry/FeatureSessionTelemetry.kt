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

import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket
import gg.essential.handlers.ShutdownHook
import gg.essential.util.GuiEssentialPlatform.Companion.platform

typealias FeatureName = String

object FeatureSessionTelemetry {

    private var startTime: Long = 0L
    private val featureSessionEvents = mutableListOf<FeatureSessionEvent>()

    fun start() {
        featureSessionEvents.clear()
        startTime = System.currentTimeMillis()
        ShutdownHook.INSTANCE.register { end() }
    }

    fun startEvent(featureName: FeatureName) {
        featureSessionEvents.add(FeatureSessionEvent(featureName, true, System.currentTimeMillis()))
    }

    fun endEvent(featureName: FeatureName) {
        featureSessionEvents.add(FeatureSessionEvent(featureName, false, System.currentTimeMillis()))
    }

    private fun end() {
        val endTime = System.currentTimeMillis()
        sendDetailedTimings(endTime)
        sendSimplifiedTimings(endTime)
    }

    private fun sendDetailedTimings(endTime: Long) {
        val events = featureSessionEvents.take(5000)
        platform.enqueueTelemetry(
            ClientTelemetryPacket(
                "FEATURE_SESSION_DETAILED_TIMINGS", mapOf(
                    "startTime" to startTime,
                    "endTime" to endTime,
                    "featureSessionEvents" to events,
                    "wasLimited" to (events.size < featureSessionEvents.size)
                )
            )
        )
    }


    private fun sendSimplifiedTimings(endTime: Long) {
        val activeEvents = mutableListOf<FeatureSessionEvent>()
        val featureSessionTimes = mutableMapOf<String, Long>()
        var last: Long? = null

        for (event in featureSessionEvents) {
            if (last != null && activeEvents.isNotEmpty()) {
                val top = activeEvents.last()
                featureSessionTimes[top.featureName] = (featureSessionTimes[top.featureName] ?: 0L) + (event.time - last)
            }

            if (event.isStart) {
                activeEvents.add(event)
            } else {
                val eventToRemove = activeEvents.findLast { it.featureName == event.featureName }
                eventToRemove?.let { activeEvents.remove(it) }
            }

            last = event.time
        }

        platform.enqueueTelemetry(
            ClientTelemetryPacket(
                "FEATURE_SESSION_TIMINGS", mapOf(
                    "startTime" to startTime,
                    "endTime" to endTime,
                    "featureSessionTimes" to featureSessionTimes
                )
            )
        )
    }

    data class FeatureSessionEvent(
        val featureName: FeatureName,
        val isStart: Boolean,
        val time: Long,
    )

}