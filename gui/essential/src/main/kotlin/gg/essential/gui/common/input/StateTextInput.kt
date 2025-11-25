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

import gg.essential.elementa.constraints.animation.*
import gg.essential.elementa.dsl.*
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.input.StateTextInput.*
import gg.essential.gui.elementa.state.v2.*
import gg.essential.vigilance.utils.onLeftClick
import org.jetbrains.annotations.ApiStatus
import java.awt.Color

/**
 * Simple text input that sets [state] on enter and focus lost. The text is converted to `T` using [parse].
 * If the input is not valid (that is, [parse] throws a [ParseException]), an error animation is shown.
 */
class StateTextInput<T>(
    val state: MutableState<T>,
    mutable: Boolean,
    textPadding: Float = 2f,
    maxLength: Int = Int.MAX_VALUE,
    private val formatToText: (T) -> String,
    private val parse: (String) -> T,
) : UITextInput(maxLength = maxLength) {

    init {
        if (mutable) {
            onLeftClick {
                grabWindowFocus()
            }
        }
        constrain {
            width = basicWidthConstraint {
                getText().width(getTextScale(), getFontProvider()) + textPadding + if (active) 1f else 0f
            }
            color = EssentialPalette.TEXT.toConstraint()
        }

        onFocusLost {
            if (!updateState()) {
                cloneStateToInput()
            }
        }
        effect(this) {
            setText(formatToText(state()))
        }
    }

    /**
     * Sets the value of the input to the current value of the state
     */
    private fun cloneStateToInput() {
        setText(formatToText(state.getUntracked()))
    }

    override fun onEnterPressed() {
        if (updateState()) {
            cloneStateToInput()
        }
    }

    /**
     * Tries to update the state based on the current value of the text input.
     * Returns true if the state was updated.
     */
    private fun updateState(): Boolean {
        val mappedValue = try {
            parse(getText())
        } catch (e: ParseException) {
            animateError()
            return false
        }
        state.set(mappedValue)
        return true
    }

    /**
     * Plays an animation indicating that the value is invalid.
     */
    private fun animateError() {
        // Already animating
        if (constraints is AnimatingConstraints) {
            return
        }
        val oldSelectionForegroundColor = selectionForegroundColor
        val oldInactiveSelectionForegroundColor = inactiveSelectionForegroundColor
        val oldColor = getColor()
        val oldX = constraints.x
        selectionForegroundColor = Color.RED
        inactiveSelectionForegroundColor = Color.RED
        setColor(Color.RED)
        animate {
            setXAnimation(Animations.IN_BOUNCE, .25f, oldX + 3.pixels)
            onComplete {
                setX(oldX)
                setColor(oldColor)
                selectionForegroundColor = oldSelectionForegroundColor
                inactiveSelectionForegroundColor = oldInactiveSelectionForegroundColor
            }
        }
    }

    /**
     * Thrown to show that the current value of the text input is not valid.
     */
    @ApiStatus.Internal
    class ParseException : Exception()
}
