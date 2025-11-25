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

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.Spacer
import gg.essential.gui.common.constraints.CenterPixelConstraint
import gg.essential.gui.common.input.UITextInput
import gg.essential.gui.common.input.essentialInput
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.overlay.ModalManager
import gg.essential.vigilance.utils.onLeftClick

/**
 * A modal that has a text field
 */
open class CancelableInputModal(
    modalManager: ModalManager,
    placeholderText: String,
    initialText: String = "",
    maxLength: Int = Int.MAX_VALUE,
    private val selectInitialText: Boolean = initialText.isNotEmpty(),
) : ConfirmDenyModal(modalManager, false) {

    private val inputContainer by UIContainer().constrain {
        x = CenterConstraint()
        y = SiblingConstraint()
        width = 106.pixels
        height = 17.pixels
    } childOf customContent

    private val input by UITextInput(placeholderText, shadowColor = EssentialPalette.BLACK, maxLength = maxLength).constrain {
        x = CenterConstraint()
        y = CenterPixelConstraint(true)
        height = 10.pixels
        color = EssentialPalette.TEXT.toConstraint()
    }.apply {
        onUpdate {
            inputTextState.set(it)
        }
    }

    // Bottom padding
    val bottomSpacer by Spacer(height = 21f) childOf customContent

    val inputTextState = mutableStateOf(initialText)

    private val errorMessageState: MutableState<String?> = mutableStateOf(null)

    init {
        configure {
            titleTextColor = EssentialPalette.TEXT_HIGHLIGHT
            contentTextColor = EssentialPalette.TEXT_MID_GRAY
            contentTextSpacingState.rebind(BasicState(17f))
        }

        inputContainer.layoutAsBox {
            essentialInput(input, errorMessageState = errorMessageState, modifier = Modifier.shadow(EssentialPalette.BLACK))
        }

        // Top padding
        spacer.setHeight(12.pixels)

        inputContainer.onLeftClick {
            input.grabWindowFocus()
        }

        @Suppress("LeakingThis")
        inputTextState.onSetValueAndNow(this) {
            clearError()
            if (input.getText() != it) {
                input.setText(it)
            }
        }
    }

    override fun afterInitialization() {
        super.afterInitialization()
        input.grabWindowFocus()
        if (selectInitialText) {
            input.selectAll()
        }
    }

    override fun onOpen() {
        super.onOpen()
        input.onKeyType(keyListener)
    }

    override fun onClose() {
        input.keyTypedListeners.remove(keyListener)
        super.onClose()
    }

    /**
     * Sets the text input's value to the supplied variable
     */
    fun setText(text: String) = apply {
        input.setText(text)
    }

    /**
     * Supply a function to determine whether the current input is valid and should allow
     * the user to press the continue / primary action button
     */
    fun mapInputToEnabled(mapper: (String) -> Boolean) = apply {
        bindConfirmAvailable(inputTextState.map(mapper).toV1(this))
    }

    /**
     * Calls [callback] when the primary action is triggered with the current value of the text input
     */
    fun onPrimaryActionWithValue(callback: (String) -> Unit) = apply {
        super.onPrimaryAction {
            callback(inputTextState.get())
        }
    }

    /**
     * Sets the input to error state with the specified error message
     */
    fun setError(errorText: String) = apply {
        errorMessageState.set(errorText)
    }

    /**
     * Clears the input error state
     */
    fun clearError() = apply {
        errorMessageState.set(null)
    }

    /**
     * Adds specified characters as allowed
     */
    fun addAllowedCharacters(vararg chars: Char) = apply {
        input.allowedCharacters.addAll(chars.asIterable())
    }

    /**
     * Adds specified characters as allowed
     */
    fun addAllowedCharacters(chars: Iterable<Char>) = apply {
        input.allowedCharacters.addAll(chars)
    }

    /**
     * Sets the maximum input field width
     */
    fun setMaxInputWidth(maxWidth: WidthConstraint) = apply {
        input.setMaxWidth(maxWidth)
    }

    /**
     * Sets the input container width constraint
     */
    fun setInputContainerWidthConstraint(widthConstraint: WidthConstraint) = apply {
        inputContainer.constraints.width = widthConstraint
    }

    /**
     * Sets the input container to scale with the input with [paddingFromInput] padding and a minimum width of [minimumWidth]
     */
    fun setDynamicInputContainerWidth(paddingFromInput: WidthConstraint, minimumWidth: WidthConstraint) = apply {
        inputContainer.constraints.width = (basicWidthConstraint { input.getWidth() } + paddingFromInput).coerceAtLeast(minimumWidth)
    }

}
