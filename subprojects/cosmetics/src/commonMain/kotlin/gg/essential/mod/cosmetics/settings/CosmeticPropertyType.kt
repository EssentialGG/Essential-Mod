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
package gg.essential.mod.cosmetics.settings

/** Types and names of properties that can be applied to cosmetics to affect
 * different aspects of them **/
enum class CosmeticPropertyType(
    val displayName: String,
    val singleton: Boolean,
) {
    ARMOR_HANDLING("Armor Handling", true),
    ARMOR_HANDLING_V2("Armor Handling V2", true),
    POSITION_RANGE("Player Position Adjustment", true),
    INTERRUPTS_EMOTE("Interrupts Emote", true),
    REQUIRES_UNLOCK_ACTION("Requires Unlock Action", true),
    PREVIEW_RESET_TIME("Preview Reset Time", true),
    LOCALIZATION("Partner Name Localization", true),
    COSMETIC_BONE_HIDING("Cosmetic Bone Hiding", false),
    EXTERNAL_HIDDEN_BONE("External Hidden Bones", false),
    TRANSITION_DELAY("Transition Delay", true),
    VARIANTS("Variants", true),
    DEFAULT_SIDE("Default Side", true),
    MUTUALLY_EXCLUSIVE("Mutually Exclusive", true),
    ALL_OTHER_COSMETIC_OR_ITEM_HIDING("Hides Cosmetics or Items", true),
    LOCKS_PLAYER_ROTATION("Locks Player Rotation", true),
}
