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

import com.mojang.blaze3d.systems.RenderSystem
import gg.essential.model.util.Color
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.shader.BlendState
import gg.essential.universal.vertex.UBufferBuilder
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.image.GpuTexture
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.render.fog.FogRenderer

private val COMPOSITE_PIPELINE = URenderPipeline.builderWithDefaultShader(
    "essential:gui_renderer_composite",
    UGraphics.DrawMode.QUADS,
    UGraphics.CommonVertexFormats.POSITION_TEXTURE,
).apply {
    blendState = BlendState.PREMULTIPLIED_ALPHA
}.build()

/**
 * Renders the given [GuiRenderState] to the render target.
 *
 * Most of the implementation is dealing with the fact that MC's gui renderer always renders to MC's framebuffer and
 * doesn't respect [RenderSystem.outputColorTextureOverride].
 */
fun renderGuiRenderStateToRenderTarget(matrixStack: UMatrixStack, guiRenderState: GuiRenderState) {
    val resultColor = renderGuiRenderStateToTexture(guiRenderState)
    blitTextureToRenderTarget(matrixStack, resultColor)
    resultColor.delete()
}

/**
 * Renders the given [GuiRenderState] to a [GpuTexture].
 *
 * Most of the implementation is dealing with the fact that MC's gui renderer always renders to MC's framebuffer and
 * doesn't respect [RenderSystem.outputColorTextureOverride].
 */
fun renderGuiRenderStateToTexture(guiRenderState: GuiRenderState): GpuTexture {
    val mc = MinecraftClient.getInstance()
    val mcColor = platform.mcFrameBufferColorTexture
    val mcDepth = platform.mcFrameBufferDepthTexture!!

    val width = mcColor.width
    val height = mcColor.height

    val resultColor = platform.newGpuTexture(width, height, GpuTexture.Format.RGBA8)

    // Backup framebuffer
    val orgColor = platform.newGpuTexture(width, height, GpuTexture.Format.RGBA8)
    val orgDepth = platform.newGpuTexture(width, height, GpuTexture.Format.DEPTH32)
    orgColor.copyFrom(mcColor)
    orgDepth.copyFrom(mcDepth)
    mcColor.clearColor(Color(0u))
    mcDepth.clearDepth(1f)

    // Backup projection matrix
    // GuiRenderer replaces this buffer with its own, which is invalidated in its `close`, so we need to backup
    // and restore the original buffer, otherwise OpenGL will end up trying to use an already-freed buffer.
    val orgProjectionMatrixBuffer = RenderSystem.getProjectionMatrixBuffer()
    val orgProjectionType = RenderSystem.getProjectionType()
    // same deal as above, just for FogRenderer
    val orgShaderFog = RenderSystem.getShaderFog()

    // Render gui (to framebuffer)
    val fogRenderer = FogRenderer()
    val guiRenderer = GuiRenderer(
        guiRenderState,
        mc.bufferBuilders.entityVertexConsumers,
        //#if MC>=12109
        //$$ mc.gameRenderer.entityRenderCommandQueue,
        //$$ mc.gameRenderer.entityRenderDispatcher,
        //#endif
        emptyList(),
    )
    guiRenderer.render(fogRenderer.getFogBuffer(FogRenderer.FogType.NONE))
    guiRenderer.close()
    fogRenderer.close()
    GuiRendererInfo.customGuiRendererUsedThisFrame = true

    // Restore projection matrix
    // Nullability annotations are a bit off, null seems fine. And we MUST NOT keep the current buffer, even if the org
    // buffer is null, otherwise we'll have use-after-free bugs, see comment above on `orgProjectionMatrixBuffer`.
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    RenderSystem.setProjectionMatrix(orgProjectionMatrixBuffer, orgProjectionType)
    // same deal as above, just for FogRenderer
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    RenderSystem.setShaderFog(orgShaderFog)

    // Copy framebuffer to texture
    resultColor.copyFrom(mcColor)

    // Restore framebuffer
    mcColor.copyFrom(orgColor)
    mcDepth.copyFrom(orgDepth)
    orgColor.delete()
    orgDepth.delete()

    return resultColor
}

private fun blitTextureToRenderTarget(
    matrixStack: UMatrixStack,
    resultColor: GpuTexture
) {
    // Draw texture to render target
    val scale = 1 / UResolution.scaleFactor
    val w = resultColor.width * scale
    val h = resultColor.height * scale
    val buffer = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE)
    buffer.pos(matrixStack, 0.0, h, 0.0).tex(0.0, 0.0).endVertex()
    buffer.pos(matrixStack, w, h, 0.0).tex(1.0, 0.0).endVertex()
    buffer.pos(matrixStack, w, 0.0, 0.0).tex(1.0, 1.0).endVertex()
    buffer.pos(matrixStack, 0.0, 0.0, 0.0).tex(0.0, 1.0).endVertex()
    buffer.build()?.drawAndClose(COMPOSITE_PIPELINE) {
        texture(0, resultColor.glId)
    }
}

object GuiRendererInfo {
    var customGuiRendererUsedThisFrame = false
}
