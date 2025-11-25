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
package gg.essential.cosmetics.state

import gg.essential.model.ParticleSystem

/** A wrapper which becomes invalid when this particular cosmetic instance is unequipped. */
class WearableLocator(override val parent: ParticleSystem.Locator,
                      var wearableVisible: Boolean) : ParticleSystem.Locator by parent {
    private var wearableIsValid = true
    override var isValid: Boolean
        get() = parent.isValid && wearableIsValid
        set(value) { wearableIsValid = value }
    override val isVisible: Boolean
        get() = parent.isVisible && wearableVisible
}
