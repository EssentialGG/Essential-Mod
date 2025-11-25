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
package gg.essential.model.util

import java.time.Instant

// Ideally we'd be using kotlin-multiplatform, but we have moved away from it for the time being because it was too
// unreliable.
/*
actual typealias Instant = Instant

actual fun now(): Instant = Instant.now()
actual fun instant(epochMillis: Long): Instant = Instant.ofEpochMilli(epochMillis)

actual fun instantFromIso8601(str: String): Instant =
    Instant.parse(str)
actual fun instantToIso8601(instant: Instant): String =
    instant.toString()
 */
typealias Instant = Instant

fun now(): Instant = Instant.now()
fun instant(epochMillis: Long): Instant = Instant.ofEpochMilli(epochMillis)

fun instantFromIso8601(str: String): Instant =
    Instant.parse(str)
fun instantToIso8601(instant: Instant): String =
    instant.toString()
