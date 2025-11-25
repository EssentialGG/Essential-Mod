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
package gg.essential.cosmetics

/**
 * Stores active/equipped/selected armor slots as bit flags.
 *
 */
data class ArmorSlots(val slots: Byte) {

    constructor(
        boots: Boolean,      // Slot 0
        leggings: Boolean,   // Slot 1
        chestplate: Boolean, // Slot 2
        helmet: Boolean,     // Slot 3
    ) : this((boots.toFlag(0) + leggings.toFlag(1) + chestplate.toFlag(2) + helmet.toFlag(3)).toByte())

    operator fun get(i: Int): Boolean = (slots.toInt() and (1 shl i)) != 0

}

private fun Boolean.toFlag(i: Int) = if (this) (1 shl i) else 0
