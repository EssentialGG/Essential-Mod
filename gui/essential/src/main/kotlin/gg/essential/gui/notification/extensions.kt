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
package gg.essential.gui.notification

import gg.essential.api.gui.NotificationBuilder
import gg.essential.api.gui.NotificationType
import gg.essential.api.gui.Slot
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.FillConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixel
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.essentialmarkdown.EssentialMarkdown
import gg.essential.gui.elementa.essentialmarkdown.MarkdownConfig
import gg.essential.gui.elementa.essentialmarkdown.ParagraphConfig
import gg.essential.gui.elementa.essentialmarkdown.TextConfig
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedMaxHeight
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.effect
import gg.essential.gui.layoutdsl.fillRemainingWidth
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.floatingBox
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width

@JvmOverloads
fun Notifications.error(
    title: String,
    message: String,
    action: () -> Unit = {},
    close: () -> Unit = {},
    configure: NotificationBuilder.() -> Unit = {}
) {
    Notifications.push(title, message, action = action, close = close) {
        type = NotificationType.ERROR
        configure()
    }
}

@JvmOverloads
fun Notifications.warning(
    title: String,
    message: String,
    action: () -> Unit = {},
    close: () -> Unit = {},
    configure: NotificationBuilder.() -> Unit = {}
) {
    Notifications.push(title, message, action = action, close = close) {
        type = NotificationType.WARNING
        configure()
    }
}

private val defaultMarkdownConfig = MarkdownConfig(
    allowColors = true,
    textConfig = TextConfig(EssentialPalette.TEXT),
    paragraphConfig = ParagraphConfig(spaceBetweenLines = 1f),
)

@JvmOverloads
fun NotificationBuilder.markdownBody(
    message: String,
    markdownConfig: MarkdownConfig = defaultMarkdownConfig
) {
    withCustomComponent(
        Slot.LARGE_PREVIEW,
        UIContainer().apply {
            EssentialMarkdown(message, markdownConfig, disableSelection = true).constrain {
                width = FillConstraint()
            } childOf this

            constrain {
                width = 100.percent
                height = ChildBasedSizeConstraint() - 1.pixel
            }
        }
    )
}

@JvmOverloads
fun NotificationBuilder.iconAndMarkdownBody(
    icon: UIComponent,
    message: String,
    markdownConfig: MarkdownConfig = defaultMarkdownConfig
) {
    val uiContainer = UIContainer()
    uiContainer.layout(Modifier.fillWidth().childBasedMaxHeight()) {
        row(Modifier.fillWidth(), Arrangement.spacedBy(5f)) {
            // Icon mechanics copied from notificationLayout
            row(Modifier.alignVertical(Alignment.Start)) {
                // Center the icon with the text if it is shorter, else align it to the baseline of the text
                val fakeText = text("")
                box(Modifier.height(fakeText).width(8f)) {
                    floatingBox(
                        Modifier.effect { ScissorEffect() }.alignVertical(Alignment.Center).alignHorizontal(Alignment.Center(true))
                    ) {
                        box {
                            icon(Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(EssentialPalette.TEXT_SHADOW_LIGHT))
                        }
                        spacer(width = 1f, height = 1f)
                    }
                }
            }
            UIContainer().apply {
                EssentialMarkdown(message, markdownConfig, disableSelection = true).constrain {
                    width = FillConstraint()
                } childOf this

                constrain {
                    width = 100.percent
                    height = ChildBasedSizeConstraint() - 1.pixel
                }
            }(Modifier.fillRemainingWidth())
        }
    }
    withCustomComponent(Slot.LARGE_PREVIEW, uiContainer)
}
