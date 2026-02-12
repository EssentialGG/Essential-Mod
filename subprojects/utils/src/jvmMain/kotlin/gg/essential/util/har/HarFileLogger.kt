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
package gg.essential.util.har

import gg.essential.util.globalEssentialDirectory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.Instant
import java.util.zip.GZIPOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.div
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream

class HarFileLogger(
    coroutineScope: CoroutineScope,
    private val creator: HarFile.Creator,
) {
    private val channel = Channel<HarFile.Entry>(1000)
    init {
        coroutineScope.launch(Dispatchers.IO) {
            main()
        }
    }

    private suspend fun main() {
        val folder = globalEssentialDirectory / "http-logs"
        folder.createDirectories()

        cleanupOldLogs(folder)

        val fileBaseName =
            Instant.now().toString()
                .replace(':', '_') // windows doesn't like colons in filenames
        val file = folder / "$fileBaseName.har.gz"
        file.outputStream().use { fileOut ->
            GZIPOutputStream(fileOut, 8196).use { gzipOut ->
                OutputStreamWriter(gzipOut, StandardCharsets.UTF_8).use { writer ->
                    writeLoop(writer)
                }
            }
        }
    }

    private fun cleanupOldLogs(folder: Path) {
        val files = try {
            folder.listDirectoryEntries().sortedByDescending { it.getLastModifiedTime() }
        } catch (e: IOException) {
            LOGGER.error("Failed to list files in {}", folder, e)
            return
        }
        for (file in files.drop(MAX_LOGS - 1)) {
            try {
                file.deleteExisting()
            } catch (e: IOException) {
                LOGGER.warn("Failed to delete {}", file, e)
                continue
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class) // for isEmpty, see https://github.com/Kotlin/kotlinx.coroutines/issues/1053#issuecomment-785138865
    private suspend fun writeLoop(writer: Writer) {
        val envelopeString = Json.encodeToString(HarFile(HarFile.Log(
            version = HarFile.VERSION,
            creator = creator,
            pages = emptyList(), // unsupported
            entries = emptyList(), // will be streamed below
        )))
        val header = envelopeString.substring(0 until envelopeString.length - 3)
        val footer = envelopeString.substring(envelopeString.length - 3)
        assert(header.endsWith("\"entries\":["))
        assert(footer == "]}}")

        writer.write(header)
        try {
            var first = true
            for (entry in channel) {
                if (first) first = false
                else writer.write(",")

                writer.write(Json.encodeToString(entry))

                if (channel.isEmpty) {
                    writer.flush()
                }
            }
        } catch (e: CancellationException) {
            writer.write(footer)
            throw e
        }
    }

    fun log(entry: HarFile.Entry) {
        channel.trySendBlocking(entry)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(HarFileLogger::class.java)
        private const val MAX_LOGS = 10
    }
}
