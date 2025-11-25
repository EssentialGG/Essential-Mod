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
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.boundTo
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.elementa.events.UIClickEvent
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialCollapsibleSearchbar
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.constraints.CenterPixelConstraint
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.layoutAsBox
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.network.connectionmanager.media.IScreenshotManager
import gg.essential.universal.UDesktop
import gg.essential.universal.USound
import gg.essential.util.createScrollbarRelativeTo
import gg.essential.vigilance.utils.onLeftClick

class ListViewComponent(
    val screenshotManager: IScreenshotManager,
    val stateManager: ScreenshotStateManager,
    val providerManager: ScreenshotProviderManager,
    private val desiredImageSize: MutableState<Pair<Int, Int>>,
    val rightBorder: UIComponent,
    val optionsDropdown: ScreenshotOptionsDropdown,
    val view: MutableState<View>,
) : ScreenshotView(view.map { it == View.List }) {
    private val screenshotsFolder by IconButton(EssentialPalette.MC_FOLDER_8X7, tooltipText = "Screenshot Folder")
        .setDimension(IconButton.Dimension.Fixed(buttonSize, buttonSize)).constrain {
            x = 0.pixels(alignOpposite = true)
            y = CenterPixelConstraint()
        }.childOf(titleBar).onLeftClick {
            UDesktop.open(screenshotManager.screenshotFolder.toFile())
        }

    private val searchBar by EssentialCollapsibleSearchbar().constrain {
        x = SiblingConstraint(3f, alignOpposite = true)
        y = CenterPixelConstraint()
        height = 100.percent boundTo screenshotsFolder
    } childOf titleBar

    val listView = object : ScreenshotListView(
        screenshotManager,
        providerManager,
        searchBar.textContentV2,
    ) {
        override fun LayoutScope.layoutItem(id: ScreenshotId) {
            ScreenshotItem(id, this@ListViewComponent, numberOfItemsPerRow, desiredImageSize)()
        }
    }

    init {
        layoutAsBox { listView() }

        setupScrollbar()
    }

    private fun setupScrollbar() {
        listView.scroller.setVerticalScrollBarComponent(
            createScrollbarRelativeTo(
                active.toV1(listView.scroller),
                rightBorder,
                rightBorder,
                this,
                false,
            ).first, // Only one instance is created, so no cleanup is required
            hideWhenUseless = true,
        )
    }

    fun handleRightClick(screenshotPreview: ScreenshotItem, it: UIClickEvent) {
        optionsDropdown.handleRightClick(
            screenshotPreview.id,
            it,
            screenshotPreview.imgTexture.getUntracked()?.error ?: true
        )
    }

    fun focus(id: ScreenshotId) {
        USound.playButtonPress()
        view.set(View.Focus(id))
    }
}
