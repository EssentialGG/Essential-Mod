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
package gg.essential.render

import gg.essential.model.light.Light
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.vertex.UVertexConsumer
import net.minecraft.util.ResourceLocation

//#if MC>=11600
//$$ import gg.essential.util.identifier
//$$ import net.minecraft.client.renderer.RenderType
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer
//$$ import net.minecraft.client.Minecraft
//#endif

/**
 * Using [create] on 1.16 and lower, [tex] and [light] will not do anything. This is because it is backed by [UGraphics] on
 * these versions (using [UGraphics.CommonVertexFormats.POSITION_COLOR]). Using [createWithTexture] on these versions,
 * [tex] and [light] are passed through, as the vertex format is [UGraphics.CommonVertexFormats.POSITION_COLOR_TEXTURE_LIGHT].
 *
 * On 1.16+, this uses [net.minecraft.client.renderer.RenderType.getText]. If [createWithTexture] is used, the specified
 * texture is used, otherwise a white texture is used (effectively acting as if there is no texture).
 */
class TextRenderTypeVertexConsumer(private val vertexConsumer: UVertexConsumer, private val passTexLight: Boolean): UVertexConsumer {
    companion object {
        //#if MC>=11600
        //$$ private val WHITE_TEXTURE = identifier("essential", "textures/white.png")
        //$$
        //$$ @JvmStatic
        //$$ @JvmOverloads
        //$$ fun create(provider: IRenderTypeBuffer, seeThrough: Boolean = false): TextRenderTypeVertexConsumer {
        //$$     return createWithTexture(provider, WHITE_TEXTURE, seeThrough)
        //$$ }
        //$$
        //$$ @JvmStatic
        //$$ @JvmOverloads
        //$$ fun createWithTexture(provider: IRenderTypeBuffer, texture: ResourceLocation, seeThrough: Boolean = false): TextRenderTypeVertexConsumer {
        //$$     val type = if (seeThrough) RenderType.getTextSeeThrough(texture) else RenderType.getText(texture)
        //$$     val consumer = UVertexConsumer.of(provider.getBuffer(type))
        //$$
        //$$     return TextRenderTypeVertexConsumer(consumer, true)
        //$$ }
        //#else
        @JvmStatic
        fun create(buffer: UGraphics): TextRenderTypeVertexConsumer {
            @Suppress("DEPRECATION")
            buffer.beginWithDefaultShader(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR)
            return TextRenderTypeVertexConsumer(buffer.asUVertexConsumer(), false)
        }

        @JvmStatic
        fun createWithTexture(buffer: UGraphics, texture: ResourceLocation): TextRenderTypeVertexConsumer {
            UGraphics.bindTexture(0, texture)
            @Suppress("DEPRECATION")
            buffer.beginWithDefaultShader(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR_TEXTURE_LIGHT)
            return TextRenderTypeVertexConsumer(buffer.asUVertexConsumer(), true)
        }
        //#endif
    }

    fun light(light: Int) = apply {
        this.light(Light(light.toUInt()))
    }

    @JvmSynthetic // prevent ambiguous calls from java (at least from intellij's perspective, in reality the function gets mangled)
    fun light(light: Light) = apply {
        // On 1.12.2 and below, BufferBuilder#lightmap (and tex, though that's not relevant here) swap the U and V components
        // for vertex format elements with (u)short and (u)byte types. Be mindful of this if changing this code or the vertex
        // formats in use by this class.

        //#if MC>=11600
        //$$ this.light(light.blockLight.toInt(), light.skyLight.toInt())
        //#else
        this.light(light.skyLight.toInt(), light.blockLight.toInt())
        //#endif
    }

    override fun tex(u: Double, v: Double) = apply {
        if (passTexLight)
            vertexConsumer.tex(u, v)
    }

    override fun light(u: Int, v: Int) = apply {
        if (passTexLight)
            vertexConsumer.light(u, v)
    }

    override fun pos(stack: UMatrixStack, x: Double, y: Double, z: Double) = apply {
        vertexConsumer.pos(stack, x, y, z)
    }

    override fun norm(stack: UMatrixStack, x: Float, y: Float, z: Float) = apply {
        vertexConsumer.norm(stack, x, y, z)
    }

    override fun overlay(u: Int, v: Int) = apply {
        vertexConsumer.overlay(u, v)
    }

    override fun color(red: Int, green: Int, blue: Int, alpha: Int) = apply {
        vertexConsumer.color(red, green, blue, alpha)
    }

    override fun endVertex(): TextRenderTypeVertexConsumer = apply {
        vertexConsumer.endVertex()
    }
}
