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
import gg.essential.gui.screenshot.downsampling.PixelBuffer
import gg.essential.util.UIdentifier
import kotlin.math.max
import kotlin.math.min

interface WindowedProvider<out T> {
    /**
     * The list of items which this instance provides a view on.
     * Implementations may load these themselves, or pass them on to other providers.
     */
    var items: List<ScreenshotId>

    /**
     * Attempts to provide all elements in the given windows.
     *
     * It is up to each particular implementation to specify whether this operation will block until all elements
     * can be provided. If so, the returned map must contain all elements in the given windows, otherwise only
     * elements already available will be returned.
     *
     * The caller may indicate that it is not really interested in a particular set of items by passing them in
     * [optional]. The values for these may be returned if they are already available but the provider should not make
     * any extra effort to provide them if they are not because the caller will likely just ignore them (probably
     * because there is already a cached version for them higher up in the chain).
     * This provider may also purge entries for these items from its cache, just like as if they were not being
     * requested in the first place.
     *
     * The provider should make an attempt to load elements in the order in which the windows are given, with each
     * window loading from low index to high index unless backwards is `true`.
     *
     * A provider may pre-load further elements or keep no longer required elements in a cache. It may use the windows
     * to decide which elements are likely to be required in the future.
     * As such, this method should ideally be called once every frame, even when the windows have not changed, to
     * allow the implementation to make internal progress.
     *
     * If an empty list of windows is passed, the provider must eventually clean up all resources it directly or
     * indirectly holds, even if there are no further calls to this method.
     *
     * Implementations must generally be thread safe unless they have special requirements.
     * Ideally they should allow multiple concurrent requests but they may also simply lock the whole method.
     */
    fun provide(windows: List<Window>, optional: Set<ScreenshotId>): Map<ScreenshotId, T>

    data class Window(val range: IntRange, val backwards: Boolean) {
        fun expandToInclude(index: Int) =
            Window(min(range.first, index)..max(range.last, index), backwards)

        /**
         * Ensures that the requested window is within the valid range of item indices
         */
        fun inRange(indices: IntRange): Window? =
            copy(range = max(range.first, indices.first)..min(range.last, indices.last))
                .takeUnless { it.range.isEmpty() }
    }
}
typealias WindowedImageProvider = WindowedProvider<PixelBuffer>
typealias WindowedTextureProvider = WindowedProvider<RegisteredTexture>

data class RegisteredTexture(val identifier: UIdentifier, val imageWidth: Int, val imageHeight: Int, val error: Boolean)

fun Int.toSingleWindowRequest(): List<WindowedProvider.Window> {
    return listOf(WindowedProvider.Window(IntRange(this, this), false))
}

fun IntRange.reversed(reversed: Boolean): IntProgression {
    return if (reversed) {
        this.last downTo this.first
    } else {
        this
    }
}
