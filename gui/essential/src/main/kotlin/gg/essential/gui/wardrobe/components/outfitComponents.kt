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

import gg.essential.api.gui.Slot
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.LoadingIcon
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.notification.Notifications
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.universal.USound
import gg.essential.vigilance.utils.onLeftClick

fun LayoutScope.outfitAddButton(state: WardrobeState, modifier: Modifier = Modifier) {
    val adding = mutableStateOf(false)
    val limitState = stateBy {
        state.outfitItems().size >= state.settings.outfitsLimit()
    }
    val buttonModifierState = limitState.map { atLimit ->
        if (atLimit) {
            Modifier.color(EssentialPalette.BLUE_BUTTON_DISABLED)
                .hoverTooltip("Outfit limit reached")
        } else {
            Modifier.color(EssentialPalette.BLUE_BUTTON).hoverColor(EssentialPalette.BLUE_BUTTON_HOVER)
                .hoverTooltip("New Outfit")
        }
    }
    val iconColorState = limitState.map { atLimit ->
        if (atLimit) EssentialPalette.TEXT_DISABLED else EssentialPalette.TEXT_HIGHLIGHT
    }
    box(
        modifier.height(17f).width(17f).shadow().then(buttonModifierState).hoverScope()
    ) {
        if_(adding, cache = false) {
            LoadingIcon(1.0)()
        } `else` {
            icon(EssentialPalette.PLUS_5X, Modifier.color(iconColorState).shadow())
        }
    }.onLeftClick {
        if (limitState.get() || adding.get()) {
            return@onLeftClick
        }
        USound.playButtonPress()
        adding.set(true)
        state.outfitManager.addOutfit { newOutfit ->
            adding.set(false)
            if (newOutfit == null) {
                Notifications.push("Unable to add new outfit.", "Please try again later.")
            } else {
                Notifications.push("Outfit created", "") {
                    withCustomComponent(Slot.ICON, EssentialPalette.COSMETICS_10X7.create())
                }

                state.outfitManager.setSelectedOutfit(newOutfit.id)
            }
        }
    }
}