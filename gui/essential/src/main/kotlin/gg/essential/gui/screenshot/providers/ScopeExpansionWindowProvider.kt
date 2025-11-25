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
package gg.essential.gui.screenshot.providers

import gg.essential.gui.screenshot.ScreenshotId
import kotlin.math.max

/**
 * Expands the scope of Window objects sent to the source provider by the amount of expansionFactor as a function of the size of the window
 * The resulting windows list will have 2 additional windows appended per input window
 */
open class ScopeExpansionWindowProvider<out T>(
    private val sourceProvider: WindowedProvider<T>,
    protected var expansionFactor: Float,
    protected var expansionPerFrame: Int = 1
) : WindowedProvider<T> {

    override var items: List<ScreenshotId> by sourceProvider::items


    // Combined ranges that were requested from the inner provider previously as
    // a result of our expansion
    private var previousScopes = listOf<IntRange>()

    override fun provide(windows: List<WindowedProvider.Window>, optional: Set<ScreenshotId>): Map<ScreenshotId, T> {
        // Create new variable to avoid concurrent modification
        val expandedWindows = windows.toMutableList()

        // Workaround for [IntRange.shift] exploding on `coerceIn(items.indices)` if items is empty
        // If items are empty, there is no new scope to expand into
        if (items.isNotEmpty()) {
            // Expand all requested windows and add resulting windows if they cover a non-zero range
            for (window in windows) {
                val elements = expand(window, previousScopes)
                for (entry in elements) {
                    if (entry.range.size() > 0) {
                        expandedWindows.add(entry)
                    }
                }
            }
        }

        // Combine previous scopes into a single range when they are connected for quicker
        // Computation next call
        previousScopes = combine(expandedWindows.map { it.range })
        return sourceProvider.provide(expandedWindows, optional)
    }

    /**
     * Combines any overlapping or adjacent IntRange's
     * Example: 1..5, 6..10, 9..13, 15..20
     * would become 1..13, 15..20
     */
    private fun combine(inputWindows: List<IntRange>): List<IntRange> {
        val combined = mutableListOf<IntRange>()

        combined.add(inputWindows.sortedBy { it.first }.reduceOrNull { range, next ->
            if (next.first - range.last <= 1) {
                IntRange(range.first, next.last)
            } else {
                combined.add(range)
                next
            }
        } ?: return emptyList())

        return combined
    }

    /**
     * Expands the supplied window in accordance to expansionFactor.
     * Will add one image per frame if the windows created are lean the max size
     */
    private fun expand(
        window: WindowedProvider.Window,
        previousScopes: List<IntRange>
    ): List<WindowedProvider.Window> {

        val backwards = window.backwards
        val windowRange = window.range

        // Calculate the range that ends at the start of our range
        val maxBackwardsSize = (windowRange.size() * expansionFactor).toInt()
        val backRange = windowRange.shift(-maxBackwardsSize)

        // The intersection of our ideal left window and the existing scopes
        var intersectedRangeBackwards = previousScopes.firstNotNullOfOrNull {
            it.intersects(backRange)
        } ?: IntRange(windowRange.first, windowRange.first)

        if (intersectedRangeBackwards.size() < maxBackwardsSize) {
            intersectedRangeBackwards = intersectedRangeBackwards.expandLeft(expansionPerFrame)
        }

        // Calculates the range that starts at the end of our range
        val maxForwardSize = (windowRange.size() * expansionFactor).toInt()
        val forwardRange = windowRange.shift(maxForwardSize)

        // The intersection of our ideal right window and the existing scopes
        var intersectedRangeForwards = previousScopes.firstNotNullOfOrNull {
            it.intersects(forwardRange)
        } ?: IntRange(windowRange.last, windowRange.last)

        if (intersectedRangeForwards.size() < maxForwardSize) {
            intersectedRangeForwards = intersectedRangeForwards.expandRight(expansionPerFrame)
        }


        val newRegions = listOf(
            WindowedProvider.Window(intersectedRangeBackwards, backwards),
            WindowedProvider.Window(intersectedRangeForwards, backwards)
        )
        return if (backwards) newRegions else newRegions.reversed()

    }

    /**
     * Distance from start to end of this IntRange
     * IntRange.count() iterates over the entire thing and
     * counts the number of iterations instead of doing this
     * math
     */
    private fun IntRange.size(): Int {
        return endInclusive - start
    }

    /**
     * Shifts the start and end indices by [amount]
     * without neither the start nor end exceeding the range
     */
    private fun IntRange.shift(amount: Int): IntRange {
        return IntRange(
            (start + amount).coerceIn(items.indices),
            (endInclusive + amount).coerceIn(items.indices)
        )
    }

    /**
     * Expands this IntRange by decreasing start by amount
     * without going negative
     */
    private fun IntRange.expandLeft(amount: Int): IntRange {
        return IntRange(max(0, start - amount), endInclusive)
    }

    /**
     * Expands this IntRange by increasing end by amount
     * without exceeding the amount of elements
     * in our items list
     */
    private fun IntRange.expandRight(amount: Int): IntRange {
        return IntRange(
            start,
            (endInclusive + amount).coerceIn(items.indices)
        )
    }

    /**
     * Returns the intersection of this IntRange and other
     * Will return null if the intersection does not exist
     */
    private fun IntRange.intersects(other: IntRange): IntRange? {
        // Make sure `this` starts before other
        if (other.first < this.first) {
            return other.intersects(this)
        }

        // other is a smaller subrange of `this`
        if (other.last <= this.last) {
            return IntRange(other.first, other.last)
        }
        // other intersects only part of `this`
        if (other.first < this.last) {
            return IntRange(other.first, this.last)
        }

        return null
    }


}

class MaxScopeExpansionWindowProvider<out T>(private val sourceProvider: WindowedProvider<T>) : WindowedProvider<T> {

    override fun provide(windows: List<WindowedProvider.Window>, optional: Set<ScreenshotId>): Map<ScreenshotId, T> {
        return sourceProvider.provide(
            windows.flatMap {

                val expandedWindows = listOf(
                    WindowedProvider.Window(IntRange(0, it.range.first), it.backwards),
                    WindowedProvider.Window(IntRange(it.range.last, items.lastIndex), it.backwards)
                )

                val aggregatedList = mutableListOf(it)
                aggregatedList.addAll(if (it.backwards) expandedWindows else expandedWindows.reversed())
                aggregatedList
            },
            optional,
        )
    }

    override var items: List<ScreenshotId> by sourceProvider::items

}