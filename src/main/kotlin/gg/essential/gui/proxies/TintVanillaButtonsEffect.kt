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
package gg.essential.gui.proxies

import gg.essential.elementa.components.UIBlock
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.shader.BlendState
import gg.essential.universal.vertex.UBufferBuilder
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.UDrawContext
import gg.essential.util.image.GpuTexture
import java.awt.Color
import java.io.Closeable
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import gg.essential.gui.proxies.TintVanillaButtonsEffectShared.*
import gg.essential.gui.proxies.TintVanillaButtonsEffectShared.Companion.NON_WHITE_TINT_PIPELINE
import gg.essential.gui.proxies.TintVanillaButtonsEffectShared.Companion.averageButtonColor

/**
 * Applies a tint value to a component area. This is done by snapshotting the framebuffer behind the component,
 * then re-rendering the area with a fragment shader.
 * This shader specifically does not tint pure white pixels, to not affect vanilla button selection outlines
 *
 * Is overridden in 1.21.6 by a very different implementation
 */
class TintVanillaButtonsEffect {

    private val resources = Resources(this)

    init {
        Resources.drainCleanupQueue()
    }

    private fun beforeDraw(matrixStack: UMatrixStack, area: AreaToBeTinted) {
        if (resources.background.width != area.width || resources.background.height != area.height) {
            resources.background.resize(area.width, area.height)
        }
        resources.background.copyFrom(listOf(GpuTexture.CopyOp(
            platform.outputColorTextureOverride ?: platform.mcFrameBufferColorTexture,
            area.xViewport, area.yViewport, 0, 0, area.width, area.height
        )))

        // Clear the render target before we draw our content
        clear(matrixStack, area)
    }

    private fun clear(matrixStack: UMatrixStack, area: AreaToBeTinted) {
        UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR).also { buffer ->
            UIBlock.drawBlock(
                buffer,
                matrixStack,
                // Note: The BlendState of CLEAR_PIPELINE will ignore this value, but MC's fragment shader will discard
                //       pixels with alpha 0, so we must use a non-0 value here.
                Color(0, 0, 0, 255), area.x1.toDouble(), area.y1.toDouble(), area.x2.toDouble(), area.y2.toDouble(),
            )
        }.build()?.drawAndClose(CLEAR_PIPELINE)
    }

    private fun afterDraw(matrixStack: UMatrixStack, area: AreaToBeTinted, color: Color) {
        // collect the texture of the button to be tinted from the render target
        if (resources.button.width != area.width || resources.button.height != area.height) {
            resources.button.resize(area.width, area.height)
        }

        resources.button.copyFrom(listOf(GpuTexture.CopyOp(
            platform.outputColorTextureOverride ?: platform.mcFrameBufferColorTexture,
            area.xViewport, area.yViewport, 0, 0, area.width, area.height
        )))

        // Get the coordinates of the component within the bounds of the screen in fractional MC pixels
        val x = area.xMC
        val y = area.yMC
        val width = area.widthMC
        val height = area.heightMC


        // blit the background texture to the framebuffer
        (platform.outputColorTextureOverride ?: platform.mcFrameBufferColorTexture).copyFrom(listOf(GpuTexture.CopyOp(
            resources.background, 0, 0, area.xViewport, area.yViewport, area.width, area.height
        )))

        val averageCol = averageButtonColor(resources.button, 0, 0, area.width, area.height) ?: return

        UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE_COLOR).also { buffer ->
            buffer.pos(matrixStack, x, y + height, 0.0).tex(0.0, 0.0).color(color).endVertex()
            buffer.pos(matrixStack, x + width, y + height, 0.0).tex(1.0, 0.0).color(color).endVertex()
            buffer.pos(matrixStack, x + width, y, 0.0).tex(1.0, 1.0).color(color).endVertex()
            buffer.pos(matrixStack, x, y, 0.0).tex(0.0, 1.0).color(color).endVertex()
        }.build()?.drawAndClose(NON_WHITE_TINT_PIPELINE) {
            texture("u_Button", resources.button.glId)
            uniform("u_AverageColor",
                averageCol.red / 255F,
                averageCol.green / 255F,
                averageCol.blue / 255F)
        }
    }

    fun drawTinted(context: UDrawContext, x1: Int, y1: Int, x2: Int, y2: Int, color: Color, draw: (UDrawContext) -> Unit) {
        val areaToBeTinted = AreaToBeTinted(x1, y1, x2, y2)
        if (areaToBeTinted.invalid) return

        beforeDraw(context.matrixStack, areaToBeTinted)
        // Draw the content that needs to be tinted
        draw(context)
        afterDraw(context.matrixStack, areaToBeTinted, color)
    }

    private class Resources(effect: TintVanillaButtonsEffect) : PhantomReference<TintVanillaButtonsEffect>(effect, referenceQueue), Closeable {
        val background: GpuTexture = platform.newGpuTexture(1, 1, GpuTexture.Format.RGBA8)
        val button: GpuTexture = platform.newGpuTexture(1, 1, GpuTexture.Format.RGBA8)

        init {
            toBeCleanedUp.add(this)
        }

        private var closed = false

        override fun close() {
            if (closed) return
            closed = true

            toBeCleanedUp.remove(this)

            background.delete()
            button.delete()
        }

        companion object {
            val referenceQueue = ReferenceQueue<TintVanillaButtonsEffect>()
            val toBeCleanedUp: MutableSet<Resources> = Collections.newSetFromMap(ConcurrentHashMap())

            fun drainCleanupQueue() {
                while (true) {
                    ((referenceQueue.poll() ?: break) as Resources).close()
                }
            }
        }
    }

    companion object {

        private val CLEAR_PIPELINE: URenderPipeline = URenderPipeline.builderWithDefaultShader(
            "essential:darken_vanilla/clear",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_COLOR,
        ).apply {
            blendState = BlendState(BlendState.Equation.ADD, BlendState.Param.ZERO, BlendState.Param.ZERO)
        }.build()
    }
}