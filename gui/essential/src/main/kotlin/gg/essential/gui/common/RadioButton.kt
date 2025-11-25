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

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock.Companion.drawBlock
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.combinators.bimap
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.onLeftClick
import gg.essential.gui.layoutdsl.width
import gg.essential.universal.UMatrixStack
import gg.essential.universal.USound
import java.awt.Color

class RadioButton(
    private val state: MutableState<Boolean>,
    private val selectedColor: Color = EssentialPalette.COMPONENT_SELECTED_OUTLINE
) : UIComponent() {

    init {
        Modifier.color(EssentialPalette.TEXT).width(7f).height(7f).hoverScope().onLeftClick { click ->
            if (!state.getUntracked()) {
                USound.playButtonPress()
                state.set(true)
            }
            click.stopPropagation()
        }.applyToComponent(this)
    }

    override fun draw(matrixStack: UMatrixStack) {
        beforeDraw(matrixStack)

        val x = getLeft().toDouble()
        val y = getTop().toDouble()

        matrixStack.push()
        matrixStack.translate(1f, 1f, 0f)
        drawInner(matrixStack, EssentialPalette.BLACK, x, y)
        matrixStack.pop()

        drawInner(matrixStack, if (state.getUntracked()) selectedColor else getColor(), x, y)

        super.draw(matrixStack)
    }

    private fun drawInner(matrixStack: UMatrixStack, color: Color, x: Double, y: Double) {
        drawBlock(matrixStack, color, x, y + 2, x + 1, y + 5)
        drawBlock(matrixStack, color, x + 1, y + 1, x + 2, y + 2)
        drawBlock(matrixStack, color, x + 1, y + 5, x + 2, y + 6)
        drawBlock(matrixStack, color, x + 2, y, x + 5, y + 1)
        drawBlock(matrixStack, color, x + 2, y + 6, x + 5, y + 7)
        drawBlock(matrixStack, color, x + 5, y + 1, x + 6, y + 2)
        drawBlock(matrixStack, color, x + 5, y + 5, x + 6, y + 6)
        drawBlock(matrixStack, color, x + 6, y + 2, x + 7, y + 5)
        if (state.getUntracked()) {
            drawBlock(matrixStack, color, x + 2, y + 2, x + 5, y + 5)
        }
    }
}

fun <T> LayoutScope.radioButton(
    value: T,
    group: MutableState<T>,
    modifier: Modifier = Modifier,
    selectedColor: Color = EssentialPalette.COMPONENT_SELECTED_OUTLINE
) {
    radioButton(group.bimap({ it == value }, { value }), modifier, selectedColor)
}

fun LayoutScope.radioButton(
    state: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    selectedColor: Color = EssentialPalette.COMPONENT_SELECTED_OUTLINE,
) {
    RadioButton(state, selectedColor = selectedColor)(modifier)
}