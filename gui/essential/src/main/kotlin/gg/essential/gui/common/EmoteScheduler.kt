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

import gg.essential.cosmetics.events.AnimationEventType
import gg.essential.elementa.UIComponent
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.onChange
import gg.essential.mod.cosmetics.settings.CosmeticProperty
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.mod.cosmetics.settings.variant
import gg.essential.model.util.PlayerPoseManager
import gg.essential.network.connectionmanager.cosmetics.AssetLoader
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.util.GuiEssentialPlatform.Companion.platform

class EmoteScheduler(
    private val component: UIComponent,
    private val emote: State<Cosmetic?>,
    private val settings: State<List<CosmeticSetting>?>
) {
    val emoteEquipped = mutableStateOf(true)

    private var emoteScheduled = false
    private var delays: MutableList<() -> Unit> = mutableListOf()

    init {
        component.addUpdateFunc { _, _ ->
            if (!emoteScheduled) {
                tryScheduleEmote()
            }
        }
        emote.onChange(component) {
            reset()
        }
        settings.onChange(component) {
            reset()
        }
    }

    private fun reset() {
        delays.forEach { it() }
        delays.clear()
        emoteEquipped.set(emote.getUntracked() != null)
        emoteScheduled = false
        tryScheduleEmote()
    }

    private fun tryScheduleEmote() {
        val emote = emote.getUntracked() ?: return
        val variant = settings.getUntracked()?.variant ?: emote.defaultVariantName
        val future = platform.modelLoader.getModel(emote, variant, AssetLoader.Priority.High)
        if (!future.isDone || future.isCompletedExceptionally) return
        val model = future.join()

        // If the emote loops until the player moves, we'll make them "move" after a couple of seconds
        // By default, this will be 3 seconds, but it may be overridden by the PREVIEW_RESET_TIME cosmetic setting
        val loopResetTime = (model.cosmetic.property<CosmeticProperty.PreviewResetTime>()?.data?.time)?.toFloat() ?: 3f

        val animLength = model.animationEvents
            .filter { it.type == AnimationEventType.EMOTE }
            .maxOfOrNull { it.getTotalTime(model, loopResetTime + PlayerPoseManager.transitionTime) }
            ?: 0f
        val previewLength = animLength - PlayerPoseManager.transitionTime

        emoteScheduled = true
        emoteEquipped.set(true)
        delays += component.delay((previewLength * 1000).toLong()) {
            emoteEquipped.set(false)
            delays += component.delay(1000L) {
                reset()
            }
        }
    }

}