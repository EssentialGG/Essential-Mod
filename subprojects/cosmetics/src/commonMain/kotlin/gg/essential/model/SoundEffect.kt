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
package gg.essential.model

import gg.essential.mod.EssentialAsset
import kotlin.random.Random

data class SoundEffect(
    val name: String,
    val category: SoundCategory,
    // Note: Min distance is not currently implemented. Minecraft uses a fixed 0 for all sounds.
    val minDistance: Float = 0f,
    val maxDistance: Float = 16f,
    val fixedPosition: Boolean,
    val sounds: List<Entry>,
) {
    data class Entry(
        val asset: EssentialAsset,
        val stream: Boolean = false,
        val interruptible: Boolean = false,
        val volume: Float = 1f,
        val pitch: Float = 1f,
        val looping: Boolean = false,
        val directional: Boolean = true,
        val weight: Int = 1,
    )

    fun randomEntry(): Entry? {
        val total = sounds.sumOf { it.weight }
        if (total <= 0) return null

        var i = Random.nextInt(total)
        return sounds.first { i -= it.weight; i <= 0 }
    }
}