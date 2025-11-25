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
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.events.UIClickEvent
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.color.toConstraint
import gg.essential.gui.elementa.state.v2.combinators.and
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toV1
import java.awt.Color

abstract class NoticeFlag(
    style: State<MenuButton.Style>,
) : UIBlock() {

    val backgroundHidden = mutableStateOf(stateOf(false))
    private val backgroundColor = memo { if (backgroundHidden()()) Color(0, 0, 0, 0) else style().buttonColor }
    private val highlightColor = backgroundColor.map { it.brighter() }
    private val shadowColor = backgroundColor.map { it.darker() }
    private val hasLeft = style.map { OutlineEffect.Side.Left in it.sides }
    private val hasRight = style.map { OutlineEffect.Side.Right in it.sides }
    private val hasTop = style.map { OutlineEffect.Side.Top in it.sides }
    private val hasBottom = style.map { OutlineEffect.Side.Bottom in it.sides }

    private val topHighlight by UIBlock(highlightColor.toV1(this)).constrain {
        width = 100.percent
        height = 1.pixel
    }.bindParent(this, hasTop)

    private val leftHighlight by UIBlock(highlightColor.toV1(this)).constrain {
        width = 1.pixel
        height = 100.percent
    }.bindParent(this, hasLeft)

    private val rightShadow by UIBlock(shadowColor.toV1(this)).constrain {
        x = 0.pixels(alignOpposite = true)
        width = 1.pixel
        height = 100.percent
    }.bindParent(this, hasRight)

    private val bottomShadow by UIBlock(shadowColor.toV1(this)).constrain {
        y = 0.pixels(alignOpposite = true)
        width = 100.percent
        height = 1.pixel
    }.bindParent(this, hasBottom)

    private val topRightCorner by UIBlock(backgroundColor.toV1(this)).constrain {
        x = 0.pixels(alignOpposite = true)
        width = 1.pixel
        height = 1.pixel
    }.bindParent(this, hasTop and hasRight)

    private val bottomLeftCorner by UIBlock(backgroundColor.toV1(this)).constrain {
        y = 0.pixels(alignOpposite = true)
        width = 1.pixel
        height = 1.pixel
    }.bindParent(this, hasLeft and hasBottom)

    protected val contentContainer by UIContainer().constrain {
        y = CenterConstraint()
        width = ChildBasedMaxSizeConstraint()
        height = ChildBasedSizeConstraint()
    } childOf this

    init {
        constrain {
            width = (100.percent boundTo contentContainer) + 8.pixels
            height = (100.percent boundTo contentContainer) + 6.pixels
            color = backgroundColor.toConstraint()
        }
    }

    fun runAction(){
        fireClickEvent(UIClickEvent(
            getLeft(), getTop(), 0, this, this, 1
        ))
    }
}

class TextFlag(
    style: State<MenuButton.Style>,
    alignment: State<MenuButton.Alignment> = stateOf(MenuButton.Alignment.CENTER),
    text: State<List<State<String>>>,
) : NoticeFlag(style) {

    constructor(
        style: State<MenuButton.Style>,
        alignment: MenuButton.Alignment = MenuButton.Alignment.CENTER,
        vararg text: State<String>,
    ) : this(style, stateOf(alignment), stateOf(listOf(*text)))

    init {
        contentContainer.bindConstraints(alignment) { xAlignment ->
            x = xAlignment.noPadding()
        }

        val addText = { lines: List<State<String>> ->
            contentContainer.clearChildren()
            lines.forEach {
                EssentialUIText(shadowColor = EssentialPalette.TEXT_SHADOW).bindText(it.toV1(this)).constrain {
                    y = if (lines.size > 1) SiblingConstraint(4f) else (CenterConstraint() - 0.pixels) // Otherwise the text will add 1 pixel to the height
                    color = EssentialPalette.TEXT_HIGHLIGHT.toConstraint()
                }.bindConstraints(alignment) { xAlignment ->
                    x = xAlignment.noPadding()
                } childOf contentContainer
            }
        }

        addText(text.get())
        text.onSetValue(this, addText)
    }
}

class IconFlag(
    style: State<MenuButton.Style>,
    // image is not a state as none of our current uses require dynamic icons
    // if this changes in future see [NoticeFlagProxy.drawBehaviourFromComponentState] as it will require adjustment
    image: ImageFactory,
) : NoticeFlag(style) {

    init {
        setWidth((100.percent boundTo contentContainer) + 7.pixels)

        contentContainer.layout(Modifier.alignHorizontal(Alignment.Center)) {
            icon(image, Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(EssentialPalette.BLACK))
        }
    }
}
