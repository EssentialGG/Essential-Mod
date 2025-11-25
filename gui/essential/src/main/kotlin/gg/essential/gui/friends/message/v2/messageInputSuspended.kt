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

import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.childBasedHeight
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.image
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.wrappedText

fun LayoutScope.messageInputSuspended(name: State<String>, modifier: Modifier = Modifier) {
    column(Modifier.fillWidth().then(modifier)) {
        column(Modifier.fillWidth(padding = 7f)) {
            column(Modifier.fillWidth().childBasedHeight(10f).color(EssentialPalette.COMPONENT_BACKGROUND)) {
                spacer(height = 1f)
                row(Modifier.alignHorizontal(Alignment.Start(10f)), Arrangement.spacedBy(7f)) {
                    image(
                        EssentialPalette.ROUND_WARNING_7X, Modifier
                        .color(EssentialPalette.RED)
                        .shadow(EssentialPalette.TEXT_SHADOW)
                    )
                    wrappedText("{name} is temporarily suspended from social features!",
                        textModifier = Modifier
                            .color(EssentialPalette.TEXT_DISABLED)
                            .shadow(EssentialPalette.TEXT_SHADOW)
                    ) {
                        "name" {
                            text(name, Modifier
                                .color(EssentialPalette.TEXT_MID_GRAY)
                                .shadow(EssentialPalette.TEXT_SHADOW)
                            )
                        }
                    }
                }
            }
            spacer(height = 7f)
        }
    }
}