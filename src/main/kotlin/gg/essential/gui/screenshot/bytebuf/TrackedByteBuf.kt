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

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import java.nio.ByteOrder

fun trackByteBuf(allocator: LimitedAllocator, buf: ByteBuf): ByteBuf {
    val capacity = buf.capacity()
    allocator.track(capacity.toLong())
    return TrackedByteBuf(buf, capacity, allocator)
}

private class TrackedByteBuf(buf: ByteBuf, private val trackedCapacity: Int, private val allocator: LimitedAllocator) : WrappedByteBuf(buf) {
    private fun trackNew(buf: ByteBuf): ByteBuf {
        return trackByteBuf(allocator, buf)
    }

    private fun trackShared(buf: ByteBuf): TrackedByteBuf {
        return buf as? TrackedByteBuf ?: TrackedByteBuf(buf, trackedCapacity, allocator)
    }

    private fun deallocate() {
        allocator.track(-trackedCapacity.toLong())
    }

    override fun release(): Boolean {
        if (super.release()) {
            deallocate()
            return true
        }
        return false
    }

    override fun release(decrement: Int): Boolean {
        if (super.release(decrement)) {
            deallocate()
            return true
        }
        return false
    }

    override fun alloc(): ByteBufAllocator {
        return allocator
    }

    override fun slice(): ByteBuf {
        return trackShared(super.slice())
    }

    //#if MC>=11200
    override fun retainedSlice(): ByteBuf {
        return trackShared(super.retainedSlice())
    }

    override fun retainedSlice(index: Int, length: Int): ByteBuf {
        return trackShared(super.retainedSlice(index, length))
    }

    override fun retainedDuplicate(): ByteBuf {
        return trackShared(super.retainedDuplicate())
    }

    override fun readRetainedSlice(length: Int): ByteBuf {
        return trackShared(super.readRetainedSlice(length))
    }
    //#endif

    override fun slice(index: Int, length: Int): ByteBuf {
        return trackShared(super.slice(index, length))
    }

    override fun duplicate(): ByteBuf {
        return trackShared(super.duplicate())
    }

    override fun readSlice(length: Int): ByteBuf {
        return trackShared(super.readSlice(length))
    }

    //#if MC>=11200
    override fun asReadOnly(): ByteBuf {
        return trackShared(super.asReadOnly())
    }
    //#endif

    override fun order(endianness: ByteOrder): ByteBuf {
        return trackShared(super.order(endianness))
    }

    override fun readBytes(length: Int): ByteBuf {
        return trackNew(super.readBytes(length))
    }

    override fun copy(): ByteBuf {
        return trackNew(super.copy())
    }

    override fun copy(index: Int, length: Int): ByteBuf {
        return trackNew(super.copy(index, length))
    }
}
