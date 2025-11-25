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
package gg.essential.gui.common.shadow

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.AutoImageSize
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.util.hoveredState
import java.awt.Color

@Deprecated("Use normal icon with ShadowEffect.")
class ShadowIcon(
    imageFactory: State<ImageFactory>,
    buttonShadow: State<Boolean>,
    primaryColor: State<Color>,
    shadowColor: State<Color>,
) : UIContainer() {

    private val iconState = imageFactory.map { it }
    private val buttonShadowState = buttonShadow.map { it }
    private val shadowColorState = shadowColor.map { it }

    constructor(imageFactory: ImageFactory, buttonShadow: Boolean) : this(
        BasicState(imageFactory),
        BasicState(buttonShadow)
    )

    constructor(
        imageFactory: State<ImageFactory>,
        buttonShadow: State<Boolean>
    ) : this(
        imageFactory,
        buttonShadow,
        BasicState(Color.BLACK), // can't access hover state until after constructor call
        BasicState(EssentialPalette.TEXT_SHADOW)
    ) {
        rebindPrimaryColor(EssentialPalette.getTextColor(hoveredState()))

    }

    init {
        setColor(primaryColor.toConstraint())
        iconState.zip(buttonShadowState).onSetValueAndNow { (icon, shadow) ->
            clearChildren()
            val image = icon.create().constrain {
                width = 100.percent
                height = 100.percent
            }.also {
                it.supply(AutoImageSize(this@ShadowIcon))
            }.setColor(CopyConstraintColor() boundTo this) childOf this@ShadowIcon

            if(shadow) {
                image effect ShadowEffect(shadowColorState.toV2())
            }

        }
    }

    fun rebindShadowColor(color: State<Color>) = apply {
        shadowColorState.rebind(color)
    }

    fun rebindPrimaryColor(color: State<Color>) = apply {
        setColor(color.toConstraint())
        return this
    }

    fun rebindIcon(imageFactory: State<ImageFactory>) = apply {
        iconState.rebind(imageFactory)
    }

    fun rebindShadow(shadow: State<Boolean>) = apply {
        buttonShadowState.rebind(shadow)
    }

    fun getShadow(): Boolean {
        return buttonShadowState.get()
    }

    fun getShadowColor(): Color {
        return shadowColorState.get()
    }

}
