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
package gg.essential.util

import gg.essential.elementa.utils.withAlpha
import java.awt.Color

data class HSBColor(var hue: Float, var saturation: Float, var brightness: Float, var alpha: Float) {

    constructor(color: Color) : this(0f, 0f, 0f, color.alpha / 255F) {
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        hue = hsb[0]
        saturation = hsb[1]
        brightness = hsb[2]
    }

    constructor(color: Int) : this(Color(color))

    fun toColor(): Color {
        return Color(Color.HSBtoRGB(hue, saturation, brightness)).withAlpha(alpha)
    }
}
