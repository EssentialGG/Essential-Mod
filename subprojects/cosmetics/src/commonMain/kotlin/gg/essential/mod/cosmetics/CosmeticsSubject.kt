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
package gg.essential.mod.cosmetics

import gg.essential.cosmetics.ArmorSlots
import gg.essential.cosmetics.events.AnimationTarget
import gg.essential.mod.Model
import gg.essential.model.molang.MolangQueryEntity

/** Information on the subject of a set of cosmetics. */
data class CosmeticsSubject(
    /** The entity used to drive animations */
    val entity: MolangQueryEntity,
    /** Skin type of the subject. */
    val skinType: Model = Model.STEVE,
    /** Parts of the subject that are covered by armor */
    val armor: ArmorSlots = ArmorSlots(0),
    /** Types of animations to play on this subject (ALL is always included) */
    val animationTargets: Set<AnimationTarget> =
        setOf(AnimationTarget.SELF, AnimationTarget.OTHERS),
)
