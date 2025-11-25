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
import gg.essential.elementa.dsl.basicXConstraint
import gg.essential.elementa.dsl.basicYConstraint
import gg.essential.elementa.utils.roundToRealPixels
import kotlin.math.ceil
import kotlin.math.floor

fun interface Alignment {
    fun align(parentSize: Float, childSize: Float): Float

    fun toXConstraint() = basicXConstraint { it.parent.getLeft() + align(it.parent.getWidth(), it.getWidth()) }
    fun toYConstraint() = basicYConstraint { it.parent.getTop() + align(it.parent.getHeight(), it.getHeight()) }

    fun applyHorizontal(component: UIComponent): () -> Unit = BasicXModifier { toXConstraint() }.applyToComponent(component)
    fun applyVertical(component: UIComponent): () -> Unit = BasicYModifier { toYConstraint() }.applyToComponent(component)

    companion object {
        @Suppress("FunctionName")
        fun Start(padding: Float): Alignment = Alignment { _, _ -> padding }
        @Suppress("FunctionName")
        fun Center(roundUp: Boolean): Alignment = Alignment { parent, child ->
            val center = parent / 2 - child / 2
            if (roundUp) ceil(center) else floor(center)
        }
        @Suppress("FunctionName")
        fun End(padding: Float): Alignment = Alignment { parent, child -> parent - padding - child }

        val Start: Alignment = Start(0f)
        val Center: Alignment = Center(false)
        val End: Alignment = End(0f)

        val TrueCenter: Alignment = Alignment { parent, child -> (parent / 2 - child / 2).roundToRealPixels() }
    }
}

fun Modifier.alignBoth(alignment: Alignment) = alignHorizontal(alignment).alignVertical(alignment)

fun Modifier.alignHorizontal(alignment: Alignment) = this then HorizontalAlignmentModifier(alignment)

fun Modifier.alignVertical(alignment: Alignment) = this then VerticalAlignmentModifier(alignment)

private class HorizontalAlignmentModifier(private val alignment: Alignment) : Modifier {
    override fun applyToComponent(component: UIComponent): () -> Unit {
        return alignment.applyHorizontal(component)
    }
}

private class VerticalAlignmentModifier(private val alignment: Alignment) : Modifier {
    override fun applyToComponent(component: UIComponent): () -> Unit {
        return alignment.applyVertical(component)
    }
}
