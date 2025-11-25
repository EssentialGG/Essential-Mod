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

import gg.essential.elementa.effects.Effect
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMinecraft
import gg.essential.universal.UMouse
import gg.essential.universal.UResolution
import gg.essential.universal.UResolution.viewportHeight
import gg.essential.universal.UResolution.viewportWidth
import gg.essential.universal.render.DrawCallBuilder
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.shader.BlendState
import gg.essential.universal.vertex.UBufferBuilder
import gg.essential.util.GlFrameBuffer
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.image.GpuTexture
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.roundToInt

class CosmeticHoverOutlineEffect(
    private val backgroundColor: Color,
    private val outlineCosmetic: State<List<Cosmetic>>,
) : Effect() {

    private var previousScissorEffectState: ScissorEffect.ScissorState? = null
    private var previousScissorState: Boolean = false
    private var previousFrameBuffer: () -> Unit = {}

    private val mutableHoveredCosmetic = mutableStateOf<Cosmetic?>(null)
    val hoveredCosmetic: State<Cosmetic?> = mutableHoveredCosmetic

    override fun beforeDraw(matrixStack: UMatrixStack) {
        check(active == null) { "Outline effects cannot be nested." }
        active = this

        previousScissorEffectState = ScissorEffect.currentScissorState
        ScissorEffect.currentScissorState = null // required for Mc12106ScissorHandler to behave correctly
        previousScissorState = GL11.glGetBoolean(GL11.GL_SCISSOR_TEST)
        GL11.glDisable(GL11.GL_SCISSOR_TEST)

        if (!mcFrameBufferSupported) {
            fallbackFrameBuffer.resize(viewportWidth, viewportHeight)
            previousFrameBuffer = fallbackFrameBuffer.bind()
        }

        mainTextureCopy.copyFrom(renderTargetColor)
        renderTargetColor.clearColor(gg.essential.model.util.Color(backgroundColor.red.toUByte(), backgroundColor.green.toUByte(), backgroundColor.blue.toUByte(), 0u))
        renderTargetDepth.clearDepth(1f)
    }

    override fun afterDraw(matrixStack: UMatrixStack) {
        compositeRenderResult.color.copyFrom(renderTargetColor)
        compositeRenderResult.depth.copyFrom(renderTargetDepth)
        renderTargetColor.copyFrom(mainTextureCopy)
        renderTargetDepth.clearDepth(1f)

        previousFrameBuffer()

        if (previousScissorState) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST)
        }
        ScissorEffect.currentScissorState = previousScissorEffectState

        renderFullScreenQuad(COMPOSITE_PIPELINE) {
            texture("ColorSampler", compositeRenderResult.color.glId)
            texture("DepthSampler", compositeRenderResult.depth.glId)
        }

        mutableHoveredCosmetic.set(computeHoveredCosmetic())

        outlineCosmetic.get().forEach { cosmetic ->
            val renderResult = renderResults[cosmetic]
            if (renderResult != null) {
                doDrawOutline(renderResult)
            }
        }

        cleanup()

        active = null
    }

    private val renderResults = mutableMapOf<Cosmetic, RenderResult>()

    fun beginOutlineRender(cosmetic: Cosmetic) {
        compositeRenderResult.color.copyFrom(renderTargetColor)
        compositeRenderResult.depth.copyFrom(renderTargetDepth)

        val renderResult = renderResults[cosmetic]
        if (renderResult != null) {
            renderTargetColor.copyFrom(renderResult.color)
            renderTargetDepth.copyFrom(renderResult.depth)
        } else {
            renderTargetColor.clearColor(gg.essential.model.util.Color(0u))
            renderTargetDepth.clearDepth(1f)
        }
    }

    fun endOutlineRender(cosmetic: Cosmetic) {
        val renderResult = renderResults[cosmetic] ?: unusedRenderResults.removeLastOrNull() ?: RenderResult()
        renderResult.color.copyFrom(renderTargetColor)
        renderResult.depth.copyFrom(renderTargetDepth)
        renderResults[cosmetic] = renderResult

        renderTargetColor.copyFrom(compositeRenderResult.color)
        renderTargetDepth.copyFrom(compositeRenderResult.depth)
    }

    private fun computeHoveredCosmetic(): Cosmetic? {
        val scissor = ScissorEffect.currentScissorState
        if (scissor != null && !scissor.contains(UMouse.Scaled.x, UMouse.Scaled.y)) {
            return null
        }

        val (hoveredCosmetic, hoveredDepth) = renderResults.entries.associate {
            it.key to it.value.depth.readHoveredDepth()
        }.minByOrNull { it.value } ?: return null

        val compositeDepth = compositeRenderResult.depth.readHoveredDepth()
        if (hoveredDepth - 0.0001f >= compositeDepth.coerceAtMost(0.999f)) {
            return null // player is obstructing the cosmetic
        }

        return hoveredCosmetic
    }

    private fun doDrawOutline(renderResult: RenderResult) {
        renderFullScreenQuad(OUTLINE_PIPELINE) {
            texture("CompositeSampler", compositeRenderResult.depth.glId)
            texture("TargetSampler", renderResult.depth.glId)
            uniform("OneTexel", 1f / viewportWidth, 1f / viewportHeight)
            uniform("OutlineWidth", UMinecraft.guiScale * 2)
        }
    }

    fun cleanup() {
        unusedRenderResults.addAll(renderResults.values)
        renderResults.clear()
    }

    private fun renderFullScreenQuad(pipeline: URenderPipeline, configure: DrawCallBuilder.() -> Unit) {
        UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE).apply {
            pos(UMatrixStack.UNIT, 0.0, 0.0, 0.0).tex(0.0, 0.0).endVertex()
            pos(UMatrixStack.UNIT, 1.0, 0.0, 0.0).tex(1.0, 0.0).endVertex()
            pos(UMatrixStack.UNIT, 1.0, 1.0, 0.0).tex(1.0, 1.0).endVertex()
            pos(UMatrixStack.UNIT, 0.0, 1.0, 0.0).tex(0.0, 1.0).endVertex()
        }.build()?.drawAndClose(pipeline, configure)
    }

    private fun ScissorEffect.ScissorState.contains(testX: Double, testY: Double): Boolean {
        val scaleFactor = UResolution.scaleFactor.toInt()
        val tx = (testX * scaleFactor).roundToInt()
        val ty = viewportHeight - (testY * scaleFactor).roundToInt()
        return x <= tx && tx < x + width && y <= ty && ty < y + height
    }

    private class RenderResult(
        val color: GpuTexture = GpuTexture(viewportWidth, viewportHeight, GpuTexture.Format.RGBA8),
        val depth: GpuTexture = GpuTexture(viewportWidth, viewportHeight, GpuTexture.Format.DEPTH32),
    )

    companion object {
        var active: CosmeticHoverOutlineEffect? = null
            private set

        // MC prior to 1.16 does not use a depth texture with its framebuffer, so we need to use a framebuffer of our
        // own on those versions
        private val mcFrameBufferSupported by lazy { platform.mcFrameBufferDepthTexture != null }
        private val fallbackFrameBuffer by lazy { GlFrameBuffer(viewportWidth, viewportHeight, depthFormat = GpuTexture.Format.DEPTH32) }
        private val renderTargetColor: GpuTexture
            get() = platform.outputColorTextureOverride ?: if (mcFrameBufferSupported) platform.mcFrameBufferColorTexture else fallbackFrameBuffer.texture
        private val renderTargetDepth: GpuTexture
            get() = platform.outputDepthTextureOverride ?: if (mcFrameBufferSupported) platform.mcFrameBufferDepthTexture!! else fallbackFrameBuffer.depthStencil

        private val mainTextureCopy by lazy { GpuTexture(viewportWidth, viewportHeight, GpuTexture.Format.RGBA8) }
        private val compositeRenderResult by lazy { RenderResult() }

        private val unusedRenderResults = mutableListOf<RenderResult>()

        private fun GpuTexture.readHoveredDepth(): Float = readPixelDepth(
            (UMouse.Scaled.x * UResolution.scaleFactor).toInt(),
            viewportHeight - (UMouse.Scaled.y * UResolution.scaleFactor).toInt(),
        )

        private val vertexShaderSource = """
            #version 120
            varying vec2 texCoord;
            void main(){
                gl_Position = vec4(gl_Vertex.xy * 2.0 - vec2(1.0), 0.5, 1.0);
                texCoord = gl_Vertex.xy;
            }
        """.trimIndent()

        private val compositeFragmentShaderSource = """
            #version 120
            uniform sampler2D ColorSampler;
            uniform sampler2D DepthSampler;
            varying vec2 texCoord;
            void main() {
                vec4 color = texture2D(ColorSampler, texCoord);
                if (color.a == 0.0) {
                    discard;
                }
                gl_FragColor = vec4(color.rgb, 1.0);
                gl_FragDepth = texture2D(DepthSampler, texCoord).r;
            }
        """.trimIndent()

        private val COMPOSITE_PIPELINE = URenderPipeline.builderWithLegacyShader(
            "essential:cosmetic_hover_outline_composite",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_TEXTURE,
            vertexShaderSource,
            compositeFragmentShaderSource,
        ).apply {
            blendState = BlendState.NORMAL
            depthTest = URenderPipeline.DepthTest.LessOrEqual
        }.build()

        private val outlineFragmentShaderSource = """
            #version 120
            uniform sampler2D CompositeSampler;
            uniform sampler2D TargetSampler;
            uniform vec2 OneTexel;
            uniform int OutlineWidth;
            varying vec2 texCoord;
            
            vec4 query(vec2 offset) {
                float composite = texture2D(CompositeSampler, texCoord + offset).r;
                float depth = texture2D(TargetSampler, texCoord + offset).r;
                if (depth > 0.99 || composite < depth) {
                    return vec4(0, 0, 0, 0);
                } else {
                    return vec4(1, 1, 1, 1);
                }
            }
            void main() {
                vec4 fragColor;
                
                bool isInside = false;
                bool shouldRender = false;
                
                for (int x = -OutlineWidth; x < OutlineWidth; x++) {
                    for (int y = -OutlineWidth; y < OutlineWidth; y++) {
                        vec2 d = vec2(float(x) * OneTexel.x, float(y) * OneTexel.y);
                        float value = query(d).a;
                        if (x == 0 && y == 0 && value == 1) {
                            isInside = true;
                        }
                        if (value == 1) {
                            shouldRender = true;
                        }
                    }
                }
                if (shouldRender && !isInside) {
                    fragColor = vec4(1, 1, 1, 1);
                } else {
                    fragColor = vec4(0, 0, 0, 0);
                }
                
                float fragDepth;
                {
                    float center = texture2D(TargetSampler, texCoord).r;
                    float left = texture2D(TargetSampler, texCoord - vec2(OneTexel.x, 0.0)).r;
                    float right = texture2D(TargetSampler, texCoord + vec2(OneTexel.x, 0.0)).r;
                    float up = texture2D(TargetSampler, texCoord - vec2(0.0, OneTexel.y)).r;
                    float down = texture2D(TargetSampler, texCoord + vec2(0.0, OneTexel.y)).r;
                    fragDepth = min(center, min(min(left, right), min(up, down)));
                }
                
                gl_FragColor = fragColor;
                gl_FragDepth = fragDepth;
            }
        """.trimIndent()

        private val OUTLINE_PIPELINE = URenderPipeline.builderWithLegacyShader(
            "essential:cosmetic_hover_outline",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_TEXTURE,
            vertexShaderSource,
            outlineFragmentShaderSource,
        ).apply {
            blendState = BlendState.NORMAL
            depthTest = URenderPipeline.DepthTest.Always
        }.build()
    }
}
