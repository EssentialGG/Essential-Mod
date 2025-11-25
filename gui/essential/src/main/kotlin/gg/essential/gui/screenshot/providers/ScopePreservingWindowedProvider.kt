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

class ScopePreservingWindowedProvider<T>(
    private val innerProvider: WindowedProvider<T>,
) : WindowedProvider<T> {

    var itemsToBePreserved: List<ScreenshotId> = emptyList()
        set(value) {
            field = value
            updateInnerItems()
        }

    override var items: List<ScreenshotId> = emptyList()
        set(value) {
            field = value
            updateInnerItems()
        }

    private fun updateInnerItems() {
        // The requested items must always be first, so their indices match up with the windows passed to [provide]
        // we'll add all other items afterwards, so that inner providers do not drop their cached values for those.
        innerProvider.items = items + (itemsToBePreserved - items.toSet())
    }

    override fun provide(windows: List<WindowedProvider.Window>, optional: Set<ScreenshotId>): Map<ScreenshotId, T> {
        return innerProvider.provide(windows, optional)
    }
}