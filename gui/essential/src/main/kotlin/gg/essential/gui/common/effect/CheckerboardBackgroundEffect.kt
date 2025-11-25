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
package gg.essential.gui.common.effect

import gg.essential.elementa.UIComponent
import gg.essential.elementa.effects.Effect
import gg.essential.gui.EssentialPalette
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.vertex.UBufferBuilder
import java.awt.Color

class CheckerboardBackgroundEffect : Effect() {
    override fun beforeDraw(matrixStack: UMatrixStack) {
        drawCheckerBoard(matrixStack, boundComponent)

    }
    private fun drawCheckerBoard(matrixStack: UMatrixStack, component: UIComponent) {
        val left = component.getLeft().toDouble()
        val top = component.getTop().toDouble()
        val right = component.getRight().toDouble()
        val bottom = component.getBottom().toDouble()
        val graphics = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR)

        for (x in 0 until (right - left).toInt()) {
            for (y in 0 until (bottom - top).toInt()) {
                val color = if ((x + y) % 2 == 0) Color.LIGHT_GRAY else EssentialPalette.TEXT_HIGHLIGHT
                drawVertex(graphics, matrixStack, left + x, top + y, color)
                drawVertex(graphics, matrixStack, left + x, top + y + 1, color)
                drawVertex(graphics, matrixStack, left + x + 1, top + y + 1, color)
                drawVertex(graphics, matrixStack, left + x + 1, top + y, color)
            }
        }

        graphics.build()?.drawAndClose(PIPELINE)
    }
    private fun drawVertex(graphics: UBufferBuilder, matrixStack: UMatrixStack, x: Double, y: Double, color: Color) {
        graphics
            .pos(matrixStack, x, y, 0.0)
            .color(
                color.red.toFloat() / 255f,
                color.green.toFloat() / 255f,
                color.blue.toFloat() / 255f,
                color.alpha.toFloat() / 255f
            )
            .endVertex()
    }

    companion object {
        private val PIPELINE = URenderPipeline.builderWithDefaultShader(
            "essential:checkerboard_background_effect",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_COLOR,
        ).build()
    }
}
