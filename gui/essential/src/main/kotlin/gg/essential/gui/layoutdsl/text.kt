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
package gg.essential.gui.layoutdsl

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.components.inspector.Inspector
import gg.essential.elementa.dsl.coerceAtMost
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.common.shadow.EssentialUIWrappedText
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.universal.ChatColor

fun LayoutScope.text(
    text: String,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    shadow: Boolean = true,
    truncateIfTooSmall: Boolean = false,
    centeringContainsShadow: Boolean = shadow,
    showTooltipForTruncatedText: Boolean = true,
    centerTruncatedText: Boolean = false,
) = text(
    BasicState(text),
    modifier,
    scale,
    shadow,
    truncateIfTooSmall,
    centeringContainsShadow,
    showTooltipForTruncatedText,
    centerTruncatedText,
)

fun LayoutScope.text(
    text: State<String>,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    shadow: Boolean = true,
    truncateIfTooSmall: Boolean = false,
    centeringContainsShadow: Boolean = shadow,
    showTooltipForTruncatedText: Boolean = true,
    centerTruncatedText: Boolean = false,
) = EssentialUIText(
    shadow = shadow,
    truncateIfTooSmall = truncateIfTooSmall,
    centeringContainsShadow = centeringContainsShadow,
    showTooltipForTruncatedText = showTooltipForTruncatedText,
    centerTruncatedText = centerTruncatedText,
).bindText(text).constrain {
    if (truncateIfTooSmall) {
        width = width.coerceAtMost(100.percent)
    }
    textScale = scale.pixels()
}(modifier)

fun LayoutScope.text(
    text: gg.essential.gui.elementa.state.v2.State<String>,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    shadow: Boolean = true,
    truncateIfTooSmall: Boolean = false,
    centeringContainsShadow: Boolean = shadow,
    showTooltipForTruncatedText: Boolean = true,
    centerTruncatedText: Boolean = false,
) = EssentialUIText(
    shadow = shadow,
    truncateIfTooSmall = truncateIfTooSmall,
    centeringContainsShadow = centeringContainsShadow,
    showTooltipForTruncatedText = showTooltipForTruncatedText,
    centerTruncatedText = centerTruncatedText,
).apply {
    bindText(text.toV1(this)).constrain {
        if (truncateIfTooSmall) {
            width = width.coerceAtMost(100.percent)
        }
        textScale = scale.pixels()
    }
}(modifier)

fun LayoutScope.wrappedText(
    text: String,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    shadow: Boolean = true,
    trimText: Boolean = false,
    lineSpacing: Float = 12f,
) = wrappedText(BasicState(text), modifier, centered, BasicState(shadow), trimText, lineSpacing)

fun LayoutScope.wrappedText(
    text: State<String>,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    shadow: State<Boolean> = BasicState(true),
    trimText: Boolean = false,
    lineSpacing: Float = 12f,
): EssentialUIWrappedText {
    val component = EssentialUIWrappedText(text, shadow, centered = centered, trimText = trimText, lineSpacing = lineSpacing)
        .constrain {
            width = width.coerceAtMost(100.percent)
        }
    component(modifier)
    return component
}

@Suppress("unused")
private val init = run {
    Inspector.registerComponentFactory(null)
}

fun Modifier.bold() = this then BasicTextPrefixModifier(ChatColor.BOLD.toString())

fun Modifier.underline() = this then BasicTextPrefixModifier(ChatColor.UNDERLINE.toString())

class BasicTextPrefixModifier(private val prefix: String) : Modifier {
    override fun applyToComponent(component: UIComponent): () -> Unit {
        return when (component) {
            is UIText -> {
                component.setText(prefix + component.getText());
                {
                    component.setText(component.getText().removePrefix(prefix))
                }
            }

            is UIWrappedText -> {
                component.setText(prefix + component.getText());
                {
                    component.setText(component.getText().removePrefix(prefix))
                }
            }

            else -> throw IllegalArgumentException("Cannot apply BasicTextPrefixModifier to ${component::class.simpleName}")
        }
    }
}