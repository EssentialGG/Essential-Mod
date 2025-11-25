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
package gg.essential.gui

import gg.essential.Essential
import gg.essential.api.EssentialAPI
import gg.essential.api.gui.EssentialGUI
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.components.UIBlock
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.onChange
import gg.essential.network.connectionmanager.telemetry.FeatureSessionTelemetry
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.shader.BlendState
import gg.essential.universal.vertex.UBufferBuilder
import java.awt.Color

abstract class InternalEssentialGUI(
    version: ElementaVersion,
    guiTitle: String,
    newGuiScale: Int = EssentialAPI.getGuiUtil().getGuiScale(),
    restorePreviousGuiOnClose: Boolean = true,
    discordActivityDescription: String? = null,
): EssentialGUI(version, guiTitle, newGuiScale, restorePreviousGuiOnClose, discordActivityDescription) {
    private val reference = ReferenceHolderImpl()
    private val screenOpenMutable = mutableStateOf(false)
    protected val screenOpen: State<Boolean> = screenOpenMutable

    private var openedAt: Long? = null

    init {
        screenOpen.onChange(reference) { open ->
            if (open) {
                FeatureSessionTelemetry.startEvent(this@InternalEssentialGUI.javaClass.name)
            } else {
                FeatureSessionTelemetry.endEvent(this@InternalEssentialGUI.javaClass.name)
            }
        }
    }

    override fun initScreen(width: Int, height: Int) {
        super.initScreen(width, height)

        // Note: initScreen also gets called on resize, but since the state will already be `true`, this is fine
        screenOpenMutable.set(true)
    }

    override fun onDrawScreen(matrixStack: UMatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.onDrawScreen(matrixStack, mouseX, mouseY, partialTicks)

        // Workaround for Elementa (or more generally `BlendState.NORMAL`) producing incorrect alpha output
        // prior to ElementaVersion.V10.
        // Upgrading past ElementaVersion.V8 is non-trivial, so we'll use this workaround until we've got all screens
        // upgraded. This assumes that the drawn content is supposed to be fully opaque, which is indeed the case for
        // all of our screens (except for `EmoteWheel`, which doesn't use this class).
        val buffer = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR)
        UIBlock.drawBlock(buffer, matrixStack, Color.BLACK, 0.0, 0.0, window.getWidth().toDouble(), window.getHeight().toDouble())
        buffer.build()?.drawAndClose(TRANSPARENCY_WORKAROUND_PIPELINE)

        if (openedAt == null) {
            openedAt = System.currentTimeMillis()
        }
    }

    override fun onScreenClose() {
        super.onScreenClose()

        sendSessionTelemetry()
        screenOpenMutable.set(false)
    }

    private fun sendSessionTelemetry() {
        val duration = openedAt?.let { System.currentTimeMillis() - it } ?: return
        openedAt = null

        Essential.getInstance().connectionManager.telemetryManager.enqueue(
            ClientTelemetryPacket(
                "GUI_SESSION_DURATION",
                mapOf("gui" to this.javaClass.name, "durationSeconds" to duration / 1000)
            )
        )
    }

    companion object {
        private val TRANSPARENCY_WORKAROUND_PIPELINE = URenderPipeline.builderWithDefaultShader(
            "essential:workaround_broken_transparency",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_COLOR,
        ).apply {
            // Overwrites the framebuffer (dst) alpha, which is incorrect, with the drawn (src) alpha, which is always
            // 1, while keeping framebuffer color untouched.
            blendState = BlendState(
                equation = BlendState.Equation.ADD,
                srcRgb = BlendState.Param.ZERO,
                dstRgb = BlendState.Param.ONE,
                srcAlpha = BlendState.Param.ONE,
                dstAlpha = BlendState.Param.ZERO,
            )
        }.build()
    }
}