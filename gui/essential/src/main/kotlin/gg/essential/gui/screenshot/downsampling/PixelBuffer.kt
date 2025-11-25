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

import io.netty.buffer.ByteBufHolder
import java.nio.ByteBuffer


/**
 *  PixelBuffer is an interface for reading pixel data without the use of BufferedImages
 */
interface PixelBuffer : ByteBufHolder {

    /**
     * Width of the image
     */
    fun getWidth(): Int

    /**
     * Height of the image
     */
    fun getHeight(): Int

    /**
     * The number of channels in this pixel buffer
     */
    fun getChannels(): Int

    /**
     * Returns a buffer ready to be uploaded to OpenGL
     */
    fun prepareDirectBuffer(): ByteBuffer

    /**
     * Returns pixel data as buffer of type RGB or RGBA, depending on whether the alpha channel is present
     */
    fun getBuffer(): ByteBuffer

}