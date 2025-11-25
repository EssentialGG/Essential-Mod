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

import gg.essential.elementa.components.GradientComponent
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.utils.invisible
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.common.IconButton
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.util.hoverScope
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color
import java.time.Instant
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

fun LayoutScope.essentialDoubleInput(
    state: MutableState<Double>,
    modifier: Modifier = Modifier,
    min: Double = Double.NEGATIVE_INFINITY,
    max: Double = Double.POSITIVE_INFINITY,
    maxLength: Int = Int.MAX_VALUE,
) = essentialStateTextInput(
    state,
    { it.toString() },
    {
        val num = try {
            it.toDouble()
        } catch (e: NumberFormatException) {
            throw StateTextInput.ParseException()
        }
        if (num > max) throw StateTextInput.ParseException()
        if (num < min) throw StateTextInput.ParseException()
        return@essentialStateTextInput num
    },
    Modifier.width(50f) then modifier,
    maxLength,
)

fun LayoutScope.essentialFloatInput(
    state: MutableState<Float>,
    modifier: Modifier = Modifier,
    min: Float = Float.NEGATIVE_INFINITY,
    max: Float = Float.POSITIVE_INFINITY,
    maxLength: Int = Int.MAX_VALUE,
) = essentialStateTextInput(
    state,
    { it.toString() },
    {
        val num = try {
            it.toFloat()
        } catch (e: NumberFormatException) {
            throw StateTextInput.ParseException()
        }
        if (num > max) throw StateTextInput.ParseException()
        if (num < min) throw StateTextInput.ParseException()
        return@essentialStateTextInput num
    },
    Modifier.width(50f) then modifier,
    maxLength,
)

fun LayoutScope.essentialIntInput(
    state: MutableState<Int>,
    modifier: Modifier = Modifier,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    maxLength: Int = Int.MAX_VALUE,
) = essentialStateTextInput(
    state,
    { it.toString() },
    {
        val num = try {
            it.toInt()
        } catch (e: NumberFormatException) {
            throw StateTextInput.ParseException()
        }
        if (num > max) throw StateTextInput.ParseException()
        if (num < min) throw StateTextInput.ParseException()
        return@essentialStateTextInput num
    },
    Modifier.width(50f) then modifier,
    maxLength,
)

fun LayoutScope.essentialLongInput(
    state: MutableState<Long>,
    modifier: Modifier = Modifier,
    min: Long = Long.MIN_VALUE,
    max: Long = Long.MAX_VALUE,
    maxLength: Int = Int.MAX_VALUE,
) = essentialStateTextInput(
    state,
    { it.toString() },
    {
        val num = try {
            it.toLong()
        } catch (e: NumberFormatException) {
            throw StateTextInput.ParseException()
        }
        if (num > max) throw StateTextInput.ParseException()
        if (num < min) throw StateTextInput.ParseException()
        return@essentialStateTextInput num
    },
    Modifier.width(50f) then modifier,
    maxLength,
)

fun LayoutScope.essentialStringInput(
    state: MutableState<String>,
    modifier: Modifier = Modifier,
    maxLength: Int = Int.MAX_VALUE,
) = essentialStateTextInput(
    state,
    { it },
    { it },
    Modifier.width(120f) then modifier,
    maxLength,
)

fun LayoutScope.essentialNullableStringInput(
    state: MutableState<String?>,
    modifier: Modifier = Modifier,
    maxLength: Int = Int.MAX_VALUE,
) = essentialStateTextInput(
    state,
    { it ?: "" },
    { it.ifEmpty { null } },
    Modifier.width(120f) then modifier,
    maxLength,
)

fun LayoutScope.essentialISODateInput(
    state: MutableState<Instant>,
    modifier: Modifier = Modifier,
) = essentialStateTextInput(
    state,
    formatToText = { it.toString() },
    {
        try {
            Instant.parse(it) // Check the input is valid
        } catch (e: DateTimeParseException) {
            throw StateTextInput.ParseException()
        }
    },
    Modifier.width(125f) then modifier,
)

fun LayoutScope.essentialNullableISODateInput(
    state: MutableState<Instant?>,
    modifier: Modifier = Modifier,
) = essentialStateTextInput(
    state,
    formatToText = { it?.toString() ?: "" },
    {
        try {
            if (it.isEmpty()) {
                null
            } else {
                Instant.parse(it) // Check the input is valid
            }
        } catch (e: DateTimeParseException) {
            throw StateTextInput.ParseException()
        }
    },
    Modifier.width(125f) then modifier,
)

fun LayoutScope.essentialManagedNullableISODateInput(
    state: MutableState<Instant?>,
    modifier: Modifier = Modifier,
): StateTextInput<Instant?> {
    lateinit var input: StateTextInput<Instant?>
    row(Arrangement.spacedBy(5f)) {
        input = essentialNullableISODateInput(state, modifier)
        if_(state.map { it != null }) {
            IconButton(EssentialPalette.CANCEL_5X, tooltipText = "Clear")().onLeftClick {
                state.set(null)
            }
        } `else` {
            IconButton(EssentialPalette.PLUS_5X, tooltipText = "Now")().onLeftClick {
                state.set(Instant.now().truncatedTo(ChronoUnit.SECONDS))
            }
        }
    }
    return input
}

fun <T> LayoutScope.essentialStateTextInput(
    state: MutableState<T>,
    formatToText: (T) -> String,
    parse: (String) -> T,
    modifier: Modifier = Modifier,
    maxLength: Int = Int.MAX_VALUE,
): StateTextInput<T> {
    val input = StateTextInput(state, true, maxLength = maxLength, formatToText = formatToText, parse = parse)
    box(Modifier.width(100f).height(17f) then modifier) {
        essentialInput(input)
    }
    return input
}

// Overload to allow supplying unboxed string
fun LayoutScope.essentialInput(
    input: AbstractTextInput,
    errorState: State<Boolean>,
    errorMessage: String,
    modifier: Modifier = Modifier,
    icon: ImageFactory? = null,
) = essentialInput(input, errorState, stateOf(errorMessage), modifier, icon)

// Overload to allow for boolean input of error message state
fun LayoutScope.essentialInput(
    input: AbstractTextInput,
    errorState: State<Boolean>,
    errorMessage: State<String?>,
    modifier: Modifier = Modifier,
    icon: ImageFactory? = null,
) {
    val state = State { if (errorState()) errorMessage() else null }
    essentialInput(input, errorMessageState = state, modifier = modifier, icon = icon)
}

fun LayoutScope.essentialInput(
    input: AbstractTextInput,
    modifier: Modifier = Modifier,
    errorMessageState: State<String?> = State { null },
    icon: ImageFactory? = null,
    backgroundColor: State<Color> = stateOf(EssentialPalette.GUI_BACKGROUND),
    iconColor: State<Color> = stateOf(EssentialPalette.TEXT_MID_GRAY),
    iconShadowColor: State<Color> = stateOf(EssentialPalette.TEXT_SHADOW),
    errorColor: State<Color> = stateOf(EssentialPalette.TEXT_WARNING),
    errorShadowColor: State<Color> = stateOf(EssentialPalette.BLACK),
    outlineColor: Color = EssentialPalette.LIGHTEST_BACKGROUND,
    outlineFocusedColor: Color = EssentialPalette.BLUE_BUTTON,
    outlineHoveredColor: Color = EssentialPalette.TEXT_DARK_DISABLED,
    outlineErrorColor: Color = EssentialPalette.TEXT_WARNING,
    iconAndInputWidthPadding: Float = 5f,
    iconAndInputPadding: Float = 6f,
    inputModifier: Modifier = Modifier,
) {
    val errorState = errorMessageState.map { it != null }

    val inputFocusedState = mutableStateOf(false)
    val inputHoveredState = input.hoverScope().toV2()
    val outlineColorState = State {
        when {
            errorState() -> outlineErrorColor
            inputFocusedState() -> outlineFocusedColor
            inputHoveredState() -> outlineHoveredColor
            else -> outlineColor
        }
    }
    input.onFocus { inputFocusedState.set(true) }
    input.onFocusLost { inputFocusedState.set(false) }

    if(input is UITextInput) {
        input.setMinWidth(1f.pixels)
        input.setMaxWidth(100.percent - 8.pixels)
    }

    val gradient by object : GradientComponent(BasicState(EssentialPalette.GUI_BACKGROUND), BasicState(EssentialPalette.GUI_BACKGROUND.invisible()), BasicState(GradientDirection.RIGHT_TO_LEFT)) {
        // Override because the gradient should be treated as if it does not exist from an input point of view
        override fun isPointInside(x: Float, y: Float) = false
    }

    box(Modifier.fillParent().color(outlineColorState).hoverScope() then modifier) {
        box(Modifier.fillParent(padding = 1f).color(backgroundColor)) {
            if(icon != null) {
                row(Modifier.fillWidth(padding = iconAndInputWidthPadding).alignVertical(Alignment.Center(true)), Arrangement.spacedBy(iconAndInputPadding, FloatPosition.START)) {
                    box(Modifier.width(10f).heightAspect(1f)) {
                        image(icon, Modifier.color(iconColor).shadow(iconShadowColor))
                    }
                    input(inputModifier)
                }
            } else {
                input(Modifier.alignVertical(Alignment.Center(true)).then(inputModifier))
            }

            if_(errorState) {
                box(Modifier.width(33f).fillHeight().alignHorizontal(Alignment.End)) {
                    gradient(Modifier.fillParent())
                    icon(
                        EssentialPalette.ROUND_WARNING_7X,
                        Modifier.alignHorizontal(Alignment.End(4f)).color(errorColor).shadow(errorShadowColor),
                    ).bindHoverEssentialTooltip(errorMessageState.map { it ?: "" }.toV1(stateScope), EssentialTooltip.Position.ABOVE, wrapAtWidth = 150f)
                }
            }

        }
    }.onMouseClick { event ->
        input.mouseClick(event.absoluteX.toDouble(), event.absoluteY.toDouble(), event.mouseButton)
        event.stopPropagation()
    }
}
