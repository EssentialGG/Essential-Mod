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

import kotlin.collections.listOf

// Note: Actions are stored in the config by their ordinal.
//       Do not remove or reorder (without a corresponding config migration).
enum class ScreenshotPreviewAction(val displayName: String) {

    COPY_PICTURE("Copy Picture"),
    COPY_LINK("Copy Link"),
    FAVORITE("Favorite"),
    DELETE("Delete"),
    SHARE("Share to Friends"),
    EDIT("Edit"),
    ;

    companion object {
        val DISPLAY_ORDER = listOf<ScreenshotPreviewAction>(
            SHARE,
            COPY_PICTURE,
            COPY_LINK,
            FAVORITE,
            EDIT,
            DELETE,
        ).also { assert(it.sorted() == ScreenshotPreviewAction.entries) }
    }
}