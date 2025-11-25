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
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.pixels
import gg.essential.universal.UMatrixStack
import java.awt.Color

class LoadingIcon(val scale: Double) : UIComponent() {
    var time = 0f
        private set

    init {
        setX(CenterConstraint())
        setY(CenterConstraint())
        setWidth((7 * scale).pixels)
        setHeight((7 * scale).pixels)

        addUpdateFunc { dt, _ -> time += dt }
    }

    override fun draw(matrixStack: UMatrixStack) {
        beforeDraw(matrixStack)

        draw(matrixStack, (getLeft() + getRight()) / 2, (getTop() + getBottom()) / 2, scale, time, getColor())

        super.draw(matrixStack)
    }

    companion object {
        const val TIME_PER_FRAME = 0.12f

        private val frames = listOf(
            "       ", "       ", "       ", "   X   ", "   X   ", "   X   ", "   X   ", "       ",
            "       ", "       ", "   X   ", "  XXX  ", "  XXX  ", "  XXX  ", "  X X  ", "       ",
            "       ", "   X   ", "  XXX  ", " XXXXX ", " XXXXX ", " XX XX ", " X   X ", "       ",
            "   X   ", "  XXX  ", " XXXXX ", "XXXXXXX", "XXX XXX", "XX   XX", "X     X", "       ",
            "       ", "   X   ", "  XXX  ", " XXXXX ", " XXXXX ", " XX XX ", " X   X ", "       ",
            "       ", "       ", "   X   ", "  XXX  ", "  XXX  ", "  XXX  ", "  X X  ", "       ",
            "       ", "       ", "       ", "   X   ", "   X   ", "   X   ", "   X   ", "       ",
        )
        const val FRAMES = 8

        fun draw(matrixStack: UMatrixStack, xCenter: Float, yCenter: Float, scale: Double, time: Float, color: Color) {
            val x0 = xCenter - 3.5 * scale
            val y0 = yCenter - 3.5 * scale
            val frame = (time / TIME_PER_FRAME).toInt() % FRAMES
            for (i in 0..6) {
                for (j in 0..6) {
                    if (frames[j * FRAMES + frame][i] == 'X') {
                        UIBlock.drawBlockSized(matrixStack, color, x0 + i * scale, y0 + j * scale, scale, scale)
                    }
                }
            }
        }
    }
}