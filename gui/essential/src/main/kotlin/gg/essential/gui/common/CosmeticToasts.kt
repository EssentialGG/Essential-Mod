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
package gg.essential.gui.common

import gg.essential.api.gui.NotificationType
import gg.essential.api.gui.Slot
import gg.essential.config.EssentialConfig
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.markdownBody
import gg.essential.gui.notification.toastButton
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.util.colored

@JvmOverloads
fun sendCosmeticUnlockedToast(cosmetic: Cosmetic, settings: State<List<CosmeticSetting>> = stateOf(listOf())) {
    Notifications.push("", "") {
        val component = UIBlock(EssentialPalette.BUTTON).constrain {
            width = 28.pixels
            height = AspectConstraint()
        }

        component.layout {
            CosmeticPreview(cosmetic, settings)(Modifier.fillParent())
        }

        markdownBody("${cosmetic.displayName.colored(EssentialPalette.TEXT_HIGHLIGHT)} has been added to your wardrobe.")

        withCustomComponent(Slot.SMALL_PREVIEW, component)
    }
}

fun sendEmotesDisabledNotification() {
    Notifications.pushPersistentToast("Emotes Disabled", "Do you want to\nenable emotes?", {}, {}) {
        type = NotificationType.WARNING
        uniqueId = object {}.javaClass
        withCustomComponent(Slot.ACTION, toastButton("Enable") { EssentialConfig.disableEmotes = false })
    }
}

fun sendCosmeticsDisabledNotification() {
    Notifications.pushPersistentToast("Cosmetics Disabled", "Do you want to\nenable cosmetics?", {}, {}) {
        type = NotificationType.WARNING
        uniqueId = object {}.javaClass
        withCustomComponent(Slot.ACTION, toastButton("Enable") { EssentialConfig.disableCosmetics = false })
    }
}

fun sendCosmeticsHiddenNotification() {
    Notifications.pushPersistentToast("Cosmetics Hidden", "Do you want to show\nyour cosmetics?", {}, {}) {
        type = NotificationType.WARNING
        uniqueId = object {}.javaClass
        withCustomComponent(Slot.ACTION, toastButton("Show") { EssentialConfig.ownCosmeticsVisibleStateWithSource.set(true to EssentialConfig.CosmeticsVisibilitySource.UserWithoutNotification) })
    }
}

