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
package gg.essential.gui.screenshot.image

import gg.essential.elementa.UIComponent
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.shader.BlendState
import gg.essential.universal.vertex.UBufferBuilder
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.UIdentifier
import java.awt.Color

open class ScreenshotImage(val texture: State<UIdentifier?>) : UIComponent() {

    constructor(texture: UIdentifier? = null) : this(stateOf(texture))

    override fun draw(matrixStack: UMatrixStack) {
        beforeDrawCompat(matrixStack)
        val textureInstance = texture.getUntracked()
        if (textureInstance != null) {
            val x = this.getLeft().toDouble()
            val y = this.getTop().toDouble()
            val width = this.getWidth().toDouble()
            val height = this.getHeight().toDouble()
            val color = this.getColor()

            if (color.alpha == 0) {
                return super.draw(matrixStack)
            }
            matrixStack.push()
            matrixStack.translate(x, y, 0.0)
            renderImage(matrixStack, color, width, height)
            matrixStack.pop()

        }

        super.draw(matrixStack)
    }

    fun renderImage(
        matrixStack: UMatrixStack,
        color: Color,
        width: Double,
        height: Double
    ) {
        val textureInstance = texture.getUntracked() ?: return
        val textureId = platform.getGlId(textureInstance)

        val red = color.red.toFloat() / 255f
        val green = color.green.toFloat() / 255f
        val blue = color.blue.toFloat() / 255f
        val alpha = color.alpha.toFloat() / 255f

        val worldRenderer = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE_COLOR)
        worldRenderer.pos(matrixStack, 0.0, height, 0.0).tex(0.0, 1.0).color(red, green, blue, alpha).endVertex()
        worldRenderer.pos(matrixStack, width, height, 0.0).tex(1.0, 1.0).color(red, green, blue, alpha)
            .endVertex()
        worldRenderer.pos(matrixStack, width, 0.0, 0.0).tex(1.0, 0.0).color(red, green, blue, alpha).endVertex()
        worldRenderer.pos(matrixStack, 0.0, 0.0, 0.0).tex(0.0, 0.0).color(red, green, blue, alpha).endVertex()
        worldRenderer.build()?.drawAndClose(PIPELINE) {
            texture(0, textureId)
        }

    }

    companion object {
        private val PIPELINE = URenderPipeline.builderWithDefaultShader(
            "essential:screenshot_image",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_TEXTURE_COLOR,
        ).apply {
            blendState = BlendState.ALPHA
        }.build()
    }
}