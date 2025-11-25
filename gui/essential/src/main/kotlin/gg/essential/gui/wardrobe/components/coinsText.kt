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

import gg.essential.elementa.UIComponent
import gg.essential.elementa.constraints.animation.*
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.network.connectionmanager.coins.CoinsManager

fun LayoutScope.coinsText(coins: Int, modifier: Modifier = Modifier) = coinsText(stateOf(coins), modifier)

fun LayoutScope.coinsText(state: WardrobeState, modifier: Modifier = Modifier) {
    val coinsVisual = mutableStateOf(state.coins.get()) // grab the current state

    val textComponent = coinsText(coinsVisual, modifier)

    val propertyHack = object {
        var coinsProperty: Int
            get() = coinsVisual.get()
            set(value) = coinsVisual.set(value)
    }::coinsProperty

    state.areCoinsVisuallyFrozen.zip(state.coins).onSetValue(this.stateScope) { (frozen, coins) ->
        if (!frozen) {
            with(textComponent) {
                propertyHack.animate(Animations.OUT_EXP, 2.5f, state.coins.get())
            }
        }
    }
}

fun LayoutScope.coinsText(coinsState: State<Int>, modifier: Modifier = Modifier): UIComponent {
    val textState = coinsState.map { CoinsManager.COIN_FORMAT.format(it) }.toV1(this.stateScope)
    return row(modifier) {
        spacer(width = 1f)
        text(textState, Modifier.shadow(EssentialPalette.TEXT_SHADOW))
        spacer(width = 5f)
        icon(EssentialPalette.COIN_7X)
    }
}
