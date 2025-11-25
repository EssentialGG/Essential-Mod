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
package gg.essential.gui.screenshot.bytebuf

import io.netty.buffer.AbstractByteBufAllocator
import io.netty.buffer.ByteBuf

class WorkStealingAllocator(
    private val alloc: LimitedAllocator,
    private val work: () -> Unit,
) : AbstractByteBufAllocator() {

    override fun isDirectBufferPooled(): Boolean = alloc.isDirectBufferPooled

    override fun newHeapBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf {
        while (true) {
            val buf = alloc.tryHeapBuffer(initialCapacity, maxCapacity)
            if (buf != null) {
                return buf
            }
            work()
        }
    }

    override fun newDirectBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf {
        while (true) {
            val buf = alloc.tryDirectBuffer(initialCapacity, maxCapacity)
            if (buf != null) {
                return buf
            }
            work()
        }
    }
}