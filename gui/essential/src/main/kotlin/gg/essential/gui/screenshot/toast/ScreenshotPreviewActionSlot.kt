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
package gg.essential.gui.screenshot.toast

import gg.essential.config.EssentialConfig

enum class ScreenshotPreviewActionSlot(val defaultAction: ScreenshotPreviewAction) {

    TOP_LEFT(ScreenshotPreviewAction.EDIT),
    TOP_RIGTH(ScreenshotPreviewAction.FAVORITE),
    BOTTOM_LEFT(ScreenshotPreviewAction.COPY_PICTURE),
    BOTTOM_RIGHT(ScreenshotPreviewAction.SHARE),
    ;

    val action: ScreenshotPreviewAction
        get() = when (this) {
            TOP_LEFT -> EssentialConfig.screenshotOverlayTopLeftAction
            TOP_RIGTH -> EssentialConfig.screenshotOverlayTopRightAction
            BOTTOM_LEFT -> EssentialConfig.screenshotOverlayBottomLeftAction
            BOTTOM_RIGHT -> EssentialConfig.screenshotOverlayBottomRightAction
        }

}