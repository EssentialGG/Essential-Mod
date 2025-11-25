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
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.EssentialPalette
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.BasicYModifier
import gg.essential.gui.layoutdsl.FloatPosition
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedHeight
import gg.essential.gui.layoutdsl.childBasedWidth
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.effect
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.floatingBox
import gg.essential.gui.layoutdsl.flowContainer
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.screenshot.DateRange

fun LayoutScope.screenshotDateGroup(
    range: DateRange,
    startTime: Long,
    navigation: UIComponent,
    scrollerParent: UIComponent,
    modifier: Modifier = Modifier,
    block: LayoutScope.() -> Unit = {},
) {
    fun titleModifier(container: UIComponent) = Modifier
        .effect { ScissorEffect(scrollerParent) }
        .then(BasicYModifier {
            // Position the title in the center of navigation
            (CenterConstraint() boundTo navigation)
                // but force it to stay within the content's bounds, so the titles of different groups never overlap
                .coerceIn(CenterConstraint(), 0.pixels(alignOpposite = true) boundTo container)
        })

    column(Modifier.fillWidth().childBasedHeight().then(modifier)) {
        val container = containerDontUseThisUnlessYouReallyHaveTo
        box(Modifier.fillWidth().height(screenshotGroupHeaderHeight)) {
            // The divider line
            box(Modifier.fillWidth().height(1f).color(EssentialPalette.COMPONENT_BACKGROUND))
            // Box around the text which "erases" the divider line around the text
            box(Modifier.childBasedWidth(4f).height(1f).color(EssentialPalette.GUI_BACKGROUND)) {
                // Floating with custom modifiers so the text can take its place in the navigation bar
                floatingBox(titleModifier(container)) {
                    text(range.getName(startTime))
                }
            }
        }
        flowContainer(
            Modifier.fillWidth(),
            xSpacingMin = screenshotPadding,
            ySpacing = screenshotPadding,
            itemArrangement = Arrangement.spacedBy(screenshotPadding, FloatPosition.START),
            block = block,
        )
    }
}
