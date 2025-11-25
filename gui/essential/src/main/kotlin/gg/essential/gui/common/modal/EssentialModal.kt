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

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.common.shadow.EssentialUIWrappedText
import gg.essential.gui.common.shadow.ShadowEffect
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.util.simulateLeftClick
import gg.essential.universal.UKeyboard
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import java.awt.Color

/**
 * Default fade in time for an EssentialModal.
 */
const val defaultEssentialModalFadeTime = 0.5f

/**
 * General modal template for modals in Essential
 *
 * Set [requiresButtonPress] to true if you want to force the user to click an action button to dismiss the modal instead of clicking out of its bounds
 */
@Deprecated(
    "Using EssentialModal is discouraged, use EssentialModal2 instead.",
    replaceWith = ReplaceWith("EssentialModal2"),
)
open class EssentialModal(
    modalManager: ModalManager,
    var requiresButtonPress: Boolean = false,
) : Modal(modalManager) {
    // Button text states
    private val titleTextState = BasicState("").map { it }
    private val contentTextState = BasicState("").map { it }
    private val primaryButtonTextState = BasicState("Continue").map { it }
    private val primaryButtonStyleState = BasicState(OutlineButtonStyle.GRAY.defaultStyle)
    private val primaryButtonHoverStyleState = BasicState(OutlineButtonStyle.GRAY.hoveredStyle)
    private val primaryButtonDisabledStyleState = BasicState(OutlineButtonStyle.GRAY.disabledStyle)
    private val contentTextColorState = BasicState(EssentialPalette.TEXT_HIGHLIGHT).map { it }
    private val titleTextColorState = BasicState(EssentialPalette.ACCENT_BLUE).map { it }
    private val widthState = BasicState(190f)

    // Remappable text spacing
    protected val contentTextSpacingState = titleTextState.zip(contentTextState)
        .map { (title, content) -> title.isNotEmpty() && content.isNotEmpty() }
        .map { if (it) 12f else 0f }
        .map { it }

    // Primary button enabled state
    private val primaryButtonEnabledState = BasicState(true).map { it }

    // State to allow overriding the above state, without rebinding it
    // Primarily added to allow disabling the button while an async operation finishes after running the action
    val primaryButtonEnableStateOverride = BasicState(true)

    // Functions to run when this modal closes
    private val dismissActions = mutableListOf<(Boolean) -> Unit>()

    // Easy properties for adjusting states via [configure]
    var titleText: String by titleTextState
    var contentText: String by contentTextState
    var primaryButtonText: String by primaryButtonTextState
    var primaryButtonStyle: MenuButton.Style by primaryButtonStyleState
    var primaryButtonHoverStyle: MenuButton.Style by primaryButtonHoverStyleState
    var primaryButtonDisabledStyle: MenuButton.Style by primaryButtonDisabledStyleState
    var contentTextColor: Color by contentTextColorState
    var titleTextColor: Color by titleTextColorState
    var modalWidth: Float by widthState

    // For selecting buttons via tab
    private var selectedButton: UIComponent? = null
        set(value) {
            (field as? MenuButton)?.hoveredStyleOverrides?.set(false)
            (field as? OutlineButton)?.forceHoverStyle?.set(false)
            field = value
            (field as? MenuButton)?.hoveredStyleOverrides?.set(true)
            (field as? OutlineButton)?.forceHoverStyle?.set(true)
        }

    protected val keyListener: UIComponent.(Char, Int) -> Unit = keyListener@{_, keyCode ->
        if (modalManager.isCurrentlyFadingIn) { return@keyListener }

        fun UIComponent.isEnabled(): Boolean {
            return when (this) {
                is MenuButton -> this.enabled
                is OutlineButton -> this.enabled
                else -> false
            }
        }

        when (keyCode) {
            // Activate selected button on enter or primary button if no button is selected
            UKeyboard.KEY_ENTER, UKeyboard.KEY_NUMPADENTER -> selectedButton.let {
                Window.enqueueRenderOperation {
                    if (it != null) {
                        it.simulateLeftClick()
                    } else {
                        primaryActionButton.simulateLeftClick()
                    }
                }
            }
            // Select next button on tab or wrap around to first button
            UKeyboard.KEY_TAB -> {
                // For getting previous button if shift+tab
                val direction = if (UKeyboard.isShiftKeyDown()) -1 else 1
                // Get all buttons in menu and set default button
                val allButtons = this@EssentialModal.findChildrenOfType<OutlineButton>(true)
                var nextButton = primaryActionButton

                if (selectedButton != null) {
                    // Get next button
                    nextButton = allButtons[(allButtons.indexOf(selectedButton) + direction).mod(allButtons.size)]
                }

                // If next button is disabled, keep going until we have one enabled or have exhausted the list
                var checked = 1
                while (!nextButton.isEnabled()) {
                    if (checked == allButtons.size) { break }

                    nextButton = allButtons[(allButtons.indexOf(nextButton) + direction).mod(allButtons.size)]
                    checked++
                }

                // Select next button if enabled
                selectedButton = if (nextButton.isEnabled()) nextButton else null
            }
            // Deselect selected button on any other key press
            else -> if (!UKeyboard.isShiftKeyDown()) { selectedButton = null }
        }
    }

    // The action to run when the primary button is clicked
    // Can be used to override the default primary action (tryDismiss(true))
    var primaryButtonAction: (() -> Unit)? = { tryDismiss(true) }

    protected val container by HighlightedBlock(
        backgroundColor = EssentialPalette.MODAL_BACKGROUND,
        highlightColor = EssentialPalette.BUTTON_HIGHLIGHT,
        highlightHoverColor = EssentialPalette.BUTTON_HIGHLIGHT,
    )

    val content by UIContainer().constrain {
        x = CenterConstraint()
        y = CenterConstraint() + 1.pixel
        width = PixelConstraint(1f).bindValue(widthState)
        height = ChildBasedSizeConstraint() + 1.pixel
    } childOf container

    private val titleContainer by UIContainer().bindConstraints(titleTextState.map { it.isNotEmpty() }) {
        x = CenterConstraint()
        y = 1.pixel
        width = 100.percent
        height = if (it) {
            ChildBasedSizeConstraint()
        } else {
            0.pixels
        }
    } childOf content

    protected val title by EssentialUIWrappedText(
        centered = true,
        shadowColor = EssentialPalette.BLACK,
    ).bindText(titleTextState).constrain {
        width = 100.percent
        color = titleTextColorState.toConstraint()
    }.bindParent(titleContainer, titleTextState.map { it.isNotEmpty() })

    private val textContainer by UIContainer().bindConstraints(contentTextSpacingState) {
        x = CenterConstraint()
        width = 100.percent
        height = ChildBasedSizeConstraint()
        y = SiblingConstraint(it)
    } childOf content

    protected val textContent by EssentialUIWrappedText(
        centered = true,
        shadowColor = EssentialPalette.BLACK,
    ).bindText(contentTextState).constrain {
        x = CenterConstraint()
        width = 100.percent
        color = contentTextColorState.toConstraint()
    }.bindParent(textContainer, contentTextState.map { it.isNotEmpty() })

    protected val customContent by UIContainer().constrain {
        y = SiblingConstraint(4f)
        x = CenterConstraint()
        height = ChildBasedSizeConstraint()
        width = 100.percent
    } childOf content

    protected val buttonContainer by UIContainer().constrain {
        x = CenterConstraint()
        y = 0.pixels(alignOpposite = true)
        width = ChildBasedSizeConstraint()
        height = ChildBasedMaxSizeConstraint()
    } childOf content

    val primaryActionButton: UIComponent by OutlineButton(
            // doing this "properly" with a memo and states each converted to v2 made this crash with a concurrent modification...
            primaryButtonStyleState.zip(primaryButtonHoverStyleState).zip(primaryButtonDisabledStyleState).toV2().map {
                StyledButton.Style(it.first.first, it.first.second, it.second)
            },
            (primaryButtonEnabledState and primaryButtonEnableStateOverride).not().toV2(),
        ) { currentStyle ->
            text(primaryButtonTextState, Modifier.alignVertical(Alignment.Center(true)).textStyle(currentStyle))
        }.effect(ShadowEffect(EssentialPalette.BLACK)).constrain {
            width = 91.pixels
        }.onLeftClick {
            USound.playButtonPress()
            primaryButtonAction?.invoke()
        } childOf buttonContainer

    init {
        container.constrainBasedOnChildren()
        container.contentContainer.constrain {
            width = ChildBasedSizeConstraint() + 32.pixels
            height = ChildBasedSizeConstraint() + 34.pixels
        }

        // Deselect selected button on click
        onMouseClick { selectedButton = null }

        platform.dismissModalOnScreenChange(this) { tryDismiss(false) }
    }

    override fun LayoutScope.layoutModal() {
        container()
    }

    /**
     * Sets the title of this modal. If title is empty, component will be hidden
     */
    fun setTitle(title: State<String>): EssentialModal {
        titleTextState.rebind(title)
        return this
    }

    /**
     * Sets the text content of this modal. If content is empty, the component will be hidden
     */
    fun setTextContent(content: State<String>) = apply {
        contentTextState.rebind(content)
    }

    /**
     * Allows the developer complete control over content
     */
    fun configureLayout(configure: (UIContainer) -> Unit) = apply {
        configure(customContent)
    }

    /**
     * Executes the supplied callback when the modal is dismissed
     *
     * Boolean parameter is true if the user presses the dismiss button and false if they click out of bounds
     */
    open fun onPrimaryOrDismissAction(callback: (Boolean) -> Unit) = apply {
        dismissActions.add(callback)
    }

    /**
     * State controlling whether the primary / confirm action is allowed to be pressed
     * This can be overriden, while preserving the current bound state, by primaryButtonEnableStateOverride
     */
    fun bindConfirmAvailable(enabled: State<Boolean>) = apply {
        primaryButtonEnabledState.rebind(enabled)
    }

    /**
     * Sets the button text of the primary button
     */
    fun bindPrimaryButtonText(text: State<String>) = apply {
        primaryButtonTextState.rebind(text)
    }

    override fun onOpen() {
        super.onOpen()
        Window.of(this).onKeyType(keyListener)
    }

    /**
     * Called when user gives input that may dismiss the modal
     */
    private fun tryDismiss(buttonPressed: Boolean) {
        if (!buttonPressed && requiresButtonPress) {
            return
        }

        for (dismissAction in dismissActions) {
            dismissAction(buttonPressed)
        }

        close()
    }

    override fun onClose() {
        super.onClose()

        Window.of(this@EssentialModal).keyTypedListeners.remove(keyListener)
    }

    override fun handleEscapeKeyPress() {
        if (requiresButtonPress) {
            return
        }

        super.handleEscapeKeyPress()
    }
}

/**
 * Easy function for configuring a modals properties inline
 */
inline fun <T : EssentialModal> T.configure(config: T.() -> Unit) = apply {
    this.config()
}
