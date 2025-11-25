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
package gg.essential.gui.wardrobe.configuration

import gg.essential.gui.common.modal.CancelableInputModal
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.common.modal.NoticeEssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.mod.Model
import gg.essential.mod.cosmetics.CosmeticBundle
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.CosmeticTier
import gg.essential.mod.cosmetics.database.GitRepoCosmeticsDatabase
import gg.essential.model.util.Instant
import gg.essential.network.connectionmanager.cosmetics.*

class ConfigurationType<I, T> private constructor(
    val displayPlural: String,
    val displaySingular: String = displayPlural.dropLast(1),
    val stateSupplier: (WardrobeState) -> Triple<MutableState<I?>, State<T?>, ListState<T>>,
    val idAndNameMapper: (T) -> Pair<I, String>,
    val comparator: Comparator<T> = Comparator.comparing { idAndNameMapper(it).second },
    val updateHandler: (CosmeticsDataWithChanges, I, T?) -> Unit,
    val resetHandler: (CosmeticsDataWithChanges, I) -> Unit,
    val createHandler: (ModalManager, CosmeticsDataWithChanges, WardrobeState) -> Modal
) {

    init {
        VALUES.add(this)
    }

    companion object {
        private val VALUES = mutableListOf<ConfigurationType<*, *>>()

        fun values(): List<ConfigurationType<*, *>> = VALUES

        val TYPES = ConfigurationType(
            displayPlural = "Types",
            stateSupplier = { Triple(it.currentlyEditingCosmeticTypeId, it.currentlyEditingCosmeticType, it.rawTypes) },
            idAndNameMapper = { it.id to (it.displayNames["en_us"] ?: it.id) },
            updateHandler = { data, id, new -> data.updateType(id, new) },
            resetHandler = { data, id -> data.resetType(id) },
            createHandler = { modalManager, cosmeticsDataWithChanges, state ->
                CancelableInputModal(modalManager, "Type ID").configure {
                    titleText = "Create New Type"
                    contentText = "Enter the ID for the new type."
                }.apply {
                    onPrimaryActionWithValue { id ->
                        if (cosmeticsDataWithChanges.getType(id) != null) {
                            setError("That id already exists!")
                            return@onPrimaryActionWithValue
                        }
                        cosmeticsDataWithChanges.registerType(
                            id,
                            "Type name",
                            CosmeticSlot.HAT,
                        )
                    }
                }
            }
        )

        val CATEGORIES = ConfigurationType(
            displayPlural = "Categories",
            displaySingular = "Category",
            stateSupplier = { Triple(it.currentlyEditingCosmeticCategoryId, it.currentlyEditingCosmeticCategory, it.rawCategories) },
            idAndNameMapper = { it.id to (it.displayNames["en_us"] ?: it.id) },
            updateHandler = { data, id, new -> data.updateCategory(id, new) },
            resetHandler = { data, id -> data.resetCategory(id) },
            createHandler = { modalManager, cosmeticsDataWithChanges, state ->
                CancelableInputModal(modalManager, "Category ID").configure {
                    titleText = "Create New Category"
                    contentText = "Enter the ID for the new category."
                }.apply {
                    onPrimaryActionWithValue { id ->
                        if (cosmeticsDataWithChanges.getCategory(id) != null) {
                            setError("That id already exists!")
                            return@onPrimaryActionWithValue
                        }
                        cosmeticsDataWithChanges.registerCategory(
                            id,
                            ConfigurationUtils.blankImageEssentialAsset,
                            "Category Name",
                            "Category Description",
                            "Compact Name",
                            0,
                            emptySet(),
                            null,
                            null,
                        )
                    }
                }
            }
        )

        val COSMETICS = ConfigurationType(
            displayPlural = "Cosmetics",
            stateSupplier = { Triple(it.currentlyEditingCosmeticId, it.currentlyEditingCosmetic, it.rawCosmetics) },
            idAndNameMapper = { it.id to (it.displayNames["en_us"] ?: it.id) },
            updateHandler = { data, id, new -> data.updateCosmetic(id, new) },
            resetHandler = { data, id -> data.resetCosmetic(id) },
            createHandler = { modalManager, cosmeticsDataWithChanges, state ->
                NoticeEssentialModal(modalManager, false).configure {
                    contentText = "Adding cosmetics in-game currently not available"
                }
            }
        )

        val BUNDLES = ConfigurationType(
            displayPlural = "Bundles",
            stateSupplier = { Triple(it.currentlyEditingCosmeticBundleId, it.currentlyEditingCosmeticBundle, it.rawBundles) },
            idAndNameMapper = { it.id to it.name },
            updateHandler = { data, id, new -> data.updateBundle(id, new) },
            resetHandler = { data, id -> data.resetBundle(id) },
            createHandler = { modalManager, cosmeticsDataWithChanges, state ->
                CancelableInputModal(modalManager, "Bundle id").configure {
                    titleText = "Create New Bundle"
                    contentText = "Enter the id for the new bundle."
                }.apply {
                    onPrimaryActionWithValue { id ->
                        if (cosmeticsDataWithChanges.getCosmeticBundle(id) != null) {
                            setError("That id already exists!")
                            return@onPrimaryActionWithValue
                        }
                        cosmeticsDataWithChanges.registerBundle(
                            id,
                            "Bundle name",
                            CosmeticTier.COMMON,
                            0f,
                            false,
                            CosmeticBundle.Skin("bff1570fdf623153e6b4a4d2ca97559b471f1ec776584ceec2ebb8bf0b7ba504", Model.ALEX), // A default skin I use for my alt, just so it's not empty :)
                            mapOf(),
                            mapOf(),
                        )
                    }
                }
            }
        )

        val FEATURED_PAGE_LAYOUT_COLLECTIONS = ConfigurationType(
            displayPlural = "Featured page collections",
            stateSupplier = { Triple(it.currentlyEditingFeaturedPageCollectionId, it.currentlyEditingFeaturedPageCollection, it.rawFeaturedPageCollections) },
            idAndNameMapper = { it.id to it.id },
            comparator = compareByDescending { it.availability?.after ?: Instant.MAX },
            updateHandler = { data, id, new -> data.updateFeaturedPageCollection(id, new) },
            resetHandler = { data, id -> data.resetFeaturedPageCollection(id) },
            createHandler = { modalManager, cosmeticsDataWithChanges, state ->
                CancelableInputModal(modalManager, "Featured page collection id").configure {
                    titleText = "Create New Featured Page Collection"
                    contentText = "Enter the id for the new Featured Page Collection."
                }.apply {
                    onPrimaryActionWithValue { id ->
                        if (cosmeticsDataWithChanges.getFeaturedPageCollection(id) != null) {
                            setError("That id already exists!")
                            return@onPrimaryActionWithValue
                        }
                        cosmeticsDataWithChanges.registerFeaturedPageCollection(
                            id,
                            null,
                            mapOf(),
                        )
                    }
                }
            }
        )

        val IMPLICIT_OWNERSHIPS = ConfigurationType(
            displayPlural = "Implicit ownerships",
            stateSupplier = { Triple(it.currentlyEditingImplicitOwnershipId, it.currentlyEditingImplicitOwnership, it.rawImplicitOwnerships) },
            idAndNameMapper = { it.id to it.id },
            updateHandler = { data, id, new -> data.updateImplicitOwnership(id, new) },
            resetHandler = { data, id -> data.resetImplicitOwnership(id) },
            createHandler = { modalManager, cosmeticsDataWithChanges, state ->
                CancelableInputModal(modalManager, "Implicit ownership id").configure {
                    titleText = "Create New Implicit Ownership"
                    contentText = "Enter the id for the new Implicit Ownership. This should typically be the same as the cosmetic being unlocked."
                }.apply {
                    onPrimaryActionWithValue { id ->
                        if (cosmeticsDataWithChanges.getImplicitOwnership(id) != null) {
                            setError("That id already exists!")
                            return@onPrimaryActionWithValue
                        }
                        cosmeticsDataWithChanges.registerImplicitOwnership(
                            id,
                            listOf(),
                            GitRepoCosmeticsDatabase.ImplicitOwnershipCriterion.Everyone
                        )
                    }
                }
            }
        )
    }
}
