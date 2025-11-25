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

import gg.essential.universal.UMatrixStack
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.image.GpuTexture
import java.awt.Color

interface GlFrameBuffer {
    val width: Int
    val height: Int

    val frameBuffer: Int
    val texture: GpuTexture
    val depthStencil: GpuTexture

    fun resize(width: Int, height: Int)
    fun delete()

    fun bind(): () -> Unit
    fun <T> use(block: () -> T): T
    fun useAsRenderTarget(block: (UMatrixStack, Int, Int) -> Unit)

    fun drawTexture(matrixStack: UMatrixStack, x: Double, y: Double, width: Double, height: Double, color: Color)

    fun clear(clearColor: Color = Color(0, 0, 0, 0), clearDepth: Double = 1.0, clearStencil: Int = 0)

    companion object {
        operator fun invoke(
            width: Int,
            height: Int,
            colorFormat: GpuTexture.Format = GpuTexture.Format.RGBA8,
            depthFormat: GpuTexture.Format = GpuTexture.Format.DEPTH24_STENCIL8,
        ): GlFrameBuffer = platform.newGlFrameBuffer(width, height, colorFormat, depthFormat)
    }
}
