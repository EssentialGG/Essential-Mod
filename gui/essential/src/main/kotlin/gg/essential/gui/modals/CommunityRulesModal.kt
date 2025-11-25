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

import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.OutlineButtonStyle
import gg.essential.gui.common.modal.EssentialModal2
import gg.essential.gui.elementa.state.v2.combinators.not
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.checkboxAlt
import gg.essential.gui.layoutdsl.childBasedHeight
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.inheritHoverScope
import gg.essential.gui.layoutdsl.onLeftClick
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.underline
import gg.essential.gui.layoutdsl.whenHovered
import gg.essential.gui.layoutdsl.wrappedText
import gg.essential.gui.overlay.ModalFlow
import gg.essential.gui.overlay.ModalManager
import gg.essential.network.connectionmanager.social.RulesManager
import gg.essential.universal.USound
import gg.essential.util.openInBrowser
import gg.essential.vigilance.utils.onLeftClick
import java.net.URI

class CommunityRulesModal(
    modalManager: ModalManager,
    private val rulesManager: RulesManager,
    private val locale: String,
    private val continuation: ModalFlow.ModalContinuation<Boolean>,
    private val allowAccept: Boolean = true
) : EssentialModal2(modalManager) {

    private val agreeState = mutableStateOf(false)
    private val awaitRulesResponseState = mutableStateOf(false)

    override fun onClose() {
        super.onClose()
        modalManager.queueModal(continuation.resumeImmediately(rulesManager.acceptedRules))
    }

    override fun LayoutScope.layoutTitle() {
        title("Community & Chat Rules")
    }

    override fun LayoutScope.layoutBody() {
        column(Modifier.fillWidth(), Arrangement.spacedBy(13f)) {
            wrappedText("Read our full {rules}\nand always follow them.",
                textModifier = Modifier.color(EssentialPalette.TEXT).shadow(EssentialPalette.BLACK),
                verticalArrangement = Arrangement.spacedBy(2f)
            ) {
                "rules" {
                    text("community rules", Modifier
                        .color(EssentialPalette.LINK)
                        .shadow(EssentialPalette.BLACK)
                        .hoverScope()
                        .whenHovered(Modifier.underline().color(EssentialPalette.LINK_HIGHLIGHT))
                        .onLeftClick {
                            openInBrowser(URI.create("https://essential.gg/wiki/community-and-chat-rules"))
                        }
                    )
                }
            }
            bind(rulesManager.rules(locale)) { rules ->
                rules(rules)
            }
            if (allowAccept) {
                row(Modifier.hoverScope(), Arrangement.spacedBy(5f)) {
                    checkboxAlt(agreeState, Modifier.inheritHoverScope().shadow(EssentialPalette.BLACK), awaitRulesResponseState)
                    text(
                        "I've read and agree to all rules", Modifier
                            .color(EssentialPalette.TEXT_DISABLED)
                            .shadow(EssentialPalette.BLACK)
                            .alignVertical(Alignment.Center(true))
                    )
                }.onLeftClick { click ->
                    click.stopPropagation()
                    if (!awaitRulesResponseState.getUntracked()) {
                        USound.playButtonPress()
                        agreeState.set { !it }
                    }
                }
            }
        }
    }

    override fun LayoutScope.layoutButtons() {
        row(Arrangement.spacedBy(8f)) {
            cancelButton("Back") {
                close()
            }
            if (allowAccept) {
                primaryButton("Submit", style = OutlineButtonStyle.BLUE, disabled = agreeState.not()) {
                    try {
                        awaitRulesResponseState.set(true)
                        if (rulesManager.acceptRules()) {
                            close()
                        }
                    } finally {
                        awaitRulesResponseState.set(false)
                    }
                }
            }
        }
    }

    private fun LayoutScope.rules(rules: List<String>) {
        box(Modifier.color(EssentialPalette.COMPONENT_BACKGROUND).shadow(EssentialPalette.BLACK).fillWidth(padding = 5f)) {
            column(Modifier.fillWidth(padding = 16f).childBasedHeight(15f), Arrangement.spacedBy(7f)) {
                for ((index, rule) in rules.withIndex()) {
                    text("${index + 1}. $rule", Modifier
                        .color(EssentialPalette.TEXT)
                        .shadow(EssentialPalette.BLACK)
                        .alignHorizontal(Alignment.Start)
                    )
                    if (index < rules.lastIndex) {
                        box(Modifier.color(EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT).fillWidth().height(1f))
                    }
                }
            }
        }
    }
}

suspend fun ModalFlow.communityRulesModal(rulesManager: RulesManager, locale: String, allowAccept: Boolean = true): Boolean =
    awaitModal { CommunityRulesModal(modalManager, rulesManager, locale, it, allowAccept) }