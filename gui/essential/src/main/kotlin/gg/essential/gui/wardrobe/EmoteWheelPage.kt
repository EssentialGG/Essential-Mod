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
package gg.essential.gui.wardrobe

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.flatten
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.util.Tag
import gg.essential.gui.util.addTag
import gg.essential.gui.util.hoveredStateV2
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.universal.UMouse
import gg.essential.universal.USound
import gg.essential.vigilance.utils.onLeftClick

class EmoteWheelPage(private val state: WardrobeState) : UIContainer() {

    init {
        val containerModifier = Modifier.childBasedMaxSize()
        layout {
            box(containerModifier) {
                column(Arrangement.spacedBy(7f)) {
                    bind({ state.emoteWheelManager.selectedEmoteWheelId() }) { emoteWheelId ->
                        if (emoteWheelId == null) {
                            return@bind
                        }
                        row(Arrangement.spacedBy(7f)) {
                            emoteSlot(slotModifier, WardrobeState.EmoteSlotId(emoteWheelId, 0))
                            emoteSlot(slotModifier, WardrobeState.EmoteSlotId(emoteWheelId, 1))
                            emoteSlot(slotModifier, WardrobeState.EmoteSlotId(emoteWheelId, 2))
                        }
                        row(Arrangement.spacedBy(7f)) {
                            emoteSlot(slotModifier, WardrobeState.EmoteSlotId(emoteWheelId, 3))
                            box(slotModifier)
                            emoteSlot(slotModifier, WardrobeState.EmoteSlotId(emoteWheelId, 4))
                        }
                        row(Arrangement.spacedBy(7f)) {
                            emoteSlot(slotModifier, WardrobeState.EmoteSlotId(emoteWheelId, 5))
                            emoteSlot(slotModifier, WardrobeState.EmoteSlotId(emoteWheelId, 6))
                            emoteSlot(slotModifier, WardrobeState.EmoteSlotId(emoteWheelId, 7))
                        }
                    }
                }
            }
        }
    }

    private fun LayoutScope.emoteSlot(modifier: Modifier, emoteSlotId: WardrobeState.EmoteSlotId) {
        val cosmetic = memo {
            val slots = state.emoteWheelManager.orderedEmoteWheels().find { it.id == emoteSlotId.emoteWheelId }?.slots ?: return@memo null
            slots[emoteSlotId.slotIndex]?.let { state.cosmeticsData.getCosmetic(it) }
        }
        val hoveredSource = mutableStateOf(stateOf(false))
        val hovered = hoveredSource.flatten()
        val draggingInProgress = state.draggingEmote.map { it != null }
        val beingDraggedFrom = memo { cosmetic() != null && state.draggingEmote()?.from == emoteSlotId }
        val beingDraggedOnto = memo {
            val draggingEmote = state.draggingEmote()
            draggingEmote?.to != draggingEmote?.from && draggingEmote?.to == emoteSlotId
        }
        val visibleCosmetic = State { cosmetic().takeUnless { beingDraggedFrom() } }

        val backgroundColor = memo {
            when {
                beingDraggedOnto() -> EssentialPalette.COMPONENT_HIGHLIGHT
                cosmetic() == null -> EssentialPalette.INPUT_BACKGROUND
                hovered() -> EssentialPalette.COMPONENT_HIGHLIGHT
                else -> EssentialPalette.COMPONENT_BACKGROUND
            }
        }

        val outline = Modifier.outline(
            color = BasicState(EssentialPalette.TEXT),
            width = beingDraggedOnto.map { if (it) 1f else 0f }.toV1(this@EmoteWheelPage),
        )

        val fadeOut = Modifier.effect { FadeEffect(backgroundColor.toV1(this@EmoteWheelPage), 0.3f) }

        fun LayoutScope.thumbnail(cosmetic: Cosmetic, modifier: Modifier): UIComponent {
            return CosmeticPreview(cosmetic)(modifier)
        }

        val tooltip = Modifier.then(memo {
            if (hovered() && !draggingInProgress()) {
                val displayName = cosmetic()?.displayName ?: return@memo Modifier
                Modifier.tooltip(displayName, position = EssentialTooltip.Position.MOUSE_OFFSET(10f, -15f), notchSize = 0)
            } else {
                Modifier
            }
        })

        val container = box(modifier.color(backgroundColor).then(outline).then(tooltip)) {
            ifNotNull(visibleCosmetic) { cosmetic ->
                box(Modifier.fillParent().whenTrue(beingDraggedOnto, fadeOut)) {
                    thumbnail(cosmetic, Modifier.fillParent())
                }
            }
        }.addTag(EmoteSlotTag(emoteSlotId))

        hoveredSource.set(container.hoveredStateV2())

        container.onLeftClick {
            val clickOffset = Pair(UMouse.Scaled.x.toFloat() - container.getLeft(), UMouse.Scaled.y.toFloat() - container.getTop())
            state.draggingEmote.set(WardrobeState.DraggedEmote(cosmetic.getUntracked()?.id, emoteSlotId, emoteSlotId, clickOffset) {
                if (cosmetic.getUntracked() != null) {
                    USound.playButtonPress()
                }
                state.emoteWheelManager.setEmote(emoteSlotId.emoteWheelId, emoteSlotId.slotIndex, null)
            })
        }
    }

    class EmoteSlotTag(val emoteSlotId: WardrobeState.EmoteSlotId) : Tag

    companion object {
        const val SLOT_SIZE = 62f
        val slotModifier = Modifier.width(SLOT_SIZE).height(SLOT_SIZE)
    }

}
