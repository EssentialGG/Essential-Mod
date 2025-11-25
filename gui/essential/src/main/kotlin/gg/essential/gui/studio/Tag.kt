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
package gg.essential.gui.studio

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.dsl.*
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.color.toConstraint
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toV1
import java.awt.Color

class Tag(
    backgroundColor: State<Color>,
    textColor: State<Color>,
    text: State<String>
) : UIBlock(backgroundColor.toConstraint()) {

    constructor(backgroundColor: Color, textColor: Color, text: String) : this(
        stateOf(backgroundColor),
        stateOf(textColor),
        stateOf(text)
    )

    private val text by EssentialUIText(shadow = false).bindText(text.toV1(this)).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        color = textColor.toConstraint()
    } childOf this

    init {
        constrain {
            width = ChildBasedSizeConstraint() + 6.pixels
            height = 13.pixels
        }
    }
}