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
package gg.essential.gui.screenshot.concurrent

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger


/**
 * Class to handle the generation of threads for the ScreenshotBrowser
 * We use a custom thread factory because we want to reduce the priority of these worker threads below everything else
 */
object ScreenshotWorkerThreadFactory : ThreadFactory {

    private val counter = AtomicInteger(0)

    override fun newThread(r: Runnable): Thread {
        return Thread(r, "Screenshot Worker Thread ${counter.incrementAndGet()}").also {
            it.priority = Thread.MIN_PRIORITY
        }
    }
}