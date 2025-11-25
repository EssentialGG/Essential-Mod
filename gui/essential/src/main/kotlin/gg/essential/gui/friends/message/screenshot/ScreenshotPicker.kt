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
package gg.essential.gui.friends.message.screenshot

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialCollapsibleSearchbar
import gg.essential.gui.elementa.state.v2.collections.trackedListOf
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.flatten
import gg.essential.gui.elementa.state.v2.mapEach
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillHeight
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.layoutdsl.fillRemainingHeight
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.floatingBox
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.heightAspect
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.components.ScreenshotListView
import gg.essential.gui.screenshot.components.ScreenshotProviderManager
import gg.essential.gui.screenshot.components.screenshotPadding
import gg.essential.gui.screenshot.constraints.TileConstraint
import gg.essential.universal.UKeyboard
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.vigilance.utils.onLeftClick

class ScreenshotPicker(
    private val screenshotAttachmentManager: ScreenshotAttachmentManager
) : UIContainer() {

    private val searchBar by EssentialCollapsibleSearchbar()

    private val previewItemsSource = mutableStateOf(stateOf(trackedListOf<ScreenshotId>()))
    private val previewItems = previewItemsSource.flatten()

    private val desiredImageSize = mutableStateOf(ScreenshotProviderManager.minResolutionTargetResolution)
    private val screenshotProvider = ScreenshotProviderManager(
        platform.screenshotManager,
        screenshotAttachmentManager.isScreenOpen,
        previewItems,
        desiredImageSize,
    )

    private val listView = object : ScreenshotListView(
        platform.screenshotManager,
        screenshotProvider,
        searchBar.textContentV2,
        alwaysVisible = screenshotAttachmentManager.selectedImages,
    ) {
        override fun LayoutScope.layoutItem(id: ScreenshotId) {
            val item = SelectableScreenshotPreview(
                id,
                desiredImageSize,
                screenshotAttachmentManager,
            )
            item.constraints.width = TileConstraint(numberOfItemsPerRow.toV1(item), screenshotPadding)
            item(Modifier.heightAspect(9 / 16f))
        }
    }

    init {
        previewItemsSource.set(listView.items.mapEach { it.id })

        val titleState = screenshotAttachmentManager.selectedImages
            .map { "Select Pictures - [${it.size}/10]" }

        layout {
            column(Modifier.fillParent()) {
                box(Modifier.fillWidth().height(30f).color(EssentialPalette.COMPONENT_BACKGROUND)) {
                    row(Modifier.fillWidth(padding = 10f), Arrangement.SpaceBetween) {
                        text(
                            titleState.toV1(this@ScreenshotPicker),
                            modifier = Modifier.color(EssentialPalette.TEXT_HIGHLIGHT)
                                .shadow(EssentialPalette.TEXT_SHADOW_LIGHT)
                        )
                        row(Arrangement.spacedBy(3f)) {
                            searchBar(Modifier.shadow())
                            screenshotAttachmentDoneButton(screenshotAttachmentManager)
                        }
                    }
                }
                box(Modifier.fillWidth().fillRemainingHeight()) {
                    listView()
                    val scrollBar: UIComponent
                    floatingBox(Modifier.width(3f).fillHeight().alignHorizontal(Alignment.End(padding = -3f))) {
                        scrollBar = box(Modifier.fillWidth().color(EssentialPalette.SCROLLBAR))
                    }
                    listView.scroller.setVerticalScrollBarComponent(scrollBar, hideWhenUseless = true)
                }
            }
        }

        onLeftClick {
            focusCheck()
        }

        onKeyType { char, keyCode ->
            if (UKeyboard.isEnterKey(keyCode) || keyCode == UKeyboard.KEY_ESCAPE) {
                screenshotAttachmentManager.isPickingScreenshots.set(false)
            } else {
                for (listener in Window.of(this).keyTypedListeners) {
                    listener(this, char, keyCode)
                }
            }
        }
    }

    fun focusCheck() {
        if (screenshotAttachmentManager.isPickingScreenshots.get()) {
            grabWindowFocus()
        }
    }

    companion object {
    }

}
