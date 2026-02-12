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
import gg.essential.elementa.constraints.ConstraintType
import gg.essential.elementa.constraints.HeightConstraint
import gg.essential.elementa.constraints.MasterConstraint
import gg.essential.elementa.constraints.PositionConstraint
import gg.essential.elementa.constraints.RadiusConstraint
import gg.essential.elementa.constraints.SuperConstraint
import gg.essential.elementa.constraints.WidthConstraint
import gg.essential.elementa.constraints.XConstraint
import gg.essential.elementa.constraints.YConstraint
import gg.essential.elementa.constraints.resolution.ConstraintVisitor

fun lazyPosition(initializer: () -> PositionConstraint) = LazyConstraint(lazy(initializer)) as PositionConstraint

fun lazyHeight(initializer: () -> HeightConstraint) = LazyConstraint(lazy(initializer)) as HeightConstraint

private class LazyConstraint(val constraint: Lazy<SuperConstraint<Float>>): MasterConstraint {

    override var cachedValue = 0f
    override var recalculate = true
    override var constrainTo: UIComponent? = null

    @Suppress("DEPRECATION")
    @Deprecated("See [ElementaVersion.V8].")
    override fun animationFrame() {
        super.animationFrame()
        constraint.value.animationFrame()
    }

    override fun getHeightImpl(component: UIComponent): Float {
        return (constraint.value as HeightConstraint).getHeightImpl(component)
    }

    override fun getRadiusImpl(component: UIComponent): Float {
        return (constraint.value as RadiusConstraint).getRadiusImpl(component)
    }

    override fun getWidthImpl(component: UIComponent): Float {
        return (constraint.value as WidthConstraint).getWidthImpl(component)
    }

    override fun getXPositionImpl(component: UIComponent): Float {
        return (constraint.value as XConstraint).getXPositionImpl(component)
    }

    override fun getYPositionImpl(component: UIComponent): Float {
        return (constraint.value as YConstraint).getYPositionImpl(component)
    }

    override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {}

}
