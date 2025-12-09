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
package gg.essential.gui.modals

import gg.essential.Essential
import gg.essential.gui.common.OutlineButtonStyle
import gg.essential.gui.common.modal.EssentialModal2
import gg.essential.gui.common.textStyle
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.await
import gg.essential.gui.elementa.state.v2.combinators.letState
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.tag
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.layoutdsl.wrappedText
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.overlay.ModalFlow
import gg.essential.gui.overlay.ModalManager
import gg.essential.network.connectionmanager.ConnectionManagerStatus
import gg.essential.gui.util.pollingStateV2

class NotAuthenticatedModal(
    modalManager: ModalManager,
    private val skipAuthCheck: Boolean = false,
    private val triedConnecting: Boolean = false,
    private val continuation: ModalFlow.ModalContinuation<Boolean>
) : EssentialModal2(modalManager, false) {

    private val connectionManager = Essential.getInstance().connectionManager
    private val connecting = connectionManager.connectionStatus.letState { it == null }
    private val authenticated = pollingStateV2 { connectionManager.isAuthenticated }
    private val status = State {
        authenticated() && connectionManager.suspensionManager.isLoaded() && connectionManager.rulesManager.isLoaded()
    }
    private val buttonText = State {
        if (connecting()) "Connecting..." else if (triedConnecting) "Retry" else "Connect"
    }
    private var unregisterEffect: (() -> Unit)? = null
    private var hasResumed = false

    override fun LayoutScope.layoutBody() {
        wrappedText("You are not connected to the Essential Network. This is required to continue.", centered = true)
    }

    override fun LayoutScope.layoutButtons() {
        row(Arrangement.spacedBy(8f)) {
            cancelButton("Cancel") {
                hasResumed = true
                replaceWith(continuation.resumeImmediately(false))
            }
            outlineButton(
                Modifier
                    .width(91f)
                    .shadow()
                    .tag(PrimaryAction),
                style = { OutlineButtonStyle.BLUE },
                disabled = connecting,
                action = {
                    hasResumed = true
                    replaceWith(continuation.resume(true))
                },
            ) { currentStyle ->
                text(buttonText, Modifier.textStyle(currentStyle))
            }
        }
    }

    override fun onOpen() {
        super.onOpen()
        if (skipAuthCheck) return
        // Immediately move on if a connection is established and authenticated
        unregisterEffect = effect(this) {
            if (status()) {
                hasResumed = true
                // TODO: Use tri-state to properly handle early return states
                replaceWith(continuation.resumeImmediately(true))
            }
        }
    }

    override fun onClose() {
        unregisterEffect?.invoke()
        if (!hasResumed) {
            modalManager.queueModal(continuation.resumeImmediately(false))
        }
    }
}

suspend fun ModalFlow.notAuthenticatedModal(): Boolean {
    val connectionManager = Essential.getInstance().connectionManager
    var triedConnecting = false
    while (awaitModal { NotAuthenticatedModal(modalManager, skipAuthCheck = false, triedConnecting, it) }) {
        connectionManager.forceImmediateReconnect()
        when (connectionManager.connectionStatus.await { it != null }) {
            ConnectionManagerStatus.Success -> return true
            is ConnectionManagerStatus.Error.AuthenticationFailure -> return accountNotValidModal()

            else -> {
                Notifications.error("Connection Error", "")
                triedConnecting = true
            }

        }

    }
    return false
}

