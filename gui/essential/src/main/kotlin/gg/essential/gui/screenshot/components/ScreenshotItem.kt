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
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.common.shadow.ShadowIcon
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignBoth
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedSize
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.hoverTooltip
import gg.essential.gui.layoutdsl.layoutAsBox
import gg.essential.gui.layoutdsl.outline
import gg.essential.gui.layoutdsl.then
import gg.essential.gui.layoutdsl.whenHovered
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.constraints.TileConstraint
import gg.essential.gui.util.hoverScope
import gg.essential.gui.util.hoverScopeV2
import gg.essential.universal.USound
import gg.essential.gui.util.hoveredStateV2
import gg.essential.vigilance.utils.onLeftClick

class ScreenshotItem(
    val id: ScreenshotId,
    private val viewComponent: ListViewComponent,
    private val numberOfItemsPerRow: State<Int>,
    desiredImageSize: MutableState<Pair<Int, Int>>,
) : ScreenshotPreview(
    id,
    desiredImageSize,
) {
    private val stateManager = viewComponent.stateManager
    private val favorite = stateManager.getFavoriteState(id)

    private fun LayoutScope.favoriteIndicator(modifier: Modifier = Modifier) {
        val tooltip = Modifier.hoverTooltip(stateManager.getFavoriteTextState(id))

        box(modifier.then(tooltip).hoverScope()) {
            if_(favorite) {
                val icon = ShadowIcon(EssentialPalette.HEART_FILLED_9X, true)
                icon.rebindPrimaryColor(BasicState(EssentialPalette.TEXT_RED))
                icon.rebindShadowColor(BasicState(EssentialPalette.BLACK))
                icon()
            } `else` {
                val icon = ShadowIcon(EssentialPalette.HEART_EMPTY_9X, true)
                icon.rebindPrimaryColor(icon.hoverScope().map {
                    if (it) {
                        EssentialPalette.TEXT_RED
                    } else {
                        EssentialPalette.TEXT
                    }
                })
                icon.rebindShadowColor(BasicState(EssentialPalette.BLACK))
                icon()
            }
        }.onLeftClick {
            USound.playButtonPress()
            // Saving value is handled by ScreenshotStateManager
            favorite.set { !it }
            it.stopPropagation()
        }
    }

    private fun LayoutScope.optionsDots(modifier: Modifier = Modifier) {
        val icon = ShadowIcon(EssentialPalette.OPTIONS_8X2, true)
        icon.rebindShadowColor(BasicState(EssentialPalette.BLACK))
        icon.rebindPrimaryColor(EssentialPalette.getTextColor(icon.hoverScope()))

        val tooltip = Modifier.hoverTooltip("Options", position = EssentialTooltip.Position.ABOVE, padding = 2f)

        box(modifier.childBasedSize(5f).then(tooltip).hoverScope()) {
            icon()
        }.onLeftClick {
            viewComponent.handleRightClick(this@ScreenshotItem, it)
            it.stopPropagation()
        }
    }

    init {
        val hoverScope = Modifier.then {
            val hovered = hoveredStateV2()
            val optionsMenuOpen = viewComponent.optionsDropdown.menuDialogOwner.map { it == id }
            Modifier.hoverScope { hovered() || optionsMenuOpen() }.applyToComponent(this)
        }

        val outlineEffect = Modifier.whenHovered(Modifier.outline(EssentialPalette.ACCENT_BLUE, 2f, false))

        var container: UIComponent
        layoutAsBox {
            container = screenshotImage(imgTexture, outlineEffect.then(hoverScope)) {
                val hovered = containerDontUseThisUnlessYouReallyHaveTo.hoverScopeV2()
                if_(hovered) {
                    optionsDots(Modifier.alignBoth(Alignment.End))
                }
                if_({ hovered() || favorite() }) {
                    favoriteIndicator(Modifier.alignHorizontal(Alignment.End(5f)).alignVertical(Alignment.Start(5f)))
                }
            }
        }

        constrain {
            width = TileConstraint(numberOfItemsPerRow.toV1(this@ScreenshotItem), screenshotPadding)
            height = AspectConstraint(9 / 16f)
        }

        container.onMouseClick {
            if (it.mouseButton == 1) {
                viewComponent.handleRightClick(this@ScreenshotItem, it)
                it.stopPropagation()
            }
        }

        onLeftClick {
            viewComponent.focus(id)
        }
    }
}