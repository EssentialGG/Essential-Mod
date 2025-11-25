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
package gg.essential.gui.effects

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.effects.Effect
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.shader.BlendState
import gg.essential.universal.vertex.UBufferBuilder
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.image.GpuTexture
import java.awt.Color
import java.io.Closeable
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Applies an alpha value to a component. This is done by snapshotting the framebuffer behind the component,
 * rendering the component, then rendering the snapshot with the inverse of the desired alpha.
 */
class AlphaEffect(private val alphaState: State<Float>) : Effect() {
    constructor(alphaState: gg.essential.elementa.state.State<Float>) : this(alphaState.toV2())

    private lateinit var resources: Resources

    override fun setup() {
        Resources.drainCleanupQueue()
        resources = Resources(this)
    }

    override fun beforeDraw(matrixStack: UMatrixStack) {
        if (!::resources.isInitialized) error("AlphaEffect has not yet been setup or has already been cleaned up! ElementaVersion.V4 or newer is required for proper operation!")

        val scale = UResolution.scaleFactor

        // Get the coordinates of the component within the bounds of the screen in real pixels
        val left = (boundComponent.getLeft() * scale).toInt().coerceIn(0..UResolution.viewportWidth)
        val right = (boundComponent.getRight() * scale).toInt().coerceIn(0..UResolution.viewportWidth)
        val top = (boundComponent.getTop() * scale).toInt().coerceIn(0..UResolution.viewportHeight)
        val bottom = (boundComponent.getBottom() * scale).toInt().coerceIn(0..UResolution.viewportHeight)

        val x = left
        val y = UResolution.viewportHeight - bottom // OpenGL screen coordinates start in the bottom left
        val width = right - left
        val height = bottom - top

        if (width == 0 || height == 0) {
            return
        }

        if (resources.texture.width != width || resources.texture.height != height) {
            resources.texture.resize(width, height)
        }
        resources.texture.copyFrom(listOf(GpuTexture.CopyOp(
            platform.outputColorTextureOverride ?: platform.mcFrameBufferColorTexture, x, y, 0, 0, width, height
        )))

        // Clear the render target before we draw our content
        clear(matrixStack)
    }

    private fun clear(matrixStack: UMatrixStack) {
        UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR).also { buffer ->
            UIBlock.drawBlock(
                buffer,
                matrixStack,
                // Note: The BlendState of CLEAR_PIPELINE will ignore this value, but MC's fragment shader will discard
                //       pixels with alpha 0, so we must use a non-0 value here.
                Color(0, 0, 0, 255),
                boundComponent.getLeft().toDouble(),
                boundComponent.getTop().toDouble(),
                boundComponent.getRight().toDouble(),
                boundComponent.getBottom().toDouble(),
            )
        }.build()?.drawAndClose(CLEAR_PIPELINE)
    }

    override fun afterDraw(matrixStack: UMatrixStack) {
        // Get the coordinates of the component within the bounds of the screen in fractional MC pixels
        val left = boundComponent.getLeft().toDouble().coerceIn(0.0..UResolution.viewportWidth / UResolution.scaleFactor)
        val right = boundComponent.getRight().toDouble().coerceIn(0.0..UResolution.viewportWidth / UResolution.scaleFactor)
        val top = boundComponent.getTop().toDouble().coerceIn(0.0..UResolution.viewportHeight / UResolution.scaleFactor)
        val bottom = boundComponent.getBottom().toDouble().coerceIn(0.0..UResolution.viewportHeight / UResolution.scaleFactor)

        val x = left
        val y = top
        val width = right - left
        val height = bottom - top

        if (width == 0.0 || height == 0.0) {
            return
        }

        // Make the thing we just rendered translucent (i.e. multiply it by the configured alpha factor)
        // Need a special case for 0 here because MC will discard fragments with alpha 0, but they are important for us
        val alpha = (alphaState.getUntracked() * 255).toInt()
        if (alpha != 0) {
            UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR).also { buffer ->
                UIBlock.drawBlock(buffer, matrixStack, Color(0, 0, 0, alpha), left, top, right, bottom)
            }.build()?.drawAndClose(MULTIPLY_PIPELINE)
        } else {
            clear(matrixStack)
        }

        // Composite the background behind the content
        UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE).also { buffer ->
            buffer.pos(matrixStack, x, y + height, 0.0).tex(0.0, 0.0).endVertex()
            buffer.pos(matrixStack, x + width, y + height, 0.0).tex(1.0, 0.0).endVertex()
            buffer.pos(matrixStack, x + width, y, 0.0).tex(1.0, 1.0).endVertex()
            buffer.pos(matrixStack, x, y, 0.0).tex(0.0, 1.0).endVertex()
        }.build()?.drawAndClose(COMPOSITE_PIPELINE) {
            texture(0, resources.texture.glId)
        }
    }

    fun cleanup() {
        resources.close()
    }

    private class Resources(effect: AlphaEffect) : PhantomReference<AlphaEffect>(effect, referenceQueue), Closeable {
        val texture: GpuTexture = platform.newGpuTexture(1, 1, GpuTexture.Format.RGBA8)

        init {
            toBeCleanedUp.add(this)
        }

        private var closed = false

        override fun close() {
            if (closed) return
            closed = true

            toBeCleanedUp.remove(this)

            texture.delete()
        }

        companion object {
            val referenceQueue = ReferenceQueue<AlphaEffect>()
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
            "elementa:alpha_effect/clear",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_COLOR,
        ).apply {
            blendState = BlendState(BlendState.Equation.ADD, BlendState.Param.ZERO, BlendState.Param.ZERO)
        }.build()

        private val MULTIPLY_PIPELINE: URenderPipeline = URenderPipeline.builderWithDefaultShader(
            "elementa:alpha_effect/multiply",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_COLOR,
        ).apply {
            blendState = BlendState(
                BlendState.Equation.ADD,
                BlendState.Param.ZERO,
                BlendState.Param.SRC_ALPHA,
            )
        }.build()

        private val COMPOSITE_PIPELINE: URenderPipeline = URenderPipeline.builderWithDefaultShader(
            "elementa:alpha_effect/composite",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_TEXTURE,
        ).apply {
            // This is BlendState.PREMULTIPLIED_ALPHA but with the role of source and destination flipped because
            // it's the background which we have captured as a texture while the foreground was rendered into the
            // render target.
            blendState = BlendState(
                BlendState.Equation.ADD,
                BlendState.Param.ONE_MINUS_DST_ALPHA,
                BlendState.Param.ONE,
            )
        }.build()
    }
}