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
package gg.essential.cosmetics.events

enum class AnimationEventType {
    EQUIP,
    JOIN_WORLD,
    LEAVE_WORLD,
    JUMP_START,
    JUMP_END,
    WALK_START,
    WALK_END,
    SWING,
    SNEAK_START,
    SNEAK_END,
    FLY_ON,
    FLY_OFF,
    FLY_MOVE_START,
    FLY_MOVE_END,
    ON_DAMAGE,
    TICK,
    IDLE,
    TEXTURE_ANIMATION_START,
    EMOTE,
    BY_OTHER, // triggered by other cosmetic animation event
}
