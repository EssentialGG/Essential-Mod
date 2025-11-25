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

import dev.folomeev.kotgl.matrix.vectors.vecUnitY
import gg.essential.cosmetics.CosmeticId
import gg.essential.cosmetics.EquippedCosmetic
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.effect
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.layoutdsl.then
import gg.essential.gui.wardrobe.Item
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.preview.PerspectiveCamera
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.model.util.Quaternion
import gg.essential.model.util.rotateBy
import gg.essential.util.GuiEssentialPlatform.Companion.platform

fun LayoutScope.skinRenderPreview(skin: Item.SkinItem, modifier: Modifier = Modifier) {
    platform.newUIPlayer(
        camera = stateOf(PerspectiveCamera.forCosmeticSlot(CosmeticSlot.FULL_BODY)),
        profile = stateOf(Pair(skin.skin, null)),
        cosmetics = stateOf(emptyMap()),
    )(Modifier.fillParent().effect { ScissorEffect() }.then(modifier))
}

fun LayoutScope.outfitRenderPreview(state: WardrobeState, outfit: Item.OutfitItem, modifier: Modifier = Modifier) {
    fullBodyRenderPreview(
        state, outfit.skin, outfit.cosmetics, outfit.settings,
        showEmotes = false, constantRotation = false, modifier = modifier
    )
}

fun LayoutScope.bundleRenderPreview(state: WardrobeState, bundle: Item.Bundle, modifier: Modifier = Modifier) {
    fullBodyRenderPreview(
        state, bundle.skin?.toMod(), bundle.cosmetics, bundle.settings,
        showEmotes = true, constantRotation = bundle.rotateOnPreview, modifier = modifier
    )
}

fun LayoutScope.fullBodyRenderPreview(
    state: WardrobeState,
    skin: gg.essential.mod.Skin?,
    cosmeticsMap: Map<CosmeticSlot, CosmeticId>,
    settings: Map<CosmeticId, List<CosmeticSetting>>,
    showEmotes: Boolean,
    constantRotation: Boolean = false,
    modifier: Modifier = Modifier
) {
    val loading = mutableStateOf(cosmeticsMap.isNotEmpty())
    val emulatedUI3DPlayerModifierState = loading.map {
        Modifier.fillParent().effect { if (it) ScissorEffect(0f, 0f, 0f, 0f) else ScissorEffect() }
    }
    val cameraAngle = mutableStateOf(0f)
    val visibleCosmeticIds = if (showEmotes) cosmeticsMap else cosmeticsMap - CosmeticSlot.EMOTE
    val resolvedCosmetics = memo { with(state) { resolveCosmeticIds(visibleCosmeticIds, settings) } }
    val emoteId = visibleCosmeticIds[CosmeticSlot.EMOTE]
    val emote = memo { resolvedCosmetics()[CosmeticSlot.EMOTE] }

    box(Modifier.fillParent().then(modifier)) {
        val container = containerDontUseThisUnlessYouReallyHaveTo
        val currentCosmetics: State<Map<CosmeticSlot, EquippedCosmetic>>
        if (emoteId != null) {
            val emoteScheduler = EmoteScheduler(container, memo { emote()?.cosmetic }, memo { emote()?.settings })
            val scheduledEmote = memo {
                val cosmetic = emote()
                if (cosmetic != null && emoteScheduler.emoteEquipped()) {
                    mapOf(CosmeticSlot.EMOTE to cosmetic)
                } else {
                    emptyMap()
                }
            }
            currentCosmetics = memo { resolvedCosmetics() - CosmeticSlot.EMOTE + scheduledEmote() }
        } else {
            currentCosmetics = resolvedCosmetics
        }
        val emulatedUI3DPlayer = platform.newUIPlayer(
            camera = cameraAngle.map { angle ->
                val rotation = Quaternion.fromAxisAngle(vecUnitY(), angle)
                with(PerspectiveCamera.forCosmeticSlot(CosmeticSlot.FULL_BODY)) {
                    copy(camera = camera.rotateBy(rotation), target = target.rotateBy(rotation))
                }
            },
            profile = stateOf(skin?.let { Pair(skin, null) }),
            cosmetics = currentCosmetics,
        ).apply {
            if (constantRotation) {
                val fullRotationMillis = 10000.0
                val angleStep = 360.0 / fullRotationMillis
                addUpdateFunc { _, _ ->
                    val angle = ((((System.currentTimeMillis() - state.wardrobeOpenTime) * angleStep) % 360.0) * Math.PI / 180.0).toFloat()
                    cameraAngle.set(angle)
                }
            }
        }(Modifier.then(emulatedUI3DPlayerModifierState))

        if_(loading) {
            LoadingIcon(2.0)().addUpdateFunc { _, _ ->
                loading.set(emulatedUI3DPlayer.wearablesManager?.state?.cosmetics.isNullOrEmpty() && cosmeticsMap.isNotEmpty())
            }
        }
    }
}
