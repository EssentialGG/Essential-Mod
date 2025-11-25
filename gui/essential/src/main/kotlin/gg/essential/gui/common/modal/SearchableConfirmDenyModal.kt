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
package gg.essential.gui.common.modal

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.common.input.UITextInput
import gg.essential.gui.common.input.essentialInput
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.fillHeight
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.floatingBox
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.util.hoveredStateV2
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color

/** A [ConfirmDenyModal] with a searchbar and scroll container */
open class SearchableConfirmDenyModal(
    modalManager: ModalManager,
    requiresButtonPress: Boolean,
    searchbarPadding: Float = 13f,
) : ConfirmDenyModal(modalManager, requiresButtonPress) {

    private val searchContainer by UIContainer().constrain {
        y = SiblingConstraint()
        width = 100.percent
        height = ChildBasedSizeConstraint()
    } childOf customContent

    val searchBarTextState = mutableStateOf("")

    protected val middleSpacer by Spacer(height = searchbarPadding)

    protected val scrollContainer by UIContainer().constrain {
        y = SiblingConstraint()
        width = 100.percent
        height = ChildBasedMaxSizeConstraint()
    } effect ScissorEffect() childOf customContent

    val scroller by ScrollComponent(emptyString = "Nothing Found").constrain {
        width = 100.percent
        height = 148.pixels
    } childOf scrollContainer scrollGradient 30.pixels

    val scrollbarContainer by UIContainer()

    private val scrollBarBackground by UIContainer()

    private val scrollBar by UIContainer()

    private val hiddenSpacer by Spacer().constrain {
        height = 100.percent boundTo searchContainer
    } hiddenChildOf customContent

    protected val bottomSpacer by Spacer(height = 8f) childOf customContent

    init {
        configure {
            titleTextColor = EssentialPalette.TEXT_HIGHLIGHT
        }

        searchContainer.layout {
            val input = UITextInput("Search", false)
            input.setColor(EssentialPalette.TEXT)
            input.placeholderShadow.set(false)
            input.placeholderColor.set(EssentialPalette.TEXT_MID_GRAY)
            effect(stateScope) {
                searchBarTextState.set(input.textState())
            }
            essentialInput(
                input,
                icon = EssentialPalette.SEARCH_7X,
                backgroundColor = stateOf(EssentialPalette.MODAL_BACKGROUND),
                iconColor = stateOf(EssentialPalette.TEXT_MID_GRAY),
                // Same as background, as hiding the shadow would be complicating essentialInput even further
                iconShadowColor = stateOf(EssentialPalette.GUI_BACKGROUND),
                outlineColor = EssentialPalette.GRAY_OUTLINE_BUTTON,
                outlineHoveredColor = EssentialPalette.GRAY_OUTLINE_BUTTON_HOVER,
                outlineFocusedColor = EssentialPalette.SEARCHBAR_BLUE_OUTLINE,
                iconAndInputWidthPadding = 3f,
                iconAndInputPadding = 3f,
                modifier = Modifier.fillWidth().height(19f).shadow(Color.BLACK),
            )
            middleSpacer()
        }

        scroller.emptyText.constrain {
            y += 2.pixels
            color = EssentialPalette.TEXT_DISABLED.toConstraint()
        }.setShadowColor(EssentialPalette.COMPONENT_BACKGROUND)
        scrollContainer.layout {
            val scrollbar: UIComponent
            val box = floatingBox(Modifier.height(scroller).width(16f)) {
                scrollbar = box(Modifier.width(6f).alignHorizontal(Alignment.Start(1f)).hoverScope()) {
                    box(Modifier.width(2f).fillHeight().color(EssentialPalette.LIGHTEST_BACKGROUND).hoverColor(EssentialPalette.SCROLLBAR))
                }
            }.constrain {
                x = SiblingConstraint() boundTo scrollContainer
                y = 0.pixels boundTo scrollContainer
            }
            scroller.setVerticalScrollBarComponent(scrollbar)
            val isScrollerBiggerThanParent = mutableStateOf(true)
            scroller.addScrollAdjustEvent(false) { _, percentageOfParent ->
                isScrollerBiggerThanParent.set(percentageOfParent >= 1f)
            }
            val scrollerHovered = scroller.hoveredStateV2()
            val boxHovered = box.hoveredStateV2()
            val scrollbarBeingDragged = mutableStateOf(false)

            scrollbar.onLeftClick { scrollbarBeingDragged.set(true) }
            scrollbar.onMouseRelease { Window.enqueueRenderOperation { scrollbarBeingDragged.set(false) } }

            val shouldHide = memo { isScrollerBiggerThanParent() || !(boxHovered() || scrollerHovered() || scrollbarBeingDragged()) }
            gg.essential.gui.elementa.state.v2.effect(stateScope) {
                if (shouldHide()) {
                    scrollbar.hide(true)
                } else {
                    scrollbar.unhide()
                }
            }
        }

        spacer.setHeight(10.pixels)
    }

    /**
     * Hides the searchbar instantly
     */
    fun hideSearchbar() {
        searchContainer.hide(true)
        hiddenSpacer.unhide()
    }

    /**
     * Shows the searchbar
     */
    fun showSearchbar() {
        searchContainer.unhide()
        hiddenSpacer.hide(true)
    }
}
