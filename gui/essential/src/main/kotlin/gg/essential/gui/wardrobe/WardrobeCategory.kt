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
package gg.essential.gui.wardrobe

import gg.essential.gui.EssentialPalette
import gg.essential.gui.image.ImageFactory
import gg.essential.mod.cosmetics.CosmeticCategory
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.util.ifNullOrEmpty
import java.awt.Color

sealed class WardrobeCategory(
    open val order: Int,
    open val fullName: String,
    open val compactName: String,
    open val style: Style?,
) {

    open val superCategory: WardrobeCategory
        get() = this

    /** Returns whether the given cosmetic is a **direct** child of this category. */
    open operator fun contains(cosmetic: Cosmetic): Boolean = false

    abstract class ParentCategory(
        override val order: Int,
        override val fullName: String,
        override val compactName: String = fullName,
        override val style: Style? = null,
    ) : WardrobeCategory(order, fullName, compactName, style)

    data class InfraCollectionSubcategory(
        val category: CosmeticCategory,
    ) : SubCategory(
        Featured,
        category.order,
        category.displayNames["en_us"].ifNullOrEmpty(category.id),
        category.compactNames["en_us"].ifNullOrEmpty(category.displayNames["en_us"].ifNullOrEmpty(category.id)),
    ) {
        override fun contains(cosmetic: Cosmetic): Boolean =
            cosmetic.categories.containsKey(category.id)
    }

    sealed class SubCategory(
        open val parent: ParentCategory,
        override val order: Int,
        override val fullName: String,
        override val compactName: String
    ) : WardrobeCategory(
        order,
        fullName,
        compactName,
        null,
    ) {
        override val superCategory: WardrobeCategory
            get() = parent
    }


    // Almost the same as InfraCollectionSubcategory, except for the parent and name
    // Since that one will be removed with the flag, it seemed ok to just make a copy and slightly modify it to not complicate feature flag stuff
    data class CosmeticCategorySubCategory(val category: CosmeticCategory, override val parent: ParentCategory) : SubCategory(
        parent,
        category.order,
        category.displayNames["en_us"].ifNullOrEmpty(category.id),
        category.compactNames["en_us"].ifNullOrEmpty(category.displayNames["en_us"].ifNullOrEmpty(category.id)),
    ) {
        override fun contains(cosmetic: Cosmetic): Boolean =
            cosmetic.categories.containsKey(category.id)
    }

    object Diagnostics : WardrobeCategory(
        -10,
        "Diagnostics",
        "Diagnostics",
        style = Style(EssentialPalette.ROUND_WARNING_7X, EssentialPalette.ESSENTIAL_RED),
    )
    object Featured : ParentCategory(
        0,
        "Featured",
        style = Style(EssentialPalette.STAR_7X, EssentialPalette.CART_ACTIVE),
    )
    object FeaturedRefresh : WardrobeCategory(
        0,
        "Featured",
        "Featured",
        style = Style(EssentialPalette.STAR_7X, EssentialPalette.FEATURED_BLUE),
    )
    object Outfits : WardrobeCategory(
        95,
        "Outfits",
        "Outfits",
        style = Style(EssentialPalette.COSMETICS_10X7, EssentialPalette.OUTFITS_AQUA),
    )
    object Skins : WardrobeCategory(
        96,
        "Skins",
        "Skins",
        style = Style(EssentialPalette.PERSON_4X6, EssentialPalette.SKINS_GREEN),
    )
    object Emotes : ParentCategory(
        97,
        "Emotes",
        style = Style(
            EssentialPalette.HEART_7X6,
            EssentialPalette.EMOTES_YELLOW),
    )
    object Cosmetics : ParentCategory(
        98,
        "Cosmetics",
        style = Style(EssentialPalette.CROWN_7X5, EssentialPalette.COSMETICS_ORANGE),
    )

    data class Style(
        val icon: ImageFactory,
        val color: Color,
    )

    companion object {
        // Source: EM-2198
        val slotOrder = arrayOf(
            CosmeticSlot.EMOTE,
            CosmeticSlot.CAPE,
            CosmeticSlot.WINGS,
            CosmeticSlot.BACK,
            CosmeticSlot.EFFECT,
            CosmeticSlot.HAT,
            CosmeticSlot.FACE,
            CosmeticSlot.EARS,
            CosmeticSlot.HEAD,
            CosmeticSlot.FULL_BODY,
            CosmeticSlot.SKIRT,
            CosmeticSlot.TOP,
            CosmeticSlot.ACCESSORY,
            CosmeticSlot.PANTS,
            CosmeticSlot.ARMS,
            CosmeticSlot.SHOES,
            CosmeticSlot.SHOULDERS,
            CosmeticSlot.PET,
            CosmeticSlot.TAIL,
            CosmeticSlot.ICON,
            CosmeticSlot.SUITS,
        )

        private val categories = mapOf(
            "emotes" to lazy { Emotes },
            "cosmetics" to lazy { Cosmetics },
            "featured" to lazy { FeaturedRefresh },
            "outfits" to lazy { Outfits },
            "skins" to lazy { Skins },
        ) + mapOf()

        fun get(category: String?) = categories[category]?.value
    }
}
