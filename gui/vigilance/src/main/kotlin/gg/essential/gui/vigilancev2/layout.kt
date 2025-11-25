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
package gg.essential.gui.vigilancev2

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.ScrollComponent
import gg.essential.gui.common.EssentialSearchbar
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.onChange
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.vigilancev2.components.settingsCategory
import gg.essential.gui.vigilancev2.components.vigilanceSidebar
import gg.essential.gui.vigilancev2.palette.VigilancePalette
import gg.essential.util.scrollGradient

const val gradientHeight = 30f
const val vigilanceSidebarWidth = 175f

fun LayoutScope.vigilanceContent(
    outlineThickness: Float,
    categories: ListState<Category>,
    currentCategoryName: MutableState<String>,
    searchState: MutableState<String>,
    mainScrollbar: UIComponent,
    sidebarSections: List<LayoutScope.() -> Unit>,
    bottomSidebarContent: LayoutScope.() -> Unit,
): UIComponent {
    val sidebarScroller: ScrollComponent

    row(Modifier.fillParent()) {
        column(Modifier.width(vigilanceSidebarWidth).fillHeight()) {
            row(Modifier.fillWidth().fillRemainingHeight()) {
               sidebarScroller = vigilanceSidebar(Modifier.fillRemainingWidth().fillHeight(), categories, currentCategoryName, searchState, sidebarSections)

                box(Modifier.fillHeight().width(outlineThickness).color(VigilancePalette.SIDEBAR_DIVIDER)) {
                    val bar = box(Modifier.fillWidth().color(VigilancePalette.SCROLLBAR))
                    sidebarScroller.setVerticalScrollBarComponent(bar, true)
                }
            }

            bottomSidebarContent()
        }

        val settingsCategoryScroller: ScrollComponent

        box(Modifier.fillHeight().fillRemainingWidth()) {
            settingsCategoryScroller = settingsCategory(categories, currentCategoryName, searchState)

            scrollGradient(settingsCategoryScroller, true, gradientHeight)
            scrollGradient(settingsCategoryScroller, false, gradientHeight)
        }

        settingsCategoryScroller.setVerticalScrollBarComponent(mainScrollbar, true)

        currentCategoryName.onChange(stateScope) {
            settingsCategoryScroller.scrollToTop(false)
        }
    }

    return sidebarScroller
}

fun LayoutScope.vigilanceTitleBar(
    outlineThickness: Float,
    leftTitleBarContent: LayoutScope.() -> Unit = {},
    rightTitleBarContent: LayoutScope.() -> Unit = {},
): MutableState<String> {
    val divider = Modifier.fillHeight().width(outlineThickness).color(VigilancePalette.SIDEBAR_DIVIDER_LIGHT)
    val searchState: MutableState<String>

    row(Modifier.fillParent()) {
        row(Modifier.width(vigilanceSidebarWidth - outlineThickness).fillHeight(), Arrangement.spacedBy(float = FloatPosition.CENTER)) {
            row(Modifier.fillWidth(padding = 10f).fillHeight(), Arrangement.spacedBy(float = FloatPosition.END)) {
                leftTitleBarContent()
            }
        }

        box(divider)

        row(Modifier.fillRemainingWidth().fillHeight(), Arrangement.spacedBy(float = FloatPosition.CENTER)) {
            row(Modifier.fillWidth(padding = 10f).fillHeight(), Arrangement.SpaceBetween) {
                EssentialSearchbar(
                    placeholder = "Search",
                    clearSearchOnEscape = true,
                ).apply {
                    searchState = this.textContentV2
                }(Modifier.width(150f))

                row(
                    Modifier.childBasedWidth(5f),
                    Arrangement.spacedBy(6f, FloatPosition.START)
                ) {
                    rightTitleBarContent()
                }
            }
        }
    }

    return searchState
}
