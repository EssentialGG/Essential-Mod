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
package gg.essential.gui.common

import gg.essential.elementa.state.State
import gg.essential.elementa.state.v2.ReferenceHolder

@Deprecated("Use StateV2 instead")
fun <T> State<T>.onSetValueAndNow(listener: (T) -> Unit) = onSetValue(listener).also { listener(get()) }

@Deprecated("See `State.onSetValue`. Use `stateBy`/`effect` instead.")
fun <T> gg.essential.gui.elementa.state.v2.State<T>.onSetValueAndNow(owner: ReferenceHolder, listener: (T) -> Unit) =
    onSetValue(owner, listener).also { listener(get()) }

@Deprecated("Use StateV2 instead")
operator fun State<Boolean>.not() = map { !it }