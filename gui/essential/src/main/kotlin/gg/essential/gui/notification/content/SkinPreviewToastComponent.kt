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
package gg.essential.gui.notification.content

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.mod.Skin
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.preview.PerspectiveCamera
import gg.essential.util.GuiEssentialPlatform.Companion.platform

class SkinPreviewToastComponent(skin: Skin) : UIContainer() {

    private val background by UIBlock(EssentialPalette.BUTTON).constrain {
        width = 26.pixels
        height = AspectConstraint()
    } childOf this

    private val preview by platform.newUIPlayer(
        camera = stateOf(PerspectiveCamera.forCosmeticSlot(CosmeticSlot.FULL_BODY)),
        profile = stateOf(Pair(skin, null)),
        cosmetics = stateOf(emptyMap()),
    ).constrain {
        width = 100.percent
        height = 100.percent
    } childOf background

    init {
        constrain {
            width = ChildBasedSizeConstraint() + 1.pixel
            height = ChildBasedSizeConstraint() + 1.pixel
        }
    }
}
