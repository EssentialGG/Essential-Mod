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

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.events.UIClickEvent
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.layoutdsl.*
import gg.essential.universal.UKeyboard
import gg.essential.universal.USound
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color

class ContextOptionMenu(
    posX: Float,
    posY: Float,
    val optionsListState: ListState<Item>,
    val maxHeight: Float = Float.POSITIVE_INFINITY,
) : UIBlock() {

    constructor(
        posX: Float,
        posY: Float,
        vararg options: Item,
        maxHeight: Float = Float.POSITIVE_INFINITY,
    ) : this(posX, posY, listStateOf(*options), maxHeight)

    private val optionColumnPadding: Float = 3f

    private val componentBackgroundColor = EssentialPalette.COMPONENT_BACKGROUND
    private val componentBackgroundHighlightColor = EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT
    private val outlineColor = EssentialPalette.BUTTON_HIGHLIGHT

    private val closeActions = mutableListOf<() -> Unit>()
    private var closed = false

    init {
        fun LayoutScope.divider() {
            spacer(height = optionColumnPadding)
            box(Modifier.height(1f).fillWidth().color(outlineColor))
            spacer(height = optionColumnPadding)
        }

        fun LayoutScope.option(option: Option) {

            val colorModifier = Modifier
                .color(option.disabled.map { if (it) EssentialPalette.TEXT_DISABLED else option.color })
                .hoverColor(option.disabled.map { if (it) EssentialPalette.TEXT_DISABLED else option.hoveredColor })
                .shadow(option.shadowColor)
                .hoverShadow(option.hoveredShadowColor)

            box(Modifier.height(15f).fillWidth().color(componentBackgroundColor).hoverColor(componentBackgroundHighlightColor).hoverScope()) {
                row(Modifier.fillHeight().alignBoth(Alignment.Start)) {
                    ifNotNull(option.image) {
                        box(Modifier.fillHeight().width(20f)) {
                            icon(it, colorModifier)
                        }
                    } `else` {
                        spacer(width = 10f)
                    }
                    text(option.textState, colorModifier, centeringContainsShadow = false)
                    spacer(width = 10f)
                }
            }.onLeftClick {
                USound.playButtonPress()
                if (!option.disabled.get()) {
                    option.action()
                }
            }

        }

        fun Modifier.customOptionMenuWidth() = this then BasicWidthModifier {
            basicWidthConstraint { it.children.maxOfOrNull { child -> ChildBasedSizeConstraint().getWidth(child) } ?: 1f }
        }

        fun Modifier.maxSiblingHeight() = this then BasicHeightModifier {
            basicHeightConstraint { it.parent.children.maxOfOrNull { child -> if (child === it) 0f else child.getHeight() } ?: 1f }
        }

        fun Modifier.limitHeight() = this then {
            val originalHeightConstraint = constraints.height
            constraints.height = originalHeightConstraint.coerceAtMost(maxHeight.pixels)

            return@then { constraints.height = originalHeightConstraint }
        }

        val scrollComponent: ScrollComponent
        val scrollBar: UIComponent

        this.layoutAsBox(Modifier.childBasedMaxSize(2f).color(outlineColor).shadow(Color.BLACK)) {
            scrollComponent = scrollable(Modifier.limitHeight(), vertical = true) {
                column(Modifier.customOptionMenuWidth().childBasedHeight(optionColumnPadding).color(componentBackgroundColor), Arrangement.spacedBy(0f, FloatPosition.CENTER)) {
                    forEach(optionsListState) {
                        when (it) {
                            is Divider -> divider()
                            is Option -> option(it)
                        }
                    }
                }
            }
            box(Modifier.maxSiblingHeight().width(2f).alignHorizontal(Alignment.End).alignVertical(Alignment.Center)) {
                scrollBar = box(Modifier.fillWidth().color(EssentialPalette.TEXT_DISABLED))
            }
        }

        scrollComponent.setVerticalScrollBarComponent(scrollBar, true)

        reposition(posX, posY)

        onFocusLost {
            close()
        }
        onKeyType { _, keyCode ->
            if (keyCode == UKeyboard.KEY_ESCAPE) {
                close()
            }
        }

        isFloating = true
    }

    fun close() {
        if (closed) return
        closed = true
        for (closeAction in closeActions) {
            closeAction()
        }
        if (hasFocus()) {
            releaseWindowFocus()
        }
        parent.removeChild(this)
    }


    fun onClose(action: () -> Unit) {
        closeActions.add(action)
    }

    fun init() {
        grabWindowFocus()
    }

    fun reposition(x: Float, y: Float) = reposition(x.pixels, y.pixels)

    fun reposition(x: XConstraint, y: YConstraint) {
        setX(x.coerceAtMost(0.pixels(alignOpposite = true)))
        setY(y.coerceAtMost(0.pixels(alignOpposite = true)))
    }

    sealed interface Item

    object Divider : Item

    data class Option(
        val textState: State<String>,
        val image: State<ImageFactory?>,
        val disabled: State<Boolean> = stateOf(false),
        val color: Color = EssentialPalette.TEXT,
        val hoveredColor: Color = EssentialPalette.TEXT_HIGHLIGHT,
        val shadowColor: Color = EssentialPalette.BLACK,
        val hoveredShadowColor: Color = shadowColor,
        val action: () -> Unit,
    ) : Item {
        constructor(
            text: String,
            image: ImageFactory?,
            disabled: State<Boolean> = stateOf(false),
            textColor: Color = EssentialPalette.TEXT,
            hoveredColor: Color = EssentialPalette.TEXT_HIGHLIGHT,
            shadowColor: Color = EssentialPalette.BLACK,
            hoveredShadowColor: Color = shadowColor,
            action: () -> Unit,
        ) : this(
            stateOf(text),
            stateOf(image),
            disabled,
            textColor,
            hoveredColor,
            shadowColor,
            hoveredShadowColor,
            action
        )

    }

    data class Position(val xConstraint: XConstraint, val yConstraint: YConstraint) {
        constructor(x: Float, y: Float) : this(x.pixels, y.pixels)

        constructor(component: UIComponent, alignOppositeX: Boolean) : this(
            0.pixels(alignOpposite = alignOppositeX) boundTo component,
            (2).pixels(
                alignOpposite = true,
                alignOutside = true
            ) boundTo component
        )

        constructor(event: UIClickEvent) : this(event.absoluteX, event.absoluteY)
    }

    companion object {

        fun create(
            boundTo: UIComponent,
            optionsListState: ListState<Item>,
            maxHeight: Float = Float.POSITIVE_INFINITY,
            onClose: () -> Unit = {}
        ) = create(
            Position(boundTo, true),
            Window.of(boundTo),
            optionsListState = optionsListState,
            maxHeight = maxHeight,
            onClose = onClose
        )

        fun create(
            boundTo: UIComponent,
            vararg option: Item,
            maxHeight: Float = Float.POSITIVE_INFINITY,
            onClose: () -> Unit = {}
        ) = create(
            Position(boundTo, true),
            Window.of(boundTo),
            option = option,
            maxHeight = maxHeight,
            onClose = onClose
        )

        fun create(
            position: Position,
            window: Window,
            vararg option: Item,
            maxHeight: Float = Float.POSITIVE_INFINITY,
            onClose: () -> Unit = {}
        ) = create(position, window, listStateOf(*option), maxHeight, onClose)

        fun create(
            position: Position,
            window: Window,
            optionsListState: ListState<Item>,
            maxHeight: Float = Float.POSITIVE_INFINITY,
            onClose: () -> Unit = {}
        ): ContextOptionMenu {
            val menu = ContextOptionMenu(
                0f,
                0f,
                optionsListState,
                maxHeight = maxHeight,
            ) childOf window
            menu.reposition(position.xConstraint, position.yConstraint)
            menu.init()
            menu.onClose(onClose)
            return menu
        }

    }

}

