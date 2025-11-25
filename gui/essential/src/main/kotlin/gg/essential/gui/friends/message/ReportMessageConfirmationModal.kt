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

import com.sparkuniverse.toolbox.chat.enums.ChannelType
import com.sparkuniverse.toolbox.chat.model.Channel
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.MenuButton
import gg.essential.gui.common.StyledButton.Style
import gg.essential.gui.common.modal.EssentialModal2
import gg.essential.gui.common.textStyle
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.onChange
import gg.essential.gui.friends.state.SocialStates
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedHeight
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillRemainingWidth
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.layoutdsl.wrappedText
import gg.essential.gui.overlay.ModalManager
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.UuidNameLookup
import java.awt.Color
import java.util.*

class ReportMessageConfirmationModal(modalManager: ModalManager, val sender: UUID, val debug: Boolean = false) : EssentialModal2(modalManager) {

    private val socialStates = platform.createSocialStates()

    private val dmChannel = socialStates.findDM(sender)
    private val mutedState = when {
        debug -> mutableStateOf(false)
        dmChannel != null && socialStates.isFriend(sender) -> socialStates.messages.getMuted(dmChannel.id)
        else -> null
    }

    private val blockedState = mutableStateOf(if (debug) false else socialStates.isBlocked(sender)).apply {
        this.onChange(this@ReportMessageConfirmationModal) { blocked ->
            if (!debug && blocked) {
                socialStates.relationships.blockPlayer(sender, false)
            }
        }
    }

    override fun LayoutScope.layoutTitle() {
        title("Thank you!")
    }

    override fun LayoutScope.layoutBody() {
        column(Modifier.fillWidth(), Arrangement.spacedBy(15f)) {
            wrappedText("Your report has been submitted.\nYour feedback helps keep\nEssential safe.",
                Modifier
                    .color(EssentialPalette.TEXT)
                    .shadow(EssentialPalette.BLACK),
                centered = true,
                lineSpacing = 10f
            )
            column(Modifier.fillWidth(), Arrangement.spacedBy(3f)) {
                if (mutedState != null) {
                    userAction("Mute", "Muted", Styles.GRAY, mutedState)
                }
                userAction("Block", "Blocked", Styles.RED, blockedState)
            }
        }
    }

    private fun LayoutScope.userAction(label: String, doneLabel: String, style: Style, state: MutableState<Boolean>) {
        val labelState = state.map { if (it) doneLabel else label }
        row(Modifier.fillWidth().childBasedHeight(2f).color(EssentialPalette.COMPONENT_BACKGROUND)) {
            spacer(width = 10f)
            box(Modifier.fillRemainingWidth().alignVertical(Alignment.Center(true))) {
                text({ "$label ${UuidNameLookup.nameState(sender)()}" }, Modifier.alignHorizontal(Alignment.Start).shadow(EssentialPalette.BLACK), truncateIfTooSmall = true)
            }
            spacer(width = 10f)
            outlineButton(Modifier.height(17f).width(52f).shadow(EssentialPalette.BLACK), { style }, action = {
                state.set(true)
            }, disabled = state) { currentStyle ->
                text(labelState, Modifier.textStyle(currentStyle))
            }
            spacer(width = 10f)
        }
    }

    override fun LayoutScope.layoutButtons() {
        primaryButton("Done") { close() }
    }

    private fun SocialStates.isFriend(uuid: UUID): Boolean = relationships.getObservableFriendList().contains(uuid)

    private fun SocialStates.isBlocked(uuid: UUID): Boolean = relationships.getObservableBlockedList().contains(uuid)

    private fun SocialStates.findDM(uuid: UUID): Channel? =
        messages.getObservableChannelList().firstOrNull { channel -> channel.type == ChannelType.DIRECT_MESSAGE && uuid in channel.members }
}

private object Styles {

    private val transparent = Color(0, 0, 0, 0)

    val RED = Style(
        MenuButton.Style(EssentialPalette.TEXT, EssentialPalette.RED_OUTLINE_BUTTON, transparent),
        MenuButton.Style(EssentialPalette.TEXT, EssentialPalette.RED_OUTLINE_BUTTON_HOVER, transparent),
        MenuButton.Style(EssentialPalette.TEXT_DISABLED, EssentialPalette.RED_OUTLINE_BUTTON.darker(), transparent)
    )

    val GRAY = Style(
        MenuButton.Style(EssentialPalette.TEXT, EssentialPalette.GRAY_OUTLINE_BUTTON, transparent),
        MenuButton.Style(EssentialPalette.TEXT, EssentialPalette.GRAY_OUTLINE_BUTTON_HOVER, transparent),
        MenuButton.Style(EssentialPalette.TEXT_DISABLED, EssentialPalette.INPUT_BACKGROUND, transparent)
    )
}