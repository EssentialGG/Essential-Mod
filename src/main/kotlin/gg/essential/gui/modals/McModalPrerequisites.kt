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
import gg.essential.gui.overlay.ModalFlow
import gg.essential.data.OnboardingData
import gg.essential.network.connectionmanager.suspension.suspensionModal
import gg.essential.universal.UMinecraft
import gg.essential.util.AutoUpdate

object McModalPrerequisites : ModalPrerequisites() {

    override suspend fun ModalFlow.doTermsOfServiceModal(): PrerequisiteResult {
        return if (!OnboardingData.hasAcceptedTos()) {
            tosModal().toResult()
        } else {
            PrerequisiteResult.PASS
        }
    }

    override suspend fun ModalFlow.doRequiredUpdateModal(): PrerequisiteResult {
        return if (AutoUpdate.requiresUpdate()) {
            updateRequiredModal()
            PrerequisiteResult.FAILURE
        } else {
            PrerequisiteResult.PASS
        }
    }

    override suspend fun ModalFlow.doAuthenticationModal(): PrerequisiteResult {
        val connectionManager = Essential.getInstance().connectionManager
        return if (!connectionManager.isAuthenticated || ((!connectionManager.suspensionManager.isLoaded.getUntracked() || !connectionManager.rulesManager.isLoaded.getUntracked()))) {
            notAuthenticatedModal().toResult()
        } else {
            PrerequisiteResult.PASS
        }
    }

    override suspend fun ModalFlow.doCosmeticsModal(): PrerequisiteResult {
        if (!Essential.getInstance().connectionManager.cosmeticsManager.cosmeticsLoaded.getUntracked()) {
            cosmeticsLoadingModal()
            return PrerequisiteResult.SUCCESS
        }
        return PrerequisiteResult.PASS
    }

    override suspend fun ModalFlow.doCommunityRulesModal(): PrerequisiteResult {
        val rulesManager = Essential.getInstance().connectionManager.rulesManager
        return if (rulesManager.hasRules.getUntracked() && !rulesManager.acceptedRules) {
            communityRulesModal(rulesManager, UMinecraft.getSettings().language).toResult()
        } else {
            PrerequisiteResult.PASS
        }
    }

    override suspend fun ModalFlow.doSocialSuspensionModal(): PrerequisiteResult {
        Essential.getInstance().connectionManager.suspensionManager.activeSuspension.getUntracked()?.let { suspension ->
            suspensionModal(suspension)
            return PrerequisiteResult.FAILURE
        }
        return PrerequisiteResult.PASS
    }

    override suspend fun ModalFlow.doPermanentSuspensionModal(): PrerequisiteResult {
        Essential.getInstance().connectionManager.suspensionManager.activeSuspension.getUntracked()?.let { suspension ->
            if (suspension.isPermanent) {
                suspensionModal(suspension)
                return PrerequisiteResult.FAILURE
            }
        }
        return PrerequisiteResult.PASS
    }
}