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

import gg.essential.gui.elementa.state.v2.await
import gg.essential.gui.overlay.ModalFlow
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

abstract class ModalPrerequisites {

    abstract suspend fun ModalFlow.doTermsOfServiceModal(): PrerequisiteResult

    abstract suspend fun ModalFlow.doRequiredUpdateModal(): PrerequisiteResult

    abstract suspend fun ModalFlow.doAuthenticationModal(): PrerequisiteResult

    abstract suspend fun ModalFlow.doCosmeticsModal(): PrerequisiteResult

    abstract suspend fun ModalFlow.doCommunityRulesModal(): PrerequisiteResult

    abstract suspend fun ModalFlow.doSocialSuspensionModal(): PrerequisiteResult

    abstract suspend fun ModalFlow.doPermanentSuspensionModal(): PrerequisiteResult

    protected fun Boolean.toResult(): PrerequisiteResult = if (this) PrerequisiteResult.SUCCESS else PrerequisiteResult.FAILURE
}

suspend fun ModalFlow.ensurePrerequisites(
    cosmetics: Boolean = false,
    social: Boolean = false,
    rules: Boolean = social
) {
    with (platform.modalPrerequisites) {
        val prerequisites = mutableListOf<suspend () -> PrerequisiteResult>()

        prerequisites.add(suspend { doPermanentSuspensionModal() })

        prerequisites.add(suspend { doTermsOfServiceModal() })
        prerequisites.add(suspend { doRequiredUpdateModal() })
        prerequisites.add(suspend { doAuthenticationModal() })

        if (cosmetics) {
            prerequisites.add(suspend { doCosmeticsModal() })
        }

        if (social) {
            prerequisites.add(suspend { doSocialSuspensionModal() })
        }

        if (rules) {
            prerequisites.add(suspend { doCommunityRulesModal() })
        }

        ensurePrerequisitesInternal(prerequisites)

        modalManager.coroutineScope.launch {
            platform.suspensionManager.activeSuspension.await { it != null && (social || it.isPermanent) }
            modalManager.coroutineScope.cancel()
        }
    }
}

private suspend fun ensurePrerequisitesInternal(prerequisites: List<suspend () -> PrerequisiteResult>) {
    loop@ while (true) {
        for (prerequisite in prerequisites) {
            when (prerequisite()) {
                PrerequisiteResult.SUCCESS -> continue@loop
                PrerequisiteResult.FAILURE -> throw CancellationException()
                PrerequisiteResult.PASS -> {}
            }
        }
        break@loop
    }
}

enum class PrerequisiteResult {

    // For when the prerequisite is checked, and the user responded positively. Used to indicate that previous passed
    // prerequisites should be tried again.
    SUCCESS,
    // For when the prerequisite modal is shown, and the user responded negatively, or it was a modal that cannot
    // be a success. This will result in a CancellationException being thrown.
    FAILURE,
    // For when the prerequisite is not checked. Once all prerequisites return this, the overall check passes.
    PASS,
}