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
import gg.essential.gui.common.modal.EssentialModal2
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.wrappedText
import gg.essential.gui.overlay.ModalFlow
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.util.pollingStateV2
import gg.essential.handlers.account.WebAccountManager

class AccountNotValidModal(
    modalManager: ModalManager,
    private val skipAuthCheck: Boolean = false,
    private val continuation: ModalFlow.ModalContinuation<Boolean?>
) : EssentialModal2(modalManager, false) {

    private val authStatus = pollingStateV2 { Essential.getInstance().connectionManager.isAuthenticated }
    private var unregisterEffect: (() -> Unit)? = null
    private var hasResumed = false

    override fun LayoutScope.layoutBody() {
        wrappedText("Something went wrong or your account is not authenticated with Essential. Log into your account on our website to securely add it in-game.", centered = true)
    }

    override fun LayoutScope.layoutButtons() {
        primaryAndCancelButtons(
            "Open Browser",
            "Cancel",
            primaryAction = { hasResumed = true; replaceWith(continuation.resume(true)) },
            cancelAction = { hasResumed = true; replaceWith(continuation.resumeImmediately(false)) }
        )
    }

    override fun onOpen() {
        super.onOpen()
        if (skipAuthCheck) return
        // Immediately move on if a connection is established and authenticated
        unregisterEffect = effect(this) {
            if (authStatus()) {
                hasResumed = true;
                replaceWith(continuation.resumeImmediately(null))
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

suspend fun ModalFlow.accountNotValidModal(): Boolean {
    while (!Essential.getInstance().connectionManager.isAuthenticated) {
        val accepted = awaitModal { continuation -> AccountNotValidModal(modalManager, false, continuation) }
        when (accepted) {
            true -> WebAccountManager.openInBrowser()
            false -> return false
            null -> return true
        }
    }
    return true
}

