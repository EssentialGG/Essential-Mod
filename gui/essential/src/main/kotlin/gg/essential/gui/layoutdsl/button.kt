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
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.MenuButton
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.image.ImageFactory
import gg.essential.util.*

fun LayoutScope.iconButton(
    imageFactory: ImageFactory,
    modifier: Modifier = Modifier,
    buttonText: String = "",
    tooltipText: String = "",
    layout: IconButton.Layout = IconButton.Layout.ICON_FIRST,
    action: () -> Unit = {},
) = iconButton(
    stateOf(imageFactory),
    modifier,
    stateOf(buttonText),
    stateOf(tooltipText),
    action = action,
    layout = layout,
)

fun LayoutScope.iconButton(
    imageFactory: State<ImageFactory>,
    modifier: Modifier = Modifier,
    buttonText: State<String> = stateOf(""),
    tooltipText: State<String> = stateOf(""),
    enabled: State<Boolean> = stateOf(true),
    iconShadow: State<Boolean> = stateOf(true),
    textShadow: State<Boolean> = stateOf(true),
    tooltipBelowComponent: Boolean = true,
    buttonShadow: Boolean = true,
    layout: IconButton.Layout = IconButton.Layout.ICON_FIRST,
    action: () -> Unit = {}
): IconButton {
    val iconButton = IconButton(
        imageFactory.toV1(stateScope),
        tooltipText.toV1(stateScope),
        enabled.toV1(stateScope),
        buttonText.toV1(stateScope),
        iconShadow.toV1(stateScope),
        textShadow.toV1(stateScope),
        tooltipBelowComponent,
        buttonShadow,
    )
    iconButton(modifier)
    iconButton.onLeftClick { action() }
    iconButton.setLayout(layout)
    return iconButton
}

fun LayoutScope.menuButton(
    buttonText: String,
    modifier: Modifier = Modifier,
    defaultStyle: MenuButton.Style = MenuButton.DARK_GRAY,
    hoverStyle: MenuButton.Style = MenuButton.GRAY,
    disabledStyle: MenuButton.Style = defaultStyle.copy(textColor = EssentialPalette.TEXT_DISABLED),
    action: () -> Unit = {}
) = menuButton(
    stateOf(buttonText),
    modifier,
    stateOf(defaultStyle),
    stateOf(hoverStyle),
    stateOf(disabledStyle),
    action = action,
)

fun LayoutScope.menuButton(
    buttonText: State<String>,
    modifier: Modifier = Modifier,
    defaultStyle: State<MenuButton.Style> = stateOf(MenuButton.DARK_GRAY),
    hoverStyle: State<MenuButton.Style> = stateOf(MenuButton.GRAY),
    disabledStyle: State<MenuButton.Style> = defaultStyle.map { it.copy(textColor = EssentialPalette.TEXT_DISABLED) },
    textAlignment: MenuButton.Alignment = MenuButton.Alignment.CENTER,
    textXOffset: State<Float> = stateOf(0f),
    collapsedText: State<String?> = stateOf(null),
    truncate: Boolean = false,
    clickSound: Boolean = true,
    shouldBeRetextured: Boolean? = null,
    action: () -> Unit = {}
): MenuButton {
    val menuButton = MenuButton(
        buttonText.toV1(stateScope),
        defaultStyle.toV1(stateScope),
        hoverStyle.toV1(stateScope),
        disabledStyle.toV1(stateScope),
        textAlignment,
        textXOffset.toV1(stateScope),
        collapsedText.toV1(stateScope),
        truncate,
        clickSound,
        shouldBeRetextured,
        action
    )
    menuButton(modifier)
    return menuButton
}

@Suppress("unused")
private val init = run {
    Inspector.registerComponentFactory(null)
}
