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
package gg.essential.gui.common.shadow

import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.components.Window
import gg.essential.elementa.dsl.basicWidthConstraint
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import gg.essential.elementa.dsl.width
import gg.essential.elementa.font.FontProvider
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.pixels
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.ReadOnlyState
import gg.essential.gui.common.and
import gg.essential.universal.ChatColor
import gg.essential.universal.UMatrixStack
import gg.essential.util.bindEssentialTooltip
import gg.essential.gui.util.hoveredState
import java.awt.Color
import kotlin.math.max

/**
 * An extension of Elementa's [UIText] where the shadow is not considered in width or height bounds
 */
class EssentialUIText @JvmOverloads constructor(
    text: String = "",
    shadow: Boolean = true,
    shadowColor: Color? = null,
    centeringContainsShadow: Boolean = shadow,
    val truncateIfTooSmall: Boolean = false,
    showTooltipForTruncatedText: Boolean = true,
    val centerTruncatedText: Boolean = false,
) : UIText(text, shadow, shadowColor) {

    private val truncatedState = BasicState(false)
    private val fullText = BasicState(text)
    private val actualTextWidth = BasicState(constraints.getWidth())

    val textWidth = ReadOnlyState(actualTextWidth)

    init {
        // UIText overrides [getWidth] and multiplies the value of the width constraint
        // by the text scale. Instead of this behavior, we instead update the default width
        // constraint so that it includes the multiplication by text scale there. As a result,
        // setting `width = min(width, FillConstraint(useSiblings = false))` will now behave as expected
        setWidth(basicWidthConstraint {
            getTextWidth() * getTextScale()
        })
        var fontProvider: FontProvider = getFontProvider().disregardShadows()
        if (!centeringContainsShadow) {
            fontProvider = fontProvider.disregardBelowLineHeight()
        }
        setFontProvider(fontProvider)
        if (truncateIfTooSmall && showTooltipForTruncatedText) {
            bindEssentialTooltip(hoveredState() and truncatedState, fullText)
        }
        setColor(EssentialPalette.TEXT_HIGHLIGHT)
    }

    override fun getWidth(): Float {
        return constraints.getWidth()
    }

    override fun draw(matrixStack: UMatrixStack) {
        val textScale = getTextScale()
        val constrainedWidth = constraints.getWidth()

        val renderedTruncated: Boolean
        if (truncateIfTooSmall && getTextWidth() * textScale > constrainedWidth) {
            val fontProvider = getFontProvider()
            val oldWidth = constraints.width
            val oldX = constraints.x
            val text = getText()
            val line = text.split("\n").first().trim()
            val suffix = "..."

            fun truncate(endIndex: Int /* exclusive */): String {
                return when {
                    endIndex <= 0 -> suffix
                    endIndex > line.lastIndex -> line + suffix
                    else -> {
                        // If this would split a color code in the middle, truncate the color char as well
                        if (line[endIndex - 1] == ChatColor.COLOR_CHAR && line[endIndex].isValidFormatCode()) {
                            return truncate(endIndex - 1)
                        }
                        line.substring(0, endIndex) + suffix
                    }
                }
            }

            var low = 0
            var high = line.lastIndex
            while (low <= high) {
                val mid = (low + high).ushr(1)
                val tooLong = truncate(mid).width(textScale, fontProvider) > constrainedWidth
                if (tooLong) {
                    high = mid - 1
                } else {
                    low = mid + 1
                }
            }
            val truncated = truncate(low - 1)

            // The truncated text can have a width that is slightly less than this component.
            // This difference would ordinarily cause the text to render an incorrect scale,
            // so we update the width of the component to exactly match the truncated text.
            val truncatedWidth = truncated.width(textScale, fontProvider)
            actualTextWidth.set(truncatedWidth)
            setWidth(actualTextWidth.pixels())
            setText(truncated)
            if (centerTruncatedText) setX(oldX + ((constrainedWidth - truncatedWidth) / 2f).pixels)
            super.draw(matrixStack)
            if (centerTruncatedText) setX(oldX)
            setText(text)
            setWidth(oldWidth)
            renderedTruncated = true
            fullText.set(text)
        } else {
            actualTextWidth.set(constraints.getWidth())
            super.draw(matrixStack)
            renderedTruncated = false
        }

        if (truncatedState.get() != renderedTruncated) {
            Window.enqueueRenderOperation {
                truncatedState.set(renderedTruncated)
            }
        }
    }


}

private fun Char.isValidFormatCode() = ChatColor.entries.any { it.char == this }

private abstract class FontProviderDelegate(delegate: FontProvider) : FontProvider by delegate

private fun FontProvider.disregardBelowLineHeight() = object : FontProviderDelegate(this) {
    override fun getBelowLineHeight(): Float {
        return 0f
    }
}

private fun FontProvider.disregardShadows() = object : FontProviderDelegate(this) {
    override fun getStringWidth(string: String, pointSize: Float): Float {
        return max(0f, super.getStringWidth(string, pointSize) - (pointSize / 10f))
    }

    override fun getStringHeight(string: String, pointSize: Float): Float {
        return max(0f, super.getStringHeight(string, pointSize) - (pointSize / 10f))
    }

    override fun getShadowHeight(): Float {
        return 0f
    }
}

/**
 * Extends Elementa's UIWrappedText to apply the default configuration for Essential text
 * 1. lineSpacing 9f -> 12f in constructor
 * 2. Shadow excluded from width and height calculations via updated font provider
 */
class EssentialUIWrappedText @JvmOverloads constructor(
    text: State<String>,
    shadow: State<Boolean> = BasicState(true),
    shadowColor: State<Color?> = BasicState(null),
    centered: Boolean = false,
    /**
     * Keeps the rendered text within the bounds of the component,
     * inserting an ellipsis ("...") if text is trimmed
     */
    trimText: Boolean = false,
    trimmedTextSuffix: String = "...",
    lineSpacing: Float = 12f,
) : UIWrappedText(text, shadow, shadowColor, centered, trimText, lineSpacing, trimmedTextSuffix) {

    @JvmOverloads
    constructor(
        text: String = "",
        shadow: Boolean = true,
        shadowColor: Color? = null,
        centered: Boolean = false,
        /**
         * Keeps the rendered text within the bounds of the component,
         * inserting an ellipsis ("...") if text is trimmed
         */
        trimText: Boolean = false,
        trimmedTextSuffix: String = "...",
        lineSpacing: Float = 12f,
    ) : this(
        BasicState(text),
        BasicState(shadow),
        BasicState(shadowColor),
        centered,
        trimText,
        trimmedTextSuffix,
        lineSpacing,
    )

    init {
        setFontProvider(getFontProvider().disregardShadows())
        setColor(EssentialPalette.TEXT_HIGHLIGHT)
    }

}
