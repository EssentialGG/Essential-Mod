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

import gg.essential.util.lwjgl3.api.ImageData
import io.netty.buffer.ByteBuf
import io.netty.buffer.DefaultByteBufHolder
import java.nio.ByteBuffer

/**
 * A PixelBuffer backed by a ByteBuffer in RGB / RGBA format
 */
class BufferBackedImage(
    private val imageWidth: Int,
    private val imageHeight: Int,
    private val nativeBuffer: ByteBuf,
) : DefaultByteBufHolder(nativeBuffer), PixelBuffer {

    //Retain this object until this object is cleared
    //Since ImageDataImpl frees the buffer on finalize
    var data: ImageData? = null

    constructor(data: ImageData) : this(data.width, data.height, data.data) {
        this.data = data
    }

    fun set(index: Int, value: Byte) {
        nativeBuffer.setByte(index, value.toInt())
    }

    override fun getWidth(): Int {
        return imageWidth
    }

    override fun getHeight(): Int {
        return imageHeight
    }

    override fun getChannels(): Int {
        return nativeBuffer.capacity() / (imageWidth * imageHeight)
    }


    override fun getBuffer(): ByteBuffer {
        return nativeBuffer.nioBuffer()
    }


    override fun prepareDirectBuffer(): ByteBuffer {
        assert(nativeBuffer.isDirect)
        return nativeBuffer.nioBuffer()
    }
}