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
package gg.essential.network.connectionmanager.social

import gg.essential.gui.elementa.state.v2.ObservedInstant
import gg.essential.model.util.Instant

data class Suspension(val reason: String, val expiresAt: Instant?, val unseen: Boolean = false) {

    fun isActive(now: ObservedInstant) = expiresAt == null || now.isBefore(expiresAt)
    fun isActiveNow() = expiresAt?.isAfter(Instant.now()) ?: true

    val isPermanent: Boolean
        get() = expiresAt == null

    val isTOSSuspension: Boolean
        get() = reason == USER_BROKE_TOS_REASON

    companion object {

        const val USER_BROKE_TOS_REASON = "TOS_BAN"
    }
}