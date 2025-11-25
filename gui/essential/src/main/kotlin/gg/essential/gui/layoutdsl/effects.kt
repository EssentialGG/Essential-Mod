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

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.effects.Effect
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.LoadingIcon
import gg.essential.gui.common.shadow.ShadowEffect
import gg.essential.gui.common.shadow.ShadowIcon
import java.awt.Color
import gg.essential.gui.elementa.state.v2.State as StateV2

fun Modifier.effect(effect: () -> Effect) = this then {
    val instance = effect()
    enableEffect(instance)
    return@then {
        removeEffect(instance)
    }
}

fun Modifier.outline(color: Color, width: Float, drawInsideChildren: Boolean = false) = effect { OutlineEffect(color, width, drawInsideChildren = drawInsideChildren) }
fun Modifier.outline(color: State<Color>, width: State<Float>, drawInsideChildren: Boolean = false) = effect { OutlineEffect(color, width, drawInsideChildren = drawInsideChildren) }
fun Modifier.outline(color: StateV2<Color>, width: StateV2<Float>, drawInsideChildren: Boolean = false) = then(StateV2 {
    val colorValue = color()
    val widthValue = width()
    Modifier.effect { OutlineEffect(colorValue, widthValue, drawInsideChildren) }
})

fun Modifier.shadow(color: Color? = null) = this then {
    when (this) {
        is UIText -> {
            val originalShadow = getShadow()
            val originalShadowColor = getShadowColor()

            setShadow(true)
            setShadowColor(color)
            return@then {
                setShadow(originalShadow)
                setShadowColor(originalShadowColor)
            }
        }

        is UIWrappedText -> {
            val originalShadow = getShadow()
            val originalShadowColor = getShadowColor()

            setShadow(true)
            setShadowColor(color)
            return@then {
                setShadow(originalShadow)
                setShadowColor(originalShadowColor)
            }
        }

        is ShadowIcon -> {
            val originalShadow = getShadow()
            val originalShadowColor = getShadowColor()

            rebindShadow(BasicState(true))
            rebindShadowColor(BasicState(color ?: EssentialPalette.BLACK))
            return@then {
                rebindShadow(BasicState(originalShadow))
                rebindShadowColor(BasicState(originalShadowColor))
            }
        }

        is UIImage, is UIBlock, is UIContainer, is LoadingIcon -> {
            return@then Modifier.effect { ShadowEffect(color ?: EssentialPalette.BLACK) }.applyToComponent(this)
        }

        else -> throw UnsupportedOperationException()
    }
}

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Using StateV1 is discouraged, use StateV2 instead")
fun Modifier.shadow(color: State<Color>) = then(color.map { Modifier.shadow(it) })

fun Modifier.shadow(color: StateV2<Color>) = then(StateV2 { Modifier.shadow(color()) })

fun Modifier.hoverShadow(shadow: Color?) = whenHovered(Modifier.shadow(shadow))

fun Modifier.hoverShadow(shadow: State<Color>) = whenHovered(Modifier.shadow(shadow))

