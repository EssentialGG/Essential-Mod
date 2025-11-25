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
package gg.essential.gui.friends.message.v2

import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.dsl.constrain
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.CosmeticPreview
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.components.openWardrobeWithHighlight
import gg.essential.gui.util.hoveredState
import gg.essential.gui.wardrobe.ItemId
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.vigilance.utils.onLeftClick

class GiftEmbedImpl(
    cosmeticId: String,
    messageWrapper: MessageWrapper
) : GiftEmbed(messageWrapper) {
    private val cosmeticsManager = platform.cosmeticsManager

    private val sent = messageWrapper.sentByClient
    private val giftText = if (sent) "Gift sent" else "Gift"
    private val cosmetic = stateBy {
        val unlockedCosmetics = cosmeticsManager.unlockedCosmetics()
        cosmeticsManager.cosmeticsData.cosmetics().firstOrNull { it.id == cosmeticId && (sent || it.id in unlockedCosmetics) }
    }
    private val shadowEffect = Modifier.shadow(EssentialPalette.TEXT_SHADOW)

    init {
        cosmeticsManager.infraCosmeticsData.requestCosmeticsIfMissing(listOf(cosmeticId))

        colorState.rebind(hoveredState().map { if (it) EssentialPalette.GRAY_BUTTON_HOVER else EssentialPalette.GRAY_BUTTON })

        constrain {
            width = ChildBasedMaxSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        }

        bubble.layoutAsBox {
            row {
                box(Modifier.width(18f).heightAspect(1f).color(EssentialPalette.COMPONENT_BACKGROUND).then(shadowEffect)) {
                    ifNotNull(cosmetic) { cosmetic ->
                        CosmeticPreview(cosmetic)(Modifier.fillParent())
                    }
                }
                spacer(width = 10f)
                column(Arrangement.spacedBy(4f), Alignment.Start) {
                    row(Arrangement.spacedBy(5f)) {
                        icon(EssentialPalette.WARDROBE_GIFT_7X, Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).then(shadowEffect))
                        text(giftText, shadowEffect)
                    }
                    text(cosmetic.map { it?.displayName ?: "Unknown" }, Modifier.color(EssentialPalette.TEXT).then(shadowEffect))
                }
                if (!sent) {
                    spacer(width = 15f)
                    viewButton()
                }
            }
        }
    }

    private fun LayoutScope.viewButton() {
        val buttonModifier = Modifier.childBasedWidth(10f).childBasedHeight(4f).shadow(EssentialPalette.TEXT_SHADOW)
            .color(EssentialPalette.BLUE_BUTTON).hoverColor(EssentialPalette.BLUE_BUTTON_HOVER)

        column(buttonModifier.hoverScope(), Arrangement.spacedBy(0f, FloatPosition.CENTER)) {
            spacer(height = 1f) // Extra pixel for text shadow
            text("View", Modifier.shadow(EssentialPalette.TEXT_SHADOW))
        }.onLeftClick {
            cosmetic.get()?.let { openWardrobeWithHighlight(ItemId.CosmeticOrEmote(it.id)) }
        }
    }

    override fun beginHighlight() { }
    override fun releaseHighlight() { }
}
