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
import gg.essential.elementa.constraints.WidthConstraint
import gg.essential.elementa.constraints.resolution.ConstraintVisitor
import gg.essential.elementa.state.State
import gg.essential.elementa.utils.guiHint
import gg.essential.elementa.utils.roundToRealPixels

/**
 * Tile constraint provides the appropriate max width and height for an image in the screenshot browser
 * based on the width of the parent, the number of items per row, and the desired padding between each item
 */
class TileConstraint(
    val state: State<Int>,
    val padding: Float
) : WidthConstraint {

    override var cachedValue = 0f
    override var recalculate = true
    override var constrainTo: UIComponent? = null

    override fun getWidthImpl(component: UIComponent): Float {
        //We add one pixel to padding here to avoid rounding issues causing an incorrect number of items per row
        return (((constrainTo ?: component.parent).getWidth() - (padding.roundToRealPixels()) * (state.get() - 1)) / state.get()).guiHint(roundDown = true)
    }

    override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {

    }

}