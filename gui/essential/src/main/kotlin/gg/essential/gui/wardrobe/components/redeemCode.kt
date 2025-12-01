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

package gg.essential.gui.wardrobe.components

import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.collapsibleButton
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.icon
import gg.essential.gui.layoutdsl.onLeftClick
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.overlay.ModalFlow
import gg.essential.gui.overlay.launchModalFlow
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.modals.RedeemCodeClaimedModal
import gg.essential.gui.wardrobe.modals.RedeemCodeModal
import gg.essential.universal.USound
import gg.essential.util.GuiEssentialPlatform.Companion.platform

fun LayoutScope.redeemCodeButton(collapsed: State<Boolean>, wardrobeState: WardrobeState) {
    collapsibleButton(
        Modifier.onLeftClick {
            USound.playButtonPress()
            launchModalFlow(platform.createModalManager()) {
                redeemCodeModalFlow(wardrobeState)
            }
        },
        collapsed,
        stateOf("Redeem Code"),
        {
            text("Redeem", Modifier.color(EssentialPalette.TEXT).hoverColor(EssentialPalette.TEXT_HIGHLIGHT)
                .shadow(EssentialPalette.TEXT_SHADOW))
        },
        {
            box(Modifier.width(9f).height(5f)) {
                icon(
                    EssentialPalette.RENAME_8X5,
                    Modifier.color(EssentialPalette.TEXT).hoverColor(EssentialPalette.TEXT_HIGHLIGHT)
                        .shadow(EssentialPalette.TEXT_SHADOW).alignHorizontal(Alignment.End)
                )
            }
        }
    )
}

private suspend fun ModalFlow.redeemCodeModalFlow(wardrobeState: WardrobeState) {
    val coinsManager = wardrobeState.coinsManager
    coinsManager.areCoinsVisuallyFrozen.set(true)
    coinsManager.isClaimingCoins.set(true)
    try {
        val packet = awaitModal { continuation ->
            RedeemCodeModal(modalManager, continuation)
        }
        awaitModal<Unit> {
            RedeemCodeClaimedModal(modalManager, packet, wardrobeState)
        }
    } finally {
        coinsManager.areCoinsVisuallyFrozen.set(false)
        coinsManager.isClaimingCoins.set(false)
    }
}