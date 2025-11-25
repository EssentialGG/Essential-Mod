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
package gg.essential.gui.wardrobe.categories

import gg.essential.cosmetics.FeaturedPageWidth
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.Window
import gg.essential.gui.EssentialPalette
import gg.essential.gui.about.components.ColoredDivider
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.util.findChildrenByTag
import gg.essential.gui.util.isInComponentTree
import gg.essential.gui.wardrobe.Item.Companion.toModItem
import gg.essential.gui.wardrobe.WardrobeCategory
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.components.*
import gg.essential.gui.wardrobe.something.CosmeticGroup
import gg.essential.mod.cosmetics.featured.BlankDivider
import gg.essential.mod.cosmetics.featured.FeaturedItem
import gg.essential.mod.cosmetics.featured.FeaturedItemRow
import gg.essential.mod.cosmetics.featured.TextDivider
import gg.essential.util.scrollToTopOf

fun LayoutScope.featuredCategory(wardrobeState: WardrobeState, scroller: ScrollComponent, modifier: Modifier = Modifier) {
    val layoutState = wardrobeState.featuredPageLayout
    val cosmeticItemHeight = cosmeticWidth + cosmeticTextHeight

    fun LayoutScope.featuredItemRow(rowIndex: Int, row: FeaturedItemRow, layoutWidth: FeaturedPageWidth, emptySlots: MutableSet<Pair<Int, Int>>) {
        val verticalPosition = rowIndex * cosmeticItemHeight + rowIndex * cosmeticYSpacing + cosmeticXSpacing
        var itemIndex = 0
        for (columnIndex in 0 until layoutWidth) {
            // If we ran out of items in this row, break
            if (itemIndex >= row.items.size) break

            // If the slot should be empty, we skip it
            if (emptySlots.contains(Pair(rowIndex, columnIndex))) continue

            val horizontalPosition = columnIndex * cosmeticWidth + columnIndex * cosmeticXSpacing
            val featuredItem = row.items[itemIndex++]
            val itemWidth = featuredItem.width
            val itemHeight = featuredItem.height

            // Add all additional slots this item occupies to the list of empty slots
            for (w in 0 until itemWidth) {
                for (h in 0 until itemHeight) {
                    emptySlots.add(Pair(rowIndex + h, columnIndex + w))
                }
            }

            if (featuredItem is FeaturedItem.Empty)
                continue

            box(Modifier.alignVertical(Alignment.Start(verticalPosition)).alignHorizontal(Alignment.Start(horizontalPosition))) {
                bind(featuredItem.toModItem(wardrobeState)) { item ->
                    if (item == null) {
                        text("Error loading item.")
                    } else {
                        cosmeticItem(
                            item,
                            WardrobeCategory.FeaturedRefresh,
                            wardrobeState,
                            Modifier.itemSize(itemWidth, itemHeight)
                        )
                    }
                }
            }
        }
    }

    var content: UIComponent? = null

    bind(layoutState) { (layoutsEmpty, layoutEntry) ->
        if (layoutsEmpty) {
            spacer(height = 20f)
            wrappedText("Error loading featured page!")
            return@bind
        }
        if (layoutEntry == null) {
            spacer(height = 20f)
            wrappedText("Not enough space to show page!\n\nTry making your window wider!")
            return@bind
        }
        val (layoutWidth, layout) = layoutEntry

        content = column(modifier) {
            val rowsRun = mutableListOf<FeaturedItemRow>()
            for (component in layout.rows + null) {
                if (component is FeaturedItemRow) {
                    rowsRun.add(component)
                    continue
                } else if (rowsRun.isNotEmpty()) {
                    val totalHeight = rowsRun.size * cosmeticItemHeight + (rowsRun.size - 1).coerceAtLeast(0) * cosmeticYSpacing + 2 * cosmeticXSpacing
                    // Slots that should be left empty because a bigger item spans over them
                    val emptySlots = mutableSetOf<Pair<Int, Int>>()
                    box(Modifier.height(totalHeight).fillWidth()) {
                        for ((rowIndex, row) in rowsRun.withIndex()) {
                            featuredItemRow(rowIndex, row, layoutWidth, emptySlots)
                        }
                    }
                    rowsRun.clear()
                }
                when (component) {
                    is BlankDivider -> {
                        spacer(height = 1f)
                    }
                    is TextDivider -> {
                        spacer(height = 7f)
                        ColoredDivider(
                            component.text,
                            dividerColor = EssentialPalette.BUTTON,
                            shadowColor = EssentialPalette.COMPONENT_BACKGROUND
                        )()
                    }
                    is FeaturedItemRow -> throw IllegalStateException("FeaturedItemRow should already be processed")
                    null -> {}
                }
            }
        }
    }

    effect(this.stateScope) {
        val highlightedItem = wardrobeState.highlightItem()

        fun doScroll() {
            if (content?.isInComponentTree() != true) {
                return
            }

            if (highlightedItem == null) {
                return
            }

            scroller.animationFrame() // Force recalculate of position to avoid scrolling an incorrect amount

            val target = content?.findChildrenByTag(CosmeticItemTag::class.java, true) {
                it.item.itemId == highlightedItem
            }?.firstOrNull() ?: return

            scroller.scrollToTopOf(target, offset = -CosmeticGroup.headerHeight)
        }

        // Double delay is needed because this component isn't added to the component tree until the next frame
        // because WardrobeContainer calls layoutSafe() on the current category. Additionally, this listener
        // on the state is called before the component is added to the component tree, so we need to wait for
        // the next frame to scroll to the correct position.
        Window.enqueueRenderOperation { Window.enqueueRenderOperation(::doScroll) }
    }
}
