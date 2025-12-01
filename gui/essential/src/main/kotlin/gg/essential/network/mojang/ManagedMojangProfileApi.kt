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
package gg.essential.network.mojang

import gg.essential.mod.Model
import gg.essential.mod.Skin
import gg.essential.network.mojang.MojangProfileApi.Profile
import gg.essential.network.mojang.MojangProfileLookupApi.Property
import gg.essential.util.USession
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Manages access to [MojangProfileApi] for one particular user, providing local caching and locking.
 */
class ManagedMojangProfileApi private constructor(val uuid: UUID) {

    private val lock = Mutex()

    private var cachedProfile: Profile? = null

    private var latestAccessToken: String? =
        USession.activeNow().takeIf { it.uuid == uuid }?.token

    private fun createApiInstance(): MojangProfileApi {
        val accessToken = USession.activeNow()
            .takeIf { it.uuid == uuid }
            ?.token
            ?.also { latestAccessToken = it }
            ?: latestAccessToken
            ?: "<unknown>"
        return MojangProfileApi(accessToken)
    }

    /** Invalidates the cached profile, such that it will be re-fetched fresh from Mojang when next needed. */
    fun invalidateProfile() {
        cachedProfile = null
    }

    suspend fun fetch(): Profile = lock.withLock {
        fetchInternal()
    }
    /** [fetch] but the caller is expected to already hold the [lock] */
    private suspend fun fetchInternal(): Profile {
        val api = createApiInstance()

        return cachedProfile ?: api.fetch().also { cachedProfile = it }
    }

    /**
     * @return the new profile, or `null` when no change was necessary.
     */
    suspend fun putSkin(skin: Skin): Profile? = lock.withLock {
        putSkinInternal(skin)
    }
    /** [putSkin] but the caller is expected to already hold the [lock] */
    private suspend fun putSkinInternal(skin: Skin?): Profile? {
        val api = createApiInstance()

        val cached = fetchInternal()
        if (cached.activeSkin == skin) {
            return null
        }

        return try {
            api.putSkin(skin)
                .also { cachedProfile = it }
        } catch (e: Exception) {
            cachedProfile = null
            throw e
        }
    }

    /**
     * @return the new profile, or `null` when no change was necessary.
     */
    suspend fun putCape(id: String?): Profile? = lock.withLock {
        putCapeInternal(id)
    }
    /** [putCape] but the caller is expected to already hold the [lock] */
    private suspend fun putCapeInternal(id: String?): Profile? {
        val api = createApiInstance()

        val cached = fetchInternal()
        if (cached.activeCapeId == id) {
            return null
        }

        return try {
            api.putCape(id)
                .also { cachedProfile = it }
        } catch (e: Exception) {
            cachedProfile = null
            throw e
        }
    }

    suspend fun uploadSkin(png: ByteArray, model: Model): Skin = lock.withLock {
        val api = createApiInstance()

        val orgProfile = fetchInternal()
        try {
            try {
                api.putSkin(png, model)
                    .also { cachedProfile = it }
            } catch (e: Exception) {
                cachedProfile = null
                throw e
            }
        } finally {
            withContext(NonCancellable) {
                try {
                    putSkinInternal(orgProfile.activeSkin)
                } catch (e: Exception) {
                    LOGGER.error("Failed to revert skin after upload", e)
                }
            }

        }.activeSkin!!
    }

    private val cachedCapeSignatures = mutableMapOf<String, Property>()
    suspend fun obtainSignaturesForCapes(missing: List<MojangProfileApi.Cape>): List<Pair<MojangProfileApi.Cape, Property>> = lock.withLock {
        val result = mutableListOf<Pair<MojangProfileApi.Cape, Property>>()

        // Fetching signatures requires changing the active cape, we need to revert that later
        val orgProfile = fetchInternal()
        try {
            for (cape in missing) {
                val cachedProperty = cachedCapeSignatures[cape.id]
                if (cachedProperty != null) {
                    result.add(cape to cachedProperty)
                    continue
                }

                // If the cape is not currently active, we need to activate it cause that's the only way to get a signature
                try {
                    putCapeInternal(cape.id)
                } catch (e: Exception) {
                    LOGGER.error("Failed to put cape {}", cape, e)
                    continue
                }
                // Fetch a signature for this cape
                val property = obtainSignedTexturesInternal() ?: continue
                cachedCapeSignatures[cape.id] = property
                result.add(cape to property)
            }
        } finally {
            // If we had to change the active cape on Mojang's end, revert it to what the user expects
            withContext(NonCancellable) {
                try {
                    putCapeInternal(orgProfile.activeCapeId)
                } catch (e: Exception) {
                    LOGGER.error("Failed to revert cape", e)
                }
            }
        }

        return result
    }

    private var cachedSignedTextures: Property? = null

    suspend fun obtainSignedTextures(): Property? = lock.withLock {
        obtainSignedTexturesInternal()
    }
    /** [obtainSignedTextures] but the caller is expected to already hold the [lock] */
    private suspend fun obtainSignedTexturesInternal(): Property? {
        val expectedProfile = fetchInternal()
        val expectedSkin = expectedProfile.activeSkin
        val expectedCape = expectedProfile.activeCape?.hash

        val cached = cachedSignedTextures?.decodedAsTextures
        if (cached != null && cached.skin == expectedSkin && cached.capeHash == expectedCape) {
            return cachedSignedTextures
        }

        // The Azure proxy in front of this API endpoint may cache responses for extended periods of time (30+ seconds).
        // If we find such an outdated response, we'll try to bypass that cache on the next request by an adding extra
        // query parameter with a previously-unused (random) value.
        // Typically this will only happen when we try to obtain multiple different signed values in quick succession
        // (e.g. to unlock multiple capes).
        // So to not unnecessarily bypass the standard caching, we only start doing this from the second request. This
        // also naturally adds a bit of delay so we don't load Mojang too much.
        var bypassProxyCache = false

        val maxDelay = 60000L
        var delay = 500L
        while (true) {
            val property = try {
                MojangProfileLookupApi.fetch(uuid, signed = true, bypassProxyCache = bypassProxyCache)?.texturesProperty
            } catch (e: Exception) {
                LOGGER.error("Failed to fetch texture property", e)
                null
            }
            // If the hash of the texture property from Mojang doesn't match, it means the API instance hasn't updated yet
            // See: EM-2640
            val textures = property?.decodedAsTextures
            if (textures != null && textures.skin == expectedSkin && textures.capeHash == expectedCape) {
                cachedSignedTextures = property
                return property
            }
            bypassProxyCache = true
            delay *= 2
            if (delay >= maxDelay) {
                LOGGER.warn("Unable to get up-to-date signed textures property")
                return null
            }
            delay(delay)
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(ManagedMojangProfileApi::class.java)

        private val instances = mutableMapOf<UUID, ManagedMojangProfileApi>()
        fun forUser(uuid: UUID): ManagedMojangProfileApi {
            return instances.getOrPut(uuid) { ManagedMojangProfileApi(uuid) }
        }
    }
}
