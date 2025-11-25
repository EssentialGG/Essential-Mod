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
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.IntEssentialSlider
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.childBasedWidth
import gg.essential.gui.layoutdsl.fillHeight
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.image
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.util.findChildrenOfType
import kotlin.math.abs

class ScreenshotItemsSlider(
    private val itemsPerRow: MutableState<Int>,
    private val getScroller: () -> ScrollComponent,
) : UIContainer() {

    private var scrollToScreenshotId: ScreenshotId? = null
    private val sliderWidth = 36
    private val slider by IntEssentialSlider(2, 7, itemsPerRow.get())

    init {
        this.layout(Modifier.childBasedWidth().height(11f)) {
            row(Modifier.childBasedWidth().fillHeight(), Arrangement.spacedBy(3f)) {
                image(EssentialPalette.IMAGE_SIZE_BIG_10X, Modifier.alignVertical(Alignment.End))
                slider(Modifier.width(sliderWidth.toFloat()).height(9f))
                row {
                    image(EssentialPalette.IMAGE_SIZE_SMALL_9X)
                    spacer(width = 1f)
                }
            }
        }
        slider.onUpdateInt {
            handleUpdateItems(it)
        }
    }

    override fun afterInitialization() {
        super.afterInitialization()

        var previousVerticalOffset = 0f
        // Need to wait for all the other components to be initialized
        val scrollComponent = getScroller()
        scrollComponent.addScrollAdjustEvent(true) { _: Float, _: Float ->
            if (scrollComponent.verticalOffset != previousVerticalOffset) {
                previousVerticalOffset = scrollComponent.verticalOffset
                scrollToScreenshotId = null
            }
        }
    }


    /**
     * Somewhat of a hack such that we can keep the items in the center of the screen
     * in the same place after updating the number of items per row
     */
    private fun handleUpdateItems(itemsPerRow: Int) {
        fun UIComponent.center(): Float {
            return (getTop() + getBottom()) / 2
        }

        val scrollComponent = getScroller()
        val scrollerCenter = scrollComponent.center()
        var closest = scrollToScreenshotId?.let { getComponentById(it) }
            ?: scrollComponent.findChildrenOfType<ScreenshotPreview>(true)
                .minByOrNull { abs(scrollerCenter - it.center()) }

        if (closest != null) {
            val screenshotId = closest.screenshotId
            val top = closest.getTop()
            val offset = scrollComponent.verticalOffset
            scrollToScreenshotId = screenshotId

            this.itemsPerRow.set(itemsPerRow)
            Window.of(scrollComponent).animationFrame()
            closest = getComponentById(screenshotId)
            if (closest != null) {
                scrollComponent.scrollTo(verticalOffset = offset - (closest.getTop() - top), smoothScroll = false)
                scrollToScreenshotId = screenshotId
            } else {
                scrollToScreenshotId = null
            }
        } else {
            this.itemsPerRow.set(itemsPerRow)
        }
    }

    private fun getComponentById(screenshotId: ScreenshotId): ScreenshotPreview? {
        return getScroller().findChildrenOfType<ScreenshotPreview>(true)
            .firstOrNull { it.screenshotId == screenshotId }
    }
}