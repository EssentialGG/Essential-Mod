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

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.Window
import gg.essential.elementa.dsl.boundTo
import gg.essential.elementa.dsl.percent
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.modal.OpenLinkModal
import gg.essential.gui.elementa.essentialmarkdown.EssentialMarkdown
import gg.essential.gui.elementa.essentialmarkdown.MarkdownConfig
import gg.essential.gui.elementa.essentialmarkdown.ParagraphConfig
import gg.essential.gui.elementa.essentialmarkdown.TextConfig
import gg.essential.gui.elementa.essentialmarkdown.URLConfig
import gg.essential.gui.layoutdsl.*
import gg.essential.network.connectionmanager.notices.NoticeBanner
import gg.essential.network.connectionmanager.notices.NoticeBannerManager
import gg.essential.network.connectionmanager.notices.WardrobeBannerColor
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color
import java.net.URI
import java.net.URISyntaxException

/** A [bannerColor]-colored banner that displays [text] and can be dismissed via [dismissAction] */
fun LayoutScope.banner(
    text: String,
    modifier: Modifier = Modifier,
    bannerColor: WardrobeBannerColor = WardrobeBannerColor.BLUE,
    textColor: Color = EssentialPalette.TEXT_HIGHLIGHT,
    dismissAction: (() -> Unit)? = null,
) {
    val buttonModifier = Modifier.width(11f).heightAspect(1f).shadow(EssentialPalette.BLACK).color(bannerColor.button).hoverColor(bannerColor.buttonHighlight)
    // FIXME: Kotlin emits invalid bytecode if this is `val`, see https://youtrack.jetbrains.com/issue/KT-48757
    var content: UIComponent

    val markdown = EssentialMarkdown(
        text,
        MarkdownConfig(
            paragraphConfig = ParagraphConfig(spaceBetweenLines = 3f, spaceBefore = 3f, spaceAfter = 0f),
            textConfig = TextConfig(color = textColor, shadowColor = EssentialPalette.TEXT_SHADOW),
            urlConfig = URLConfig(
                fontColor = EssentialPalette.TEXT,
                fontColorOnHover = EssentialPalette.TEXT_HIGHLIGHT,
                underline = true
            ),
        ),
        disableSelection = true
    ).apply {
        onLinkClicked(platform.essentialUriListener)
        onLinkClicked { event ->
            event.stopImmediatePropagation()

            // Example: `server://mc.hypixel.net`
            if (event.url.startsWith("server://")) {
                val address = event.url.replace("server://", "")

                platform.connectToServer(address, address)

                return@onLinkClicked
            }

            // FIXME workaround for EM-1830
            Window.of(this).mouseRelease()
            try {
                OpenLinkModal.openUrl(URI.create(event.url))
            } catch (e: URISyntaxException) {
                // Ignored, if the link is invalid we just do nothing
            }
        }
    }

    box(Modifier.fillWidth().color(EssentialPalette.COMPONENT_BACKGROUND).then(modifier)) {
        val bannerContent = row(Modifier.fillWidth().color(bannerColor.background)) {
            box(Modifier.width(3f).fillHeight().color(bannerColor.main))
            spacer(width = 9f)
            content = row(Modifier.fillRemainingWidth().childBasedHeight(10f)) {
                markdown(Modifier.fillRemainingWidth())
            }
            box(Modifier.fillWidth(0.1f).fillHeight()) {
                dismissAction?.let { dismissAction ->
                    box(buttonModifier.alignHorizontal(Alignment.End(8f)).alignVertical(Alignment.Start(8f)).hoverScope()) {
                        icon(EssentialPalette.CANCEL_5X, Modifier.color(EssentialPalette.TEXT).hoverColor(EssentialPalette.TEXT_HIGHLIGHT))
                    }.onLeftClick {
                        dismissAction.invoke()
                    }
                }
            }
        }
        bannerContent.setHeight(100.percent boundTo content)
    }
}

/** A [LayoutScope.banner] that obtains its text, color, and dismissibility from the given [banner] */
fun LayoutScope.banner(
    banner: NoticeBanner,
    manager: NoticeBannerManager,
    modifier: Modifier = Modifier,
) {
    banner(banner.lines.joinToString("\n\n"), modifier, banner.color, dismissAction = if (banner.dismissible) {{
        manager.dismiss(banner)
    }} else null)
}
