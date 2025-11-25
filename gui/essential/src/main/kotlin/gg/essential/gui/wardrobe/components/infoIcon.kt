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

import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.layoutdsl.*
import gg.essential.util.bindHoverEssentialTooltip
import java.awt.Color

fun LayoutScope.infoIcon(
    tooltipText: String,
    modifier: Modifier = Modifier,
    wrapAtWidth: Float? = null,
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
) = infoIcon(stateOf(tooltipText), modifier, wrapAtWidth, position)

fun LayoutScope.infoIcon(
    tooltipText: State<String>,
    modifier: Modifier = Modifier,
    wrapAtWidth: Float? = null,
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
) {
    var infoIconStyle = modifier

    infoIconStyle = infoIconStyle.shadow(Color.BLACK)

    box(infoIconStyle.width(9f).height(9f).color(EssentialPalette.BUTTON).hoverColor(EssentialPalette.BUTTON_HIGHLIGHT).hoverScope()) {
        val color = Modifier.color(EssentialPalette.TEXT).hoverColor(EssentialPalette.TEXT_HIGHLIGHT)
        column(Arrangement.spacedBy(1f)) {
            box(Modifier.width(1f).height(1f).then(color))
            box(Modifier.width(1f).height(3f).then(color))
        }
    }.bindHoverEssentialTooltip(tooltipText.toV1(stateScope), wrapAtWidth = wrapAtWidth, position = position)
}
