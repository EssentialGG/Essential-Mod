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

import gg.essential.elementa.components.inspector.Inspector
import gg.essential.elementa.state.State
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.elementa.state.v2.*
import gg.essential.util.*

fun Modifier.tooltip(
    content: gg.essential.gui.elementa.state.v2.State<String>,
    textModifier: Modifier = Modifier,
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
    padding: Float = 5f,
    wrapAtWidth: Float? = null,
    notchSize: Int = 3,
    configureTooltip: EssentialTooltip.() -> Unit = {},
) = this then {
    val tooltip = createEssentialTooltip(
        content,
        position,
        padding,
        wrapAtWidth,
        textModifier::applyToComponent,
        notchSize = notchSize,
        configureTooltip = configureTooltip,
    )
    tooltip.showTooltip()
    return@then { tooltip.hideTooltip() }
}

fun Modifier.tooltip(
    content: State<String>,
    textModifier: Modifier = Modifier,
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
    padding: Float = 5f,
    wrapAtWidth: Float? = null,
    notchSize: Int = 3,
    configureTooltip: EssentialTooltip.() -> Unit = {},
) = tooltip(content.toV2(), textModifier, position, padding, wrapAtWidth, notchSize, configureTooltip)

fun Modifier.tooltip(
    content: String,
    textModifier: Modifier = Modifier,
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
    padding: Float = 5f,
    wrapAtWidth: Float? = null,
    notchSize: Int = 3,
    configureTooltip: EssentialTooltip.() -> Unit = {},
) = tooltip(stateOf(content), textModifier, position, padding, wrapAtWidth, notchSize, configureTooltip)

fun Modifier.hoverTooltip(
    content: String,
    textModifier: Modifier = Modifier,
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
    padding: Float = 5f,
    wrapAtWidth: Float? = null,
    notchSize: Int = 3,
    configureTooltip: EssentialTooltip.() -> Unit = {},
) = whenHovered(Modifier.tooltip(content, textModifier, position, padding, wrapAtWidth, notchSize, configureTooltip))

fun Modifier.hoverTooltip(
    content: State<String>,
    textModifier: Modifier = Modifier,
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
    padding: Float = 5f,
    wrapAtWidth: Float? = null,
    notchSize: Int = 3,
    configureTooltip: EssentialTooltip.() -> Unit = {},
) = whenHovered(Modifier.tooltip(content.toV2(), textModifier, position, padding, wrapAtWidth, notchSize, configureTooltip))

fun Modifier.hoverTooltip(
    content: gg.essential.gui.elementa.state.v2.State<String>,
    textModifier: Modifier = Modifier,
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
    padding: Float = 5f,
    wrapAtWidth: Float? = null,
    notchSize: Int = 3,
    configureTooltip: EssentialTooltip.() -> Unit = {},
) = whenHovered(Modifier.tooltip(content, textModifier, position, padding, wrapAtWidth, notchSize, configureTooltip))

fun Modifier.tooltip(
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
    padding: Float = 5f,
    windowPadding: Float? = null,
    layout: LayoutScope.() -> Unit,
) = this then {
    val tooltip = createLayoutDslTooltip(position, padding, windowPadding, layout)
    tooltip.showTooltip()
    return@then { tooltip.hideTooltip() }
}

fun Modifier.hoverTooltip(
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
    padding: Float = 5f,
    windowPadding: Float? = null,
    layout: LayoutScope.() -> Unit,
) = whenHovered(Modifier.tooltip(position, padding, windowPadding, layout))

@Suppress("unused")
private val init = run {
    Inspector.registerComponentFactory(null)
}
