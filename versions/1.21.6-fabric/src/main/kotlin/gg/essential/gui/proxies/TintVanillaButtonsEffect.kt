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

import gg.essential.Essential
import gg.essential.event.gui.GuiDrawScreenEvent
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution
import gg.essential.universal.vertex.UBufferBuilder
import gg.essential.util.UDrawContext
import java.awt.Color
import net.minecraft.client.gui.render.state.GuiRenderState
import gg.essential.universal.UMinecraft
import gg.essential.util.AdvancedDrawContext
import gg.essential.util.renderGuiRenderStateToTexture
import me.kbrewster.eventbus.Subscribe
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.AbstractTexture
import net.minecraft.util.Identifier
import gg.essential.gui.proxies.TintVanillaButtonsEffectShared.*
import gg.essential.gui.proxies.TintVanillaButtonsEffectShared.Companion.NON_WHITE_TINT_PIPELINE
import gg.essential.gui.proxies.TintVanillaButtonsEffectShared.Companion.averageButtonColor

/**
 * 1.21.6+ implementation of tinting our vanilla proxy buttons.
 *
 * This class uses [Page]s to collect all the vanilla button draws onto one texture, that can be tinted later in a single
 * pass before it is used by vanilla.
 * This texture is also enqueued to the vanilla context, before it is tinted, so that it is rendered in the correct order
 * by the vanilla gui rendering that happens later on.
 */
class TintVanillaButtonsEffect {

    fun drawTinted(context: UDrawContext, x1: Int, y1: Int, x2: Int, y2: Int, color: Color, draw: (UDrawContext) -> Unit) {
        val areaToBeTinted = AreaToBeTinted(x1, y1, x2, y2)
        if (areaToBeTinted.invalid) return

        Page.drawToPage(context, areaToBeTinted, color, draw)
    }
    /**
     * Represents a page of vanilla button draws to be tinted later.
     * Each page collects all the draws in non overlapping spaces of the context's screen space, and then later draws
     * them all to a texture that can be tinted in one pass.
     *
     * We do it this way as the setup for renderGuiRenderStateToTexture() is quite expensive, so we want to avoid multiple calls to it.
     *
     * The page texture is handled like a sprite sheet, with logic to fit the draws into the first available space, with
     * additional pages created as space is needed, which is handled automatically by the initial page.
     */
    private class Page(
        private val pageWidth: Int = UResolution.viewportWidth,
        private val pageHeight: Int = UResolution.viewportHeight,
    ) {
        val guiRenderState = GuiRenderState()
        val drawContext = DrawContext(UMinecraft.getMinecraft(), guiRenderState)
        private val texture = AdvancedDrawContext.textureAllocator.allocate(pageWidth, pageHeight)

        // List of free spaces in this page, initialized with the full page size
        private val freeSpaces = mutableListOf(Rect(0, 0, pageWidth, pageHeight))

        // List of rectangles that have been placed/allocated on the page, will be reused by the shader pass later
        private val placedRects = mutableListOf<PlacedRect>()

        // The next page to be created if this one runs out of space
        private var nextPage: Page? = null

        /**
         * Tints the page by rendering our DrawContext/GuiRenderState to a texture and then drawing that texture with a shader
         */
        fun finalizeAndTintPage() {
            assert(placedRects.isNotEmpty())

            val drawnPageTexture = renderGuiRenderStateToTexture(guiRenderState) // the expensive part, only called once per page

            val randomRect = placedRects.first()
            val averageCol = averageButtonColor(drawnPageTexture, randomRect.x, randomRect.y, randomRect.width, randomRect.height)

            if (averageCol == null) {
                drawnPageTexture.delete()
                return
            }

            AdvancedDrawContext.drawToTexture(texture) { stack ->
                UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE_COLOR).also { buffer ->
                    for (rectGL in placedRects) {
                        // parse the rectangle to OpenGL UV coordinates
                        val u = rectGL.x / pageWidth.toDouble()
                        val v = (pageHeight - (rectGL.y + rectGL.height)) / pageHeight.toDouble()
                        val u2 = (rectGL.x + rectGL.width) / pageWidth.toDouble()
                        val v2 = (pageHeight - rectGL.y) / pageHeight.toDouble()

                        // convert to MC coordinates scaling required by the AdvancedDrawContext projection matrix
                        val rect = rectGL.toMC()
                        val x = rect.x.toDouble()
                        val y = rect.y.toDouble()
                        val x2 = (rect.x + rect.width).toDouble()
                        val y2 = (rect.y + rect.height).toDouble()

                        // add the quad to the buffer with the tint color
                        buffer.pos(stack, x, y2, 0.0).tex(u, v).color(rect.color).endVertex()
                        buffer.pos(stack, x2, y2, 0.0).tex(u2, v).color(rect.color).endVertex()
                        buffer.pos(stack, x2, y, 0.0).tex(u2, v2).color(rect.color).endVertex()
                        buffer.pos(stack, x, y, 0.0).tex(u, v2).color(rect.color).endVertex()
                    }
                }.build()?.drawAndClose(NON_WHITE_TINT_PIPELINE) {
                    texture("u_Button", drawnPageTexture.glId)
                    uniform("u_AverageColor", averageCol.red / 255F, averageCol.green / 255F, averageCol.blue / 255F)
                }
            }

            drawnPageTexture.delete()

            nextPage?.finalizeAndTintPage()
        }

        /**
         * Draws the given draw function to the page's drawContext, placing it in the first available non-overlapping space.
         * If no space is available, it will try to draw on the next page.
         *
         * Then enqueues the page's texture to the vanilla context so it is rendered in the correct order by the vanilla gui rendering that happens later.
         */
        fun drawVanillaToPage(vanillaContext: UDrawContext, vanillaDrawArea: AreaToBeTinted, color: Color, draw: (UDrawContext) -> Unit) {
            val placedRect = tryPlaceRect(vanillaDrawArea.width, vanillaDrawArea.height, color)
            if (placedRect == null) {
                // no space, try next page
                val next = nextPage ?: Page(pageWidth, pageHeight).also { nextPage = it }

                next.drawVanillaToPage(vanillaContext, vanillaDrawArea, color, draw)
                return
            }
            // we have space in this page so draw this to our context

            val placedMC = placedRect.toMC()

            drawContext.matrices.pushMatrix()
            // scissor the area to be drawn to the placed rectangle, this is especially necessary if the original draw
            // was done partially out of screen bounds as the draw() block may try to draw outside this rectangle
            drawContext.enableScissor(placedMC.x, placedMC.y, placedMC.x + placedMC.width, placedMC.y + placedMC.height)
            // offset the matrices by the difference between the vanilla draw position and the placed rect position so the draw()
            // block will be made to draw in the placed rect position
            drawContext.matrices.translate(
                (placedMC.x - vanillaDrawArea.xMC).toFloat(),
                (placedMC.y - vanillaDrawArea.yMC).toFloat()
            )
            // draw to our context
            draw(drawContext.let { UDrawContext(it, UMatrixStack(it.matrices)) })
            drawContext.disableScissor()
            drawContext.matrices.popMatrix()

            // now enqueue the draw to the vanilla context

            // pass in the textureView to the drawTexture() call via a temporary texture registration
            val textureManager = MinecraftClient.getInstance().textureManager
            val identifier = Identifier.of("essential", "__tmp_texture__tint_vanilla_buttons_effect")
            textureManager.registerTexture(identifier, object : AbstractTexture() {
                init { glTextureView = this@Page.texture.textureView }
                override fun close() {} // we don't want the later `destroyTexture` to close our texture
            })

            // draw the texture to the vanilla context, which won't actually read the referenced texture until later
            vanillaContext.mc.drawTexture(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                identifier,
                // x, y
                vanillaDrawArea.xMC.toInt(), vanillaDrawArea.yMC.toInt(),
                // u, v
                placedRect.x.toFloat(), -placedRect.y.toFloat(),
                // width, height
                vanillaDrawArea.widthMC.toInt(), vanillaDrawArea.heightMC.toInt(),
                // uWidth, vHeight
                placedRect.width, -placedRect.height,
                // textureWidth, textureHeight
                pageWidth, pageHeight
            )
            textureManager.destroyTexture(identifier)
        }

        /**
         * Attempts to place a rectangle of the given width and height in the free spaces.
         * Returns the placed rectangle if successful, or null if no space is available.
         */
        private fun tryPlaceRect(width: Int, height: Int, color: Color): PlacedRect? {
            var bestRect: Rect? = null

            for (free in freeSpaces) {
                if (width <= free.width && height <= free.height) {
                    if (bestRect == null || free.area < bestRect.area) {
                        bestRect = free
                    }
                }
            }

            bestRect?.let {
                val placed = PlacedRect(it.x, it.y, width, height, color)
                placedRects.add(placed)
                splitFreeSpaceRect(it, width, height)
                return placed
            }

            return null // no space
        }

        /**
         * Splits the given free space rectangle into smaller rectangles after placing a used rectangle.
         * Removes the original free space and adds the remaining spaces back to the list.
         */
        private fun splitFreeSpaceRect(free: Rect, usedW: Int, usedH: Int) {
            freeSpaces.remove(free)

            val spaceRemainingRight = free.width > usedW
            val spaceRemainingBottom = free.height > usedH

            if (spaceRemainingRight) {
                freeSpaces.add(Rect(free.x + usedW, free.y, free.width - usedW, usedH))
            }
            if (spaceRemainingBottom) {
                freeSpaces.add(Rect(free.x, free.y + usedH, free.width, free.height - usedH))
            }
            if (spaceRemainingRight && spaceRemainingBottom) {
                freeSpaces.add(Rect(free.x + usedW, free.y + usedH, free.width - usedW, free.height - usedH))
            }
        }

        private data class Rect(val x: Int, val y: Int, val width: Int, val height: Int) {
            val area = width * height
        }
        data class PlacedRect(val x: Int, val y: Int, val width: Int, val height: Int, val color: Color) {
            fun toMC(): PlacedRect {
                val scale = UResolution.scaleFactor.toInt()
                return copy(
                    x = x / scale,
                    y = y / scale,
                    width = width / scale,
                    height = height / scale
                )
            }
        }

        companion object {

            private var rootPage: Page? = null
                set(value) {
                    // register or unregister the event listener based on whether we are transitioning from null to not-null and vice versa
                    if (field == null && value != null) Essential.EVENT_BUS.register(this)
                    if (field != null && value == null) Essential.EVENT_BUS.unregister(this)

                    field = value
                }

            @Subscribe
            fun runTint(event: GuiDrawScreenEvent) {
                if (event.isPre) return

                rootPage?.finalizeAndTintPage()
                rootPage = null
            }

            fun drawToPage(vanillaContext: UDrawContext, area: AreaToBeTinted, color: Color, draw: (UDrawContext) -> Unit) {
                val page = rootPage ?: Page().also { rootPage = it }
                page.drawVanillaToPage(vanillaContext, area, color, draw)
            }
        }
    }
}