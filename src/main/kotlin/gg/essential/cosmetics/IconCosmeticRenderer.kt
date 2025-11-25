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
package gg.essential.cosmetics

import gg.essential.config.EssentialConfig
import gg.essential.data.OnboardingData
import gg.essential.handlers.OnlineIndicator
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.model.ModelInstance
import gg.essential.model.backend.minecraft.MinecraftRenderBackend
import gg.essential.model.light.Light
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager
import gg.essential.render.TextRenderTypeVertexConsumer
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.shader.BlendState
import gg.essential.util.UDrawContext
import gg.essential.util.identifier
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.util.UUID

//#if MC>=12106
//$$ import net.minecraft.client.gl.RenderPipelines
//$$ import kotlin.math.roundToInt
//#endif

//#if MC>=11600
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer
//#endif

object IconCosmeticRenderer {

    // Icons are always a 7x7 image
    private const val SIZE = 7f

    private const val WHITE = 0xFFFFFFFFu

    // Constant from vanilla
    //#if MC>=12102
    //$$ private const val SNEAKING_COLOR = 0x80FFFFFFu
    //#else
    private const val SNEAKING_COLOR = 0x20FFFFFFu
    //#endif

    // 1/3 intensity on rgb channels
    private const val TAB_SHADOW_COLOR = 0xFF555555u

    private val WHITE_TEXTURE = identifier("essential", "textures/white.png")

    fun getIconCosmetic(equippedOutfitsManager: EquippedOutfitsManager, uuid: UUID): EquippedCosmetic? {
        return equippedOutfitsManager.getVisibleCosmeticsState(uuid).getUntracked()[CosmeticSlot.ICON]
    }

    fun getIconCosmetic(networkPlayerInfo: NetworkPlayerInfo?): EquippedCosmetic? {
        if (!OnboardingData.hasAcceptedTos() || !EssentialConfig.showEssentialIndicatorOnTab || networkPlayerInfo == null) {
            return null
        }

        val gameProfile = networkPlayerInfo.gameProfile
        var uuid = gameProfile.id ?: return null

        if (uuid.version() == 2) {
            val actualUuid = OnlineIndicator.findUUIDFromDisplayName(networkPlayerInfo.displayName)
            if (actualUuid != null) {
                uuid = actualUuid
            }
        }

        return getIconCosmetic(networkPlayerInfo.equippedOutfitsManager, uuid)
    }

    fun getNameplateXOffset(entity: Entity?): Float {
        return getNameplateXOffset(CosmeticsRenderState.Live(entity as? AbstractClientPlayer ?: return 0f))
    }

    fun getNameplateXOffset(cState: CosmeticsRenderState): Float {
        if (cState.nametagIcon() != null) {
            return 10f / 2
        }
        return 0f
    }

    private fun drawVersionConsistentNameplatePadding(
        matrixStack: UMatrixStack,
        //#if MC>=11600
        //$$ buffer: IRenderTypeBuffer,
        //#else
        buffer: UGraphics,
        //#endif
        light: Light,
        vanillaX: Float,
        stringWidth: Int,
        @Suppress("UNUSED_PARAMETER") alwaysOnTop: Boolean, // only unused for MC<11600
        extraLeftSidePadding: Int,
    ){
        // Desired nameplate padding, for all versions:
        // - 2px left from text             (handled here, is always 1px in vanilla)
        // - 2px right from text            (handled here in 1.21.2+, is 1px in those versions) (also requires handling here for <=1.12.2 when stringWidth is odd)
        // - 1px up from text               (matches vanilla)
        // - 2px down from text baseline    (handled here in <=1.12.2, is 1px in those versions) (also handled in icon rendering)
        // - 2px left from icon             (handled in icon rendering)

        val backgroundOpacity = OnlineIndicator.getTextBackgroundOpacity()

        //#if MC>=12102
        //$$ val z = -0.01
        //#elseif MC>=11600
        //$$ val z = 0.01
        //#else
        val z = 0.0
        //#endif

        TextRenderTypeVertexConsumer.create(
            buffer,
            //#if MC>=11600
            //$$ alwaysOnTop
            //#endif
        ).run {
            fun drawPadding(x1: Double, x2: Double, y1: Double, y2: Double) {
                pos(matrixStack, x1, y1, z).color(0, 0, 0, backgroundOpacity).tex(0.0, 0.0).light(light).endVertex()
                pos(matrixStack, x1, y2, z).color(0, 0, 0, backgroundOpacity).tex(0.0, 0.0).light(light).endVertex()
                pos(matrixStack, x2, y2, z).color(0, 0, 0, backgroundOpacity).tex(0.0, 0.0).light(light).endVertex()
                pos(matrixStack, x2, y1, z).color(0, 0, 0, backgroundOpacity).tex(0.0, 0.0).light(light).endVertex()
            }

            val vanillaTop = -1.0

            //#if MC>=11600
            //$$ val vanillaBottom = 9.0
            //#else
            val vanillaBottom = 8.0
            //#endif

            val vanillaLeft = (vanillaX - 1).toDouble()
            // draw an extra pixel on the left side to match the padding of the right side
            // additionally apply extraLeftSidePadding
            val paddedLeft = vanillaLeft - extraLeftSidePadding - 1
            drawPadding(paddedLeft, vanillaLeft, vanillaTop, vanillaBottom)

            //#if MC>=12102
            //$$ // draw an extra pixel on the right side to replicate the padding of nameplates pre-1.21.2
            //$$ val vanillaRight = (vanillaX + stringWidth).toDouble()
            //$$ val paddedRight = vanillaRight + 1
            //$$ drawPadding(vanillaRight, paddedRight, vanillaTop, vanillaBottom)
            //#else
            val paddedRight = (vanillaX + stringWidth + 1).toDouble()
            //#if MC<11300
            if (stringWidth % 2 == 1) {
                // vanilla 1.12.2 & 1.8.9 uses vanillaX inverted for their right side bound of the background, thus
                // losing 1 width for odd stringWidth values, which we account for here
                drawPadding(paddedRight - 1, paddedRight, vanillaTop, vanillaBottom)
                // 1.16 moves the background drawing within the fontRenderer which resolves this inconsistency
            }
            //#endif
            //#endif

            //#if MC<=11202
            // bottom padding, using the left/right side padding values above
            drawPadding(paddedLeft, paddedRight, vanillaBottom, vanillaBottom + 1)
            //#endif

            //#if MC<11600
            buffer.drawDirect()
            //#endif
        }
    }

    fun drawStandaloneVersionConsistentPadding(
        matrixStack: UMatrixStack,
        //#if MC>=11600
        //$$ buffer: IRenderTypeBuffer,
        //#endif
        isSneaking: Boolean,
        str: String,
        light: Int
    ) {
        // FIXME kotlin mangles the function name if an inline class is used (despite the function resolving fine in intellij)
        val light = Light(light.toUInt())

        val alwaysOnTop = !isSneaking

        val stringWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(str)
        //#if MC>=12102
        //$$ val vanillaX = (-stringWidth).toFloat() / 2f
        //#else
        val vanillaX = -(stringWidth / 2).toFloat()
        //#endif

        //#if MC<11600
        UGraphics.enableAlpha()
        UGraphics.disableLighting()
        @Suppress("DEPRECATION")
        UGraphics.depthMask(false)
        //#endif

        //#if MC<11600
        if (alwaysOnTop) {
            @Suppress("DEPRECATION")
            UGraphics.disableDepth()
        }
        //#endif

        //#if MC<12105
        @Suppress("DEPRECATION")
        BlendState.NORMAL.activate()
        //#endif

        //#if MC<11600
        val buffer = UGraphics.getFromTessellator()
        //#endif

        drawVersionConsistentNameplatePadding(matrixStack, buffer, light, vanillaX, stringWidth, alwaysOnTop, 0)

        //#if MC<=11202
        if (alwaysOnTop) {
            @Suppress("DEPRECATION")
            UGraphics.enableDepth()
        }
        @Suppress("DEPRECATION")
        UGraphics.depthMask(true)
        //#endif

        //#if MC<11600
        UGraphics.enableLighting()
        //#endif
    }


    fun drawNameTagIconAndVersionConsistentPadding(
        matrixStack: UMatrixStack,
        //#if MC>=11600
        //$$ buffer: IRenderTypeBuffer,
        //#endif
        cState: CosmeticsRenderState,
        str: String,
        light: Int
    ) {
        // FIXME kotlin mangles the function name if an inline class is used (despite the function resolving fine in intellij)
        val light = Light(light.toUInt())

        val alwaysOnTop = !cState.isSneaking()

        val stringWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(str)
        //#if MC>=12102
        //$$ val vanillaX = (-stringWidth).toFloat() / 2f
        //#else
        val vanillaX = -(stringWidth / 2).toFloat()
        //#endif

        //#if MC<11600
        UGraphics.enableAlpha()
        UGraphics.disableLighting()
        @Suppress("DEPRECATION")
        UGraphics.depthMask(false)
        //#endif

        //#if MC<11600
        if (alwaysOnTop) {
            @Suppress("DEPRECATION")
            UGraphics.disableDepth()
        }
        //#endif

        //#if MC<12105
        @Suppress("DEPRECATION")
        BlendState.NORMAL.activate()
        //#endif

        //#if MC<11600
        val buffer = UGraphics.getFromTessellator()
        //#endif

        val model: ModelInstance? = cState.nametagIcon()

        // draw version consisted nameplate padding independently of whether there is an icon to render
        drawVersionConsistentNameplatePadding(matrixStack, buffer, light, vanillaX, stringWidth, alwaysOnTop,
            // if there is an icon cosmetic though, we need to add 9px padding to the left side of the nameplate
            if (model == null) 0 else 9)

        // draw the icon cosmetic itself if it exists
        if (model != null) {
            val centreY = 3.5f
            val centreX = vanillaX - 5.5f

            val texture = (model.model.texture as MinecraftRenderBackend.MinecraftTexture).identifier

            TextRenderTypeVertexConsumer.createWithTexture(
                buffer, texture,
                //#if MC>=11600
                //$$ alwaysOnTop
                //#endif
            ).run {
                drawIcon(matrixStack, this, SIZE, centreX, centreY, 0f, SNEAKING_COLOR, light)

                //#if MC<11600
                buffer.drawDirect()
                //#endif
            }

            //#if MC<=11202
            if (alwaysOnTop) {
                @Suppress("DEPRECATION")
                UGraphics.enableDepth()
            }
            @Suppress("DEPRECATION")
            UGraphics.depthMask(true)
            //#endif

            if (alwaysOnTop) {
                val vertexConsumer = TextRenderTypeVertexConsumer.createWithTexture(buffer, texture)
                drawIcon(matrixStack, vertexConsumer, SIZE, centreX, centreY, 0f, WHITE,
                    //#if MC>=12102
                    //$$ light.withMinimumLight(2) // same behaviour as the non-sneaking nameplate text in 1.21.2+
                    //#else
                    light
                    //#endif
                )

                //#if MC<11600
                buffer.drawDirect()
                //#endif
            }
        } else {
            //#if MC<=11202
            if (alwaysOnTop) {
                @Suppress("DEPRECATION")
                UGraphics.enableDepth()
            }
            @Suppress("DEPRECATION")
            UGraphics.depthMask(true)
            //#endif
        }

        //#if MC<11600
        UGraphics.enableLighting()
        //#endif
    }

    fun drawTextureInTabList(
        drawContext: UDrawContext,
        x: Float,
        y: Float,
        texture: ResourceLocation,
        size: Float,
        shadow: Boolean
    ) {
        OnlineIndicator.beforeTabDraw()

        //#if MC<12105
        val prevBlendState = BlendState.active()
        @Suppress("DEPRECATION")
        BlendState.NORMAL.activate()
        //#endif

        //#if MC<11600
        // On 1.12.2 and below, the lightmap doesn't have any area guaranteed to be maximum brightness. So, we bind our
        // own texture which is solid white in order to ensure the icon cosmetics rendered in tab aren't affected by
        // lighting conditions. We save the previous lightmap texture id in order to restore it.
        var prevLightmap = 0
        UGraphics.configureTextureUnit(1) {
            prevLightmap = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
        }

        UGraphics.bindTexture(1, WHITE_TEXTURE)
        //#endif

        if (shadow) {
            drawIcon(drawContext, texture, size, x + 1, y + 1, 5f, TAB_SHADOW_COLOR)
        }
        drawIcon(drawContext, texture, size, x, y, 10f, WHITE)

        //#if MC<12105
        @Suppress("DEPRECATION")
        prevBlendState.activate()
        //#endif

        //#if MC<11600
        UGraphics.bindTexture(1, prevLightmap)
        //#endif

        OnlineIndicator.afterTabDraw()
    }

    private fun drawIcon(
        drawContext: UDrawContext,
        texture: ResourceLocation,
        size: Float,
        centreX: Float,
        centreY: Float,
        zOffset: Float,
        color: UInt,
    ) {
        //#if MC>=12106
        //$$ // [OnlineIndicator.drawTabIndicator] uses sub-MC pixel sizing down to eights, so we need to scale up
        //$$ // the coordinates we pass while scaling down the matrix stack, since `drawTexture` only takes `Int`s
        //$$ val scale = 8f
        //$$ drawContext.mc.matrices.pushMatrix()
        //$$ drawContext.mc.matrices.scale(1 / scale, 1 / scale)
        //$$ drawContext.mc.drawTexture(
        //$$     RenderPipelines.GUI_TEXTURED,
        //$$     texture,
        //$$     /* x */ ((centreX - size / 2) * scale).roundToInt(),
        //$$     /* y */ ((centreY - size / 2) * scale).roundToInt(),
        //$$     /* u */ 0f, /* v */ 0f,
        //$$     /* width */ (size * scale).roundToInt(),
        //$$     /* height */ (size * scale).roundToInt(),
        //$$     /* uWidth */ 1, /* vHeight */ 1,
        //$$     /* textureWidth */ 1, /* textureHeight */ 1,
        //$$     color.toInt(),
        //$$ )
        //$$ drawContext.mc.matrices.popMatrix()
        //#else
        //#if MC>=12102
        //$$ drawContext.mc.draw { buffer ->
        //#elseif MC>=12000
        //$$ val buffer = drawContext.mc.vertexConsumers
        //#elseif MC>=11600
        //$$ val buffer = Minecraft.getInstance().renderTypeBuffers.bufferSource
        //#else
        val buffer = UGraphics.getFromTessellator()
        //#endif
        val vertexConsumer = TextRenderTypeVertexConsumer.createWithTexture(buffer, texture)

        drawIcon(drawContext.matrixStack, vertexConsumer, size, centreX, centreY, zOffset, color, Light.MAX_VALUE)

        //#if MC>=12102
        //$$ }
        //#elseif MC>=11600
        //$$ buffer.finish()
        //#else
        buffer.drawDirect()
        //#endif
        //#endif
    }

    private fun drawIcon(
        matrixStack: UMatrixStack,
        vertexConsumer: TextRenderTypeVertexConsumer,
        size: Float,
        centreX: Float,
        centreY: Float,
        zOffset: Float,
        color: UInt,
        light: Light
    ) {
        val left = centreX - size / 2
        val top = centreY - size / 2
        val z = zOffset.toDouble()

        val alpha = color.toInt() shr 24 and 255
        val red = color.toInt() shr 16 and 255
        val green = color.toInt() shr 8 and 255
        val blue = color.toInt() and 255

        vertexConsumer.pos(matrixStack, left.toDouble(), top.toDouble(), z).color(red, green, blue, alpha).tex(0.0, 0.0).light(light).endVertex()
        vertexConsumer.pos(matrixStack, left.toDouble(), (top + size).toDouble(), z).color(red, green, blue, alpha).tex(0.0, 1.0).light(light).endVertex()
        vertexConsumer.pos(matrixStack, (left + size).toDouble(), (top + size).toDouble(), z).color(red, green, blue, alpha).tex(1.0, 1.0).light(light).endVertex()
        vertexConsumer.pos(matrixStack, (left + size).toDouble(), top.toDouble(), z).color(red, green, blue, alpha).tex(1.0, 0.0).light(light).endVertex()
    }

}