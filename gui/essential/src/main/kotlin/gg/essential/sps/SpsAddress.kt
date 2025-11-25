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
package gg.essential.sps

import java.util.*

/** @see [SPS_TLD] */
data class SpsAddress(val host: UUID) {
    override fun toString(): String {
        return "$host$SPS_TLD"
    }

    companion object {
        @JvmStatic
        fun parse(address: String): SpsAddress? {
            if (!address.endsWith(SPS_TLD)) return null
            val uuidStr = address.substring(0, address.length - SPS_TLD.length)
            val uuid = try {
                UUID.fromString(uuidStr)
            } catch (ignored: IllegalArgumentException) {
                return null
            }
            return SpsAddress(uuid)
        }
    }
}
