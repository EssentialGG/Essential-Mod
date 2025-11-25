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

import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.IconButton
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.remove
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignBoth
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.components.ScreenshotPreview
import gg.essential.gui.screenshot.components.screenshotImage
import gg.essential.vigilance.utils.onLeftClick

class RemoveableScreenshotPreview(
    screenshotId: ScreenshotId,
    desiredImageSize: MutableState<Pair<Int, Int>>,
    private val screenshotAttachmentManager: ScreenshotAttachmentManager,
) : ScreenshotPreview(
    screenshotId,
    desiredImageSize,
) {

    init {
        this.layout {
            box(Modifier.fillParent().color(EssentialPalette.GUI_BACKGROUND)) {
                screenshotImage(imgTexture, Modifier.alignBoth(Alignment.TrueCenter))
                box(Modifier.width(16f).height(16f).alignHorizontal(Alignment.End).alignVertical(Alignment.Start)) {
                    IconButton(
                        EssentialPalette.CANCEL_5X,
                        tooltipText = "Remove attachment",
                        tooltipBelowComponent = false
                    ).setDimension(IconButton.Dimension.Fixed(12f, 12f))()
                        .onLeftClick {
                            screenshotAttachmentManager.selectedImages.remove(screenshotId)
                        }
                }
            }
        }
    }

}