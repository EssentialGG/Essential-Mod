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
package gg.essential.gui.screenshot.constraints

import gg.essential.elementa.UIComponent
import gg.essential.elementa.constraints.ConstraintType
import gg.essential.elementa.constraints.HeightConstraint
import gg.essential.elementa.constraints.WidthConstraint
import gg.essential.elementa.constraints.resolution.ConstraintVisitor
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.elementa.state.State as StateV1

class AspectPreservingFillConstraint(
    private val aspect: State<Float>
) : WidthConstraint, HeightConstraint {

    constructor(aspect: StateV1<Float>) : this(aspect.toV2())

    override var cachedValue = 0f
    override var recalculate = true
    override var constrainTo: UIComponent? = null

    /**
     * Based on the aspect ratio of the image, return the dimension of the image that is as large as possible
     * without the width or height exceeding the container the image is constrained within
     */

    private fun getSize(component: UIComponent): Pair<Float, Float> {
        val target = (constrainTo ?: component.parent)
        val inverseAspectRatio = 1 / aspect.getUntracked()

        val containerWidth = target.getWidth()
        val containerHeight = target.getHeight()

        var proposedWidth = containerWidth
        var proposedHeight = proposedWidth * inverseAspectRatio

        //Check if the image would be too tall for the proposed width
        if (proposedHeight > containerHeight) {
            proposedHeight = containerHeight
            proposedWidth = containerHeight / inverseAspectRatio
        }
        return proposedWidth to proposedHeight
    }


    override fun getWidthImpl(component: UIComponent): Float {
        return getSize(component).first
    }

    override fun getHeightImpl(component: UIComponent): Float {
        return getSize(component).second
    }

    override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {
    }

}