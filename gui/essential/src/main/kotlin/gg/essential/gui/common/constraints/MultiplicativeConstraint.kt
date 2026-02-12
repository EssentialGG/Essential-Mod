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
package gg.essential.gui.common.constraints

import gg.essential.elementa.UIComponent
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.resolution.ConstraintVisitor

class MultiplicativeConstraint(
    private val constraint1: SuperConstraint<Float>,
    private val constraint2: SuperConstraint<Float>
) : MasterConstraint {
    override var cachedValue = 0f
    override var recalculate = true
    override var constrainTo: UIComponent? = null

    @Deprecated("See [ElementaVersion.V8].")
    @Suppress("DEPRECATION")
    override fun animationFrame() {
        super.animationFrame()
        constraint1.animationFrame()
        constraint2.animationFrame()
    }

    override fun getXPositionImpl(component: UIComponent): Float {
        val base = component.parent.getLeft()
        val value1 = (constraint1 as XConstraint).getXPosition(component)
        val value2 = (constraint2 as XConstraint).getXPosition(component)
        return (value1 - base) * (value2 - base) + base
    }

    override fun getYPositionImpl(component: UIComponent): Float {
        val base = component.parent.getTop()
        val value1 = (constraint1 as YConstraint).getYPosition(component)
        val value2 = (constraint2 as YConstraint).getYPosition(component)
        return (value1 - base) * (value2 - base) + base
    }

    override fun getWidthImpl(component: UIComponent): Float {
        return (constraint1 as WidthConstraint).getWidth(component) *
            (constraint2 as WidthConstraint).getWidth(component)
    }

    override fun getHeightImpl(component: UIComponent): Float {
        return (constraint1 as HeightConstraint).getHeight(component) *
            (constraint2 as HeightConstraint).getHeight(component)
    }

    override fun getRadiusImpl(component: UIComponent): Float {
        return (constraint1 as RadiusConstraint).getRadius(component) *
            (constraint2 as RadiusConstraint).getRadius(component)
    }

    override fun to(component: UIComponent): SuperConstraint<Float> {
        throw UnsupportedOperationException("Constraint.to(UIComponent) is not available in this context, please apply this to the components beforehand.")
    }

    override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {
        constraint1.visit(visitor, type, setNewConstraint = false)
        constraint2.visit(visitor, type, setNewConstraint = false)
    }
}