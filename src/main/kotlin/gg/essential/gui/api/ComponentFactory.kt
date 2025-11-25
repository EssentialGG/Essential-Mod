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
package gg.essential.gui.api

import gg.essential.api.gui.ConfirmationModalBuilder
import gg.essential.api.gui.EmulatedPlayerBuilder
import gg.essential.api.gui.EssentialComponentFactory
import gg.essential.api.gui.IconButtonBuilder
import gg.essential.api.profile.wrapped
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.inspector.Inspector
import gg.essential.gui.common.EmulatedUI3DPlayer
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.modal.CancelableInputModal
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.image.ResourceImageFactory
import gg.essential.gui.overlay.UIContainerModalManagerImpl

object ComponentFactory : EssentialComponentFactory {
    override fun build(builder: EmulatedPlayerBuilder): UIComponent = with(builder) {
        val state = wrappedProfileState ?: profileState.map { it?.wrapped() }
        EmulatedUI3DPlayer(showCapeState, draggableState, state, renderNameTagState)
    }

    override fun build(builder: ConfirmationModalBuilder): UIComponent = with(builder) {
        val manager = UIContainerModalManagerImpl()

        val inputPlaceholder = inputPlaceholder
        val secondaryText = secondaryText

        val modal = if (inputPlaceholder != null) {
            CancelableInputModal(manager, inputPlaceholder, "").onPrimaryActionWithValue(onConfirm)
        } else {
            ConfirmDenyModal(manager, false).onPrimaryAction {
                onConfirm("")
            }
        }.configure {
            titleText = text
            if (secondaryText != null) {
                contentText = secondaryText
            }
            primaryButtonText = confirmButtonText
            cancelButtonText = denyButtonText
        }.onCancel {
            onDeny()
        }

        manager.apply { queueModal(modal) }
    }

    override fun build(builder: IconButtonBuilder): UIComponent {
        return IconButton(
            builder.iconResourceState.map { ResourceImageFactory(it) },
            builder.tooltipTextState,
            builder.enabledState,
            builder.buttonTextState,
            builder.iconShadowState,
            builder.textShadowState,
            builder.tooltipBelowComponent,
        )
    }

    init {
        Inspector.registerComponentFactory(ComponentFactory::class.java)
    }
}