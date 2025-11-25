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
package gg.essential.image

import java.nio.file.Path
import kotlin.io.path.inputStream

object PNGFile {
    // https://www.w3.org/TR/png/#3PNGsignature
    private val PNG_SIGNATURE = arrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    @JvmStatic
    fun hasValidSignature(path: Path): Boolean {
        val bytes = ByteArray(8)
        path.inputStream().use {
            it.read(bytes)
        }

        return hasValidSignature(bytes)
    }

    @JvmStatic
    fun hasValidSignature(bytes: ByteArray): Boolean {
        val signatureBytes = bytes.take(8).toTypedArray()
        return signatureBytes.contentEquals(PNG_SIGNATURE)
    }
}
