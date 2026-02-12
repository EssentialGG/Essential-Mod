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
package gg.essential.network.connectionmanager.cosmetics

import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*

class WardrobeSettings {

    private val _outfitsLimit: MutableState<Int?> = mutableStateOf(null)
    private val _skinsLimit: MutableState<Int?> = mutableStateOf(null)
    private val _giftingCoinSpendRequirement: MutableState<Int?> = mutableStateOf(null)
    private val _youNeedMinimumAmount: MutableState<Int?> = mutableStateOf(null)

    val outfitsLimit = _outfitsLimit.letState { it ?: 0 }
    val skinsLimit = _skinsLimit.letState { it ?: 0 }
    val giftingCoinSpendRequirement = _giftingCoinSpendRequirement.letState { it ?: 0 }
    val youNeedMinimumAmount = _youNeedMinimumAmount.letState { it ?: 0 }

    fun populateOutfitsLimit(limit: Int) =
        _outfitsLimit.set(limit)

    fun populateSkinsLimit(limit: Int) =
        _skinsLimit.set(limit)

    fun populateGiftingCoinSpendRequirement(requirement: Int) =
        _giftingCoinSpendRequirement.set(requirement)

    fun populateYouNeedMinimumAmount(amount: Int) =
        _youNeedMinimumAmount.set(amount)

    fun isSettingsLoaded(): Boolean {
        return _outfitsLimit.getUntracked() != null &&
                _skinsLimit.getUntracked() != null &&
                _giftingCoinSpendRequirement.getUntracked() != null &&
                _youNeedMinimumAmount.getUntracked() != null
    }

}
