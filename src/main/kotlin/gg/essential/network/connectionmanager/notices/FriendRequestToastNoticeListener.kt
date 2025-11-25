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
package gg.essential.network.connectionmanager.notices

import gg.essential.api.gui.Slot
import gg.essential.gui.EssentialPalette
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.content.ConfirmDenyNotificationActionComponent
import gg.essential.gui.notification.markdownBody
import gg.essential.network.connectionmanager.ConnectionManager
import gg.essential.notices.NoticeType
import gg.essential.notices.model.Notice
import gg.essential.util.UUIDUtil
import gg.essential.util.colored
import gg.essential.util.thenAcceptOnMainThread
import java.util.*

class FriendRequestToastNoticeListener(
    private val connectionManager: ConnectionManager,
    private val noticeManager: NoticesManager,
) : NoticeListener {

    override fun noticeAdded(notice: Notice) {
        if (notice.type != NoticeType.FRIEND_REQUEST_TOAST) {
            return
        }
        val uuid = UUID.fromString(notice.metadata["uuid"] as? String ?: return)
        UUIDUtil.getName(uuid).thenAcceptOnMainThread { name ->
            Notifications.pushPersistentToast(
                title = "Friend Request",
                message = "",
                action = {},
                close = {},
            ) {
                markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} wants to be your friend.")
                val component = ConfirmDenyNotificationActionComponent(
                    confirmTooltip = "Accept",
                    denyTooltip = "Decline",
                    confirmAction = {
                        connectionManager.relationshipManager.acceptFriend(uuid)
                        connectionManager.socialMenuNewFriendRequestNoticeManager.clearUnseenFriendRequests(uuid)
                    },
                    denyAction = {
                        connectionManager.relationshipManager.denyFriend(uuid)
                        connectionManager.socialMenuNewFriendRequestNoticeManager.clearUnseenFriendRequests(uuid)
                    },
                    dismissNotification = dismissNotification,
                )
                withCustomComponent(
                    slot = Slot.ICON,
                    component = EssentialPalette.ENVELOPE_9X7.create()
                )
                withCustomComponent(
                    slot = Slot.ACTION,
                    component = component,
                )
            }
            noticeManager.dismissNotice(notice.id)
        }
    }

    override fun noticeRemoved(notice: Notice) {
    }

    override fun onConnect() {}
}