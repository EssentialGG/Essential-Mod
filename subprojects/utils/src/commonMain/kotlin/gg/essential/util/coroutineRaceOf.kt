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
package gg.essential.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select

// See also https://github.com/Kotlin/kotlinx.coroutines/issues/2867
/**
 * Runs the given functions concurrently. Once any one of them completes, its value is returned and
 * all others are cancelled.
 */
suspend fun <T> raceOf(vararg funcs: suspend CoroutineScope.() -> T): T {
    return coroutineScope {
        select {
            for (func in funcs) {
                async(block = func).onAwait { it }
            }
        }
            .also { coroutineContext.cancelChildren() }
    }
}
