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
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.collections.*
import gg.essential.mod.cosmetics.CosmeticBundle
import gg.essential.mod.cosmetics.CosmeticCategory
import gg.essential.mod.cosmetics.CosmeticType
import gg.essential.mod.cosmetics.featured.FeaturedPageCollection
import gg.essential.network.cosmetics.Cosmetic

class MutableCosmeticsData : CosmeticsData {
    override val categories: MutableListState<CosmeticCategory> = mutableListStateOf()
    override val types: MutableListState<CosmeticType> = mutableListStateOf()
    override val bundles: MutableListState<CosmeticBundle> = mutableListStateOf()
    override val featuredPageCollections: MutableListState<FeaturedPageCollection> = mutableListStateOf()
    override val implicitOwnerships: MutableListState<ImplicitOwnership> = mutableListStateOf()
    override val cosmetics: MutableListState<Cosmetic> = mutableListStateOf()

    private val refHolder = ReferenceHolderImpl()
    private val categoriesMap = categories.asMap(refHolder) { it.id to it }
    private val typesMap = types.asMap(refHolder) { it.id to it }
    private val bundlesMap = bundles.asMap(refHolder) { it.id to it }
    private val featuredPageCollectionsMap = featuredPageCollections.asMap(refHolder) { it.id to it }
    private val implicitOwnershipsMap = implicitOwnerships.asMap(refHolder) { it.id to it }
    private val cosmeticsMap = cosmetics.asMap(refHolder) { it.id to it }

    override fun getCategory(id: CosmeticCategoryId): CosmeticCategory? = categoriesMap[id]

    override fun getType(id: CosmeticTypeId): CosmeticType? = typesMap[id]

    override fun getCosmeticBundle(id: CosmeticBundleId): CosmeticBundle? = bundlesMap[id]

    override fun getFeaturedPageCollection(id: FeaturedPageCollectionId): FeaturedPageCollection? = featuredPageCollectionsMap[id]

    override fun getImplicitOwnership(id: ImplicitOwnershipId): ImplicitOwnership? = implicitOwnershipsMap[id]

    override fun getCosmetic(id: CosmeticId): Cosmetic? = cosmeticsMap[id]
}
