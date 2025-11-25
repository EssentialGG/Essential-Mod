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

import gg.essential.cosmetics.EquippedCosmetic
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UpdateFunc
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableListStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.mod.Model
import gg.essential.mod.Skin
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.CAPE_DISABLED_COSMETIC
import gg.essential.mod.cosmetics.preview.PerspectiveCamera
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.util.GuiEssentialPlatform.Companion.platform

class CosmeticPreview(val cosmetic: Cosmetic, val settings: State<List<CosmeticSetting>> = mutableListStateOf()) : UIContainer() {

    private var loadingIcon = LoadingIcon(2.0)
    private val emulatedUI3DPlayer: UIPlayer
    private val emoteScheduler: EmoteScheduler?

    init {
        val profile = if (cosmetic.type.slot == CosmeticSlot.EMOTE) {
            null // A null profile will copy the player's profile so it will adapt to skin changes
        } else {
            Pair(Skin("f91f0820500c414d308c5678594631b917e51e06a31fedaacac5dad1a44a49d8", Model.STEVE), null)
        }
        val slot = cosmetic.type.slot
        emoteScheduler = if (slot == CosmeticSlot.EMOTE) EmoteScheduler(this, stateOf(cosmetic), settings) else null
        emulatedUI3DPlayer = platform.newUIPlayer(
            camera = stateOf(PerspectiveCamera.forCosmeticSlot(slot)),
            profile = stateOf(profile),
            cosmetics =
                memo {
                    buildMap {
                        if (emoteScheduler == null || emoteScheduler.emoteEquipped()) {
                            put(slot, EquippedCosmetic(cosmetic, settings()))
                        }
                        if (slot == CosmeticSlot.EMOTE) {
                            // hide third party capes for emote previews, which use the player's profile
                            put(CosmeticSlot.CAPE, EquippedCosmetic(CAPE_DISABLED_COSMETIC, emptyList()))
                        }
                    }
                }
        ).apply {
            constrain {
                width = 100.percent
                height = 100.percent
            }
        }
        addChild(emulatedUI3DPlayer)

        // Hide player while loading (we do need to add it as a child so it actually loads)
        emulatedUI3DPlayer.enableEffect(ScissorEffect(0f, 0f, 0f, 0f))
        addChild(loadingIcon)

        addUpdateFunc(UpdateLoadingState())
    }

    inner class UpdateLoadingState : UpdateFunc {
        override fun invoke(dt: Float, dtMs: Int) {
            val wearablesManager = emulatedUI3DPlayer.wearablesManager
            if (wearablesManager != null && wearablesManager.state.cosmetics.isNotEmpty()) {
                removeUpdateFunc(this)
                removeChild(loadingIcon)
                // Replace the zero-size scissor effect we used while loading with a regularly sized one
                emulatedUI3DPlayer.removeEffect<ScissorEffect>()
                emulatedUI3DPlayer.enableEffect(ScissorEffect())

            }
        }
    }
}
