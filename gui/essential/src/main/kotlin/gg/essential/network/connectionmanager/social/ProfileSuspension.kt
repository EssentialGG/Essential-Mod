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

import gg.essential.connectionmanager.common.model.profile.ProfilePunishmentStatus
import gg.essential.gui.elementa.state.v2.ObservedInstant
import gg.essential.network.connectionmanager.common.model.profile.PunishmentType
import java.time.Instant
import java.util.*

sealed interface ProfileSuspension {

    data class PermanentBan(override val user: UUID) : ProfileSuspension {

        override fun isActive(now: ObservedInstant) = true

        override fun isActiveNow() = true
    }

    data class SocialBan(override val user: UUID, val expiresAt: Instant) : ProfileSuspension {

        override fun isActive(now: ObservedInstant) = now.isBefore(expiresAt)

        override fun isActiveNow() = expiresAt.isAfter(Instant.now())
    }

    val user: UUID

    fun isActive(now: ObservedInstant): Boolean

    fun isActiveNow(): Boolean

    companion object {

        fun fromInfra(user: UUID, punishment: ProfilePunishmentStatus): ProfileSuspension {
            return when (punishment.punishmentType) {
                PunishmentType.PERMANENT_BAN -> PermanentBan(user)
                PunishmentType.SOCIAL_BAN -> SocialBan(user, Instant.ofEpochMilli(punishment.expiresAt ?: 0L))
            }
        }
    }
}