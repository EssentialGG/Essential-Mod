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
package gg.essential.commands.impl

import gg.essential.Essential
import gg.essential.commands.engine.EssentialFriend
import gg.essential.gui.modals.ensurePrerequisites
import gg.essential.network.connectionmanager.EarlyResponseHandler
import gg.essential.util.GuiUtil
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

fun handleMessageCommand(friend: EssentialFriend, message: String, handler: EarlyResponseHandler) {
    GuiUtil.launchModalFlow {
        try {
            ensurePrerequisites(social = true)
        } catch (exception: CancellationException) {
            handler.accept(Optional.empty())
            throw exception
        }
        Essential.getInstance().connectionManager.chatManager.sendMessage(friend.channel.id, message, handler)
    }
}