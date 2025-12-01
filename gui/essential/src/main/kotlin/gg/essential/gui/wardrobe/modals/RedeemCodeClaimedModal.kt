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
package gg.essential.gui.wardrobe.modals

import gg.essential.connectionmanager.common.packet.store.Product
import gg.essential.connectionmanager.common.packet.store.ProductRedemptionState
import gg.essential.connectionmanager.common.packet.store.ServerRedeemStoreClaimResponsePacket
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.dsl.effect
import gg.essential.elementa.dsl.pixel
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.utils.withAlpha
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.CosmeticPreview
import gg.essential.gui.common.effect.HorizontalScissorEffect
import gg.essential.gui.common.modal.EssentialModal2
import gg.essential.gui.common.sendCosmeticUnlockedToast
import gg.essential.gui.elementa.state.v2.filter
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedMaxHeight
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.layoutdsl.fillRemainingWidth
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.gradient
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.heightAspect
import gg.essential.gui.layoutdsl.maxHeight
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.scrollable
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.layoutdsl.wrappedText
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.wardrobe.Item.Companion.toItemTier
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.components.coinPackImage
import gg.essential.gui.wardrobe.components.coinsText
import gg.essential.util.scrollGradient
import java.awt.Color

class RedeemCodeClaimedModal(
    modalManager: ModalManager,
    private val serverRedeemStoreClaimResponsePacket: ServerRedeemStoreClaimResponsePacket,
    private val wardrobeState: WardrobeState,
) : EssentialModal2(modalManager, false) {

    private lateinit var scroller: ScrollComponent

    init {
        wardrobeState.cosmeticsManager.infraCosmeticsData.requestCosmeticsIfMissing(
            serverRedeemStoreClaimResponsePacket.products.filter { it.type == "COSMETIC" }.map { it.key }
        )
    }

    override fun LayoutScope.layoutTitle() {
        wrappedText(serverRedeemStoreClaimResponsePacket.redemptionMessage["en_us"] ?: "", Modifier.shadow(Color.BLACK), centered = true)
    }

    override fun LayoutScope.layoutBody() {
        scroller = scrollable(
            Modifier.fillWidth().childBasedMaxHeight().maxHeight(166f),
            vertical = true
        ) {
            column(Modifier.fillWidth(), Arrangement.spacedBy(5f)) {
                for (product in serverRedeemStoreClaimResponsePacket.products.sortedBy { it.state }) {
                    entry(product)
                }
            }
        }
        scroller.removeEffect<ScissorEffect>()
        scroller.effect(HorizontalScissorEffect(bottomMargin = 1.pixel))
        scroller.scrollGradient(20.pixels)
    }

    override fun LayoutScope.layoutBodyScrollBar() = layoutBodyScrollBarImpl(scroller)

    override fun LayoutScope.layoutButtons() {
        primaryButton("Claim") {
            close()
        }
    }

    private fun LayoutScope.entry(product: Product) {
        val titleModifier = Modifier.alignHorizontal(Alignment.Start).shadow(Color.BLACK)
        when {
            product.type == "COSMETIC" -> {
                bind({ wardrobeState.rawCosmetics().firstOrNull { it.id == product.key } }) { cosmetic ->
                    entry(
                        product.state,
                        cosmetic?.tier?.toItemTier()?.barColor ?: Color.WHITE,
                        {
                            if (cosmetic != null) {
                                CosmeticPreview(cosmetic)(Modifier.fillParent())
                            }
                        }
                    ) { text(cosmetic?.displayName ?: "Unknown Item", titleModifier) }
                }
            }
            product.type == "CURRENCY" && product.key == "coins" -> {
                entry(
                    product.state,
                    EssentialPalette.COINS_BLUE,
                    {
                        coinPackImage(wardrobeState.coinsManager, product.amount, Modifier.fillParent())
                    }
                ) { coinsText(product.amount, titleModifier) }
            }
            else -> {
                entry(product.state, Color.WHITE, {}) { text("Unknown Item", titleModifier) }
            }
        }
    }

    private fun LayoutScope.entry(redemptionState: ProductRedemptionState, color: Color, image: LayoutScope.() -> Unit, title: LayoutScope.() -> Unit) {
        box(Modifier.width(189f).height(50f).color(EssentialPalette.MODAL_BACKGROUND).shadow()) {
            // Gradient background
            box(Modifier.height(48f).fillWidth().alignVertical(Alignment.Start)
                    .gradient(top = color.withAlpha(0.05f), bottom = color.withAlpha(0.2f)))
            // Bottom colour bar
            box(Modifier.height(2f).fillWidth().color(color).alignVertical(Alignment.End))
            // Content
            row(Modifier.fillWidth(padding = 10f), Arrangement.spacedBy(6f)) {
                box(Modifier.width(32f).heightAspect(1f).color(EssentialPalette.COMPONENT_BACKGROUND).shadow()) {
                    image()
                }
                box(Modifier.fillRemainingWidth()) {
                    title()
                }
                when (redemptionState) {
                    ProductRedemptionState.ALREADY_OWNED -> text("OWNED", Modifier.shadow(EssentialPalette.TEXT_SHADOW))
                    ProductRedemptionState.REDEEMED -> text("FREE", Modifier.shadow(EssentialPalette.TEXT_SHADOW))
                }
            }
            if (redemptionState == ProductRedemptionState.ALREADY_OWNED) {
                box(Modifier.fillParent().color(EssentialPalette.MODAL_BACKGROUND.withAlpha(0.5f)))
            }
        }
    }

    override fun onClose() {
        super.onClose()
        wardrobeState.triggerPurchaseAnimation()
        serverRedeemStoreClaimResponsePacket.products.filter { it.state == ProductRedemptionState.REDEEMED && it.type == "COSMETIC" }
            .mapNotNull { product -> wardrobeState.rawCosmetics.getUntracked().find { it.id == product.key } }
            .forEach { sendCosmeticUnlockedToast(it) }
    }

}
