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

import kotlin.test.Test
import kotlin.test.assertEquals

class MutableStateTest {
    @Test
    fun testMutableState() {
        val state = mutableStateOf(0)
        assertEquals(0, state.getUntracked())
        state.set(1)
        assertEquals(1, state.getUntracked())
        state.set { it + 1 }
        assertEquals(2, state.getUntracked())
    }
}
