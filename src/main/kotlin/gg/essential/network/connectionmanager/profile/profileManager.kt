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
package gg.essential.network.connectionmanager.profile

import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.collections.MutableTrackedList
import gg.essential.gui.elementa.state.v2.mapList
import gg.essential.gui.elementa.state.v2.withSystemTime
import gg.essential.network.connectionmanager.social.ProfileSuspension

fun filterActiveSuspensions(suspensions: MutableState<MutableTrackedList<ProfileSuspension>>): ListState<ProfileSuspension> =
    suspensions.mapList { list -> withSystemTime { now -> list.filter { suspension -> suspension.isActive(now) } } }