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

import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.modals.CoinsPurchaseModal
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick

fun LayoutScope.coinsButton(state: WardrobeState, modifier: Modifier = Modifier) {
    box(Modifier.childBasedMaxWidth(9f).height(17f).color(EssentialPalette.COINS_BLUE).hoverColor(EssentialPalette.COINS_BLUE_HOVER).shadow().hoverScope() then modifier) {
        // We need to align horizontally to end, because the button has a min width
        coinsText(state, Modifier.alignVertical(Alignment.Center(true)).alignHorizontal(Alignment.End(9f)))
        // Used to force the width of the box to the width as if there were 4 digits
        coinsText(stateOf(9999), Modifier.effect { ScissorEffect(0f, 0f, 0f, 0f) })
    }.onLeftClick {
        USound.playButtonPress()
        CoinsPurchaseModal.open(state)
    }
}
