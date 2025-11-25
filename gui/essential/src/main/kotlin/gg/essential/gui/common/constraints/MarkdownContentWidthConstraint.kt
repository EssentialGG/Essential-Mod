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
import gg.essential.elementa.constraints.ConstraintType
import gg.essential.elementa.constraints.WidthConstraint
import gg.essential.elementa.constraints.resolution.ConstraintVisitor
import gg.essential.gui.elementa.essentialmarkdown.EssentialMarkdown

/**
 * Sets the component's width to the longest line width of the [EssentialMarkdown] the component is constrained to
 */
class MarkdownContentWidthConstraint : WidthConstraint {
    override var cachedValue = 0f
    override var recalculate = true
    override var constrainTo: UIComponent? = null

    private val markdown: EssentialMarkdown
        get() = constrainTo as? EssentialMarkdown
            ?: throw UnsupportedOperationException("MarkdownContentWidthConstraint must be constrained to a MarkdownComponent")

    override fun getWidthImpl(component: UIComponent): Float {
        // One common pattern for this constraint is to bind it to fake components that aren't truly part of the
        // component tree and merely exist to size some container correctly. Since these components aren't part of
        // the tree, their update and draw will never be called, so we need to call it for them before we
        // can read its effective width.
        // Surplus calls should be harmless.
        markdown.animationFrame()
        markdown.update()

        return markdown.maxTextLineWidth
    }

    override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {}
}
