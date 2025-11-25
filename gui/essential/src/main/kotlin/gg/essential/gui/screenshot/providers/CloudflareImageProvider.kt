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
package gg.essential.gui.screenshot.providers

import gg.essential.gui.screenshot.RemoteScreenshot
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.downsampling.BufferBackedImage
import gg.essential.gui.screenshot.downsampling.ErrorImage
import gg.essential.gui.screenshot.downsampling.PixelBuffer
import gg.essential.media.model.Media
import gg.essential.util.httpGetToBytesBlocking
import gg.essential.util.lwjgl3.api.NativeImageReader
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import java.io.IOException

/**
 * Downloads pre-scaled versions for [RemoteScreenshot] via Cloudflare's Image API so we do not need to download the
 * full image if we only need a thumbnail.
 */
class CloudflareImageProvider(
    private val fallbackProvider: WindowedImageProvider,
    private val nativeImageReader: NativeImageReader,
    private val allocator: ByteBufAllocator,
    private val targetResolution: Pair<Int, Int>?,
) : WindowedImageProvider {

    override var items: List<ScreenshotId> by fallbackProvider::items

    override fun provide(windows: List<WindowedProvider.Window>, optional: Set<ScreenshotId>): Map<ScreenshotId, PixelBuffer> {
        val map = mutableMapOf<ScreenshotId, PixelBuffer>()
        for (window in windows) {
            for (i in window.range.reversed(window.backwards)) {
                val item = items[i]
                if (item in optional) continue
                if (item !is RemoteScreenshot) continue
                map[item] = loadImage(item.media) ?: continue
            }
        }

        map.putAll(fallbackProvider.provide(windows, optional + map.keys))

        return map
    }

    private fun loadImage(media: Media): PixelBuffer? {
        val url = if (targetResolution != null) {
            val (width, height) = targetResolution
            val baseUrl = media.variants["flexible"]?.url ?: return null
            val options = "width=$width,height=$height"

            if (baseUrl.endsWith("/")) "$baseUrl$options" else "$baseUrl/$options"
        } else {
            media.variants["original"]?.url ?: return null
        }
        return try {
            val bytes = httpGetToBytesBlocking(url)
            val imageData = nativeImageReader.getImageData(Unpooled.wrappedBuffer(bytes), allocator)
            BufferBackedImage(imageData)
        } catch (e: IOException) {
            e.printStackTrace()
            ErrorImage()
        }
    }
}
