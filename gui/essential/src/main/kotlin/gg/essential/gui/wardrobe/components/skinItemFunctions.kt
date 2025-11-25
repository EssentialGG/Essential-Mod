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
package gg.essential.gui.wardrobe.components

import gg.essential.elementa.components.Window
import gg.essential.elementa.events.UIClickEvent
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.ContextOptionMenu
import gg.essential.gui.modals.ensurePrerequisites
import gg.essential.gui.overlay.launchModalFlow
import gg.essential.gui.sendCheckmarkNotification
import gg.essential.gui.skin.createSkinShareModal
import gg.essential.gui.wardrobe.Item
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.modals.SkinModal
import gg.essential.universal.UDesktop
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform

fun handleSkinLeftClick(skin: Item.SkinItem, wardrobeState: WardrobeState) {
    USound.playButtonPress()
    wardrobeState.selectedItem.set(skin)
    wardrobeState.outfitManager.updateOutfitSkin(skin.id, false)
}

fun handleSkinRightClick(skin: Item.SkinItem, wardrobeState: WardrobeState, event: UIClickEvent) {
    val options = mutableListOf<ContextOptionMenu.Item>(
        ContextOptionMenu.Option("Edit", EssentialPalette.PENCIL_7x7) {
            platform.pushModal { SkinModal.edit(it, skin) }
        },
        ContextOptionMenu.Option("Copy Link", EssentialPalette.LINK_8X7) {
            UDesktop.setClipboardString(skin.skin.url)
            sendCheckmarkNotification("Link copied to clipboard.")
        },
        ContextOptionMenu.Option("Share", EssentialPalette.UPLOAD_9X) {
            launchModalFlow(platform.createModalManager()) {
                ensurePrerequisites(social = true)
                modalManager.queueModal(createSkinShareModal(modalManager, skin))
            }
        },
        ContextOptionMenu.Option(if (skin.isFavorite) "Remove Favorite" else "Favorite", EssentialPalette.HEART_7X6) {
            wardrobeState.skinsManager.setFavoriteState(skin.id, !skin.isFavorite)
        },
    )

    if (wardrobeState.skinsManager.skins.get().size > 1) {
        options.add(ContextOptionMenu.Divider)
        options.add(ContextOptionMenu.Option("Delete", EssentialPalette.TRASH_9X, hoveredColor = EssentialPalette.TEXT_WARNING) {
            wardrobeState.skinsManager.openDeleteSkinModal(skin.id, wardrobeState.outfitManager)
        })
    }

    ContextOptionMenu.create(
        ContextOptionMenu.Position(event.absoluteX, event.absoluteY),
        Window.of(event.currentTarget),
        *options.toTypedArray()
    )
}
