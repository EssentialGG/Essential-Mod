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
import gg.essential.gui.elementa.state.v2.Observer
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.memo

/** Maps this state into a new state */
@Deprecated("This method always applies `memo` even though it is often unnecessary. " +
        "Use `letState` instead, and explicitly call `.memo()` on the result only where required.")
fun <T, U> State<T>.map(mapper: (T) -> U): State<U> {
    return memo { mapper(get()) }
}

/**
 * Derives a new [State] from this [State].
 *
 * This method is equivalent to `.let { state -> State { block(state()) } }`.
 *
 * Note: For repeated or more complex derivations, an explicit [State] or [memo] lambda is likely easier to use.
 */
inline fun <T, U> State<T>.letState(crossinline block: Observer.(T) -> U): State<U> {
    val sourceState = this
    return State { block(sourceState()) }
}

/** Maps this mutable state into a new mutable state. */
@Deprecated("This method always applies `memo` even though it is often unnecessary. " +
        "Use `bimapState` or `bimapMemo` instead.")
fun <T, U> MutableState<T>.bimap(map: (T) -> U, unmap: (U) -> T): MutableState<U> {
    return bimapMemo(map, unmap)
}

/**
 * Derives a new [MutableState] from this [MutableState].
 *
 * This variant uses [memo] internally. If this is not required, use [bimapState] instead.
 */
fun <T, U> MutableState<T>.bimapMemo(map: (T) -> U, unmap: (U) -> T): MutableState<U> {
    val sourceState = this
    return object : MutableState<U>, State<U> by (memo { map(sourceState()) }) {
        override fun set(mapper: (U) -> U) {
            sourceState.set { unmap(mapper(map(it))) }
        }
    }
}

/**
 * Derives a new [MutableState] from this [MutableState].
 *
 * @see [bimapMemo]
 */
fun <T, U> MutableState<T>.bimapState(map: (T) -> U, unmap: (U) -> T): MutableState<U> {
    val sourceState = this
    return object : MutableState<U> {
        override fun Observer.get(): U = map(sourceState())
        override fun set(mapper: (U) -> U) = sourceState.set { unmap(mapper(map(it))) }
    }
}

/** Zips this state with another state */
@Deprecated("Exists primarily for easier migration from State v1. Prefer using [State] lambda (with `memo` where necessary) instead.",
    replaceWith = ReplaceWith("State { Pair(this(), other()) }"))
@Suppress("DEPRECATION")
fun <T, U> State<T>.zip(other: State<U>): State<Pair<T, U>> = zip(other, ::Pair)

/** Zips this state with another state using [mapper] */
@Deprecated("Exists primarily for easier migration from State v1. Prefer using [State] lambda (with `memo` where necessary) instead.",
    replaceWith = ReplaceWith("State { mapper(this(), other()) }"))
fun <T, U, V> State<T>.zip(other: State<U>, mapper: (T, U) -> V): State<V> {
    return memo { mapper(this@zip(), other()) }
}
