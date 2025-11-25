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

import gg.essential.connectionmanager.common.packet.cosmetic.ClientCosmeticRequestPacket
import gg.essential.connectionmanager.common.packet.cosmetic.ServerCosmeticsPopulatePacket
import gg.essential.connectionmanager.common.packet.cosmetic.categories.ClientCosmeticCategoriesRequestPacket
import gg.essential.connectionmanager.common.packet.cosmetic.categories.ServerCosmeticCategoriesPopulatePacket
import gg.essential.connectionmanager.common.packet.wardrobe.ClientWardrobeStoreBundleRequestPacket
import gg.essential.connectionmanager.common.packet.wardrobe.ServerWardrobeStoreBundlePacket
import gg.essential.cosmetics.CosmeticBundleId
import gg.essential.cosmetics.CosmeticCategoryId
import gg.essential.cosmetics.CosmeticId
import gg.essential.cosmetics.model.CosmeticStoreBundle
import gg.essential.gui.elementa.state.v2.add
import gg.essential.gui.elementa.state.v2.clear
import gg.essential.gui.elementa.state.v2.set
import gg.essential.mod.EssentialAsset
import gg.essential.mod.cosmetics.CosmeticAssets
import gg.essential.mod.cosmetics.settings.CosmeticProperty
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.CosmeticType
import gg.essential.mod.cosmetics.featured.FeaturedItem
import gg.essential.mod.cosmetics.featured.FeaturedItemRow
import gg.essential.network.CMConnection
import gg.essential.network.cosmetics.toMod
import gg.essential.util.Client
import gg.essential.util.logExceptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import gg.essential.cosmetics.model.Cosmetic as InfraCosmetic
import gg.essential.cosmetics.model.CosmeticCategory as InfraCategory
import gg.essential.cosmetics.model.CosmeticType as InfraType

class InfraCosmeticsData private constructor(
    private val cmConnection: CMConnection,
    private val assetLoader: AssetLoader,
    private val state: MutableCosmeticsData,
) : CosmeticsData by state {
    constructor(cmConnection: CMConnection, assetLoader: AssetLoader) : this(cmConnection, assetLoader, MutableCosmeticsData())

    private val categoriesKnownOrRequested = mutableSetOf<CosmeticCategoryId>()
    private val activeCategoryRequests = mutableMapOf<CosmeticCategoryId, Instant>()

    private val cosmeticsKnownOrRequested = mutableSetOf<CosmeticId>()
    private val activeCosmeticRequests = mutableMapOf<CosmeticId, Instant>()
    private val cosmeticsLoading = mutableMapOf<CosmeticId, Instant>()

    private val bundlesKnownOrRequested = mutableSetOf<CosmeticBundleId>()
    private val activeBundleRequests = mutableMapOf<CosmeticBundleId, Instant>()

    private var featuredPageCollectionLoading: Instant? = null

    init {
        resetState()
    }

    fun resetState() {
        state.categories.clear()
        state.types.clear()
        state.cosmetics.clear()

        categoriesKnownOrRequested.clear()
        activeCategoryRequests.clear()
        cosmeticsKnownOrRequested.clear()
        activeCosmeticRequests.clear()
        bundlesKnownOrRequested.clear()
        activeBundleRequests.clear()

        // Popular is a special category that we should ignore when determining if we have received all cosmetics
        categoriesKnownOrRequested.add("popular")
    }

    fun addCategory(infraCategory: InfraCategory) {
        categoriesKnownOrRequested.add(infraCategory.id)
        activeCategoryRequests.remove(infraCategory.id)

        val category = infraCategory.toMod()
        val existingIndex = state.categories.get().indexOfFirst { it.id == category.id }
        if (existingIndex >= 0) {
            state.categories.set(existingIndex, category)
        } else {
            state.categories.add(category)
        }
    }

    fun addType(infraType: InfraType) {
        val type = infraType.toMod()
        val existingIndex = state.types.get().indexOfFirst { it.id == type.id }
        if (existingIndex >= 0) {
            state.types.set(existingIndex, type)
        } else {
            state.types.add(type)
        }

        for ((index, cosmetic) in state.cosmetics.get().withIndex()) {
            if (cosmetic.type.id == type.id && cosmetic.type != type) {
                state.cosmetics.set(index, cosmetic.copy(type = type))
            }
        }
    }

    fun addCosmetic(infraCosmetic: InfraCosmetic) {
        cosmeticsKnownOrRequested.add(infraCosmetic.id)
        activeCosmeticRequests.remove(infraCosmetic.id)

        val assets = CosmeticAssets(infraCosmetic.assetsMap.mapValues { it.value.toMod() })
        val settingsAsset = assets.settings
        val settingsFuture = if (settingsAsset != null) {
            cosmeticsLoading[infraCosmetic.id] = Instant.now()
            assetLoader.getAssetBytes(settingsAsset, AssetLoader.Priority.Blocking)
                .thenApplyAsync { CosmeticProperty.fromJsonArray(String(it)) }
                .whenCompleteAsync({ _, _ -> cosmeticsLoading.remove(infraCosmetic.id) }, Dispatchers.Client.asExecutor())
                .logExceptions()
        } else {
            CompletableFuture.completedFuture(emptyList())
        }

        settingsFuture.thenAcceptAsync({ settings ->
            val type: CosmeticType = getType(infraCosmetic.type)
                ?: CosmeticType(infraCosmetic.type, CosmeticSlot.FULL_BODY, emptyMap(), emptyMap())
            val cosmetic = infraCosmetic.toMod(type, settings)

            val existingIndex = state.cosmetics.get().indexOfFirst { it.id == cosmetic.id }
            if (existingIndex >= 0) {
                state.cosmetics.set(existingIndex, cosmetic)
            } else {
                state.cosmetics.add(cosmetic)
            }

            requestCategoriesIfMissing(cosmetic.categories.keys)
        }, Dispatchers.Client.asExecutor())
    }

    fun addBundle(infraBundle: CosmeticStoreBundle) {
        bundlesKnownOrRequested.add(infraBundle.id)
        activeBundleRequests.remove(infraBundle.id)

        val bundle = infraBundle.toMod()
        val existingIndex = state.bundles.get().indexOfFirst { it.id == bundle.id }
        if (existingIndex >= 0) {
            state.bundles.set(existingIndex, bundle)
        } else {
            state.bundles.add(bundle)
        }
    }

    fun addFeaturedPageCollection(essentialAsset: EssentialAsset) {
        featuredPageCollectionLoading = Instant.now()
        assetLoader.getAsset(essentialAsset, AssetLoader.Priority.Low, AssetLoader.AssetType.FeaturedPageCollection)
            .parsed
            .whenCompleteAsync(
                { collection, _ ->
                    featuredPageCollectionLoading = null

                    if (collection == null)
                        return@whenCompleteAsync

                    val featuredItems = collection.pages.values.flatMap { it.rows }.filterIsInstance<FeaturedItemRow>().flatMap { it.items }
                    requestCosmeticsIfMissing(featuredItems.filterIsInstance<FeaturedItem.Cosmetic>().map { it.cosmetic }.toSet())
                    requestBundlesIfMissing(featuredItems.filterIsInstance<FeaturedItem.Bundle>().map { it.bundle }.toSet())

                    val existingIndex = state.featuredPageCollections.get().indexOfFirst { it.id == collection.id }
                    if (existingIndex >= 0) {
                        state.featuredPageCollections.set(existingIndex, collection)
                    } else {
                        state.featuredPageCollections.add(collection)
                    }
                },
                Dispatchers.Client.asExecutor()
            ).logExceptions()
    }

    /** Requests unknown categories from the connection manager if they are not already populated or loading */
    fun requestCategoriesIfMissing(categoryIds: Collection<CosmeticCategoryId>) {
        val unknownIds = categoryIds.filter { categoriesKnownOrRequested.add(it) }
        if (unknownIds.isEmpty()) return

        for (id in unknownIds) {
            activeCategoryRequests[id] = Instant.now()
        }

        cmConnection.connectionScope.launch {
            cmConnection.call(ClientCosmeticCategoriesRequestPacket(unknownIds.toSet(), null, null))
                .exponentialBackoff()
                .await<ServerCosmeticCategoriesPopulatePacket>()

            for (id in unknownIds) {
                activeCategoryRequests.remove(id)
            }
        }
    }

    /** Requests unknown cosmetics from the connection manager if they are not already populated or loading */
    fun requestCosmeticsIfMissing(cosmeticIds: Collection<CosmeticId>) {
        val unknownIds = cosmeticIds.filter { cosmeticsKnownOrRequested.add(it) }
        if (unknownIds.isEmpty()) return

        for (id in unknownIds) {
            activeCosmeticRequests[id] = Instant.now()
        }

        cmConnection.connectionScope.launch {
            cmConnection.call(ClientCosmeticRequestPacket(unknownIds.toSet(), null))
                .exponentialBackoff()
                .await<ServerCosmeticsPopulatePacket>()

            for (id in unknownIds) {
                activeCosmeticRequests.remove(id)
            }
        }
    }

    /** Requests unknown bundles from the connection manager if they are not already populated or loading */
    fun requestBundlesIfMissing(bundleIds: Collection<CosmeticBundleId>) {
        val unknownIds = bundleIds.filter { bundlesKnownOrRequested.add(it) }
        if (unknownIds.isEmpty()) return

        for (id in unknownIds) {
            activeBundleRequests[id] = Instant.now()
        }

        cmConnection.connectionScope.launch {
            cmConnection.call(ClientWardrobeStoreBundleRequestPacket(unknownIds.toSet()))
                .exponentialBackoff()
                .await<ServerWardrobeStoreBundlePacket>()

            for (id in unknownIds) {
                activeBundleRequests.remove(id)
            }
        }
    }

    fun hasActiveRequests(timeoutMs: Long): Boolean {
        val now = Instant.now()

        fun Map<String, Instant>.anyActive(): Boolean =
            values.any { Duration.between(it, now).toMillis() < timeoutMs }

        return activeCategoryRequests.anyActive()
                || activeCosmeticRequests.anyActive()
                || (activeBundleRequests.anyActive())
                || ((featuredPageCollectionLoading?.let { Duration.between(it, now).toMillis() < timeoutMs } ?: false))
                || cosmeticsLoading.anyActive()
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }
}
