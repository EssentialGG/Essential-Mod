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
package gg.essential.gui.elementa

import gg.essential.elementa.UIComponent
import gg.essential.elementa.UIConstraints
import gg.essential.elementa.constraints.ConstraintType
import gg.essential.elementa.constraints.PositionConstraint
import gg.essential.elementa.constraints.SizeConstraint
import gg.essential.elementa.constraints.resolution.ConstraintVisitor
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.effects.Effect
import net.minecraft.client.gui.GuiButton

/**
 * @see VanillaButtonConstraint
 * Creates a constraint which is bound to a group of vanilla buttons.
 * If assigned to a UIContainer via [constrainTo], the container will cover all buttons.
 */
class VanillaButtonGroupConstraint(val buttons: List<() -> GuiButton?>, private val fallback: UIConstraints) :
    PositionConstraint, SizeConstraint {
    override var cachedValue = 0f
    override var recalculate = true
    override var constrainTo: UIComponent? = null

    override fun getXPositionImpl(component: UIComponent): Float {
        val result = buttons.minOfOrNull { it()?.x?.toFloat() ?: Float.MAX_VALUE }
        if (result == null || result == Float.MAX_VALUE) {
            return fallback.x.getXPositionImpl(component)
        }
        return result
    }

    override fun getYPositionImpl(component: UIComponent): Float {
        val result = buttons.minOfOrNull { it()?.y?.toFloat() ?: Float.MAX_VALUE }
        if (result == null || result == Float.MAX_VALUE) {
            return fallback.y.getYPositionImpl(component)
        }
        return result
    }

    override fun getWidthImpl(component: UIComponent): Float {
        val buttons = buttons.mapNotNull { it() }.takeUnless { it.isEmpty() }
            ?: return fallback.width.getWidthImpl(component)
        return (buttons.maxOf { it.x + it.width } - buttons.minOf { it.x }).toFloat()
    }

    override fun getHeightImpl(component: UIComponent): Float {
        val buttons = buttons.mapNotNull { it() }.takeUnless { it.isEmpty() }
            ?: return fallback.height.getHeightImpl(component)
        return (buttons.maxOf { it.y + it.height } - buttons.minOf { it.y }).toFloat()
    }

    override fun getRadiusImpl(component: UIComponent): Float = getWidthImpl(component) / 2

    override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {
    }

    companion object {
        /**
         * Constrains the component's position and size to the position and size of the group of [buttons].
         * If [buttons] is empty, the applicable constraints are replaced  with those specified in [fallback].
         */
        @JvmStatic
        @JvmOverloads
        fun <T : UIComponent> T.constrainTo(buttons: List<() -> GuiButton?>, fallback: UIConstraints.() -> Unit = {}) =
            constrain {
                val fallbackConstraint = UIConstraints(this@constrainTo)
                fallbackConstraint.fallback()

                enableEffect(object : Effect() {
                    @Deprecated("See [ElementaVersion.V8].")
                    @Suppress("DEPRECATION")
                    override fun animationFrame() {
                        fallbackConstraint.x.animationFrame()
                        fallbackConstraint.y.animationFrame()
                        fallbackConstraint.width.animationFrame()
                        fallbackConstraint.height.animationFrame()
                        fallbackConstraint.radius.animationFrame()
                    }
                })

                x = VanillaButtonGroupConstraint(buttons, fallbackConstraint)
                y = VanillaButtonGroupConstraint(buttons, fallbackConstraint)
                width = VanillaButtonGroupConstraint(buttons, fallbackConstraint)
                height = VanillaButtonGroupConstraint(buttons, fallbackConstraint)
            }
    }
}