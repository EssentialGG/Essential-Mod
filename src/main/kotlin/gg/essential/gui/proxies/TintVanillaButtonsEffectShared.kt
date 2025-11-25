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

import gg.essential.universal.UGraphics
import gg.essential.universal.UResolution
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.shader.BlendState
import gg.essential.util.ResourceManagerUtil
import gg.essential.util.image.GpuTexture
import gg.essential.util.image.bitmap.forEachPixel
import java.awt.Color

class TintVanillaButtonsEffectShared {

    // Represents the positions of the area to be tinted.
    // Handles scaling and viewport limits, and provides access to the co-ordinates as required by both GpuTexture and Minecraft.
    class AreaToBeTinted(
        // MC screen position corners
        val x1: Int, val y1: Int, val x2: Int, val y2: Int
    ) {
        val scale = UResolution.scaleFactor

        // Corner positions in actual pixels clamped to screen size
        val left = (x1 * scale).toInt().coerceIn(0..UResolution.viewportWidth)
        val right = (x2 * scale).toInt().coerceIn(0..UResolution.viewportWidth)
        val top = (y1 * scale).toInt().coerceIn(0..UResolution.viewportHeight)
        val bottom = (y2 * scale).toInt().coerceIn(0..UResolution.viewportHeight)
        val width = right - left
        val height = bottom - top

        // Co-ords as needed by gpu textures
        val xViewport get() = left
        val yViewport = UResolution.viewportHeight - bottom // OpenGL screen coordinates start in the bottom left

        // Co-ords as needed for MC drawing
        val xMC get() = left / scale
        val yMC get() = top / scale
        val widthMC get() = width / scale
        val heightMC get() = height / scale

        val invalid get() = width == 0 || height == 0
    }

    companion object {

        init {
            // reset averageVanillaButtonColor when the resources are reloaded
            ResourceManagerUtil.onResourceManagerReload {
                averageVanillaButtonColor = null
                satisfiedWithAverageColor = false
                lastHighestButtonAlpha = 0
                lastHighestAlphaTime = 0L
            }
        }

        private var averageVanillaButtonColor: Color? = null
        private var satisfiedWithAverageColor = false
        private var lastHighestButtonAlpha = 0
        private var lastHighestAlphaTime = 0L

        fun averageButtonColor(texture: GpuTexture, x: Int, y: Int, width: Int, height: Int): Color? {
            if (satisfiedWithAverageColor) return averageVanillaButtonColor

            if (lastHighestAlphaTime != 0L && System.currentTimeMillis() - lastHighestAlphaTime > 5000L) {
                // it has been 5 seconds since the last time we saw the highest alpha value increase, so we can assume that
                // the loaded resource-pack must just have transparent buttons
                satisfiedWithAverageColor = true

                // averageVanillaButtonColor may still be slightly off the real average due to limited precision for un-premultiplying the alpha
                return averageVanillaButtonColor
            }

            var r = 0f
            var g = 0f
            var b = 0f
            var count = 0

            val yGl = texture.height - (y + height) // flip y for OpenGL coordinates

            val bitmap = texture.readPixelColors(x, yGl, width, height)

            val prevHighestAlpha = lastHighestButtonAlpha

            bitmap.forEachPixel { pixel, _, _ ->
                val alpha = pixel.a.toInt()
                val red = pixel.r.toInt()
                val green = pixel.g.toInt()
                val blue = pixel.b.toInt()

                if (alpha > 0 && (red > 0 || green > 0 || blue > 0)) { // ignore black or fully transparent pixels
                    val alphaMultiplier = alpha / 255.0f
                    r += red / alphaMultiplier // un-premultiply the alpha
                    g += green / alphaMultiplier
                    b += blue / alphaMultiplier
                    count++

                    // title screen buttons fade in at the start and thus the low alpha may result in a low precision un-premultiplied color
                    // so we will track the highest alpha we have seen so far and improve our average color until we are satisfied
                    if (alpha > lastHighestButtonAlpha) {
                        lastHighestButtonAlpha = alpha
                        lastHighestAlphaTime = System.currentTimeMillis()
                        satisfiedWithAverageColor = alpha > 64 // testing shows this is sufficient precision to un-premultiply
                    }
                }
            }
            // only recalculate the average color if we have an improved un-premultiplied precision, this will also
            // prevent the average color cycling between each individual button on screen in pre 1.21.6 versions as only
            // the first draw with a newly increased alpha will trigger this
            // the 1.21.6+ impl only calls once per frame so avoids this issue
            if (count != 0 && prevHighestAlpha != lastHighestButtonAlpha) {
                averageVanillaButtonColor = Color(r / count / 255f, g / count / 255f, b / count / 255f)
            }

            return averageVanillaButtonColor
        }

        val NON_WHITE_TINT_PIPELINE: URenderPipeline = URenderPipeline.builderWithLegacyShader(
            "essential:darken_vanilla/tint",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_TEXTURE_COLOR,
            """
                #version 110
   
                varying vec2 f_Position;
                varying vec2 f_TexCoord;
                varying vec4 f_Color;
    
                void main() {
                    f_Position = gl_Vertex.xy;
                    f_TexCoord = gl_MultiTexCoord0.st;
                    
                    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
                    f_Color = gl_Color;
                }
            """.trimIndent(),
            """
                #version 110
    
                uniform sampler2D u_Button;
                uniform vec3 u_AverageColor;
    
                varying vec2 f_Position;
                varying vec2 f_TexCoord;
                varying vec4 f_Color;
    
                void main() {
                    vec4 buttonColor = texture2D(u_Button, f_TexCoord);
                    
                    // Avoid tinting pure white pixels
                    if (buttonColor == vec4(1.0)) {
                        gl_FragColor = buttonColor;
                    } else {
                        // Tint scaled by button alpha
                        vec3 tint = clamp(u_AverageColor - f_Color.rgb, 0.0, 1.0) * buttonColor.a;
                        vec3 tintedColor = clamp(buttonColor.rgb - tint, 0.0, 1.0);
                        gl_FragColor = vec4(tintedColor, buttonColor.a);
                    }
                }
            """.trimIndent()
        ).apply {
            blendState = BlendState.PREMULTIPLIED_ALPHA
            depthTest = URenderPipeline.DepthTest.Always
        }.build()
    }
}