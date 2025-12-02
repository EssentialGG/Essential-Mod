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
package gg.essential.gui.common

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.letState
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.onChange
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.fillHeight
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.then
import gg.essential.gui.layoutdsl.tooltip
import gg.essential.gui.layoutdsl.whenTrue
import gg.essential.gui.layoutdsl.width
import gg.essential.universal.USound
import gg.essential.gui.util.hoveredStateV2
import gg.essential.vigilance.utils.onLeftClick
import kotlin.math.round

abstract class EssentialSlider(
    initialValueFraction: Float
) : UIContainer() {

    private val notchWidth = 3

    val fraction = mutableStateOf(initialValueFraction)
    private val updates = mutableListOf<(Float) -> Unit>()

    private val sliderBar by UIBlock(EssentialPalette.BUTTON_HIGHLIGHT)

    private val sliderNotch by UIBlock()

    private val sliderCovered by UIBlock(EssentialPalette.ACCENT_BLUE)

    private var hoveredState: State<Boolean>

    init {
        // Elementa's onMouseDrag does not check whether the mouse is within the component
        // So we need to do that ourselves. We want to ignore any drag that does not start within
        // this component
        val mouseHeld = mutableStateOf(false)

        onLeftClick {
            USound.playButtonPress()
            mouseHeld.set(true)
            updateSlider(it.absoluteX - this@EssentialSlider.getLeft())
            it.stopPropagation()
        }
        onMouseRelease {
            mouseHeld.set(false)
        }
        sliderBar.onMouseDrag { mouseX, _, _ ->

            if (mouseHeld.get()) {
                updateSlider(mouseX)
            }
        }
        hoveredState = State { hoveredStateV2()() || mouseHeld() }

        this.layout {
            sliderBar(Modifier.fillWidth().fillHeight(padding = 1f).alignVertical(Alignment.Center)) {
                sliderCovered(Modifier.fillHeight().then(State { Modifier.fillWidth(fraction()) }))
            }
            sliderNotch(
                Modifier.width(notchWidth.toFloat())
                    .fillHeight()
                    .alignHorizontal { parentSize, _ -> fraction.get() * (parentSize - notchWidth) }
                    .color(EssentialPalette.getTextColor(hoveredState))
                    .whenTrue(hoveredState, Modifier.tooltip(fraction.letState { reduceFractionToDisplay(it) }))
            )
        }
    }

    /**
     * Updates the slider based on the mouseX position
     */
    private fun updateSlider(mouseX: Float) {
        val updatedValue = updateSliderValue(
            ((mouseX - sliderNotch.getWidth() / 2) / (getWidth() - sliderNotch.getWidth())).coerceIn(0f..1f)
        )
        fraction.set(updatedValue)
    }

    abstract fun reduceFractionToDisplay(fraction: Float): String

    /**
     * Allows overriding of notch position of the slider to snap to desired values
     */
    open fun updateSliderValue(fraction: Float): Float {
        return fraction
    }
}

class IntEssentialSlider(
    private val minValue: Int,
    private val maxValue: Int,
    initialValue: Int
) : EssentialSlider(
    (initialValue - minValue) / (maxValue - minValue).toFloat()
) {

    private val updates = mutableListOf<(Int) -> Unit>()

    private val intValue = fraction.map {
        mapFractionToRange(it)
    }

    private fun mapFractionToRange(fraction: Float): Int {
        val range = maxValue - minValue
        return (minValue + round(fraction * range)).toInt().coerceIn(minValue..maxValue)
    }

    init {
        intValue.onChange(this) {
            for (update in updates) {
                update(it)
            }
        }
    }

    fun onUpdateInt(callback: (Int) -> Unit) {
        updates.add(callback)
    }

    override fun reduceFractionToDisplay(fraction: Float): String {
        return mapFractionToRange(fraction).toString()
    }

    override fun updateSliderValue(fraction: Float): Float {
        val range = maxValue - minValue
        return round(fraction * range) / range
    }
}


