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
package gg.essential.gui.screenshot.handler

import gg.essential.lib.gson.Gson
import gg.essential.lib.gson.JsonSyntaxException
import gg.essential.vigilance.impl.nightconfig.core.utils.ObservedMap
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.IOException
import java.nio.file.Path

/**
 * Manages the file -> checksum relationship for screenshot metadata
 */
class ScreenshotChecksumManager(
    private val screenshotFolder: File,
    private val cacheFile: File,
) {
    private val gson = Gson()
    private var persistChanges = true
    private val entries = ObservedMap(mutableMapOf<ChecksumSnapshot, String>()) {
        saveState()
    }

    init {
        synchronized(entries) {
            if (cacheFile.exists()) {
                try {
                    // Initial population shouldn't trigger a save to disk
                    persistChanges = false

                    val data = gson.fromJson(
                        cacheFile.readText(),
                        Array<SerializedChecksum>::class.java
                    )
                    if (data != null) {
                        for (entry in data) {
                            entries[entry.snapshot] = entry.checksum
                        }
                    }
                } catch (e: JsonSyntaxException) {
                    // The file is corrupted, let's delete it and let the cache rebuild.
                    cacheFile.delete()
                } finally {
                    persistChanges = true
                }
            }
        }
    }

    /**
     * Adds an entry from this file to the provided checksum\
     */
    operator fun set(file: File, checksum: String) {
        synchronized(entries) {
            entries[getChecksumSnapshot(file)] = checksum
        }
    }

    /**
     * Returns the checksum of the given file
     */
    operator fun get(file: File): String? {
        synchronized(entries) {
            val checksumSnapshot = getChecksumSnapshot(file)

            // computeIfAbsent always initiates the callback, even if the map is not updated
            val get = entries[checksumSnapshot]
            return if (get != null) {
                get
            } else {
                val fileChecksum = readFileChecksum(file)
                fileChecksum?.also {
                    entries[checksumSnapshot] = fileChecksum
                }
            }
        }
    }

    /**
     * @return  The [Path] corresponding to the given [checksum] or null if it does not exist
     */
    fun getPathsForChecksum(checksum: String): List<Path> {
        synchronized(entries) {
            return entries.entries.filter {
                it.value == checksum
            }.map {
                File(screenshotFolder, it.key.name).toPath()
            }
        }
    }

    /**
     * Removes the item with the supplied name and returns the checksum, if present.
     */
    fun remove(name: String): String? {
        synchronized(entries) {
            return entries.entries.firstOrNull {
                it.key.name == name
            }?.let {
                entries.remove(it.key)
                it.value
            }
        }
    }

    private fun saveState() {
        if (!persistChanges) {
            return
        }

        synchronized(entries) {
            cacheFile.writeText(
                gson.toJson(entries.map { SerializedChecksum(it.value, it.key) }),
            )
        }
    }

    /**
     * Reads the checksum of the file from disk
     */
    private fun readFileChecksum(file: File): String? {
        return try {
            DigestUtils.md5Hex(file.readBytes())
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun getChecksumSnapshot(file: File): ChecksumSnapshot {
        return ChecksumSnapshot(file.name, file.lastModified(), file.length())
    }

    /**
     * Deletes the checksum relationship from the supplied file
     */
    fun delete(file: File) {
        entries.remove(getChecksumSnapshot(file))
    }
}

private data class SerializedChecksum(val checksum: String, val snapshot: ChecksumSnapshot)

private data class ChecksumSnapshot(val name: String, val lastModified: Long, val size: Long)