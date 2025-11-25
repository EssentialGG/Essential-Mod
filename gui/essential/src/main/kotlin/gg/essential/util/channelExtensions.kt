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

import com.sparkuniverse.toolbox.chat.enums.ChannelType
import com.sparkuniverse.toolbox.chat.model.Channel
import java.util.UUID

fun Channel.getOtherUser(): UUID? =
    if (type == ChannelType.DIRECT_MESSAGE) members.firstOrNull { it != USession.activeNow().uuid } else null

private val BOT_UUID = UUID.fromString("cd899a14-de78-4de8-8d31-9d42fff31d7a") // EssentialBot
fun Channel.isAnnouncement(): Boolean =
    this.type == ChannelType.ANNOUNCEMENT || BOT_UUID in members

