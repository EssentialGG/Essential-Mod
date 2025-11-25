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

import gg.essential.cosmetics.diagnoseParsingException
import gg.essential.cosmetics.skinmask.SkinMask
import gg.essential.mod.EssentialAsset
import gg.essential.mod.asset.AssetProvider
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.model.backend.RenderBackend
import gg.essential.mod.cosmetics.featured.FeaturedPageCollection as CosmeticFeaturedPageCollection
import gg.essential.model.file.AnimationFile
import gg.essential.model.file.ModelFile
import gg.essential.model.file.ParticlesFile
import gg.essential.model.file.SoundDefinitionsFile
import gg.essential.network.cosmetics.Cosmetic.Diagnostic
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.LimitedExecutor
import gg.essential.util.httpClient
import gg.essential.util.image.bitmap.Bitmap
import gg.essential.util.image.bitmap.fromOrThrow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Request
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URL
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class AssetLoader(private val cachePath: Path) {
    companion object {
        private val logger = LoggerFactory.getLogger(AssetLoader::class.java)
    }

    private val pool = ThreadPoolExecutor(
        0, Int.MAX_VALUE,
        10L, TimeUnit.SECONDS,
        LinkedBlockingQueue(),
        AtomicInteger().let { threadId ->
            ThreadFactory { Thread(it, "Essential Asset Loader " + threadId.incrementAndGet()) }
        },
    )
    private val networkExecutor = LimitedExecutor(pool, 10, PriorityBlockingQueue())
    private val diskExecutor = LimitedExecutor(pool, 10, PriorityBlockingQueue())

    private val assets: MutableMap<String, AssetState> = ConcurrentHashMap()

    fun getAssetBytes(asset: EssentialAsset, priority: Priority): CompletableFuture<ByteArray> {
        val state = assets.computeIfAbsent(asset.checksum) { AssetState(asset) }
        state.ensurePriorityAtLeast(priority)
        return state.future
    }

    fun getProvider(priority: Priority) =
        object : AssetProvider {
            override suspend fun getBytes(asset: EssentialAsset): ByteArray =
                getAssetBytes(asset, priority).await()
        }

    fun <T> getAsset(asset: EssentialAsset, priority: Priority, type: AssetType<T>): Asset<T> {
        val state = assets.computeIfAbsent(asset.checksum) { AssetState(asset) }
        state.ensurePriorityAtLeast(priority)
        @Suppress("UNCHECKED_CAST")
        return state.parsed.computeIfAbsent(type) {
            val future: CompletableFuture<T> = state.future.thenApplyAsync { bytes ->
                try {
                    type.parse(asset, bytes)
                } catch (e: Exception) {
                    throw ParseException(asset, type, bytes, e)
                }
            }
            Asset(state.info, type, state.future, future)
        } as Asset<T>
    }

    fun getKnownAsset(checksum: String, priority: Priority): CompletableFuture<ByteArray>? {
        val state = assets[checksum] ?: return null
        state.ensurePriorityAtLeast(priority)
        return state.future
    }

    private fun EssentialAsset.getAssetCachePath(): Path {
        val checksum = checksum
        return cachePath
            .resolve(checksum[0].toString())
            .resolve(checksum[1].toString())
            .resolve(checksum.substring(2))
    }

    class Asset<T> internal constructor(
        val info: EssentialAsset,
        val type: AssetType<T>,
        val bytes: CompletableFuture<ByteArray>,
        val parsed: CompletableFuture<T>,
    ) {
        val diagnostics: CompletableFuture<List<Diagnostic>> by lazy {
            if (type is JsonAssetType<*>) {
                bytes.thenApplyAsync { bytes ->
                    val fileContent = bytes.decodeToString()
                    try {
                        type.strictJson.decodeFromString(type.serializer, fileContent)
                        emptyList()
                    } catch (e: SerializationException) {
                        listOf(diagnoseParsingException(Diagnostic.Type.Warning, e, fileContent))
                    } catch (e: Exception) {
                        val msg = "Failed to parse as $type with strict validation"
                        listOf(Diagnostic.warning(msg, stacktrace = e.stackTraceToString()))
                    }
                }
            } else {
                CompletableFuture.completedFuture(emptyList())
            }
        }

        /**
         * The [parsed] value or `null` if it failed to load due to an exception.
         * The exception is logged.
         */
        val parsedOrNull: CompletableFuture<T?> by lazy {
            parsed.exceptionally { t ->
                logger.error("Failed to load $type from ${info.url}", t)
                null
            }
        }
    }

    private inner class AssetState(val info: EssentialAsset) {
        val future = CompletableFuture<ByteArray>()

        val parsed: MutableMap<AssetType<*>, Asset<*>> = ConcurrentHashMap()

        private val priority = AtomicReference(Priority.Passive)
        private var currentStep: Step? = TryLoadFromCache(this)

        fun ensurePriorityAtLeast(atLeast: Priority) {
            if (priority.getAndUpdate { if (it < atLeast) atLeast else it } < atLeast) {
                currentStep?.submit(atLeast)
            }
        }

        fun nextStep(step: Step) {
            currentStep = step
            step.submit(priority.get())
        }
    }

    private inner class TryLoadFromCache(private val assetState: AssetState) : Step(diskExecutor) {
        override fun run() {
            val bytes = tryLoadFromCache(assetState.info)
            if (bytes != null) {
                assetState.future.complete(bytes)
            } else {
                assetState.nextStep(Download(assetState))
            }
        }

        fun tryLoadFromCache(asset: EssentialAsset): ByteArray? {
            val cachePath = asset.getAssetCachePath()
            if (!Files.exists(cachePath)) {
                return null
            }
            try {
                val bytes = Files.readAllBytes(cachePath)
                val checksum = when (asset.checksum.length) {
                    64 -> DigestUtils.sha256Hex(bytes)
                    40 -> DigestUtils.sha1Hex(bytes)
                    32 -> DigestUtils.md5Hex(bytes)
                    else -> {
                        logger.warn("Unknown checksum format for ${asset.url}: ${asset.checksum}")
                        return null
                    }
                }
                if (!checksum.equals(asset.checksum, ignoreCase = true)) {
                    logger.warn("Checksum mismatch for {}: {}", cachePath, checksum)
                    return null
                }
                return bytes
            } catch (e: IOException) {
                logger.warn("Failed to read asset from cache at $cachePath: ", e)
                return null
            }
        }
    }

    private inner class Download(private val assetState: AssetState) : Step(networkExecutor) {
        override fun run() {
            val bytes = try {
                when (assetState.info.url.substringBefore(":")) {
                    "http", "https" -> {
                        val request = Request.Builder()
                            .url(assetState.info.url)
                            .header("User-Agent", "Mozilla/4.76 (Essential Asset Downloader)")
                            .build()
                        httpClient.join().newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw IOException("Unexpected response $response")
                            response.body()!!.bytes()
                        }
                    }
                    "file" -> {
                        URL(assetState.info.url).openStream().use { it.readBytes() }
                    }
                    "data" -> {
                        val path = assetState.info.url.removePrefix("data:")
                        val (options, data) = path.split(',', limit = 2)
                        if (options.endsWith(";base64")) {
                            Base64.getDecoder().decode(data)
                        } else {
                            URLDecoder.decode(data, "UTF-8").encodeToByteArray()
                        }
                    }
                    else -> throw IllegalArgumentException("unsupported url scheme: ${assetState.info.url}")
                }
            } catch (e: IOException) {
                assetState.future.completeExceptionally(e)
                return
            }
            assetState.future.complete(bytes)

            // Saving the cache to disk always has lowest priority and we don't need to wait for it.
            // In the unlikely worst case, if the JVM quits before we finish saving, we'll just have to download again.
            SaveToCache(assetState, bytes).submit(Priority.Background)
        }
    }

    private inner class SaveToCache(private val assetState: AssetState, private val bytes: ByteArray) : Step(diskExecutor) {
        override fun run() {
            val cachePath = assetState.info.getAssetCachePath()
            try {
                Files.createDirectories(cachePath.parent)
                Files.write(cachePath, bytes)
            } catch (e: IOException) {
                logger.error("Failed to cache \"${assetState.info.url}\" at \"$cachePath\": ", e)
            }
        }
    }

    private abstract class Step(private val executor: Executor) : Runnable {
        private val state = AtomicReference(State.Pending)
        private val priority = AtomicReference(Priority.Passive)

        fun submit(atLeast: Priority) {
            if (state.get() != State.Pending) {
                return // too late, task is already executing or done
            }
            if (priority.getAndUpdate { if (it < atLeast) atLeast else it } < atLeast) {
                executor.execute(PrioritizedJob(atLeast))
            }
        }

        private inner class PrioritizedJob(private val priority: Priority) : Runnable, Comparable<PrioritizedJob> {
            override fun compareTo(other: PrioritizedJob): Int =
                -priority.compareTo(other.priority)

            override fun run() {
                if (!state.compareAndSet(State.Pending, State.Running)) {
                    return // another job (probably with higher priority) already got it
                }
                try {
                    this@Step.run()
                } finally {
                    state.set(State.Done)
                }
            }
        }

        private enum class State {
            Pending,
            Running,
            Done,
        }
    }

    enum class Priority {
        /** Will not load the asset by itself, only once someone requests a higher priority. */
        Passive,
        /** Just in case we need it in the future. Lower priority than even [Background]. */
        BackgroundUnlikely,
        /** Just in case we need it in the future. */
        Background,
        /** The user may start looking at this at any moment. */
        Low,
        /** The user is probably looking at this right now. */
        High,
        /** User is actively waiting for this. */
        Blocking,
    }

    class ParseException(
        val asset: EssentialAsset,
        val type: AssetType<*>,
        val bytes: ByteArray,
        cause: Throwable,
    ) : Exception(cause)

    abstract class AssetType<T>(
        val parse: (asset: EssentialAsset, bytes: ByteArray) -> T
    ) {
        data object Raw : AssetType<ByteArray>({ _, bytes -> bytes })
        data object Model : JsonAssetType<ModelFile>(ModelFile.serializer())
        data object Animation : JsonAssetType<AnimationFile>(AnimationFile.serializer())
        data object Particle : JsonAssetType<ParticlesFile>(ParticlesFile.serializer())
        data object SoundDefinitions : JsonAssetType<SoundDefinitionsFile>(SoundDefinitionsFile.serializer())
        data object Texture : AssetType<RenderBackend.Texture>({ asset, bytes ->
            runBlocking { platform.renderBackend.readTexture(asset.checksum, bytes) }
        })
        data object Mask : AssetType<SkinMask>({ _, bytes ->
            SkinMask.read(Bitmap.fromOrThrow(bytes.inputStream()))
        })
        data object FeaturedPageCollection : JsonAssetType<CosmeticFeaturedPageCollection>(CosmeticFeaturedPageCollection.serializer())
    }

    abstract class JsonAssetType<T>(
        val serializer: KSerializer<T>,
        val json: Json = Json {
            serializersModule = CosmeticSetting.TheSerializer.module
            ignoreUnknownKeys = true
            coerceInputValues = true
        },
        val strictJson: Json = Json(json) {
            ignoreUnknownKeys = false
            coerceInputValues = false
        },
    ) : AssetType<T>({ _, bytes ->
        json.decodeFromString(serializer, bytes.decodeToString())
    })
}
