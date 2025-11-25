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

import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.downsampling.ErrorImage
import gg.essential.gui.screenshot.downsampling.PixelBuffer
import gg.essential.image.imagescaling.DimensionConstrain
import gg.essential.image.imagescaling.ResampleFilters
import gg.essential.image.imagescaling.ResampleOp

/**
 * Loads images from the provider in the constructor and applies a mapping function to it (to resize for example) before returning it
 */
class PostProcessWindowedImageProvider(
    private val sourceProvider: WindowedImageProvider,
    val mapper: PixelBuffer.() -> PixelBuffer
) : WindowedImageProvider {

    override var items: List<ScreenshotId> by sourceProvider::items

    override fun provide(windows: List<WindowedProvider.Window>, optional: Set<ScreenshotId>): Map<ScreenshotId, PixelBuffer> {
        return sourceProvider.provide(windows, optional).mapValues { (_, value) ->
            try {
                mapper(value)
            } finally {
                value.release()
            }
        }
    }

    companion object {

        /** Scales the image to fit within a box of the given size (±1) */
        fun bicubicFilter(maxWidth: Int, maxHeight: Int): PixelBuffer.() -> PixelBuffer = {
            if(this is ErrorImage) {
                this.also { retain() }
            } else {
                val dimension =
                    DimensionConstrain.createMaxDimension(maxWidth, maxHeight).coerceAtLeast(3)
                val resampleOp = ResampleOp(dimension)
                resampleOp.filter = ResampleFilters.getBiCubicFilter()
                resampleOp.filter(this)
            }
        }

    }
}
