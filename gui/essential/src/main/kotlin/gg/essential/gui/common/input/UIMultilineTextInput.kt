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
package gg.essential.gui.common.input

import gg.essential.elementa.constraints.HeightConstraint
import gg.essential.elementa.dsl.coerceAtMost
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.width
import gg.essential.gui.EssentialPalette
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMatrixStack
import java.awt.Color

class UIMultilineTextInput @JvmOverloads constructor(
    placeholder: String = "",
    shadow: Boolean = true,
    shadowColor: Color? = null,
    selectionBackgroundColor: Color = EssentialPalette.TEXT_HIGHLIGHT_BACKGROUND,
    selectionForegroundColor: Color = EssentialPalette.TEXT_HIGHLIGHT,
    allowInactiveSelection: Boolean = false,
    inactiveSelectionBackgroundColor: Color = Color(176, 176, 176),
    inactiveSelectionForegroundColor: Color = Color.WHITE,
    cursorColor: Color = EssentialPalette.TEXT_HIGHLIGHT,
) : AbstractTextInput(
    placeholder,
    shadow,
    shadowColor,
    selectionBackgroundColor,
    selectionForegroundColor,
    allowInactiveSelection,
    inactiveSelectionBackgroundColor,
    inactiveSelectionForegroundColor,
    cursorColor,
) {
    private var maxHeight: HeightConstraint? = null

    fun setMaxHeight(maxHeight: HeightConstraint) = apply {
        this.maxHeight = maxHeight
    }

    override fun getText() = textualLines.joinToString("\n") { it.text }

    override fun textToLines(text: String): List<String> {
        return text.split('\n')
    }

    override fun scrollIntoView(pos: LinePosition) {
        val visualPos = pos.toVisualPos()

        val visualLineOffset = visualPos.line * -lineHeightWithPadding

        if (targetVerticalScrollingOffset < visualLineOffset) {
            targetVerticalScrollingOffset = visualLineOffset
        } else if (visualLineOffset - lineHeightWithPadding < targetVerticalScrollingOffset - getHeight()) {
            targetVerticalScrollingOffset += visualLineOffset - lineHeightWithPadding - (targetVerticalScrollingOffset - getHeight())
        }
    }

    override fun recalculateDimensions() {

        var height: HeightConstraint = (lineHeightWithPadding * visualLines.size - linePadding).pixels()

        if (maxHeight != null) {
            height = height.coerceAtMost(maxHeight!!)
        }

        setHeight(height)
    }

    override fun onEnterPressed() {
        if (UKeyboard.isShiftKeyDown()) {
            commitTextAddition("\n")
            updateAction(getText())
            textState.set(getText())
        } else {
            activateAction(getText())
        }
    }

    override fun draw(matrixStack: UMatrixStack) {
        beforeDraw(matrixStack)

        val textScale = getTextScale()
        if (!hasText()) {
            drawPlaceholder(matrixStack)
        }

        if (hasSelection()) {
            cursorComponent.hide(instantly = true)
        } else if (active) {
            cursorComponent.unhide()
            val (cursorPosX, cursorPosY) = cursor.toScreenPos()
            cursorComponent.setX((cursorPosX ).pixels())
            cursorComponent.setY((cursorPosY ).pixels())
        }

        val (selectionStart, selectionEnd) = getSelection()

        for ((i, visualLine) in visualLines.withIndex()) {
            val topOffset = (lineHeightWithPadding * i * getTextScale()) + verticalScrollingOffset
            if (topOffset < -lineHeightWithPadding * getTextScale() || topOffset > getHeight() + lineHeightWithPadding * getTextScale())
                continue

            if (!hasSelection() || i < selectionStart.line || i > selectionEnd.line) {
                drawUnselectedText(matrixStack, visualLine.text, getLeft(), i)
            } else {
                val startText = when {
                    i == selectionStart.line && selectionStart.column > 0 -> {
                        visualLine.text.substring(0, selectionStart.column)
                    }

                    else -> ""
                }

                val selectedText = when {
                    selectionStart.line == selectionEnd.line -> visualLine.text.substring(
                        selectionStart.column,
                        selectionEnd.column
                    )
                    i > selectionStart.line && i < selectionEnd.line -> visualLine.text
                    i == selectionStart.line -> visualLine.text.substring(selectionStart.column)
                    i == selectionEnd.line -> visualLine.text.substring(0, selectionEnd.column)
                    else -> ""
                }

                val endText = when {
                    i == selectionEnd.line && selectionEnd.column < visualLines[i].length -> {
                        visualLine.text.substring(selectionEnd.column)
                    }
                    else -> ""
                }

                val startTextWidth = startText.width(textScale)
                val selectedTextWidth = selectedText.width(textScale)

                val newlinePadding = if (i < selectionEnd.line) ' '.width(textScale) else 0f

                if (startText.isNotEmpty())
                    drawUnselectedText(matrixStack, startText, getLeft(), i)

                if (selectedText.isNotEmpty() || newlinePadding != 0f) {
                    drawSelectedText(
                        matrixStack,
                        selectedText,
                        getLeft() + startTextWidth,
                        getLeft() + startTextWidth + selectedTextWidth + newlinePadding,
                        i
                    )
                }

                if (endText.isNotEmpty())
                    drawUnselectedText(matrixStack, endText, getLeft() + startTextWidth + selectedTextWidth, i)
            }
        }

        super.draw(matrixStack, )
    }

    override fun screenPosToVisualPos(x: Float, y: Float): LinePosition {
        val realY = y - verticalScrollingOffset

        if (realY < 0)
            return LinePosition(0, 0, isVisual = true)

        val line = (realY / (lineHeightWithPadding * getTextScale())).toInt()
        if (line > visualLines.lastIndex)
            return LinePosition(visualLines.lastIndex, visualLines.last().text.length, isVisual = true)

        val text = visualLines[line].text
        var column = 0
        var currWidth = 0f

        if (x <= 0)
            return LinePosition(line, 0, isVisual = true)
        if (x >= getWidth())
            return LinePosition(line, visualLines[line].text.length, isVisual = true)

        for (char in text.toCharArray()) {
            val charWidth = char.width(getTextScale())
            if (currWidth + (charWidth / 2) >= x)
                return LinePosition(line, column, isVisual = true)

            currWidth += charWidth
            column++
        }

        return LinePosition(line, column, isVisual = true)
    }
}
