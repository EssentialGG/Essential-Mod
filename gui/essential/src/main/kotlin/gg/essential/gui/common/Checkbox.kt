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
package gg.essential.gui.common

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.GuiScaleOffsetConstraint
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.color.toConstraint
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.heightAspect
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.layoutAsBox
import gg.essential.gui.layoutdsl.width
import gg.essential.universal.USound
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color

class Checkbox(
    initialValue: Boolean = false,
    boxColor: State<Color> = stateOf(EssentialPalette.BUTTON),
    checkmarkColor: State<Color> = stateOf(EssentialPalette.TEXT_HIGHLIGHT),
    checkmarkScaleOffset: Float = 0f,
    private val playClickSound: Boolean = true,
    private val callback: ((Boolean) -> Unit)? = null,
) : UIBlock(EssentialPalette.BUTTON) {

    val isChecked = mutableStateOf(initialValue)

    private val checkmark by Checkmark(checkmarkScaleOffset, checkmarkColor).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
    }

    init {
        layoutAsBox(Modifier.width(9f).heightAspect(1f).hoverScope().color(boxColor).hoverColor({ boxColor().brighter() })) {
            if_(isChecked) {
                checkmark()
            }
        }

        onLeftClick {click ->
            click.stopPropagation()
            toggle()
        }
    }

    fun toggle() {
        isChecked.set { !it }

        if (playClickSound) {
            USound.playButtonPress()
        }

        callback?.invoke(isChecked.get())
    }
}

private class Checkmark(scaleOffset: Float, color: State<Color>) : UIContainer() {
    init {
        repeat(5) {
            UIBlock(color.toConstraint()).constrain {
                x = SiblingConstraint(alignOpposite = true)
                y = SiblingConstraint()
                width = AspectConstraint()
                height = GuiScaleOffsetConstraint(scaleOffset)
            } childOf this
        }

        repeat(2) {
            UIBlock(color.toConstraint()).constrain {
                x = SiblingConstraint(alignOpposite = true)
                y = SiblingConstraint(alignOpposite = true)
                width = AspectConstraint()
                height = GuiScaleOffsetConstraint(scaleOffset)
            } childOf this
        }

        constrain {
            width = ChildBasedSizeConstraint()
            height = ChildBasedMaxSizeConstraint() * 5
        }
    }
}
