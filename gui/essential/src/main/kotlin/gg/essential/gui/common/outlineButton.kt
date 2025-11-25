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
package gg.essential.gui.common

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.StyledButton.Style
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedWidth
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.effect
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.layoutAsBox
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.util.hoverScope
import gg.essential.util.darker
import gg.essential.vigilance.utils.onLeftClick

/**
 * An outline button with only text for use in various menus.
 *
 * @param style The style for this button, containing the [MenuButton.Style] for the default, hovered & disabled states.
 * @param disabled Whether this button is disabled or not. When true, click events will not be propagated.
 * @param text The text to display in the button.
 */
fun LayoutScope.outlineButton(
    text: State<String>,
    modifier: Modifier = Modifier,
    style: Style = OutlineButtonStyle.GRAY,
    disabled: State<Boolean> = stateOf(false),
): OutlineButton {
    return outlineButton(
        modifier,
        stateOf(style),
        disabled,
    ) { currentStyle ->
        text(text, Modifier.alignVertical(Alignment.Center(true)).textStyle(currentStyle))
    }
}

/**
 * An outline button builder for use in various menus.
 *
 * @param style The style for this button, containing the [MenuButton.Style] for the default, hovered & disabled states.
 * @param disabled Whether this button is disabled or not. When true, click events will not be propagated.
 * @param content The actual content of the button, text can be styled using [Modifier.textStyle].
 */
fun LayoutScope.outlineButton(
    modifier: Modifier = Modifier,
    style: Style = OutlineButtonStyle.GRAY,
    disabled: State<Boolean> = stateOf(false),
    content: LayoutScope.(style: State<MenuButton.Style>) -> Unit
): OutlineButton {
    return outlineButton(
        modifier,
        stateOf(style),
        disabled,
        content,
    )
}

/**
 * An outline button builder for use in various menus.
 *
 * @param style The style for this button, containing the [MenuButton.Style] for the default, hovered & disabled states.
 * @param disabled Whether this button is disabled or not. When true, click events will not be propagated.
 * @param content The actual content of the button, text can be styled using [Modifier.textStyle].
 */
fun LayoutScope.outlineButton(
    modifier: Modifier = Modifier,
    style: State<Style>,
    disabled: State<Boolean> = stateOf(false),
    content: LayoutScope.(style: State<MenuButton.Style>) -> Unit
): OutlineButton {
    return OutlineButton(style, disabled, content)(modifier)
}

/**
 * An immutable outline button.
 * To build one of these, see [outlineButton].
 */
class OutlineButton(
    /** The style for this button. */
    private val style: State<Style>,

    /** Whether this button is disabled or not. When true, click events will not be propagated. */
    private val disabled: State<Boolean>,

    private val content: LayoutScope.(style: State<MenuButton.Style>) -> Unit,
) : UIBlock() {

    val enabled: Boolean
        get() = !disabled.getUntracked()

    @Deprecated("Workaround for legacy EssentialModal, don't use")
    val forceHoverStyle = mutableStateOf(false)

    private val hovered = hoverScope().toV2()

    private val currentStyle = memo {
        val style = style()
        when {
            disabled() -> style.disabledStyle
            forceHoverStyle() || hovered() -> style.hoveredStyle
            else -> style.defaultStyle
        }
    }

    init {
        layoutAsBox(
            Modifier.childBasedWidth(5f).height(19f).color(memo { currentStyle().buttonColor })
                .effect {
                    OutlineEffect(memo { currentStyle().outlineColor }.toV1(this), stateOf(1f).toV1(this), drawInsideChildren = true)
                }.hoverScope()
        ) {
            box(Modifier.alignVertical(Alignment.Center(true))) {
                content(currentStyle)
            }
        }
        onLeftClick { event ->
            if (disabled.getUntracked()) {
                event.stopImmediatePropagation()
            }
        }
    }
}

class OutlineButtonStyle {
    companion object {
        val GREEN = Style(
            MenuButton.Style(EssentialPalette.TEXT_HIGHLIGHT, EssentialPalette.GREEN_OUTLINE_BUTTON, EssentialPalette.GREEN_OUTLINE_BUTTON_OUTLINE),
            MenuButton.Style(EssentialPalette.TEXT_HIGHLIGHT, EssentialPalette.GREEN_OUTLINE_BUTTON_HOVER, EssentialPalette.GREEN_OUTLINE_BUTTON_OUTLINE_HOVER),
            MenuButton.Style(EssentialPalette.TEXT_DISABLED, EssentialPalette.GREEN_OUTLINE_BUTTON.darker(0.5f), EssentialPalette.GREEN_OUTLINE_BUTTON_OUTLINE.darker(0.5f))
        )
        val RED = Style(
            MenuButton.Style(EssentialPalette.TEXT_HIGHLIGHT, EssentialPalette.RED_OUTLINE_BUTTON, EssentialPalette.RED_OUTLINE_BUTTON_OUTLINE),
            MenuButton.Style(EssentialPalette.TEXT_HIGHLIGHT, EssentialPalette.RED_OUTLINE_BUTTON_HOVER, EssentialPalette.RED_OUTLINE_BUTTON_OUTLINE_HOVER),
            MenuButton.Style(EssentialPalette.TEXT_DISABLED, EssentialPalette.RED_OUTLINE_BUTTON.darker(0.5f), EssentialPalette.RED_OUTLINE_BUTTON_OUTLINE.darker(0.5f))
        )
        val BLUE = Style(
            MenuButton.Style(EssentialPalette.TEXT_HIGHLIGHT, EssentialPalette.BLUE_OUTLINE_BUTTON, EssentialPalette.BLUE_OUTLINE_BUTTON_OUTLINE),
            MenuButton.Style(EssentialPalette.TEXT_HIGHLIGHT, EssentialPalette.BLUE_OUTLINE_BUTTON_HOVER, EssentialPalette.BLUE_OUTLINE_BUTTON_OUTLINE_HOVER),
            MenuButton.Style(EssentialPalette.TEXT_DISABLED, EssentialPalette.BLUE_OUTLINE_BUTTON.darker(0.5f), EssentialPalette.BLUE_OUTLINE_BUTTON_OUTLINE.darker(0.5f))
        )
        val GRAY = Style(
            MenuButton.Style(EssentialPalette.TEXT_HIGHLIGHT, EssentialPalette.GRAY_OUTLINE_BUTTON, EssentialPalette.GRAY_OUTLINE_BUTTON_OUTLINE),
            MenuButton.Style(EssentialPalette.TEXT_HIGHLIGHT, EssentialPalette.GRAY_OUTLINE_BUTTON_HOVER, EssentialPalette.GRAY_OUTLINE_BUTTON_OUTLINE_HOVER),
            MenuButton.Style(EssentialPalette.TEXT_DISABLED, EssentialPalette.GRAY_OUTLINE_BUTTON.darker(0.5f), EssentialPalette.GRAY_OUTLINE_BUTTON_OUTLINE.darker(0.5f))
        )
        val YELLOW = Style(
            MenuButton.Style(EssentialPalette.TEXT_HIGHLIGHT, EssentialPalette.YELLOW_OUTLINE_BUTTON, EssentialPalette.YELLOW_OUTLINE_BUTTON_OUTLINE),
            MenuButton.Style(EssentialPalette.TEXT_HIGHLIGHT, EssentialPalette.YELLOW_OUTLINE_BUTTON_HOVER, EssentialPalette.YELLOW_OUTLINE_BUTTON_OUTLINE_HOVER),
            MenuButton.Style(EssentialPalette.TEXT_DISABLED, EssentialPalette.YELLOW_OUTLINE_BUTTON.darker(0.5f), EssentialPalette.YELLOW_OUTLINE_BUTTON_OUTLINE.darker(0.5f))
        )
        val PURPLE = Style(
            MenuButton.Style(EssentialPalette.TEXT_HIGHLIGHT, EssentialPalette.PURPLE_OUTLINE_BUTTON, EssentialPalette.PURPLE_OUTLINE_BUTTON_OUTLINE),
            MenuButton.Style(EssentialPalette.TEXT_HIGHLIGHT, EssentialPalette.PURPLE_OUTLINE_BUTTON_HOVER, EssentialPalette.PURPLE_OUTLINE_BUTTON_OUTLINE_HOVER),
            MenuButton.Style(EssentialPalette.TEXT_DISABLED, EssentialPalette.PURPLE_OUTLINE_BUTTON.darker(0.5f), EssentialPalette.PURPLE_OUTLINE_BUTTON_OUTLINE.darker(0.5f))
        )
    }
}
