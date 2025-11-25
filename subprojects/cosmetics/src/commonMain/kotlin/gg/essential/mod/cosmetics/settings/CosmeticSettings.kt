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

import gg.essential.model.Side

typealias CosmeticSettings = List<CosmeticSetting>

inline fun <reified T : CosmeticSetting> CosmeticSettings.setting(): T? = firstNotNullOfOrNull { it as? T }
inline fun <reified T : CosmeticSetting> CosmeticSettings.settings(): List<T> = filterIsInstance<T>()

val CosmeticSettings.side: Side?
    get() = setting<CosmeticSetting.Side>()?.data?.side

val CosmeticSettings.variant: String?
    get() = setting<CosmeticSetting.Variant>()?.data?.variant

val CosmeticSettings.animationVariant: String?
    get() = setting<CosmeticSetting.AnimationVariant>()?.data?.animationVariant
