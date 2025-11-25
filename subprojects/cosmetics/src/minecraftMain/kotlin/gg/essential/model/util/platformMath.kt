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

import java.security.MessageDigest
import java.util.*

// Ideally we'd be using kotlin-multiplatform, but we have moved away from it for the time being because it was too
// unreliable.
/*
internal actual fun cbrt(value: Float): Float = Math.cbrt(value.toDouble()).toFloat()

internal actual fun base64Encode(value: ByteArray): String =
    Base64.getEncoder().encodeToString(value)
internal actual fun base64Decode(value: String): ByteArray =
    Base64.getDecoder().decode(value)
internal actual fun md5Hex(value: ByteArray): String =
    MessageDigest.getInstance("MD5").digest(value).joinToString("") { "%02x".format(it) }
 */
internal fun cbrt(value: Float): Float = Math.cbrt(value.toDouble()).toFloat()

internal fun base64Encode(value: ByteArray): String =
    Base64.getEncoder().encodeToString(value)
internal fun base64Decode(value: String): ByteArray =
    Base64.getDecoder().decode(value)
internal fun md5Hex(value: ByteArray): String =
    MessageDigest.getInstance("MD5").digest(value).joinToString("") { "%02x".format(it) }
