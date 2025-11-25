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
package gg.essential.gui.transitions

import gg.essential.elementa.constraints.animation.AnimatingConstraints
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.transitions.BoundTransition
import gg.essential.gui.effects.AlphaEffect
import kotlin.properties.Delegates

/**
 * Fades a component and all of its children in. This is done using
 * [AlphaEffect]. When the transition is finished, the effect is removed.
 */
class FadeInTransition @JvmOverloads constructor(
    private val time: Float = 1f,
    private val animationType: Animations = Animations.OUT_EXP,
) : BoundTransition() {

    private val alphaState = BasicState(0f)
    private var alpha by Delegates.observable(0f) { _, _, newValue ->
        alphaState.set(newValue)
    }

    private val effect = AlphaEffect(alphaState)

    override fun beforeTransition() {
        boundComponent.enableEffect(effect)
    }

    override fun doTransition(constraints: AnimatingConstraints) {
        constraints.setExtraDelay(time)
        boundComponent.apply {
            ::alpha.animate(animationType, time, 1f)
        }
    }

    override fun afterTransition() {
        boundComponent.removeEffect(effect)
        effect.cleanup()
    }
}