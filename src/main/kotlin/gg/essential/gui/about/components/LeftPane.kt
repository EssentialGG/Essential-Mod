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
package gg.essential.gui.about.components

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.about.Category
import gg.essential.gui.common.Spacer
import gg.essential.gui.common.modal.OpenLinkModal
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.common.shadow.ShadowIcon
import gg.essential.gui.elementa.GuiScaleOffsetConstraint
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.modals.updateAvailableModal
import gg.essential.universal.USound
import gg.essential.util.AutoUpdate
import gg.essential.util.GuiUtil
import gg.essential.gui.util.hoveredState
import gg.essential.util.times
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color
import java.net.URI

class LeftPane(pages: Map<Category, Page>, selectedPage: BasicState<Page>, bottomDivider: UIBlock, private val dividerWidth: Float) : UIContainer() {

    private val bottomBarHeight = 30f

    init {
        val updateButtonModifier = Modifier.childBasedWidth(10f).childBasedHeight(4f)
            .color(EssentialPalette.GREEN_BUTTON).hoverColor(EssentialPalette.GREEN_BUTTON_HOVER)
            .onLeftClick { GuiUtil.launchModalFlow { updateAvailableModal() } }

        layout {
            column(Modifier.fillParent()) {
                column(Modifier.fillWidth(leftPadding = 10f).fillRemainingHeight().effect { ScissorEffect() }, horizontalAlignment = Alignment.Start) {
                    spacer(height = 10f)
                    pages.values.forEach { page -> MenuItem(page, selectedPage)() }
                    Spacer()(Modifier.fillRemainingHeight())
                    VersionInfo()(Modifier.fillWidth().childBasedHeight())
                    spacer(height = 10f)
                }
                if_(AutoUpdate.updateAvailable) {
                    bottomBar(Modifier.color(EssentialPalette.COMPONENT_BACKGROUND)) {
                        row(Modifier.fillWidth(padding = 10f), Arrangement.SpaceBetween) {
                            text("Update Available!", Modifier.color(EssentialPalette.GREEN).shadow(EssentialPalette.TEXT_SHADOW))
                            box(updateButtonModifier.hoverScope()) {
                                spacer(height = 1f)
                                text("Update", Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(EssentialPalette.TEXT_SHADOW).alignBoth(Alignment.Center(true)))
                            }
                        }
                    }
                }
            }
        }

        bottomDivider.layout {
            if_(AutoUpdate.updateAvailable) {
                row(BasicWidthModifier { (100.percent boundTo this@LeftPane) + (dividerWidth * 2).pixels }.alignVertical(Alignment.End), Arrangement.SpaceBetween) {
                    val divider = Modifier.width(dividerWidth).height(bottomBarHeight).color(EssentialPalette.LIGHT_DIVIDER)
                    // Light divider to the left of update banner
                    box(divider)
                    // Light divider to the right of update banner
                    box(divider)
                }
            }
        }
    }

    private fun LayoutScope.bottomBar(modifier: Modifier, block: LayoutScope.() -> Unit): UIComponent? {
        // The design includes the bottom divider in the vertical space for the bottom bar. However, that divider is
        // outside the `content` component, and therefore this component must be $dividerWidth pixels smaller because those
        // pixels aren't inside `content`.
        // We'll then add them back inside the outer box (intentionally going out of bounds) so that the components
        // can be implemented as if the outline was part of the content all along.
        return box(Modifier.fillWidth().height(bottomBarHeight - dividerWidth).then(modifier)) {
            box(Modifier.fillWidth().height(bottomBarHeight).alignVertical(Alignment.Start)) {
                block()
            }
        }
    }

    private class MenuItem(page: Page, selectedPage: BasicState<Page>) : UIContainer() {

        private val selected = selectedPage.map { it == page }
        private val hovered = hoveredState()
        private val color = selected.zip(hovered).map { (selected, hovered) ->
            if (selected) {
                EssentialPalette.ACCENT_BLUE
            } else if (hovered) {
                EssentialPalette.TEXT_HIGHLIGHT
            } else {
                EssentialPalette.TEXT
            }
        }
        private val isLink = page is LinkPage

        init {
            constrain {
                y = SiblingConstraint()
                width = ChildBasedSizeConstraint()
                height = ChildBasedMaxSizeConstraint() + 8.pixels
            }

            onLeftClick {
                if (!selected.get()) {
                    // Only set as selected if the page is opened in the client
                    if (page is LinkPage) {
                        OpenLinkModal.openUrl(URI(page.link))
                    } else {
                        selectedPage.set(page)
                    }

                    USound.playButtonPress()
                }
            }

            EssentialUIText().bindText(page.name)
                .bindShadowColor(selected.map { if (it) EssentialPalette.BLUE_SHADOW else EssentialPalette.COMPONENT_BACKGROUND })
                .setColor(color.toConstraint()).constrain {
                    y = CenterConstraint()
                    textScale = GuiScaleOffsetConstraint(1f)
                } childOf this

            // Adds an indicator icon to LinkPage MenuItems
            if (isLink) {
                ShadowIcon(
                    BasicState(EssentialPalette.ARROW_UP_RIGHT_5X5),
                    BasicState(true),
                    color,
                    BasicState(Color(0x222222)),
                ).constrain {
                    x = SiblingConstraint() + 3.pixels * GuiScaleOffsetConstraint(1f)
                    y = CenterConstraint()
                    width = AspectConstraint()
                    height = 5.pixels * GuiScaleOffsetConstraint(1f)
                    color = this@MenuItem.color.toConstraint()
                } childOf this
            }
        }
    }
}
