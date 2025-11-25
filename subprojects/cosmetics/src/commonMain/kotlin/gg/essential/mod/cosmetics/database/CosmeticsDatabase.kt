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
package gg.essential.mod.cosmetics.database

import gg.essential.cosmetics.CosmeticBundleId
import gg.essential.cosmetics.CosmeticCategoryId
import gg.essential.cosmetics.CosmeticId
import gg.essential.cosmetics.CosmeticTypeId
import gg.essential.cosmetics.FeaturedPageCollectionId
import gg.essential.cosmetics.ImplicitOwnership
import gg.essential.cosmetics.ImplicitOwnershipId
import gg.essential.mod.cosmetics.CosmeticBundle
import gg.essential.mod.cosmetics.CosmeticCategory
import gg.essential.mod.cosmetics.CosmeticType
import gg.essential.mod.cosmetics.featured.FeaturedPageCollection
import gg.essential.network.cosmetics.Cosmetic

/** Provides access to cosmetics metadata */
interface CosmeticsDatabase {
    suspend fun getCategory(id: CosmeticCategoryId): CosmeticCategory?
    suspend fun getCategories(): List<CosmeticCategory>

    suspend fun getType(id: CosmeticTypeId): CosmeticType?
    suspend fun getTypes(): List<CosmeticType>

    suspend fun getCosmeticBundle(id: CosmeticBundleId): CosmeticBundle? // rename to getBundle() when removing feature flag
    suspend fun getCosmeticBundles(): List<CosmeticBundle> // rename to getBundles() when removing feature flag

    suspend fun getFeaturedPageCollection(id: FeaturedPageCollectionId): FeaturedPageCollection?
    suspend fun getFeaturedPageCollections(): List<FeaturedPageCollection>

    suspend fun getImplicitOwnership(id: ImplicitOwnershipId): ImplicitOwnership?
    suspend fun getImplicitOwnerships(): List<ImplicitOwnership>

    suspend fun getCosmetic(id: CosmeticId): Cosmetic?
    suspend fun getCosmetics(): List<Cosmetic>

}
