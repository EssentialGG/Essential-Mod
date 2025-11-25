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
package gg.essential.gui.notification

import gg.essential.api.gui.NotificationType
import gg.essential.api.gui.Slot
import gg.essential.gui.EssentialPalette
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.shadow
import gg.essential.sps.SpsAddress
import gg.essential.util.CachedAvatarImage
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.UuidNameLookup
import gg.essential.util.colored
import gg.essential.util.thenAcceptOnMainThread
import java.awt.Color
import java.util.UUID

fun sendTosNotification(viewButtonAction: () -> Unit) {
    Notifications.pushPersistentToast(
        "Terms of Service",
        "This feature requires you to accept the Essential ToS.",
        action = {},
        close = {},
    ) {
        uniqueId = object {}.javaClass
        withCustomComponent(Slot.ICON, EssentialPalette.ROUND_WARNING_7X.create())
        withCustomComponent(Slot.ACTION, toastButton("View", action = viewButtonAction))
    }
}

fun sendCheckoutFailedNotification() {
    Notifications.pushPersistentToast(
        "Error",
        "An issue occurred while trying to send you to checkout. Please try again later.",
        {},
        {},
    ) {
        type = NotificationType.ERROR
    }
}

fun sendSpsInviteNotification(uuid: UUID) =
    UuidNameLookup.getName(uuid).thenAcceptOnMainThread { name ->
        sendSpsInviteNotification(uuid, name)
    }

fun sendSpsInviteNotification(uuid: UUID, name: String) {
    Notifications.pushPersistentToast(name, "Sent you an invite\nto their world.", {}, {}) {
        withCustomComponent(Slot.ICON, CachedAvatarImage.create(uuid))

        val button = toastButton(
            "Join",
            backgroundModifier = Modifier.color(EssentialPalette.BLUE_BUTTON).hoverColor(EssentialPalette.BLUE_BUTTON_HOVER).shadow(Color.BLACK),
            textModifier = Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(EssentialPalette.TEXT_SHADOW)
        ) {
            dismissNotification()
            platform.connectToServer(name, SpsAddress(uuid).toString())
        }
        withCustomComponent(Slot.ACTION, button)
    }
}

fun sendOutgoingSpsInviteNotification(name: String) {
    Notifications.push("", "") {
        iconAndMarkdownBody(EssentialPalette.ENVELOPE_9X7.create(), "${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} invited")
    }
}
