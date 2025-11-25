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
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.animateTransitions
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.onLeftClick
import gg.essential.gui.util.hoverScopeV2
import gg.essential.model.lerp
import gg.essential.universal.UMatrixStack
import gg.essential.universal.USound
import gg.essential.util.darker
import java.awt.Color

abstract class EssentialToggle(
    protected val value: MutableState<Boolean>,
    protected val enabled: State<Boolean> = stateOf(true),
) : UIComponent() {

    protected val referenceHolder = ReferenceHolderImpl()

    protected val hovered = hoverScopeV2()

    private val switchState = value.map { if (it) 1f else 0f }.animateTransitions(this, MOVE_ANIMATION_TIME)
    private val valueColorProgress = value.map { if (it) 1f else 0f }.animateTransitions(this, COLOR_ANIMATION_TIME)
    private val colorState = memo {
        val valueFalse = color(hovered(), false)
        val valueTrue = color(hovered(), true)
        valueFalse.lerp(valueTrue, valueColorProgress())
    }

    init {
        Modifier.hoverScope().color(colorState).onLeftClick { click ->
            if (enabled.getUntracked()) {
                USound.playButtonPress()
                value.set { !it }
            }
            click.stopPropagation()
        }.applyToComponent(this)
    }

    override fun draw(matrixStack: UMatrixStack) {
        beforeDraw(matrixStack)

        val x = getLeft().toDouble()
        val y = getTop().toDouble()
        val width = getWidth().toDouble()
        val height = getHeight().toDouble()
        val switchPos = switchState.getUntracked() * (width * 0.5)

        matrixStack.push()
        matrixStack.translate(1f, 1f, 0f)
        drawInner(matrixStack, EssentialPalette.BLACK, x, y, width, height, switchPos)
        matrixStack.pop()

        drawInner(matrixStack, getColor(), x, y, width, height, switchPos)

        drawIndicator(matrixStack, x + switchPos, y, switchState)

        super.draw(matrixStack)
    }

    private fun drawInner(matrixStack: UMatrixStack, color: Color, x: Double, y: Double, width: Double, height: Double,
                          switchPos: Double) {
        drawBlock(matrixStack, color, x, y, x + width, y + 1)
        drawBlock(matrixStack, color, x, y + height - 1, x + width, y + height)
        drawBlock(matrixStack, color, x, y + 1, x + 1, y + height - 1)
        drawBlock(matrixStack, color, x + width - 1, y + 1, x + width, y + height - 1)

        drawBlock(matrixStack, color, x + switchPos, y + 1, x + (width * 0.5) + switchPos, y + height - 1)
    }

    protected abstract fun drawIndicator(matrixStack: UMatrixStack, x: Double, y: Double, switchState: State<Float>)

    protected abstract fun color(hovered: Boolean, value: Boolean): Color

    companion object {

        private const val MOVE_ANIMATION_TIME = 0.25f
        private const val COLOR_ANIMATION_TIME = 0.15f
    }
}

class FullEssentialToggle(
    value: MutableState<Boolean>,
    enabled: State<Boolean> = stateOf(true)
) : EssentialToggle(value, enabled) {

    private val onIndicator = EssentialPalette.TOGGLE_ON.create()
    private val offIndicator = EssentialPalette.TOGGLE_OFF.create()

    init {
        constrain {
            width = 20.pixels
            height = 11.pixels
        }
    }

    override fun drawIndicator(matrixStack: UMatrixStack, x: Double, y: Double, switchState: State<Float>) {
        if (switchState.getUntracked() > 0.5f) {
            onIndicator.drawImage(matrixStack, x + 4.5, y + 3, 1.0, 5.0, toggleIndicatorColor(hovered.getUntracked(), true))
        } else {
            offIndicator.drawImage(matrixStack, x + 3, y + 3, 4.0, 5.0, toggleIndicatorColor(hovered.getUntracked(), false))
        }
    }

    override fun color(hovered: Boolean, value: Boolean): Color {
        val enabled = enabled.getUntracked()
        return if (enabled && hovered) {
            if (value) {
                EssentialPalette.TOGGLE_ON_BACKGROUND_HOVERED
            } else {
                EssentialPalette.TOGGLE_OFF_BACKGROUND_HOVERED
            }
        } else {
            if (enabled) {
                if (value) {
                    EssentialPalette.TOGGLE_ON_BACKGROUND
                } else {
                    EssentialPalette.TOGGLE_OFF_BACKGROUND
                }
            } else {
                if (value) {
                    EssentialPalette.BLUE_BUTTON
                } else {
                    EssentialPalette.GRAY_BUTTON_HOVER
                }
            }
        }
    }

    private fun toggleIndicatorColor(hovered: Boolean, value: Boolean): Color {
        return if (value) {
            EssentialPalette.BLACK
        } else {
            if (!enabled.getUntracked()) {
                EssentialPalette.GRAY_OUTLINE_BUTTON
            } else if (hovered) {
                EssentialPalette.TOGGLE_OFF_BACKGROUND
            } else {
                EssentialPalette.TOGGLE_OFF_FOREGROUND
            }
        }
    }
}

class CompactEssentialToggle(
    value: MutableState<Boolean>,
    enabled: State<Boolean> = stateOf(true),
    private val offColor: Color = EssentialPalette.TEXT_MID_GRAY,
    private val onColor: Color = EssentialPalette.GREEN
) : EssentialToggle(value, enabled) {

    init {
        constrain {
            width = 10.pixels
            height = 6.pixels
        }
    }

    override fun drawIndicator(matrixStack: UMatrixStack, x: Double, y: Double, switchState: State<Float>) {
        // noop
    }

    override fun color(hovered: Boolean, value: Boolean): Color {
        val enabled = enabled.getUntracked()
        return if (enabled && hovered) {
            if (value) {
                onColor.brighter()
            } else {
                offColor.brighter()
            }
        } else {
            if (value) {
                onColor
            } else {
                offColor
            }.darker(if(enabled) 0f else 0.3f)
        }
    }
}

fun LayoutScope.compactFullEssentialToggle(
    value: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    enabled: State<Boolean> = stateOf(true),
    offColor: Color = EssentialPalette.TEXT_MID_GRAY,
    onColor: Color = EssentialPalette.GREEN
): EssentialToggle {
    return CompactEssentialToggle(value, enabled, offColor, onColor) (modifier)
}

