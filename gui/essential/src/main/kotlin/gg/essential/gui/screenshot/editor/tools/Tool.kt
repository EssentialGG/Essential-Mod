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
package gg.essential.gui.screenshot.editor.tools

import gg.essential.gui.screenshot.editor.ScreenshotCanvas

/**
 * Tools within the [ScreenshotCanvas] gui
 */
abstract class Tool(val editableScreenshot: ScreenshotCanvas) {

    /**
     * This event occurs when the tool is selected from the toolbar
     */
    abstract fun enable()

}