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
package gg.essential.gui.common.modal

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.dsl.*
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.layoutAsBox
import gg.essential.gui.overlay.ModalManager
import gg.essential.network.connectionmanager.telemetry.FeatureSessionTelemetry
import gg.essential.universal.UKeyboard
import gg.essential.util.Client
import gg.essential.vigilance.utils.onLeftClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.slf4j.LoggerFactory

abstract class Modal(val modalManager: ModalManager) : UIContainer() {

    /**
     * A coroutine scope which lives until this modal is closed.
     * This coroutine scope is created in [onOpen], and cannot be used before then (i.e. in a constructor).
     */
    protected lateinit var coroutineScope: CoroutineScope

    private var windowListListener: (UIComponent.(Char, Int) -> Unit)? = null
    private val escapeListener: UIComponent.(Char, Int) -> Unit = { _, keyCode ->
        if (keyCode == UKeyboard.KEY_ESCAPE) {
            // onClose (indirectly called by `handleEscapeKeyPress`) updates the Window's key listeners.
            // To avoid a concurrent modification exception, we wait until next frame
            Window.enqueueRenderOperation {
                handleEscapeKeyPress()
            }
        }
    }

    open val modalName: String? = this.javaClass.name

    init {
        constrain {
            x = 0.percentOfWindow()
            y = 0.percentOfWindow()
            width = 100.percentOfWindow()
            height = 100.percentOfWindow()
        }

        onLeftClick { event ->
            if (!modalManager.isCurrentlyFadingIn && event.target == this) {
                handleEscapeKeyPress()
            }
        }
    }

    abstract fun LayoutScope.layoutModal()

    override fun afterInitialization() {
        super.afterInitialization()
        this.layoutAsBox { layoutModal() }
    }

    open fun replaceWith(modal: Modal? = null) {
        if (this !in parent.children) {
            return
        }

        modal?.let { modalManager.queueModal(it) }
        close()
    }

    open fun close() {
        if (this !in parent.children) {
            return
        }

        hide(instantly = true)
        onClose()

        modalManager.modalClosed()
        coroutineScope.cancel()
    }

    open fun onOpen() {

        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Client)

        windowListListener = Window.of(this).keyTypedListeners.removeFirstOrNull()
        Window.of(this).onKeyType(escapeListener)

        modalName?.let { FeatureSessionTelemetry.startEvent(it) }
    }

    open fun onClose() {

        val windowListListener = windowListListener
        val window = Window.of(this)
        if (windowListListener != null) {
            window.keyTypedListeners.add(0, windowListListener)
        }

        window.keyTypedListeners.remove(escapeListener) // Clean ourselves up

        modalName?.let { FeatureSessionTelemetry.endEvent(it) }
    }

    open fun handleEscapeKeyPress() {
        close()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(Modal::class.java)
    }
}
