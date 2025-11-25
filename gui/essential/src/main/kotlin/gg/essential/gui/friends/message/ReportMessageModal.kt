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
package gg.essential.gui.friends.message

import gg.essential.elementa.dsl.provideDelegate
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.OutlineButtonStyle
import gg.essential.gui.common.modal.EssentialModal2
import gg.essential.gui.common.radioButton
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.friends.message.v2.ClientMessage
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.FloatPosition
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedHeight
import gg.essential.gui.layoutdsl.childBasedMaxHeight
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillRemainingWidth
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.inheritHoverScope
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.wrappedText
import gg.essential.gui.overlay.ModalManager
import gg.essential.universal.USound
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.UuidNameLookup
import gg.essential.util.centered
import gg.essential.util.onLeftClick

class NewReportMessageModal(modalManager: ModalManager, val message: ClientMessage) : EssentialModal2(modalManager) {

    private val reasons = platform.reportReasons

    private val selectedReason = mutableStateOf<String?>(null)
    private val submitDisabledState = selectedReason.map { it == null }

    override fun LayoutScope.layoutTitle() {
        title({ "Report ${UuidNameLookup.nameState(message.sender)()}" })
    }

    override fun LayoutScope.layoutBody() {
        column(Modifier.fillWidth(), Arrangement.spacedBy(12f)) {
            wrappedText(
                "Select the option that best\ndescribes the problem.", Modifier
                    .color(EssentialPalette.TEXT)
                    .shadow(EssentialPalette.BLACK),
                centered = true
            )
            column(Modifier.fillWidth(), Arrangement.spacedBy(3f)) {
                for ((id, displayText) in reasons) {
                    reason(id, displayText)
                }
            }
        }
    }

    override fun LayoutScope.layoutButtons() {
        row(Arrangement.spacedBy(8f)) {
            cancelButton("Cancel")
            primaryButton("Submit", style = OutlineButtonStyle.RED, disabled = submitDisabledState) {
                platform.fileReport(
                    modalManager,
                    message.channel.id,
                    message.id,
                    message.sender,
                    selectedReason.getUntracked()!!,
                )
                close()
            }
        }
    }

    private fun LayoutScope.reason(id: String, displayText: String) {
        box(Modifier.fillWidth().childBasedHeight(3f).hoverScope().color(EssentialPalette.COMPONENT_BACKGROUND).hoverColor(EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT).shadow(EssentialPalette.BLACK)) {
            row(Modifier.fillWidth(padding = 7f).childBasedMaxHeight(4f), Arrangement.spacedBy(8f, FloatPosition.START)) {
                radioButton(id, selectedReason, Modifier.inheritHoverScope())
                box(Modifier.fillRemainingWidth()) {
                    wrappedText(displayText, Modifier
                        .alignHorizontal(Alignment.Start)
                        .color(EssentialPalette.TEXT)
                        .shadow(EssentialPalette.BLACK)
                    )
                }
            }
        }.onLeftClick { click ->
            if (selectedReason.getUntracked() != id) {
                USound.playButtonPress()
                selectedReason.set(id)
            }
            click.stopPropagation()
        }
    }
}