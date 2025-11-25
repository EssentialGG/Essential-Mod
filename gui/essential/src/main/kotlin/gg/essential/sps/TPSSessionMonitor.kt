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
package gg.essential.sps

import kotlin.math.max
import kotlin.math.min

class TPSSessionMonitor {

    private val allSession = Session()
    private val windowedSession = Session()

    private var minTPS: Float = Float.MAX_VALUE
    private var maxTPS: Float = 0f

    fun tick() {
        val now = System.nanoTime()
        allSession.tick(now)
        windowedSession.tick(now)
        // Get results and reset windowed session after 100 ticks (~5s)
        if (windowedSession.tickCount >= 100) {
            val windowedTPS = windowedSession.getAverageTPS()
            minTPS = min(minTPS, windowedTPS)
            maxTPS = max(maxTPS, windowedTPS)
            windowedSession.reset()
        }
    }

    fun getAverageTPS(): Float {
        return allSession.getAverageTPS()
    }

    fun getMinTPS(): Float {
        if (minTPS == Float.MAX_VALUE) return getAverageTPS()
        return minTPS
    }

    fun getMaxTPS(): Float {
        if (maxTPS == 0f) return getAverageTPS()
        return maxTPS
    }

    class Session {
        var tickCount: Long = 0L
        // Time in nanoseconds
        private var firstTickTime: Long = 0L
        private var lastTickTime: Long = 0L

        init {
            reset()
        }

        fun tick(now: Long) {
            if (firstTickTime == 0L) {
                firstTickTime = now
                lastTickTime = now
                return
            }
            lastTickTime = now
            tickCount++
        }

        fun getAverageTPS(): Float {
            val totalTime = lastTickTime - firstTickTime
            if (totalTime == 0L || tickCount == 0L) return 0F
            val averageTimePerTick = (totalTime / tickCount).toDouble()
            return (1_000_000_000.0 / averageTimePerTick).toFloat()
        }

        fun reset() {
            tickCount = 0L
            firstTickTime = 0L
            lastTickTime = 0L
        }
    }
}