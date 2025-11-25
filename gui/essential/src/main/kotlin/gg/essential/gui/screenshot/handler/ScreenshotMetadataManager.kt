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

import com.google.common.collect.MapMaker
import com.google.common.collect.Sets
import com.sparkuniverse.toolbox.serialization.DateTimeTypeAdapter
import com.sparkuniverse.toolbox.serialization.UUIDTypeAdapter
import com.sparkuniverse.toolbox.util.DateTime
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.handlers.screenshot.ClientScreenshotMetadata.Location
import gg.essential.lib.gson.GsonBuilder
import gg.essential.lib.gson.JsonSyntaxException
import gg.essential.lib.gson.annotations.SerializedName
import gg.essential.network.connectionmanager.media.IScreenshotMetadataManager
import gg.essential.util.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Note: Metadata returned by this class does not yet have [ClientScreenshotMetadata.ownedMediaId] set!
 *       The caller is expected to derive it from a list of known owned remote media.
 */
class ScreenshotMetadataManager(
    private val metadataFolder: File,
    private val screenshotChecksumManager: ScreenshotChecksumManager,
) : IScreenshotMetadataManager {
    private val gson = GsonBuilder()
        .registerTypeAdapter(UUID::class.java, UUIDTypeAdapter())
        .registerTypeAdapter(DateTime::class.java, DateTimeTypeAdapter())
        .create()

    private val metadataCache: MutableMap<String, ClientScreenshotMetadata> = ConcurrentHashMap()

    // Screenshot checksums which have no metadata
    private val negativeChecksumCache = Sets.newConcurrentHashSet<String>()

    fun updateMetadata(screenshotMetadata: ClientScreenshotMetadata) {
        metadataCache[screenshotMetadata.checksum] = screenshotMetadata
        writeMetadata(screenshotMetadata)
        updateState(screenshotMetadata.checksum, screenshotMetadata)
    }

    private fun readMetadata(imageChecksum: String): ClientScreenshotMetadata? {
        return try {
            val fileContents = File(metadataFolder, imageChecksum).readText()
            gson.fromJson(fileContents, SerializedMetadata::class.java)?.toMod()
        } catch (exception: JsonSyntaxException) {
            LOGGER.error("Metadata corrupt for checksum $imageChecksum. Attempting recovery.", exception)
            tryRecoverMetadata(imageChecksum)
        } catch (ignored: FileNotFoundException) {
            null
        }
    }

    /**
     * Attempts to recover metadata from cached metadata. If that fails, attempts to create new metadata if the metadata file exists.
     * @return  The recovered [ClientScreenshotMetadata] if it exists in the cache, new metadata if the metadata file exists, or null if neither exist
     */
    private fun tryRecoverMetadata(checksum: String): ClientScreenshotMetadata? {
        val metadata = screenshotChecksumManager.getPathsForChecksum(checksum).firstOrNull()?.let {
            ClientScreenshotMetadata.createUnknown(it, checksum)
        }
        metadata?.let { writeMetadata(it) }
        return metadata
    }

    private fun getMetadata(checksum: String): ClientScreenshotMetadata? {
        if (negativeChecksumCache.contains(checksum)) {
            return null
        }
        val metadata = metadataCache[checksum]
            ?: readMetadata(checksum)?.also { metadata ->
                metadataCache[checksum] = metadata
                updateState(metadata.checksum, metadata)
            }
        if (metadata == null) {
            negativeChecksumCache.add(checksum)
        }
        return metadata
    }

    override fun getMetadata(path: Path): ClientScreenshotMetadata? {
        return getMetadata(path.toFile())
    }

    override fun getMetadata(file: File): ClientScreenshotMetadata? {
        val imageChecksum = screenshotChecksumManager[file] ?: return null

        return getMetadata(imageChecksum)
    }

    /**
     * Get metadata straight from the cache using a media id.
     */
    fun getMetadataCache(mediaId: String): ClientScreenshotMetadata? {
        return metadataCache.values.firstOrNull { mediaId in it.mediaIds }
    }

    private fun writeMetadata(metadata: ClientScreenshotMetadata) {
        negativeChecksumCache.remove(metadata.checksum)
        try {
            File(metadataFolder, metadata.checksum).writeText(gson.toJson(SerializedMetadata.fromMod(metadata)))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Called when a screenshot file is deleted externally
     * such as the user deleting it in their file manager
     */
    fun handleExternalDelete(fileName: String) {
        val checksum = screenshotChecksumManager.remove(fileName) ?: return
        val metadata = getMetadata(checksum)
        if (metadata != null) {
            deleteMetadata(metadata)
        }
    }

    private fun deleteMetadata(metadata: ClientScreenshotMetadata) {
        val metadataFile = File(metadataFolder, metadata.checksum)
        metadataCache.remove(metadata.checksum)
        updateState(metadata.checksum, null)
        metadataFile.delete()
    }

    fun deleteMetadata(file: File) {
        val metadata = getMetadata(file)
        if (metadata != null) {
            deleteMetadata(metadata)
            screenshotChecksumManager.delete(file)
        }
    }

    @Synchronized
    override fun getOrCreateMetadata(path: Path): ClientScreenshotMetadata {
        val file = path.toFile()

        val existingMetadata = getMetadata(file)
        if (existingMetadata != null) {
            return existingMetadata
        }

        val checksum = screenshotChecksumManager[file] ?: throw IllegalStateException("No checksum for file $file. Was the file deleted?")

        return ClientScreenshotMetadata.createUnknown(path, checksum).also {
            updateMetadata(it)
        }
    }

    private val stateByChecksum: MutableMap<String, MutableState<ClientScreenshotMetadata?>> = MapMaker().weakValues().makeMap()

    private fun updateState(checksum: String, newMetadata: ClientScreenshotMetadata?) {
        runBlocking(Dispatchers.Client) {
            stateByChecksum[checksum]?.set(newMetadata)
        }
    }

    fun metadata(checksum: String): State<ClientScreenshotMetadata?> {
        return stateByChecksum.getOrPut(checksum) { mutableStateOf(getMetadata(checksum)) }
    }

    private data class SerializedMetadata(
        @SerializedName("authorId", alternate = ["a"])
        val authorId: UUID,
        @SerializedName("time", alternate = ["b"])
        val time: DateTime,
        @SerializedName("checksum", alternate = ["c"])
        val checksum: String,
        @SerializedName("editTime")
        val editTime: DateTime?,
        @SerializedName("locationMetadata", alternate = ["d"])
        val locationMetadata: Location,
        @SerializedName("favorite", alternate = ["e"])
        val favorite: Boolean,
        @SerializedName("edited", alternate = ["f"])
        val edited: Boolean,
        val mediaId: String? = null,
        val moreMediaIds: List<String>? = null,
    ) {
        fun toMod() = ClientScreenshotMetadata(
            authorId,
            time,
            checksum,
            editTime,
            locationMetadata,
            favorite,
            edited,
            mediaIds = when {
                mediaId == null -> emptySet()
                moreMediaIds == null -> setOf(mediaId)
                else -> buildSet { add(mediaId); addAll(moreMediaIds) }
            },
            ownedMediaId = null,
        )

        companion object {
            fun fromMod(mod: ClientScreenshotMetadata) = with(mod) {
                val mediaIdsIter = mediaIds.iterator()
                val firstMediaId = if (mediaIdsIter.hasNext()) mediaIdsIter.next() else null
                val moreMediaIds = if (mediaIdsIter.hasNext()) mediaIdsIter.asSequence().toList() else null
                SerializedMetadata(
                    authorId,
                    time,
                    checksum,
                    editTime,
                    locationMetadata,
                    favorite,
                    edited,
                    firstMediaId,
                    moreMediaIds,
                )
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ScreenshotMetadataManager::class.java)
    }
}