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

import gg.essential.util.GuiEssentialPlatform.Companion.platform
import io.netty.buffer.AbstractByteBufAllocator
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import java.util.concurrent.atomic.AtomicLong

class LimitedAllocator(
    private val alloc: ByteBufAllocator,
    private val limit: Long,
) : AbstractByteBufAllocator() {
    private val allocatedBytes = AtomicLong()

    fun getAllocatedBytes() = allocatedBytes.get()

    fun tryHeapBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf? =
        tryAlloc(initialCapacity, maxCapacity, ::heapBuffer)

    fun tryDirectBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf? =
        tryAlloc(initialCapacity, maxCapacity, ::directBuffer)

    private fun tryAlloc(
        initialCapacity: Int,
        maxCapacity: Int,
        alloc: (initialCapacity: Int, maxCapacity: Int) -> ByteBuf,
    ): ByteBuf? {
        if (initialCapacity > limit) {
            // This allocation is eternally doomed, so instead of live-locking it, let's just grant it
            return alloc(initialCapacity, maxCapacity)
        }

        // First try to reserve space for us
        var prev: Long
        var next: Long
        do {
            prev = allocatedBytes.get()
            next = prev + initialCapacity
            if (next > limit) {
                return null
            }
        } while (!allocatedBytes.compareAndSet(prev, next))

        // then allocate the buffer
        return alloc(initialCapacity, maxCapacity).also {
            // finally free the reservation again (the buffer itself now holds the memory)
            allocatedBytes.addAndGet(-initialCapacity.toLong())
        }
    }

    // For internal use only!
    fun track(capacity: Long) {
        allocatedBytes.addAndGet(capacity)
    }

    override fun newHeapBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf =
        platform.trackByteBuf(this, alloc.heapBuffer(initialCapacity, maxCapacity))

    override fun newDirectBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf =
        platform.trackByteBuf(this, alloc.directBuffer(initialCapacity, maxCapacity))

    override fun isDirectBufferPooled(): Boolean = alloc.isDirectBufferPooled
}