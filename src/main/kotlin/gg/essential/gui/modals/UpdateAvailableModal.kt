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

import gg.essential.config.EssentialConfig
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.common.OutlineButtonStyle
import gg.essential.gui.common.compactFullEssentialToggle
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.BasicYModifier
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedHeight
import gg.essential.gui.layoutdsl.childBasedWidth
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.inheritHoverScope
import gg.essential.gui.layoutdsl.layoutAsBox
import gg.essential.gui.layoutdsl.onLeftClick
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.notification.Notifications
import gg.essential.gui.overlay.ModalFlow
import gg.essential.gui.overlay.ModalManager
import gg.essential.universal.USound
import gg.essential.util.AutoUpdate
import gg.essential.util.MinecraftUtils.shutdown
import gg.essential.util.bindHoverEssentialTooltip
import java.awt.Color

class UpdateAvailableModal(modalManager: ModalManager) : ConfirmDenyModal(modalManager, false) {

    init {
        AutoUpdate.dismissUpdateToast?.invoke()

        titleText = AutoUpdate.getNotificationTitle()
        contentTextColor = EssentialPalette.TEXT
        primaryButtonText = "Update"
        primaryButtonStyle = OutlineButtonStyle.GREEN.defaultStyle
        primaryButtonHoverStyle = OutlineButtonStyle.GREEN.hoveredStyle
        contentTextSpacingState.rebind(BasicState(17f))

        if (AutoUpdate.requiresUpdate()) {
            titleTextColor = EssentialPalette.MODAL_WARNING
        }

        val autoUpdate = EssentialConfig.autoUpdateState

        customContent.layoutAsBox(BasicYModifier { SiblingConstraint(15f) }) {
            column {
                row(
                    Modifier.childBasedWidth(3f).hoverScope().onLeftClick {
                        USound.playButtonPress()
                        autoUpdate.set { !it }
                    },
                    Arrangement.spacedBy(9f),
                ) {
                    text("Auto-updates", Modifier.color(EssentialPalette.TEXT_DISABLED).shadow(Color.BLACK))
                    box(Modifier.childBasedHeight()) {
                        compactFullEssentialToggle(
                            autoUpdate,
                            Modifier.inheritHoverScope(),
                            offColor = EssentialPalette.TEXT_DISABLED,
                        )
                    }
                }

                spacer(height = 14f)
            }
        }

        spacer.setHeight(0.pixels)

        if(AutoUpdate.changelog.isDone) {
            AutoUpdate.changelog.join()?.let {
                contentText = it
            }
        } else {
            AutoUpdate.changelog.whenCompleteAsync({ changelog, _ ->
                changelog?.let { contentText = it }
            }, Window::enqueueRenderOperation)
        }

        onPrimaryAction {
            AutoUpdate.update(autoUpdate.get())

            replaceWith(EssentialRebootUpdateModal(modalManager).onCancel {
                shutdown()
            }.onPrimaryAction {
                Notifications.push("Update Confirmed", "Essential will update next time you launch the game!")
            })
        }

        onCancel { AutoUpdate.ignoreUpdate() }
    }
}

class EssentialRebootUpdateModal(modalManager: ModalManager) : ConfirmDenyModal(modalManager, true) {
    init {
        configure {
            contentText = "Essential will update the next time\nyou launch the game."
            primaryButtonText = "Okay"
            cancelButtonText = "Quit & Update"
            cancelButton.bindHoverEssentialTooltip(
                stateOf("This will close your game!").toV1(this),
                EssentialTooltip.Position.ABOVE,
                4f,
            )
        }
    }
}

class UpdateRequiredModal(modalManager: ModalManager) : ConfirmDenyModal(modalManager, false) {

    init {
        contentText = "Sorry, you are on an outdated version of Essential. Restart your game to update."
        primaryButtonText = "Quit & Update"
        primaryButtonStyle = OutlineButtonStyle.GRAY.defaultStyle
        primaryButtonHoverStyle = OutlineButtonStyle.GRAY.hoveredStyle
        primaryActionButton.bindHoverEssentialTooltip(
            stateOf("This will close your game!").toV1(this),
            EssentialTooltip.Position.ABOVE,
            4f,
        )
        onPrimaryAction { shutdown() }
    }
}

suspend fun ModalFlow.updateRequiredModal() {
    awaitModal<Unit> { UpdateRequiredModal(modalManager) }
}
