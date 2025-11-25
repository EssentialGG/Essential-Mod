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
package gg.essential.gui.friends.message.v2

import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.notification.Notifications
import gg.essential.gui.util.hoveredState
import gg.essential.gui.wardrobe.modals.SkinModal
import gg.essential.mod.Skin
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.preview.PerspectiveCamera
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.vigilance.utils.onLeftClick

class SkinEmbedImpl(
    skin: Skin,
    messageWrapper: MessageWrapper,
) : SkinEmbed(skin, messageWrapper) {

    init {
        colorState.rebind(hoveredState().map { if (it) EssentialPalette.GRAY_BUTTON_HOVER else EssentialPalette.GRAY_BUTTON })

        constrain {
            width = ChildBasedMaxSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        }

        val ui3DPlayer = platform.newUIPlayer(
            camera = stateOf(PerspectiveCamera.forCosmeticSlot(CosmeticSlot.FULL_BODY)),
            profile = stateOf(Pair(skin, null)),
            cosmetics = stateOf(emptyMap()),
        )

        bubble.layoutAsBox(Modifier.width(103f).height(106f).hoverScope()) {
            ui3DPlayer(Modifier.height(73f))
            icon(EssentialPalette.EXPAND_6X, Modifier.alignBoth(Alignment.End(6f)).color(EssentialPalette.TEXT).hoverColor(EssentialPalette.TEXT_HIGHLIGHT))
        }

        val skinsManager = platform.skinsManager

        bubble.onLeftClick {
            if (skinsManager.skins.get().size >= platform.wardrobeSettings.skinsLimit.get()) {
                Notifications.push("Error adding skin", "You have the maximum number of skins!")
                return@onLeftClick
            }
            USound.playButtonPress()
            platform.pushModal {
                SkinModal.add(it, skin, initialName = skinsManager.getNextIncrementalSkinName())
            }
        }

        bubble.onRightClick {
            messageWrapper.openOptionMenu(it, this@SkinEmbedImpl)
        }
    }

    override fun beginHighlight() {}
    override fun releaseHighlight() {}

}
