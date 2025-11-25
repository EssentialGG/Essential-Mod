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
package gg.essential.gui.elementa.state.v2

import gg.essential.elementa.state.v2.ReferenceHolder
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class StateSchedulerTest {
    @Test
    fun test() {
        val startTime = Instant.EPOCH.plusMillis(2)
        val currentTime = mutableStateOf(startTime)
        val scheduler = StateScheduler(currentTime)

        val completed = Array(100000) { false }
        val effects = completed.indices.map { i ->
            effect(ReferenceHolder.Weak) {
                val targetTime = Instant.EPOCH.plusMillis(i.toLong())
                val reachedTarget = with(scheduler) { observe(targetTime) }
                // Only time the effect should run is when we reached the target time and during setup.
                if (reachedTarget) {
                    completed[i] = true
                    assertEquals(if (targetTime > startTime) targetTime else startTime, currentTime.getUntracked())
                } else {
                    assertEquals(startTime, currentTime.getUntracked())
                }
            }
        }

        assertEquals(true, completed[0])
        assertEquals(true, completed[1])
        assertEquals(true, completed[2])
        assertEquals(false, completed[3])

        for (i in completed.indices) {
            currentTime.set(startTime.plusMillis(i.toLong()))
        }

        effects.forEach { it() }

        assert(completed.all { it })
    }
}
