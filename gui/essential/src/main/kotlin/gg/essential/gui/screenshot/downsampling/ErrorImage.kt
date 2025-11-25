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
package gg.essential.gui.screenshot.downsampling

import io.netty.buffer.DefaultByteBufHolder
import io.netty.buffer.Unpooled
import java.nio.ByteBuffer

class ErrorImage : DefaultByteBufHolder(Unpooled.EMPTY_BUFFER), PixelBuffer {

    override fun getWidth(): Int {
        return 1
    }

    override fun getHeight(): Int {
        return 1
    }

    override fun getChannels(): Int {
        throw IllegalStateException("getChannels() not supported on ErrorImage")
    }

    override fun prepareDirectBuffer(): ByteBuffer {
        throw IllegalStateException("prepareDirectBuffer() not supported on ErrorImage")
    }

    override fun getBuffer(): ByteBuffer {
        throw IllegalStateException("getBuffer() not supported on ErrorImage")
    }
}