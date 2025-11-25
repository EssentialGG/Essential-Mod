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

import gg.essential.gui.common.modal.Modal
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.overlay.ModalFlow.ModalContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

/**
 * Launches a coroutine which manages a UI flow expressed via a dynamic sequence of modals.
 *
 * Each modal in the sequence should be displayed by calling [ModalFlow.awaitModal], which will similarly to
 * [suspendCancellableCoroutine] suspend the calling coroutine until the modal calls [ModalContinuation.resume] with
 * a result, which is then returned from the [ModalFlow.awaitModal] call.
 * The [ModalContinuation.resume] call itself is also suspending and will only return once a new modal has been queued
 * via [ModalFlow.awaitModal], which it then returns. This allows the previous modal to have control over what is
 * displayed while the main ModalFlow coroutine is preparing the next modal, e.g. it can continue to be visible but
 * have the button disabled until the next modal is ready.
 *
 * Calling [ModalFlow.awaitModal] will therefore automatically prompt the previous modal to replace itself with the new
 * modal and no manual tracking of the previous modal is necessary.
 *
 * When the coroutine ends, the previous modal is prompted to replace itself with `null`.
 *
 * @sample launchModalFlowExample
 */
fun launchModalFlow(modalManager: ModalManager, block: suspend ModalFlow.() -> Unit) {
    modalManager.queueModal(object : Modal(modalManager) {
        override fun onOpen() {
            super.onOpen()

            coroutineScope.launch {
                try {
                    replaceWith(createModalFlow(modalManager, block))
                } finally {
                    close()
                }
            }
        }

        override val modalName: String?
            get() = null

        override fun LayoutScope.layoutModal() {}
        override fun handleEscapeKeyPress() {}
    })
}

/**
 * Same as [launchModalFlow] but instead of automatically queuing the first modal, it is simply returned and the caller
 * is responsible for queuing it.
 */
suspend fun createModalFlow(modalManager: ModalManager, block: suspend ModalFlow.() -> Unit): Modal? {
    val modalFlow = ModalFlow(modalManager)
    val firstModal = CompletableDeferred<Modal?>(coroutineContext.job)
    modalFlow.replacePreviousModalWith = firstModal
    modalManager.coroutineScope.launch {
        try {
            block(modalFlow)
        } finally {
            modalFlow.replacePreviousModalWith.complete(null)
        }
    }
    return firstModal.await()
}

/**
 * Like [launchModalFlow], but instead of launching a new coroutine, this runs the flow directly on this coroutine.
 * It will suspend until the flow is complete and return its result.
 * If this coroutine is cancelled, the modal flow is cancelled and the currently active modal is closed.
 */
suspend fun <T> modalFlow(modalManager: ModalManager, block: suspend ModalFlow.() -> T): T = coroutineScope {
    val ourJob = coroutineContext.job
    val modalFlow = ModalFlow(modalManager)

    // Once the flow has produced the first modal, we need to open it to get things going
    val firstModal = CompletableDeferred<Modal?>(ourJob)
    launch { firstModal.await()?.let { modalManager.queueModal(it) } }
    modalFlow.replacePreviousModalWith = firstModal

    // If the modal manager scope is cancelled prematurely (e.g. because the modal was closed without resuming the
    // flow), cancel the flow / this coroutine.
    var cancelOnClose = true
    val modalCloseDetectionJob = modalManager.coroutineScope.launch {
        try {
            awaitCancellation()
        } finally {
            if (cancelOnClose) {
                ourJob.cancel()
            }
        }
    }

    try {
        block(modalFlow)
    } finally {
        cancelOnClose = false
        modalCloseDetectionJob.cancel()
        // Close last modal at end of flow
        modalFlow.replacePreviousModalWith.complete(null)
    }
}

/**
 * Manages a dynamic sequence of modals.
 * @see launchModalFlow
 */
class ModalFlow(val modalManager: ModalManager) {
    var replacePreviousModalWith: CompletableDeferred<Modal?> = CompletableDeferred(null)

    suspend fun <T> awaitModal(block: (continuation: ModalContinuation<T>) -> Modal): T {
        val (deferred, result) = suspendCancellableCoroutine { continuation ->
            val modal = block(ModalContinuation(modalManager, continuation))
            continuation.invokeOnCancellation {
                modal.close()
            }
            replacePreviousModalWith.complete(modal)
        }
        replacePreviousModalWith = deferred
        return result
    }

    class ModalContinuation<T>(
        private val modalManager: ModalManager,
        private val coroutineContinuation: Continuation<Pair<CompletableDeferred<Modal?>, T>>,
    ) {
        /**
         * Resumes the [ModalFlow] coroutine and suspends until the next modal is queued via [awaitModal].
         *
         * If you cannot suspend, you may instead use [resumeImmediately] which will return an empty temporary [Modal]
         * immediately and then later replace it with the real one once that has been determined.
         */
        suspend fun resume(result: T): Modal? {
            val job = CompletableDeferred<Modal?>(parent = coroutineContext.job)
            coroutineContinuation.resume(Pair(job, result))
            return job.await()
        }

        fun resumeImmediately(result: T): Modal {
            return object : Modal(modalManager) {
                override val modalName: String?
                    get() = null
                override fun onOpen() {
                    super.onOpen()

                    coroutineScope.launch {
                        replaceWith(resume(result))
                    }
                }
                override fun LayoutScope.layoutModal() {}
                override fun handleEscapeKeyPress() {}
            }
        }
    }
}

