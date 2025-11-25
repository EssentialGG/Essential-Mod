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
package gg.essential.gui.screenshot.components

import gg.essential.elementa.UIComponent
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.layoutdsl.BasicHeightModifier
import gg.essential.gui.layoutdsl.BasicWidthModifier
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.screenshot.constraints.AspectPreservingFillConstraint
import gg.essential.gui.screenshot.image.ScreenshotImage
import gg.essential.gui.screenshot.providers.RegisteredTexture

fun LayoutScope.screenshotImage(
    texture: State<RegisteredTexture?>,
    modifier: Modifier = Modifier,
    block: LayoutScope.() -> Unit = {},
): UIComponent {
    val isScreenshotErrored = texture.map { it?.error == true }

    val viewAspectRatio = texture.map { texture ->
        if (texture == null || texture.error) 16 / 9f
        else texture.imageWidth / texture.imageHeight.toFloat()
    }.toV1(stateScope)

    val width = BasicWidthModifier { AspectPreservingFillConstraint(viewAspectRatio) }
    val height = BasicHeightModifier { AspectPreservingFillConstraint(viewAspectRatio) }

    return box(width.then(height).then(modifier)) {
        if_(isScreenshotErrored) {
            invalidScreenshotView()
        } `else` {
            ScreenshotImage(texture.map { it?.identifier })(Modifier.fillParent())

            block()
        }
    }
}
