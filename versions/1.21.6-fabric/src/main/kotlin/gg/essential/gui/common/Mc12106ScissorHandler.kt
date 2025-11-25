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

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.systems.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.shader.BlendState
import gg.essential.universal.vertex.UBufferBuilder
import net.minecraft.client.render.ProjectionMatrix2
import net.minecraft.client.texture.GlTexture
import java.io.Closeable
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Mc12106ScissorHandler {

    var viewportWidth: Int = 0
        private set
    var viewportHeight: Int = 0
        private set

    private var resources: Resources? = null

    private var orgOutputColorTextureOverride: GpuTextureView? = null
    private var orgOutputDepthTextureOverride: GpuTextureView? = null
    private var orgProjectionMatrix: Pair<GpuBufferSlice?, ProjectionType>? = null

    fun beforeDraw(matrixStack: UMatrixStack) {
        Resources.drainCleanupQueue()

        val scissorState = ScissorEffect.currentScissorState
        if (scissorState == null) {
            viewportWidth = UResolution.viewportWidth
            viewportHeight = UResolution.viewportHeight
            return
        }

        val scale = 1 / UResolution.scaleFactor
        val width = scissorState.width
        val height = scissorState.height

        viewportWidth = width
        viewportHeight = height

        if (width == 0 || height == 0) {
            // Can't create a zero-sized texture, so we'll just draw it far off-screen instead
            matrixStack.push()
            matrixStack.translate(-1e10, -1e10, 0.0)
            return
        }

        var resources = resources
        if (resources == null || resources.width != width || resources.height != height) {
            resources?.close()
            resources = Resources(this, width, height)
            this.resources = resources
        }

        RenderSystem.getDevice().createCommandEncoder()
            .clearColorAndDepthTextures(resources.texture, 0, resources.depthTexture, 1.0)

        orgOutputColorTextureOverride = RenderSystem.outputColorTextureOverride
        orgOutputDepthTextureOverride = RenderSystem.outputDepthTextureOverride
        RenderSystem.outputColorTextureOverride = resources.textureView
        RenderSystem.outputDepthTextureOverride = resources.depthTextureView

        orgProjectionMatrix = Pair(RenderSystem.getProjectionMatrixBuffer(), RenderSystem.getProjectionType())
        RenderSystem.setProjectionMatrix(
            resources.projectionMatrix.set((width * scale).toFloat(), (height * scale).toFloat()),
            ProjectionType.ORTHOGRAPHIC,
        )

        matrixStack.push()
        val x = scissorState.x * scale
        val y = (UResolution.viewportHeight - scissorState.y - scissorState.height) * scale
        matrixStack.translate(-x, -y, 0.0)
    }

    fun afterDraw(matrixStack: UMatrixStack) {
        val scissorState = ScissorEffect.currentScissorState ?: return

        matrixStack.pop()

        if (scissorState.width == 0 || scissorState.height == 0) {
            return
        }

        orgProjectionMatrix?.let { (buf, type) ->
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // missing Nullable annotation
            RenderSystem.setProjectionMatrix(buf, type)
        }

        RenderSystem.outputColorTextureOverride = orgOutputColorTextureOverride
        RenderSystem.outputDepthTextureOverride = orgOutputDepthTextureOverride

        val scale = 1 / UResolution.scaleFactor
        val x = scissorState.x * scale
        val y = (UResolution.viewportHeight - scissorState.y - scissorState.height) * scale
        val width = scissorState.width * scale
        val height = scissorState.height * scale

        val buffer = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE)
        buffer.pos(matrixStack, x, y + height, 0.0).tex(0.0, 0.0).endVertex()
        buffer.pos(matrixStack, x + width, y + height, 0.0).tex(1.0, 0.0).endVertex()
        buffer.pos(matrixStack, x + width, y, 0.0).tex(1.0, 1.0).endVertex()
        buffer.pos(matrixStack, x, y, 0.0).tex(0.0, 1.0).endVertex()
        buffer.build()?.drawAndClose(PIPELINE) {
            texture(0, (resources?.texture as GlTexture).glId)
        }
    }

    private class Resources(
        owner: Any,
        val width: Int,
        val height: Int,
    ) : PhantomReference<Any>(owner, referenceQueue), Closeable {
        private val gpuDevice = RenderSystem.getDevice()

        var texture = gpuDevice.createTexture(
            { "Scissored texture" },
            GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_TEXTURE_BINDING,
            TextureFormat.RGBA8,
            width,
            height,
            1,
            1
        ).apply { setTextureFilter(FilterMode.NEAREST, false) }
        var depthTexture = gpuDevice.createTexture(
            { "Scissored depth texture" },
            GpuTexture.USAGE_RENDER_ATTACHMENT,
            TextureFormat.DEPTH32,
            width,
            height,
            1,
            1
        )

        var textureView = gpuDevice.createTextureView(texture)
        var depthTextureView = gpuDevice.createTextureView(depthTexture)

        val projectionMatrix = ProjectionMatrix2("scissored gui", 1000f, 11000f, true)

        init {
            toBeCleanedUp.add(this)
        }

        private var closed = false

        override fun close() {
            if (closed) return
            closed = true

            toBeCleanedUp.remove(this)

            projectionMatrix.close()
            depthTextureView.close()
            textureView.close()
            depthTexture.close()
            texture.close()
        }

        companion object {
            val referenceQueue = ReferenceQueue<Any>()
            val toBeCleanedUp: MutableSet<Resources> = Collections.newSetFromMap(ConcurrentHashMap())

            fun drainCleanupQueue() {
                while (true) {
                    ((referenceQueue.poll() ?: break) as Resources).close()
                }
            }
        }
    }

    companion object {
        private val PIPELINE: URenderPipeline = URenderPipeline.builderWithDefaultShader(
            "essential:scissor",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_TEXTURE,
        ).apply {
            blendState = BlendState.PREMULTIPLIED_ALPHA
        }.build()
    }
}
