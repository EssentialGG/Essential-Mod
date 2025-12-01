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

import gg.essential.connectionmanager.common.packet.response.ResponseActionPacket
import gg.essential.connectionmanager.common.packet.store.ClientRedeemStoreClaimPacket
import gg.essential.connectionmanager.common.packet.store.ServerRedeemStoreClaimResponsePacket
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.input.UITextInput
import gg.essential.gui.common.input.essentialInput
import gg.essential.gui.common.modal.EssentialModal2
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.heightAspect
import gg.essential.gui.layoutdsl.iconButton
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.overlay.ModalFlow
import gg.essential.gui.overlay.ModalManager
import gg.essential.universal.UDesktop
import gg.essential.util.GuiEssentialPlatform.Companion.platform

class RedeemCodeModal(
    modalManager: ModalManager,
    private val modalContinuation: ModalFlow.ModalContinuation<ServerRedeemStoreClaimResponsePacket>,
) : EssentialModal2(modalManager, false) {

    private val errorMessageState: MutableState<String?> = mutableStateOf(null)

    private val input by UITextInput("Enter Code", shadowColor = EssentialPalette.BLACK).apply {
        onUpdate {
            errorMessageState.set(null)
        }
    }

    override fun LayoutScope.layoutTitle() {
        title("Redeem Code")
    }

    override fun LayoutScope.layoutBody() {
        row(Modifier.fillWidth(), Arrangement.spacedBy(5f)) {
            spacer(width = 17f) // To center the input box
            box(Modifier.width(106f).height(17f)) {
                essentialInput(input, errorMessageState = errorMessageState, inputModifier = Modifier.height(10f).color(EssentialPalette.TEXT))
            }
            iconButton(EssentialPalette.PASTE_10X8, Modifier.width(17f).heightAspect(1f), tooltipText = "Paste") {
                input.setText(UDesktop.getClipboardString())
            }
        }
    }

    override fun LayoutScope.layoutButtons() {
        row(Modifier.fillWidth(), Arrangement.spacedBy(5f)) {
            cancelButton("Cancel")
            primaryButton("Redeem", disabled = memo { errorMessageState() != null || input.textState().isBlank() }) {
                val packet = platform.cmConnection.call(ClientRedeemStoreClaimPacket(input.getText()))
                    .awaitOneOf(ResponseActionPacket::class.java, ServerRedeemStoreClaimResponsePacket::class.java)
                if (packet is ResponseActionPacket) {
                    when (packet.errorMessage) {
                        "EMPTY_REDEMPTION" -> errorMessageState.set("You already own all of these items")
                        "ALREADY_REDEEMED" -> errorMessageState.set("You have already redeemed this code")
                        "INVALID_CODE" -> errorMessageState.set("Invalid or expired code")
                        else -> errorMessageState.set("An unknown error has occurred")
                    }
                } else if (packet is ServerRedeemStoreClaimResponsePacket) {
                    replaceWith(modalContinuation.resume(packet))
                }
            }
        }
    }

}
