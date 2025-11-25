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
package gg.essential.gui.notification.content

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.boundTo
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.Spacer
import gg.essential.gui.util.hoveredState
import gg.essential.vigilance.utils.onLeftClick

class ConfirmDenyNotificationActionComponent(
    confirmTooltip: String = "Confirm",
    denyTooltip: String = "Deny",
    confirmAction: (() -> Unit)? = null,
    denyAction: (() -> Unit)? = null,
    val timerEnabledState: BasicState<Boolean> = BasicState(false),
    dismissNotification: () -> Unit,
) : UIContainer() {

    private val confirmButton = IconButton(
        EssentialPalette.CHECKMARK_7X5,
        tooltipText = confirmTooltip,
        tooltipBelowComponent = false,
    ).constrain {
        height = AspectConstraint()
    }.apply {
        rebindIconColor(hoveredState().map {
            if (it) EssentialPalette.GREEN else EssentialPalette.TEXT
        })
    }.onLeftClick {
        confirmAction?.invoke()
        dismissNotification()
        timerEnabledState.set(true)
    } childOf this

    private val spacer by Spacer(width = 3f) childOf this

    private val denyButton = IconButton(
        EssentialPalette.CANCEL_5X,
        tooltipText = denyTooltip,
        tooltipBelowComponent = false,
    ).constrain {
        x = SiblingConstraint()
        width = 100.percent boundTo confirmButton
        height = AspectConstraint()
    }.apply {
        rebindIconColor(hoveredState().map {
            if (it) EssentialPalette.RED else EssentialPalette.TEXT
        })
    }.onLeftClick {
        denyAction?.invoke()
        dismissNotification()
        timerEnabledState.set(true)
    } childOf this

    init {
        constrain {
            width = ChildBasedSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        }
    }
}