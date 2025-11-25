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
import gg.essential.config.EssentialConfig
import gg.essential.data.MenuData
import gg.essential.data.VersionData
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.Checkbox
import gg.essential.gui.common.MenuButton
import gg.essential.gui.common.modal.VerticalConfirmDenyModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.common.textStyle
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.checkboxAlt
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.image
import gg.essential.gui.layoutdsl.inheritHoverScope
import gg.essential.gui.layoutdsl.layoutAsRow
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.wrappedText
import gg.essential.gui.overlay.ModalManager
import gg.essential.universal.USound
import gg.essential.util.findChildOfTypeOrNull
import gg.essential.util.openInBrowser
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color
import java.net.URI

class UpdateNotificationModal(modalManager: ModalManager) : VerticalConfirmDenyModal(
    modalManager,
    requiresButtonPress = false,
    buttonPadding = 17f
) {

    init {
        configure {
            titleText = "Essential has been updated!"
            titleTextColor = EssentialPalette.ACCENT_BLUE
            cancelButtonText = "Changelog"
        }

        spacer.setHeight(11.pixels)

        // Notification checkbox and label container
        val notifyContainer by UIContainer().constrain {
            x = CenterConstraint()
            y = SiblingConstraint(14f)
            width = ChildBasedSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        }.onLeftClick { findChildOfTypeOrNull<Checkbox>()?.toggle() } childOf customContent

        notifyContainer.layoutAsRow(Modifier.hoverScope(), Arrangement.spacedBy(5f)) {
            checkboxAlt(EssentialConfig.updateModalState, Modifier.shadow(EssentialPalette.BLACK).inheritHoverScope())
            text("Don’t notify me about updates", modifier = Modifier.alignVertical(Alignment.Center(true)).color(EssentialPalette.TEXT_DISABLED).shadow(EssentialPalette.BLACK))
        }.onLeftClick {
            it.stopPropagation()
            USound.playButtonPress()
            EssentialConfig.updateModalState.set { !it }
        }


        onCancel { buttonClicked ->
            if (buttonClicked) {
                openInBrowser(URI.create("https://essential.gg/changelog"))
            }
            VersionData.updateLastSeenModal()
        }

        onPrimaryAction {
            VersionData.updateLastSeenModal()
        }

        val current = VersionData.getMajorComponents(VersionData.essentialVersion)
        val saved = VersionData.getMajorComponents(VersionData.getLastSeenModal())
        val versionComponents = mutableListOf("0", "0", "0")

        for ((index, component) in current.withIndex()) {
            versionComponents[index] = component
            if (index >= saved.size || saved[index] != component) {
                break
            }
        }

        val displayVersion = versionComponents.joinToString(".")

        MenuData.CHANGELOGS.get(displayVersion).whenCompleteAsync({ (_, log), exception ->

            if (exception != null) {
                Essential.logger.error("An error occurred fetching the changelog for version $displayVersion", exception)
            } else {
                // The changelog message
                val changelog by UIWrappedText(
                    log.summary,
                    shadowColor = Color.BLACK,
                    centered = true,
                    trimText = true,
                    lineSpacing = 12f,
                ).constrain {
                    x = CenterConstraint()
                    y = SiblingConstraint()
                    width = 100.percent
                    color = EssentialPalette.TEXT.toConstraint()
                }
                customContent.insertChildBefore(changelog, notifyContainer)
            }
        }, Window::enqueueRenderOperation)
    }

    override fun LayoutScope.layoutCancelButton(text: State<String>, currentStyle: State<MenuButton.Style>) {
        wrappedText("{text} {icon}", Modifier.alignVertical(Alignment.Center(true))) {
            "text" { text(text, Modifier.textStyle(currentStyle)) }
            "icon" { image(EssentialPalette.ARROW_UP_RIGHT_5X5, Modifier.textStyle(currentStyle)) }
        }
    }

}
