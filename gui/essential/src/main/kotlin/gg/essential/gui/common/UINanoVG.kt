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

import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.shader.BlendState
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.lwjgl3.api.nanovg.NanoVG

/**
 * Component which displays a frame buffer texture generated using NanoVG in [renderVG].
 *
 * This extends [UIFrameBuffer] and all its behavior applies here as well.
 */
abstract class UINanoVG : UIFrameBuffer() {
    private var nanoVG: NanoVG? = null

    override fun delete() {
        nanoVG?.delete()
        nanoVG = null

        super.delete()
    }

    override fun render(matrixStack: UMatrixStack, width: Float, height: Float) {
        val nanoVG = nanoVG ?: platform.lwjgl3.get<NanoVG>().also { nanoVG = it }
        nanoVG.beginFrame(width, height, 1.0f)
        renderVG(matrixStack, nanoVG, width, height)
        nanoVG.endFrame()

        platform.restoreMcStateAfterNanoVGDrawCall()

        // Restore standard gui rendering state
        @Suppress("DEPRECATION")
        BlendState.DISABLED.activate()
        @Suppress("DEPRECATION")
        UGraphics.disableDepth()
    }

    /**
     * Uses the given [NanoVG] instance to render into the frame buffer.
     *
     * All calls to this method are wrapped in [NanoVG.beginFrame]/[NanoVG.endFrame].
     *
     * The [matrixStack], [width] and [height] follow the specifications in [render].
     */
    protected abstract fun renderVG(matrixStack: UMatrixStack, vg: NanoVG, width: Float, height: Float)
}