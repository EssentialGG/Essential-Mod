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
@file:OptIn(ForTestingOnly::class)

package gg.essential.gui.elementa.state.v2

import kotlin.test.Test
import kotlin.test.assertEquals

class ObservedLongTest {
    private fun <R> explore(range: ClosedRange<Long>, func: (ObservedLong) -> R): List<R> =
        explore(range, ::ObservedLong, func)

    @Test
    fun testGetValue() {
        assertEquals(listOf(-3L, -2, -1, 0, 1, 2, 3), explore(-3L..3) { it.getValue() })
    }

    @Test
    fun testComparisons() {
        assertEquals(listOf(false, true, false), explore(0L..5L) { it.equal(2) })
        assertEquals(listOf(true, false), explore(2L..5L) { it.equal(2) })
        assertEquals(listOf(false, true), explore(0L..2L) { it.equal(2) })

        assertEquals(listOf(true, false), explore(0L..5L) { it.lessOrEqual(2) })
        assertEquals(listOf(true, false), explore(0L..5L) { it.less(2) })
        assertEquals(listOf(false, true), explore(0L..5L) { it.greaterOrEqual(2) })
        assertEquals(listOf(false, true), explore(0L..5L) { it.greater(2) })

        assertEquals(listOf(true, false), explore(2L..5L) { it.lessOrEqual(2) })
        assertEquals(listOf(false), explore(2L..5L) { it.less(2) })
        assertEquals(listOf(true), explore(2L..5L) { it.greaterOrEqual(2) })
        assertEquals(listOf(false, true), explore(2L..5L) { it.greater(2) })
    }

    @Test
    fun testTimes() {
        assertEquals(listOf(-15L, -10, -5, 0, 5, 10, 15), explore(-3L..3) { (it * 5).getValue() })
        assertEquals(listOf(15L, 10, 5, 0, -5, -10, -15), explore(-3L..3) { (it * -5).getValue() })
    }

    @Test
    fun testDiv() {
        assertEquals(listOf(-3L, -2, -1, 0, 1, 2, 3), explore(-15L..15) { (it / 5).getValue() })
        assertEquals(listOf(3L, 2, 1, 0, -1, -2, -3), explore(-15L..15) { (it / -5).getValue() })
    }
}
