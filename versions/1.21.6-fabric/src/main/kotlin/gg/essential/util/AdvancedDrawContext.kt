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
package gg.essential.util

import com.mojang.blaze3d.systems.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import gg.essential.event.render.RenderTickEvent
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution
import me.kbrewster.eventbus.Subscribe
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.ProjectionMatrix2
import net.minecraft.client.texture.AbstractTexture
import net.minecraft.util.Identifier

// Note: Copied from UniversalCraft, changes here should probably be applied over there as well.
// Changes from UC:
//   - `class` -> `object`
//   - `nextFrame` is called automatically via `RenderTickEvent.Pre`
//   - save and restore projection matrix
//   - split drawImmediate() functionality into `drawImmediate()` and `drawToTexture()` as the latter is reused by essential
/**
 * Allows rendering of raw OpenGL into [DrawContext] by drawing to a temporary texture which is then submitted as a
 * plain textured quad to [DrawContext].
 *
 * You **MUST** call [nextFrame] before the next frame begins but no sooner than MC's GuiRenderer actually using the
 * submitted textures. Repeated calls to [drawImmediate] without [nextFrame] or [close] inbetween will keep allocating
 * more and more gpu memory!
 * If you cannot guarantee further calls to [nextFrame], you must call [close] to release all resources.
 * The [AdvancedDrawContext] remains usable, [close] merely frees all resources, further calls to [drawImmediate] will
 * simply re-allocate them.
 */
internal object AdvancedDrawContext : AutoCloseable {
    private var allocatedProjectionMatrix: ProjectionMatrix2? = null

    val textureAllocator = TemporaryTextureAllocator {
        allocatedProjectionMatrix?.close()
        allocatedProjectionMatrix = null
    }

    fun drawImmediate(context: DrawContext, block: (UMatrixStack) -> Unit) {
        val width = UResolution.viewportWidth
        val height = UResolution.viewportHeight

        val texture = textureAllocator.allocate(width, height)

        drawToTexture(texture, block)

        draw(context, texture)
    }

    fun drawToTexture(
        texture: TemporaryTextureAllocator.TextureAllocation,
        block: (UMatrixStack) -> Unit
    ) {
        val width = texture.width
        val height = texture.height
        val scaleFactor = UResolution.scaleFactor.toFloat()

        var projectionMatrix = allocatedProjectionMatrix
        if (projectionMatrix == null) {
            projectionMatrix = ProjectionMatrix2("pre-rendered screen", 1000f, 21000f, true)
            allocatedProjectionMatrix = projectionMatrix
        }
        val orgProjectionMatrixBuffer = RenderSystem.getProjectionMatrixBuffer()
        val orgProjectionType = RenderSystem.getProjectionType()
        RenderSystem.setProjectionMatrix(
            projectionMatrix.set(width.toFloat() / scaleFactor, height.toFloat() / scaleFactor),
            ProjectionType.ORTHOGRAPHIC,
        )

        val orgOutputColorTextureOverride = RenderSystem.outputColorTextureOverride
        val orgOutputDepthTextureOverride = RenderSystem.outputDepthTextureOverride
        RenderSystem.outputColorTextureOverride = texture.textureView
        RenderSystem.outputDepthTextureOverride = texture.depthTextureView

        val matrixStack = UMatrixStack()
        matrixStack.translate(0f, 0f, -10000f)
        block(matrixStack)

        RenderSystem.outputColorTextureOverride = orgOutputColorTextureOverride
        RenderSystem.outputDepthTextureOverride = orgOutputDepthTextureOverride

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        RenderSystem.setProjectionMatrix(orgProjectionMatrixBuffer, orgProjectionType)
    }

    fun draw(context: DrawContext, texture: TemporaryTextureAllocator.TextureAllocation) {
        val width = texture.width
        val height = texture.height
        val scaleFactor = UResolution.scaleFactor.toFloat()

        val textureManager = MinecraftClient.getInstance().textureManager
        val identifier = Identifier.of("universalcraft", "__tmp_texture__")
        textureManager.registerTexture(identifier, object : AbstractTexture() {
            init { glTextureView = texture.textureView }
            override fun close() {} // we don't want the later `destroyTexture` to close our texture
        })

        context.matrices.pushMatrix()
        context.matrices.scale(1/scaleFactor, 1/scaleFactor) // drawTexture only accepts `int`s
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
            identifier,
            // x, y
            0, 0,
            // u, v
            0f, height.toFloat(),
            // width, height
            width, height,
            // uWidth, vHeight
            width, -height,
            // textureWidth, textureHeight
            width, height,
        )
        context.matrices.popMatrix()

        textureManager.destroyTexture(identifier)
    }

    @Subscribe
    fun nextFrame(event: RenderTickEvent) {
        if (!event.isPre) return
        textureAllocator.nextFrame()
    }

    override fun close() {
        textureAllocator.close()
    }
}
