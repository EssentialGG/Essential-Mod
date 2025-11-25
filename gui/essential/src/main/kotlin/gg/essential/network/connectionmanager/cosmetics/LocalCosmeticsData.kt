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
package gg.essential.network.connectionmanager.cosmetics

import gg.essential.cosmetics.CosmeticBundleId
import gg.essential.cosmetics.CosmeticCategoryId
import gg.essential.cosmetics.CosmeticId
import gg.essential.cosmetics.CosmeticTypeId
import gg.essential.cosmetics.FeaturedPageCollectionId
import gg.essential.cosmetics.ImplicitOwnership
import gg.essential.cosmetics.ImplicitOwnershipId
import gg.essential.gui.elementa.state.v2.add
import gg.essential.gui.elementa.state.v2.removeAt
import gg.essential.gui.elementa.state.v2.set
import gg.essential.handlers.io.DirectoryWatcher
import gg.essential.handlers.io.FileEventType
import gg.essential.handlers.io.FileSystemEvent
import gg.essential.mod.cosmetics.CosmeticBundle
import gg.essential.mod.cosmetics.CosmeticCategory
import gg.essential.mod.cosmetics.CosmeticType
import gg.essential.mod.cosmetics.database.GitRepoCosmeticsDatabase
import gg.essential.mod.cosmetics.featured.FeaturedPageCollection
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.util.Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo
import kotlin.io.path.writeBytes
import kotlin.streams.toList

class LocalCosmeticsData private constructor(
    private val rootPath: Path,
    private val assetLoader: AssetLoader,
    private val state: MutableCosmeticsData = MutableCosmeticsData(),
) : CosmeticsData by state {
    constructor(rootPath: Path, assetLoader: AssetLoader) : this(rootPath, assetLoader, MutableCosmeticsData())

    private val database = GitRepoCosmeticsDatabase(lazy = false, fetchAsset = {
        assetLoader.getAssetBytes(it, AssetLoader.Priority.High).await()
    })

    private val watcher = DirectoryWatcher(rootPath, true, 200, TimeUnit.MILLISECONDS)
    private val updateChannel = Channel<Msg>(Int.MAX_VALUE)
    private val updateScope = CoroutineScope(Dispatchers.Default)

    init {
        val files = Files.walk(rootPath).use { it.toList() }.filter { it.isRegularFile() }
        updateChannel.trySend(Msg.FilesChanged(files.map { FileSystemEvent(it, FileEventType.CREATE) }))

        watcher.onBatchUpdate { events ->
            updateChannel.trySend(Msg.FilesChanged(events))
        }

        updateScope.launch {
            while (true) {
                processMsg(updateChannel.receive())
            }
        }
    }

    private suspend fun processMsg(msg: Msg) {
        try {
            when (msg) {
                is Msg.FilesChanged -> processFileChangedEvents(msg)
                is Msg.WriteData -> processWriteDataMsg(msg)
            }
        } catch (e: Exception) {
            LOGGER.error("Error handling $msg:", e)
        }
    }

    private suspend fun processFileChangedEvents(initialMsg: Msg.FilesChanged) {
        val addedFiles = mutableSetOf<Path>()
        val removedFiles = mutableSetOf<Path>()

        fun collectEvents(events: List<FileSystemEvent>) {
            for (event in events) {
                when (event.eventType) {
                    FileEventType.CREATE, FileEventType.MODIFY -> {
                        removedFiles.remove(event.path)
                        if (Files.isRegularFile(event.path)) {
                            addedFiles.add(event.path)
                        }
                    }
                    FileEventType.DELETE -> {
                        addedFiles.remove(event.path)
                        removedFiles.add(event.path)
                    }
                }
            }
        }

        collectEvents(initialMsg.updates)
        var nextMsg: Msg? = null
        while (true) {
            nextMsg = updateChannel.tryReceive().getOrNull() ?: break
            if (nextMsg is Msg.FilesChanged) {
                collectEvents(nextMsg.updates)
                nextMsg = null
            }
        }

        var changes = GitRepoCosmeticsDatabase.Changes.Empty
        if (removedFiles.isNotEmpty()) {
            changes += database.removeFiles(removedFiles.map { it.relativeTo(rootPath).toString() }.toSet())
        }
        if (addedFiles.isNotEmpty()) {
            val files = coroutineScope {
                addedFiles.associateWith {
                    async(Dispatchers.IO) { it.readBytes() }
                }.mapKeys { (path, _) ->
                    path.relativeTo(rootPath).toString()
                }.mapValues { (_, bytes) ->
                    suspend { bytes.await() }
                }
            }
            changes += database.addFiles(files)
        }

        withContext(Dispatchers.Client) {
            update(changes)
        }

        if (nextMsg != null) {
            processMsg(nextMsg)
        }
    }

    private fun update(changes: GitRepoCosmeticsDatabase.Changes) {
        for (id in changes.categories) {
            val existingIndex = state.categories.get().indexOfFirst { it.id == id }
            val category = database.loadedCategories[id]
            if (category != null) {
                if (existingIndex >= 0) {
                    state.categories.set(existingIndex, category)
                } else {
                    state.categories.add(category)
                }
            } else {
                state.categories.removeAt(existingIndex)
            }
        }

        for (id in changes.types) {
            val existingIndex = state.types.get().indexOfFirst { it.id == id }
            val type = database.loadedTypes[id]
            if (type != null) {
                if (existingIndex >= 0) {
                    state.types.set(existingIndex, type)
                } else {
                    state.types.add(type)
                }
            } else {
                state.types.removeAt(existingIndex)
            }
        }

        for (id in changes.bundles) {
            val existingIndex = state.bundles.get().indexOfFirst { it.id == id }
            val bundle = database.loadedBundles[id]
            if (bundle != null) {
                if (existingIndex >= 0) {
                    state.bundles.set(existingIndex, bundle)
                } else {
                    state.bundles.add(bundle)
                }
            } else {
                state.bundles.removeAt(existingIndex)
            }
        }

        for (id in changes.featuredPageCollections) {
            val existingIndex = state.featuredPageCollections.get().indexOfFirst { it.id == id }
            val featuredPageCollection = database.loadedFeaturedPageCollections[id]
            if (featuredPageCollection != null) {
                if (existingIndex >= 0) {
                    state.featuredPageCollections.set(existingIndex, featuredPageCollection)
                } else {
                    state.featuredPageCollections.add(featuredPageCollection)
                }
            } else {
                state.featuredPageCollections.removeAt(existingIndex)
            }
        }

        for (id in changes.implicitOwnerships) {
            val existingIndex = state.implicitOwnerships.get().indexOfFirst { it.id == id }
            val implicitOwnership = database.loadedImplicitOwnerships[id]
            if (implicitOwnership != null) {
                if (existingIndex >= 0) {
                    state.implicitOwnerships.set(existingIndex, implicitOwnership)
                } else {
                    state.implicitOwnerships.add(implicitOwnership)
                }
            } else {
                state.implicitOwnerships.removeAt(existingIndex)
            }
        }

        for (id in changes.cosmetics) {
            val existingIndex = state.cosmetics.get().indexOfFirst { it.id == id }
            val cosmetic = database.loadedCosmetics[id]
            if (cosmetic != null) {
                if (existingIndex >= 0) {
                    state.cosmetics.set(existingIndex, cosmetic)
                } else {
                    state.cosmetics.add(cosmetic)
                }
            } else {
                state.cosmetics.removeAt(existingIndex)
            }
        }
    }

    private suspend fun processWriteDataMsg(msg: Msg.WriteData) {
        // Compute all necessary changes
        val changes = mutableMapOf<String, ByteArray?>()
        for ((id, category) in msg.categories) {
            changes.putAll(database.computeChanges(id, category))
        }
        for ((id, type) in msg.types) {
            changes.putAll(database.computeChanges(id, type))
        }
        for ((id, bundle) in msg.bundles) {
            changes.putAll(database.computeChanges(id, bundle))
        }
        for ((id, featuredPage) in msg.featuredPageCollections) {
            changes.putAll(database.computeChanges(id, featuredPage))
        }
        for ((id, implicitOwnership) in msg.implicitOwnerships) {
            changes.putAll(database.computeChanges(id, implicitOwnership))
        }
        for ((id, cosmetic) in msg.cosmetics) {
            changes.putAll(database.computeChanges(id, cosmetic))
        }

        // Apply changes to file system
        withContext(Dispatchers.IO) {
            for ((pathStr, bytes) in changes) {
                val path = rootPath.resolve(pathStr)
                launch {
                    if (bytes != null) {
                        path.parent.createDirectories()
                        path.writeBytes(bytes)
                    } else {
                        path.deleteIfExists()
                    }
                }
            }
        }

        // Once done, immediately apply changes to in-memory database as well, so everything is up-to-date by the time
        // we resolve the future
        processFileChangedEvents(Msg.FilesChanged(changes.map { (path, bytes) ->
            // We don't need to differentiate between MODIFY and NEW, they both do the same thing
            val type = if (bytes == null) FileEventType.DELETE else FileEventType.MODIFY
            FileSystemEvent(rootPath.fileSystem.getPath(path), type)
        }))

        // All done, report back
        msg.future.complete(Unit)
    }

    fun writeChanges(
        categories: Map<CosmeticCategoryId, CosmeticCategory?>,
        types: Map<CosmeticTypeId, CosmeticType?>,
        bundles: Map<CosmeticBundleId, CosmeticBundle?>,
        featuredPageCollections: Map<FeaturedPageCollectionId, FeaturedPageCollection?>,
        implicitOwnerships: Map<ImplicitOwnershipId, ImplicitOwnership?>,
        cosmetics: Map<CosmeticId, Cosmetic?>,
    ): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        updateChannel.trySend(
            Msg.WriteData(
                categories,
                types,
                bundles,
                featuredPageCollections,
                implicitOwnerships,
                cosmetics,
                future
            )
        )
        return future
    }

    private sealed interface Msg {
        class FilesChanged(val updates: List<FileSystemEvent>) : Msg
        class WriteData(
            val categories: Map<CosmeticCategoryId, CosmeticCategory?>,
            val types: Map<CosmeticTypeId, CosmeticType?>,
            val bundles: Map<CosmeticBundleId, CosmeticBundle?>,
            val featuredPageCollections: Map<FeaturedPageCollectionId, FeaturedPageCollection?>,
            val implicitOwnerships: Map<ImplicitOwnershipId, ImplicitOwnership?>,
            val cosmetics: Map<CosmeticId, Cosmetic?>,
            val future: CompletableFuture<Unit>,
        ) : Msg
    }

    companion object {
        private val LOGGER = LogManager.getLogger()
    }
}
