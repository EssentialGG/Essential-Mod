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

import com.sparkuniverse.toolbox.util.DateTime
import gg.essential.api.gui.Slot
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.modal.PropertiesModal
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.asyncMap
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.screenshot.components.ScreenshotProperties
import gg.essential.gui.screenshot.image.ForkedImageClipboard
import gg.essential.gui.sendPictureCopiedNotification
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.sps.SpsAddress
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.UuidNameLookup
import gg.essential.util.formatDate
import gg.essential.util.formatTime
import gg.essential.util.lwjgl3.api.NativeImageReader
import io.netty.buffer.Unpooled
import io.netty.buffer.UnpooledByteBufAllocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import javax.imageio.ImageIO
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.outputStream

fun createDateOnlyCalendar(time: Long = System.currentTimeMillis()): Calendar {
    val date: Calendar = GregorianCalendar()
    date.timeInMillis = time
    date.set(Calendar.HOUR_OF_DAY, 0)
    date.set(Calendar.MINUTE, 0)
    date.set(Calendar.SECOND, 0)
    date.set(Calendar.MILLISECOND, 0)
    return date
}

fun getImageTime(path: Path, metadata: ClientScreenshotMetadata?, useEditIfPresent: Boolean): DateTime =
    getImageTime(ScreenshotProperties(LocalScreenshot(path), metadata), useEditIfPresent)

fun getImageTime(properties: ScreenshotProperties, useEditIfPresent: Boolean): DateTime {
    val (id, metadata) = properties
    return if (metadata != null) {
        if (useEditIfPresent) {
            metadata.editTime ?: metadata.time
        } else {
            metadata.time
        }
    } else if (id is LocalScreenshot) {
        val path = id.path
        val name = path.fileName.toString()
            // If multiple screenshots were taken on this second, trim the trailing counter
            .split("_").take(2).joinToString("_")
            // and in any case, remove the file extension
            .removeSuffix(".png")
        val millis = try {
            SCREENSHOT_DATETIME_FORMAT.parse(name).time
        } catch (e: Exception) {
            try {
                Files.getLastModifiedTime(path).toMillis()
            } catch (e: Exception) {
                0
            }
        }
        DateTime(millis)
    } else {
        DateTime(0)
    }
}

fun openScreenshotPropertiesModal(properties: ScreenshotProperties) {
    platform.pushModal { manager ->
        PropertiesModal(manager, generateImageProperties(properties, manager.coroutineScope).toMap())
    }
}

private fun generateImageProperties(properties: ScreenshotProperties, coroutineScope: CoroutineScope): List<Pair<String, State<String>>> {
    val pairList = mutableListOf<Pair<String, State<String>>>()
    pairList.add("Name" to stateOf(properties.id.name))
    val metadata = properties.metadata
    if (metadata != null) {
        val locationIdentifier = metadata.locationMetadata.identifier ?: ""
        when (metadata.locationMetadata.type) {
            ClientScreenshotMetadata.Location.Type.UNKNOWN -> pairList.add("Location" to stateOf("Unknown"))
            ClientScreenshotMetadata.Location.Type.MULTIPLAYER -> pairList.add("Server" to stateOf(locationIdentifier))
            ClientScreenshotMetadata.Location.Type.MENU -> pairList.add("Menu" to stateOf(locationIdentifier))
            ClientScreenshotMetadata.Location.Type.SINGLE_PLAYER -> pairList.add("World" to stateOf(locationIdentifier))
            ClientScreenshotMetadata.Location.Type.SHARED_WORLD -> {
                val host = SpsAddress.parse(locationIdentifier)?.host
                if (host != null) {
                    pairList.add("Location" to stateOf("Shared World"))
                    pairList.add("Host" to UuidNameLookup.nameState(host, "Loading..."))
                } else {
                    pairList.add("Location" to stateOf(locationIdentifier))
                }
            }
        }
        pairList.add("Creator" to UuidNameLookup.nameState(metadata.authorId, "Loading..."))
    }
    val imageTime = getImageTime(properties, false)
    if (imageTime.time > 0) {
        val instant = Instant.ofEpochMilli(imageTime.time)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        pairList.add("Date" to stateOf(imageTime.format(SimpleDateFormat("EEEE", Locale.ENGLISH)) + ", " + formatDate(localDate)))
        pairList.add("Time" to stateOf(formatTime(instant, true)))
    }
    val editTime = properties.metadata?.editTime
    if (editTime != null) {
        val instant = Instant.ofEpochMilli(editTime.time)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        pairList.add("Edit Date" to stateOf(editTime.format(SimpleDateFormat("EEEE", Locale.ENGLISH)) + ", " + formatDate(localDate)))
        pairList.add("Edit Time" to stateOf(formatTime(instant, true)))
    }
    pairList.add("Dimensions" to stateOf("").asyncMap(coroutineScope) { _ ->
        try {
            val image = withContext(Dispatchers.IO) {
                val bytes = Unpooled.wrappedBuffer(properties.id.open().use { it.readBytes() })
                platform.lwjgl3.get<NativeImageReader>().getImageData(bytes, UnpooledByteBufAllocator.DEFAULT)
                    .also { it.release() }
            }

            "${image.width}x${image.height} pixels"
        } catch (e: Exception) {
            LoggerFactory.getLogger(PropertiesModal::class.java).error("Failed to read image dimensions: ", e)
            "Unknown"
        }
    }.map { it ?: "Loading..." })
    return pairList
}


fun copyScreenshotToClipboard(screenshot: ScreenshotId) {
    val tmpFile = Files.createTempFile("screenshot", ".png")
    try {
        screenshot.open().use { stream ->
            Files.copy(stream, tmpFile, StandardCopyOption.REPLACE_EXISTING)
        }
        copyScreenshotToClipboard(tmpFile)
    } catch (e: IOException) {
        e.printStackTrace()
        Notifications.push("Error Copying Screenshot", "An unknown error occurred. Check logs for details")
    } finally {
        tmpFile.deleteIfExists()
    }
}

fun copyScreenshotToClipboard(screenshot: Path) {
    ForkedImageClipboard().use { clipboard ->
        if (clipboard.copy(screenshot.toFile())) {
            // When removing feature flag, the message param will no longer be used, so maybe clean it up
            sendPictureCopiedNotification()
        } else {
            Notifications.error("Failed to copy picture", "")
        }
    }
}

suspend fun saveImageAsScreenshot(image: BufferedImage) {
    withContext(Dispatchers.IO) {
        val folder = platform.screenshotFolder
        val baseName = "saved_" + SCREENSHOT_DATETIME_FORMAT.format(Date())
        val file = (0..Int.MAX_VALUE).firstNotNullOf { i ->
            folder.resolve(baseName + (if (i > 0) "_$i" else "") + ".png")
                .takeUnless { it.exists() }
        }
        file.outputStream().use { ImageIO.write(image, "png", it) }
    }
    Notifications.push("Picture saved", "", action = { platform.openScreenshotBrowser() }) {
        withCustomComponent(Slot.ICON, EssentialPalette.PICTURES_SHORT_9X7.create())
    }
}
