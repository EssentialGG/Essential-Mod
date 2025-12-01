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

import gg.essential.elementa.UIComponent
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.not
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedWidth
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.hoverTooltip
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.whenTrue
import gg.essential.gui.layoutdsl.width

fun LayoutScope.collapsibleButton(
    modifier: Modifier = Modifier,
    collapsed: State<Boolean>,
    collapsedTooltipText: State<String>,
    textContainer: LayoutScope.() -> Unit,
    iconContainer: LayoutScope.() -> Unit,
    containerSpacing: Float = 4f,
): UIComponent {
    return box(
        Modifier.whenTrue(collapsed, Modifier.width(17f).hoverTooltip(collapsedTooltipText), Modifier.childBasedWidth(10f))
            .height(17f)
            .color(EssentialPalette.GRAY_BUTTON)
            .hoverColor(EssentialPalette.GRAY_BUTTON_HOVER)
            .shadow()
            .hoverScope()
            .then(modifier)
    ) {
        row(Modifier.alignVertical(Alignment.Center(true)), Arrangement.spacedBy(containerSpacing)) {
            if_(!collapsed) {
                textContainer()
            }
            iconContainer()
        }
    }
}
