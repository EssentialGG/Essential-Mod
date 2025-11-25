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
package gg.essential.gui.friends.message.v2

import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.gui.EssentialPalette
import gg.essential.gui.about.components.ColoredDivider
import java.time.Instant

class UnreadDividerImpl(
    timeStamp: Instant,
) : UnreadDivider(timeStamp) {

    private val divider by ColoredDivider(
        "NEW",
        EssentialPalette.RED,
        false,
        dividerColor = EssentialPalette.RED,
        scaleOffset = getGuiScaleOffset(),
    ).constrain {
        y = 12.pixels
    } childOf this

    init {
        constrain {
            x = CenterConstraint()
            y = SiblingConstraint()
            width = 100.percent - 14.pixels // Subtract padding
            height = 20.pixels
        }

    }
}