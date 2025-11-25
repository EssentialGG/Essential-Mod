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
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.EssentialPalette
import gg.essential.gui.about.components.ColoredDivider
import gg.essential.util.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DateDividerImpl(
    timeStamp: Instant,
    unread: State<Boolean> = BasicState(false),
) : DateDivider(timeStamp, unread) {

    private val color = unread.map { if (it) EssentialPalette.RED else EssentialPalette.TEXT_DISABLED }

    private val date = timeStamp.atZone(ZoneId.systemDefault()).toLocalDate()

    private val divider by ColoredDivider(
        BasicState(formatDate(date, date.year != LocalDate.now().year)),
        color,
        false,
        dividerColor = color,
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