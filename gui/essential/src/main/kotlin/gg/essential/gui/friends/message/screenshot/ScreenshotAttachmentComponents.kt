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
package gg.essential.gui.friends.message.screenshot

import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.pixels
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.MenuButton
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.animateWidth
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedMaxHeight
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.fillHeight
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.icon
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.universal.USound
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color

fun LayoutScope.screenshotAttachmentUploadBox(
    screenshotAttachmentManager: ScreenshotAttachmentManager
) {
    row(Modifier.childBasedMaxHeight(9f).color(EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT)) {
        spacer(width = 10f)
        icon(EssentialPalette.PICTURES_10X10, Modifier.color(EssentialPalette.TEXT))
        spacer(width = 6f)
        text("Uploading...", Modifier.color(EssentialPalette.TEXT).shadow(Color.BLACK))
        spacer(width = 11f)
        box(Modifier.width(100f).height(3f).color(EssentialPalette.GUI_BACKGROUND).shadow(Color.BLACK)) {
            box(
                Modifier.animateWidth(
                    screenshotAttachmentManager.totalProgressPercentage.map { { it.pixels } },
                    0.5f,
                    Animations.LINEAR
                ).fillHeight().color(EssentialPalette.TOAST_PROGRESS).alignHorizontal(Alignment.Start)
            )
        }
        spacer(width = 10f)
    }.addUpdateFunc { _, _ ->
        screenshotAttachmentManager.updateProgress()
    }
}

fun LayoutScope.screenshotAttachmentDoneButton(screenshotAttachmentManager: ScreenshotAttachmentManager) {
    box(
        Modifier.width(43f).height(17f)
            .color(MenuButton.BLUE.buttonColor)
            .hoverColor(MenuButton.LIGHT_BLUE.buttonColor)
            .shadow()
            .hoverScope()
    ) {
        text(
            "Done",
            Modifier.shadow(),
            centeringContainsShadow = false
        )
    }.onLeftClick {
        USound.playButtonPress()
        screenshotAttachmentManager.isPickingScreenshots.set(false)
    }
}
