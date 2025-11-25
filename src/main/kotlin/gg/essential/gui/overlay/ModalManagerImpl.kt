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
package gg.essential.gui.overlay

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percentOfWindow
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.utils.withAlpha
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.common.modal.defaultEssentialModalFadeTime
import gg.essential.gui.transitions.FadeInTransition
import gg.essential.util.Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.awt.Color

/**
 * Queues modals to be displayed onto a managed [EphemeralLayer].
 */
class ModalManagerImpl(
    private val overlayManager: OverlayManager,
    private val backgroundColor: Color = Color.BLACK.withAlpha(150),
) : ModalManager {
    override val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Client)

    private val modalQueue = mutableListOf<Modal>()

    /**
     * The [Layer] which [Modal]s will be displayed on.
     */
    private var layer: Layer? = null

    override var isCurrentlyFadingIn: Boolean = false
        private set(value) {
            if (field == value) return
            field = value
        }

    override fun modalClosed() {
        val nextModal = modalQueue.removeFirstOrNull()
        if (nextModal != null) {
            layer?.pushModal(nextModal)
        } else {
            // If we have no modals left to push, we should remove the layer.
            layer?.let { overlayManager.removeLayer(it) }
            layer = null
            isCurrentlyFadingIn = false
            coroutineScope.cancel()
        }
    }

    override fun queueModal(modal: Modal) {
        val currentLayer = this.layer

        // If the layer is null, this means that this is our first modal, or, that the previous layer
        // was cleaned up. We should create a new one for this modal to be pushed on to.
        if (currentLayer == null) {
            val layer = createAndSetupLayer()
            layer.pushModal(modal)
            return
        }

        // If the layer currently has a modal on it, the next one will be pushed
        // when [modalClose] is called by the current modal.
        modalQueue.add(modal)
    }

    private fun createAndSetupLayer(): Layer {
        return overlayManager.addLayer(LayerPriority.Modal).apply {
            UIBlock(backgroundColor).constrain {
                x = 0.pixels
                y = 0.pixels
                width = 100.percentOfWindow()
                height = 100.percentOfWindow()
            } childOf window

            isCurrentlyFadingIn = true
            FadeInTransition(defaultEssentialModalFadeTime).transition(window) {
                isCurrentlyFadingIn = false
            }

            this@ModalManagerImpl.layer = this
        }
    }

    private fun Layer.pushModal(modal: Modal) {
        val background = window.children.first()

        modal childOf background

        // Beware: This call may recursively call [queueModal] and/or [modalClosed]. As such, it should ideally be the
        // last thing which effectively happens in these methods.
        modal.onOpen()
    }
}