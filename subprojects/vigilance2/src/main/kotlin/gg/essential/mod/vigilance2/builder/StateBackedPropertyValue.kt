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
package gg.essential.mod.vigilance2.builder

import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.PropertyValue

class StateBackedPropertyValue<T>(
    val state: MutableState<T>,
    override val writeDataToFile: Boolean,
) : PropertyValue() {
    override fun getValue(instance: Vigilant): Any? = state.getUntracked()

    override fun setValue(value: Any?, instance: Vigilant) {
        @Suppress("UNCHECKED_CAST")
        state.set(value as T)
    }
}
