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
package gg.essential.gui.elementa.state.v2.combinators

import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State

@Deprecated("Exists primarily for easier migration from State v1. Prefer using [State] lambda (with `memo` where necessary) instead.",
    replaceWith = ReplaceWith("State { this() && other() }"))
@Suppress("DEPRECATION")
infix fun State<Boolean>.and(other: State<Boolean>) =
    zip(other) { a, b -> a && b }

@Deprecated("Exists primarily for easier migration from State v1. Prefer using [State] lambda (with `memo` where necessary) instead.",
    replaceWith = ReplaceWith("State { this() || other() }"))
@Suppress("DEPRECATION")
infix fun State<Boolean>.or(other: State<Boolean>) =
    zip(other) { a, b -> a || b }

/**
 * Creates a new [State] which has the inverse value of this [State].
 *
 * This is mostly a convenience method so one can write `if_(!myState) {` instead of the more verbose
 * `if_({ !myState() }) {`. Both are equivalent.
 * This method shouldn't be overused though. If the expression is more complex, the verbose version is usually
 * preferred because it generalizes much better.
 */
operator fun State<Boolean>.not() = letState { !it }

operator fun MutableState<Boolean>.not() = bimapState({ !it }, { !it })
