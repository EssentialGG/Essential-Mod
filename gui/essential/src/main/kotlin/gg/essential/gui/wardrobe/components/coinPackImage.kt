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

import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.layoutdsl.*
import gg.essential.network.connectionmanager.coins.CoinsManager

fun LayoutScope.coinPackImage(coinsManager: CoinsManager, numberOfCoins: Int, modifier: Modifier = Modifier) = coinPackImage(coinsManager, stateOf(numberOfCoins), modifier)

fun LayoutScope.coinPackImage(coinsManager: CoinsManager, numberOfCoins: State<Int>, modifier: Modifier = Modifier) = coinPackImage(stateBy {
    val bundles = coinsManager.pricing()
    val coins = numberOfCoins()
    // Select the largest bundle that has less or equal the number of coins we want
    // If we have too little coins, use the "fallback" image for low amounts of coins
    // After 1.3/1.4 this should be replaced by an infra provided "fallback"
    bundles.filter { it.numberOfCoins <= coins }.maxByOrNull { it.numberOfCoins }?.iconFactory ?: EssentialPalette.COIN_BUNDLE_0_999
}, modifier)

fun LayoutScope.coinPackImage(image: ImageFactory, modifier: Modifier = Modifier) = coinPackImage(stateOf(image), modifier)

fun LayoutScope.coinPackImage(image: State<ImageFactory>, modifier: Modifier = Modifier) {
    bind(image) {
        image(it, Modifier.width(80f).height(80f).then(modifier))
    }
}
