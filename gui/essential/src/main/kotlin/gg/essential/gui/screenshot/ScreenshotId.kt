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
package gg.essential.gui.screenshot

import gg.essential.media.model.Media
import gg.essential.util.httpGetToInputStreamBlocking
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat

sealed interface ScreenshotId {
    val name: String
    fun open(): InputStream
}

data class LocalScreenshot(val path: Path) : ScreenshotId {
    override val name: String
        get() = path.fileName.toString()

    override fun open(): InputStream = Files.newInputStream(path)
}

data class RemoteScreenshot(val media: Media) : ScreenshotId {
    // TODO would like to use `media.title` for this before falling back to the date but we currently put quite useless
    //      stuff in there
    override val name: String
        get() = SCREENSHOT_DATETIME_FORMAT.format(media.createdAt)

    override fun open(): InputStream = httpGetToInputStreamBlocking(media.variants.getValue("original").url)

    override fun hashCode(): Int {
        return media.id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemoteScreenshot

        if (media.id != other.media.id) return false

        return true
    }
}

val SCREENSHOT_DATETIME_FORMAT: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")
