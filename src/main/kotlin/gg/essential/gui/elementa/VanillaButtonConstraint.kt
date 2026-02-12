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

class VanillaButtonConstraint(val button: () -> GuiButton?, private val fallback: UIConstraints) : PositionConstraint, SizeConstraint {
    override var cachedValue = 0f
    override var recalculate = true
    override var constrainTo: UIComponent? = null

    override fun getXPositionImpl(component: UIComponent): Float = button()?.x?.toFloat() ?: fallback.x.getXPositionImpl(component)
    override fun getYPositionImpl(component: UIComponent): Float = button()?.y?.toFloat() ?: fallback.y.getYPositionImpl(component)
    override fun getWidthImpl(component: UIComponent): Float = button()?.width?.toFloat() ?: fallback.width.getWidthImpl(component)
    //#if MC>=11600
    //$$ override fun getHeightImpl(component: UIComponent): Float = button()?.heightRealms?.toFloat() ?: fallback.height.getHeightImpl(component)
    //#else
    override fun getHeightImpl(component: UIComponent): Float = button()?.height?.toFloat() ?: fallback.height.getHeightImpl(component)
    //#endif
    override fun getRadiusImpl(component: UIComponent): Float = getWidthImpl(component) / 2

    override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {
    }

    companion object {
        /**
         * Constrains the component's position and size to the position and size of [button].
         * If [button] is null, the applicable constraints are replaced  with those specified in [fallback].
         */
        @JvmStatic
        @JvmOverloads
        fun <T : UIComponent> T.constrainTo(button: () -> GuiButton?, fallback: UIConstraints.() -> Unit = {}) = constrain {
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

            x = VanillaButtonConstraint(button, fallbackConstraint)
            y = VanillaButtonConstraint(button, fallbackConstraint)
            width = VanillaButtonConstraint(button, fallbackConstraint)
            height = VanillaButtonConstraint(button, fallbackConstraint)
        }
    }
}