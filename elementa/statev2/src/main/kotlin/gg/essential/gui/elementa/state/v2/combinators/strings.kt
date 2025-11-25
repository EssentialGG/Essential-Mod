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

import gg.essential.gui.elementa.state.v2.State

@Deprecated("Exists primarily for easier migration from State v1. Prefer using [State] lambda (with `memo` where necessary) instead.",
    replaceWith = ReplaceWith("State { this().contains(other(), ignoreCase = ignoreCase) }"))
@Suppress("DEPRECATION")
fun State<String>.contains(other: State<String>, ignoreCase: Boolean = false) =
    zip(other) { a, b -> a.contains(b, ignoreCase) }

@Deprecated("Exists primarily for easier migration from State v1. Prefer using [State] lambda (with `memo` where necessary) instead.",
    replaceWith = ReplaceWith("State { this().isEmpty() }"))
@Suppress("DEPRECATION")
fun State<String>.isEmpty() = map { it.isEmpty() }

@Deprecated("Exists primarily for easier migration from State v1. Prefer using [State] lambda (with `memo` where necessary) instead.",
    replaceWith = ReplaceWith("State { this().isNotEmpty() }"))
@Suppress("DEPRECATION")
fun State<String>.isNotEmpty() = map { it.isNotEmpty() }
