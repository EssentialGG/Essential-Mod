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
package gg.essential.gui.elementa.state.v2

/**
 * Creates a state that derives its value using the given [block]. The value of any state may be accessed within this
 * block via [StateByScope.invoke]. These accesses are tracked and the block is automatically re-evaluated whenever any
 * one of them changes.
 */
@Suppress("DEPRECATION")
@Deprecated("Use `memo` (result is cached) or `State` lambda (result is not cached)")
fun <T> stateBy(block: StateByScope.() -> T): State<T> {
    return memo {
        val scope = object : StateByScope {
            override fun <T> State<T>.invoke(): T {
                return with(this@memo) { get() }
            }
        }
        block(scope)
    }
}

@Deprecated("Superseded by `Observer`")
interface StateByScope {
    operator fun <T> State<T>.invoke(): T
}
