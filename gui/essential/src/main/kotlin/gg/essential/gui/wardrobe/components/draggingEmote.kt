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

import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.MousePositionConstraint
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.pixels
import gg.essential.gui.common.CosmeticPreview
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.BasicXModifier
import gg.essential.gui.layoutdsl.BasicYModifier
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignBoth
import gg.essential.gui.layoutdsl.floatingBox
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.then
import gg.essential.gui.layoutdsl.tooltip
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.util.getTag
import gg.essential.gui.util.layoutSafePollingState
import gg.essential.gui.util.selfAndParents
import gg.essential.gui.wardrobe.EmoteWheelPage
import gg.essential.gui.wardrobe.WardrobeContainer
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.universal.UMouse
import gg.essential.util.findParentOfTypeOrNull
import kotlin.math.abs

fun LayoutScope.draggingEmote(state: WardrobeState, draggedEmote: WardrobeState.DraggedEmote) {
    val clickStart = Pair(UMouse.Scaled.x.toFloat(), UMouse.Scaled.y.toFloat())
    val isFromWardrobe = draggedEmote.from == null
    val cosmeticId = draggedEmote.emoteId
    val cosmetic = cosmeticId?.let { state.cosmeticsData.getCosmetic(it) }
    val clickOffset = draggedEmote.clickOffset
    val mayBeClick = mutableStateOf(true)

    val positioning = Modifier.then(BasicXModifier { MousePositionConstraint() - clickOffset.first.pixels })
        .then(BasicYModifier { MousePositionConstraint() - clickOffset.second.pixels })

    val container = floatingBox(Modifier.width(0f).height(0f).then(positioning)) { // Zero size so it cannot be hovered
        if (cosmetic != null) {
            val tooltip = Modifier.then(memo {
                val tooltipText = when {
                    isFromWardrobe && state.draggingOntoOccupiedEmoteSlot() -> "Replace"
                    state.draggingOntoOccupiedEmoteSlot() -> "Swap"
                    !isFromWardrobe && state.draggingEmote()?.to == null -> "Remove"
                    else -> cosmetic.displayName
                }
                Modifier.tooltip(tooltipText, position = EssentialTooltip.Position.MOUSE_OFFSET(10f, -15f), notchSize = 0)
            })

            if_({ !isFromWardrobe || !mayBeClick() }) {
                CosmeticPreview(cosmetic)(EmoteWheelPage.slotModifier.alignBoth(Alignment.Start).then(tooltip))
            }
        }
    }.onMouseDrag { _, _, _ ->
        val distance = abs(UMouse.Scaled.x.toFloat() - clickStart.first) + abs(UMouse.Scaled.y.toFloat() - clickStart.second)
        if (distance > 5) {
            mayBeClick.set(false)
        }
    }.onMouseRelease {
        Window.enqueueRenderOperation {
            val dragged = state.draggingEmote.getUntracked() ?: return@enqueueRenderOperation
            if (!mayBeClick.getUntracked()) {
                val manager = state.emoteWheelManager
                val targetEmote = dragged.to?.let { manager.getEmoteWheel(it.emoteWheelId)?.slots?.get(it.slotIndex) }
                dragged.from?.let { manager.setEmote(it.emoteWheelId, it.slotIndex, targetEmote) }
                dragged.to?.let { manager.setEmote(it.emoteWheelId, it.slotIndex, cosmeticId) }
            } else {
                dragged.onInstantLeftClick()
            }
            state.draggingEmote.set(null)
        }
    }

    val targetState = container.layoutSafePollingState {
        val mouseX = UMouse.Scaled.x.toFloat()
        val mouseY = UMouse.Scaled.y.toFloat()
        Window.of(container).hitTest(mouseX, mouseY)
    }

    effect(container) {
        val target = targetState()
        val slotTarget = target.selfAndParents().firstNotNullOfOrNull { it.getTag<EmoteWheelPage.EmoteSlotTag>()?.emoteSlotId }
        val removeTarget = target.findParentOfTypeOrNull<WardrobeContainer>()
        state.draggingEmote.set { draggingEmote ->
            val to = when {
                slotTarget != null -> WardrobeState.EmoteSlotId(slotTarget.emoteWheelId, slotTarget.slotIndex)
                removeTarget != null -> null
                else -> draggingEmote?.from
            }
            draggingEmote?.copy(to = to) ?: draggingEmote
        }
    }
}