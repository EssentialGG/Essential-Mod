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
package gg.essential.gui.wardrobe.components

import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.utils.withAlpha
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.filter
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.WardrobeCategory
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.universal.ChatColor
import gg.essential.universal.USound
import gg.essential.util.scrollGradient
import gg.essential.vigilance.utils.onLeftClick
import kotlin.math.floor

private const val MAIN_PADDING = 10f
private const val SUB_PADDING = 7f

fun LayoutScope.wardrobeSidebar(
    state: WardrobeState,
    modifier: Modifier = Modifier,
): ScrollComponent {
    return scrollable(modifier, vertical = true) {
        column(Modifier.fillWidth().alignVertical(Alignment.Start)) {
            spacer(height = MAIN_PADDING)
            box(Modifier.fillWidth().height(1f).color(EssentialPalette.COMPONENT_BACKGROUND))
            forEach(state.allCategories.filter { it.superCategory == it }) { category ->
                category(state, category, state.currentCategory, state.allCategories.filter { it != category && it.superCategory == category })
                box(Modifier.fillWidth().height(1f).color(EssentialPalette.COMPONENT_BACKGROUND))
            }
            spacer(height = SUB_PADDING)
        }
    }.apply {
        scrollGradient(gradientHeight.pixels)
    }
}

private fun LayoutScope.category(
    state: WardrobeState,
    category: WardrobeCategory,
    currentCategory: MutableState<WardrobeCategory>,
    subCategories: ListState<WardrobeCategory>,
) {
    val selected = currentCategory.map { it.superCategory == category }
    val bgColor = selected.map { EssentialPalette.COMPONENT_BACKGROUND.withAlpha(if (it) 1f else 0f) }
    val heightState = selected.map { { if (it) ChildBasedSizeConstraint() else 0.pixels } }
    val heightAnimation = Modifier.animateHeight(heightState, 0.25f, Animations.IN_OUT_EXP)

    box(Modifier.fillWidth().color(bgColor).hoverColor(EssentialPalette.COMPONENT_BACKGROUND).hoverScope()) {
        row(Modifier.fillWidth(leftPadding = MAIN_PADDING).alignVertical(Alignment.Start), Arrangement.spacedBy(6f, FloatPosition.START)) {
            box(Modifier.childBasedHeight(MAIN_PADDING)) {
                text(category.compactName, Modifier.color(category.style?.color ?: EssentialPalette.TEXT_MID_GRAY).shadow(EssentialPalette.BLACK))
            }
            category.style?.let { image(it.icon, Modifier.color(it.color).shadow(EssentialPalette.BLACK)) }
        }
        if_(subCategories.map { it.isNotEmpty() }) {
            subCategories(BasicYModifier { SiblingConstraint() - floor(SUB_PADDING / 2f).pixels }.then(heightAnimation.fillWidth()), state, currentCategory, subCategories)
        }
    }.onLeftClick {
        it.stopPropagation()
        USound.playButtonPress()
        currentCategory.set(category)
    }
}

private fun LayoutScope.subCategories(
    modifier: Modifier,
    state: WardrobeState,
    currentCategory: MutableState<WardrobeCategory>,
    subCategories: ListState<WardrobeCategory>,
) {
    val subPaddingAbove = floor(SUB_PADDING / 2f)
    val subPaddingBelow = SUB_PADDING - subPaddingAbove
    column(modifier.effect { ScissorEffect() }, Arrangement.spacedBy(0f, FloatPosition.START), Alignment.Start) {
        forEach(subCategories) { subCategory ->
            val disabled = state.visibleCosmetics.map { list -> !list.any { it in subCategory } }
            val name = disabled.map { if (it) ChatColor.STRIKETHROUGH + subCategory.compactName else subCategory.compactName }
            val hoverColor = disabled.map { if (it) EssentialPalette.TEXT_DARK_DISABLED else EssentialPalette.TEXT_HIGHLIGHT }
            val textColor = stateBy {
                when {
                    disabled() -> EssentialPalette.TEXT_DARK_DISABLED
                    currentCategory.map { it == subCategory }() -> EssentialPalette.TEXT_HIGHLIGHT
                    else -> EssentialPalette.TEXT_MID_GRAY
                }
            }

            column(Modifier.fillWidth().hoverScope(), horizontalAlignment = Alignment.Start) {
                spacer(height = subPaddingAbove)
                row {
                    spacer(width = MAIN_PADDING)
                    text(name, Modifier.color(textColor).shadow(EssentialPalette.BLACK).hoverColor(hoverColor))
                }
                spacer(height = subPaddingBelow)
            }.onLeftClick {
                it.stopPropagation()
                if (!disabled.get()) {
                    USound.playButtonPress()
                    currentCategory.set(subCategory)
                }
            }
        }
        spacer(height = MAIN_PADDING - SUB_PADDING)
    }
}
