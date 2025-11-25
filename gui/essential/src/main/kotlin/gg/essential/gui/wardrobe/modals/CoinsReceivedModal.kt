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
package gg.essential.gui.wardrobe.modals

import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.dsl.*
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.HighlightedBlock
import gg.essential.gui.common.OutlineButtonStyle
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.common.outlineButton
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.wardrobe.components.coinPackImage
import gg.essential.network.connectionmanager.coins.CoinsManager
import gg.essential.universal.USound
import gg.essential.util.EssentialSounds.playCoinsSound
import gg.essential.util.onLeftClick

class CoinsReceivedModal private constructor(
    modalManager: ModalManager,
    coinsManager: CoinsManager,
    receivedAmount: Int,
    extraTitle: String?,
    verb: String,
    buttonText: String,
) : Modal(modalManager) {
    private val container = HighlightedBlock(
        backgroundColor = EssentialPalette.MODAL_BACKGROUND,
        highlightColor = EssentialPalette.BUTTON_HIGHLIGHT,
        highlightHoverColor = EssentialPalette.BUTTON_HIGHLIGHT,
    )

    init {
        container.constrainBasedOnChildren()
        container.contentContainer.constrain {
            width = ChildBasedSizeConstraint() + 20.pixels
            height = ChildBasedSizeConstraint() + 20.pixels
        }

        coinsManager.areCoinsVisuallyFrozen.set(true)

        container.contentContainer.layoutAsBox(Modifier.width(222f).childBasedHeight()) {
            column {
                spacer(height = 19f)
                if (extraTitle != null) {
                    text(extraTitle, Modifier.color(EssentialPalette.TEXT).shadow(EssentialPalette.BLACK))
                    spacer(height = 7f)
                }
                row {
                    text("$verb ${CoinsManager.COIN_FORMAT.format(receivedAmount)} ", Modifier.shadow(EssentialPalette.BLACK))
                    spacer(width = 1f)
                    image(EssentialPalette.COIN_7X, Modifier.shadow(EssentialPalette.BLACK))
                }
                spacer(height = 17f)
                box(Modifier.childBasedSize(20f).color(EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT)) {
                    coinPackImage(coinsManager, receivedAmount)
                }
                spacer(height = 17f)
                outlineButton(stateOf(buttonText), style = OutlineButtonStyle.BLUE, modifier = Modifier.width(91f).shadow()).onLeftClick {
                    USound.playButtonPress()
                    playCoinsSound()
                    coinsManager.areCoinsVisuallyFrozen.set(false)
                    super.close()
                }
                spacer(height = 16f)
            }
        }
    }

    override fun LayoutScope.layoutModal() {
        container()
    }

    override fun close() {
        // Prevent clicking outside the modal...
    }

    companion object {

        fun fromPurchase(modalManager: ModalManager, coinsManager: CoinsManager, purchasedAmount: Int) = CoinsReceivedModal(modalManager, coinsManager, purchasedAmount, null, "Purchased", "Okay!")

        fun fromCoinClaim(modalManager: ModalManager, coinsManager: CoinsManager, claimedAmount: Int) = CoinsReceivedModal(modalManager, coinsManager, claimedAmount, "Welcome <3", "Received", "Claim!")

    }

}
