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
import io.netty.util.ReferenceCountUtil.release

/**
 * Queries all providers and returns the results
 * Providers with a lower index in the array have a higher priority for being returned
 */
class PriorityDelegatedWindowProvider<T>(val providers: Array<WindowedProvider<T>>) : WindowedProvider<T> {

    //All children providers should have the same scope
    override var items: List<ScreenshotId>
        get() = providers[0].items
        set(value) {
            for (provider in providers) {
                provider.items = value
            }
        }

    override fun provide(windows: List<WindowedProvider.Window>, optional: Set<ScreenshotId>): Map<ScreenshotId, T> {
        val output = mutableMapOf<ScreenshotId, T>()

        //We need to call the first / higher priority provider first, so it gets to queue its async tasks first.
        //Otherwise, low priority async tasks may be queued and begin executing before the high priority ones are
        // queued, and so the high priority ones would have to wait until all the ones already running are done.
        // We also want to pass in items we already got from the higher priority providers as optional for the lower
        // priority ones.
        for (provider in providers) {
            for ((key, value) in provider.provide(windows, optional + output.keys)) {
                if (key !in output) {
                    output[key] = value
                } else {
                    release(value)
                }
            }
        }

        return output
    }
}