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

import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialDropDown
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.common.HighlightedBlock
import gg.essential.gui.common.input.UITextInput
import gg.essential.gui.common.input.essentialInput
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.components.coinPackImage
import gg.essential.gui.wardrobe.components.coinsText
import gg.essential.gui.wardrobe.components.infoIcon
import gg.essential.network.connectionmanager.coins.CoinBundle
import gg.essential.network.connectionmanager.features.Feature
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.vigilance.utils.onLeftClick
import kotlin.math.max

class CoinsPurchaseModal private constructor(
    modalManager: ModalManager,
    val state: WardrobeState,
    coinsNeeded: Int? = null,
) : Modal(modalManager) {
    private val container = HighlightedBlock(
        backgroundColor = EssentialPalette.MODAL_BACKGROUND,
        highlightColor = EssentialPalette.BUTTON_HIGHLIGHT,
        highlightHoverColor = EssentialPalette.BUTTON_HIGHLIGHT,
    )

    init {
        container.constrainBasedOnChildren()
        container.contentContainer.constrain {
            width = ChildBasedSizeConstraint() + 20.pixels
            height = ChildBasedSizeConstraint() + 20.pixels
        }

        val coinsManager = state.coinsManager

        val coinsNeededState = State { 0 }
        val hasCoinsNeededState = coinsNeededState.letState { it > 0 }

        val creatorCodeTooltip = coinsManager.creatorCodeName.letState { "Your purchase supports $it." }

        val creatorCodeInput = UITextInput("Creator Code", shadowColor = EssentialPalette.BLACK).apply {
            setText(coinsManager.creatorCode.getUntracked())
            onUpdate { newText ->
                val upper = newText.uppercase()
                // If not all uppercase, set it to uppercase. The resulting new update call will then actually save it
                if (upper != newText) {
                    setText(upper)
                } else {
                    coinsManager.creatorCodeConfigured.set(newText)
                }
            }
        }

        // We disable the dropdown if we have more one or fewer currencies configured
        val currencyDropdownDisabled = coinsManager.currencies.letState { it.size <= 1 }

        val dropdown = EssentialDropDown(
            coinsManager.currency.getUntracked() ?: USD_CURRENCY,
            coinsManager.currencies.mapEach { EssentialDropDown.Option(it.currencyCode, it) },
            disabled = currencyDropdownDisabled,
        )

        dropdown.selectedOption.onChange(this) {
            coinsManager.currencyRaw.set(it.value.currencyCode)
        }

        fun LayoutScope.bundleBox(originalBundle: CoinBundle) {
            val bundleState = memo {
                val missingCoins = coinsNeededState()
                if (originalBundle.isExchangeBundle && missingCoins > 0) {
                    val minimumBundleSize = state.settings.youNeedMinimumAmount()
                    originalBundle.getBundleForNumberOfCoins(max(minimumBundleSize, missingCoins))
                } else originalBundle
            }
            bind(bundleState) { bundle ->
                val colorBackground = if (bundle.isHighlighted || bundle.isSpecificAmount) EssentialPalette.COINS_BLUE_BACKGROUND else EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT
                val colorBackgroundHover = if (bundle.isHighlighted || bundle.isSpecificAmount) EssentialPalette.COINS_BLUE_BACKGROUND_HOVER else EssentialPalette.LIGHTEST_BACKGROUND
                val colorPriceBackground = if (bundle.isHighlighted || bundle.isSpecificAmount) EssentialPalette.COINS_BLUE_PRICE_BACKGROUND else EssentialPalette.LIGHTEST_BACKGROUND
                val colorPriceBackgroundHover = if (bundle.isHighlighted || bundle.isSpecificAmount) EssentialPalette.COINS_BLUE_PRICE_BACKGROUND_HOVER else EssentialPalette.SCROLLBAR
                val extraCoins = ((bundle.extraPercent.toDouble() / (bundle.extraPercent.toDouble() + 100.0)) * bundle.numberOfCoins.toDouble()).toInt()
                val coinsWithExtraRemoved = bundle.numberOfCoins - extraCoins

                box(Modifier.width(112f).height(168f).color(colorBackground).hoverColor(colorBackgroundHover).shadow().hoverScope()) {
                    box(Modifier.alignVertical(Alignment.Start(padding = 19f))) {
                        bind(coinsNeededState) { missingCoins ->
                            if (bundle.isExchangeBundle && missingCoins > 0) {
                                coinPackImage(coinsManager, bundle.numberOfCoins)
                            } else {
                                coinPackImage(bundle.iconFactory)
                            }
                        }
                    }
                    column(Modifier.fillWidth().alignVertical(Alignment.End)) {
                        coinsText(coinsWithExtraRemoved)
                        if (extraCoins > 0) {
                            spacer(height = 4f)
                            row {
                                text("Bonus! ", Modifier.color(EssentialPalette.BONUS_COINS_COLOR).shadow(EssentialPalette.TEXT_SHADOW))
                                text("+", Modifier.shadow(EssentialPalette.TEXT_SHADOW))
                                coinsText(extraCoins)
                            }
                        }
                        spacer(height = 8f)
                        box(Modifier.fillWidth().height(23f).color(colorPriceBackground).hoverColor(colorPriceBackgroundHover)) {
                            text(bundle.formattedPrice, Modifier.shadow(EssentialPalette.TEXT_SHADOW)
                                .alignVertical(Alignment.End(7f)))
                        }
                    }
                    if (bundle.isExchangeBundle) {
                        if_(hasCoinsNeededState) {
                            column(
                                Modifier.childBasedSize(2f).alignBoth(Alignment.Start(-2f)).color(EssentialPalette.BANNER_BLUE).shadow(),
                                Arrangement.spacedBy(0f, FloatPosition.CENTER),
                            ) {
                                spacer(height = 1f)
                                text("You need...", Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(EssentialPalette.TEXT_TRANSPARENT_SHADOW))
                            }
                        }
                    }
                    if (bundle.isHighlighted) {
                        column(
                            Modifier.childBasedSize(2f).alignBoth(Alignment.Start(-2f)).color(EssentialPalette.BANNER_BLUE).shadow(),
                            Arrangement.spacedBy(0f, FloatPosition.CENTER),
                        ) {
                            spacer(height = 1f)
                            text("Most Popular", Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(EssentialPalette.TEXT_TRANSPARENT_SHADOW))
                        }
                    }
                    /* The Extra Banner may come back in the future so keeping it here for now.
                    if (bundle.extraPercent > 0) {
                        column(
                            Modifier.childBasedSize(2f).alignHorizontal(Alignment.End(-2f)).alignVertical(Alignment.Start(-2f)).color(EssentialPalette.BANNER_RED).shadow(),
                            Arrangement.spacedBy(0f, FloatPosition.CENTER),
                        ) {
                            spacer(height = 1f)
                            text("${bundle.extraPercent}% EXTRA", Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(EssentialPalette.TEXT_TRANSPARENT_SHADOW))
                        }
                    }
                    */
                }.onLeftClick {
                    USound.playButtonPress()
                    coinsManager.purchaseBundle(bundle) { uri ->
                        close()
                        openInBrowser(uri)
                    }
                }
            }
        }

        container.contentContainer.layoutAsBox(Modifier.width(510f).childBasedHeight()) {
            column(Modifier.fillWidth(padding = 16f)) {
                spacer(height = 11f)
                box(Modifier.fillWidth().height(17f)) {
                    row(Modifier.fillHeight().alignHorizontal(Alignment.Start), Arrangement.spacedBy(5f)) {
                        essentialInput(creatorCodeInput, coinsManager.creatorCodeValid.letState { it == false }, "Invalid Creator Code", Modifier.width(90f).fillHeight())

                        if_({ coinsManager.creatorCodeValid() == true }) {
                            box(Modifier.width(13f).height(13f).color(EssentialPalette.GREEN_BUTTON_HOVER).shadow().hoverScope().hoverTooltip(creatorCodeTooltip.toV1(this.stateScope), position = EssentialTooltip.Position.ABOVE)) {
                                image(EssentialPalette.CHECKMARK_7X5, Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(EssentialPalette.TEXT_TRANSPARENT_SHADOW))
                            }
                        } `else` {
                            infoIcon("Support your favorite creator\nusing their Essential Creator Code.", position = EssentialTooltip.Position.ABOVE)
                        }
                    }
                    row(Modifier.fillHeight(), Arrangement.spacedBy(5f)) {
                        val minimumAmount = state.settings.youNeedMinimumAmount.getUntracked()

                        text("Essential Coins", Modifier.alignVertical(Alignment.Center(true)).shadow(EssentialPalette.BLACK))
                        infoIcon("Unlock cosmetics and emotes with Essential Coins", position = EssentialTooltip.Position.ABOVE)
                    }
                    row(Modifier.fillHeight().alignHorizontal(Alignment.End), Arrangement.spacedBy(5f)) {
                        dropdown(Modifier.width(47f).shadow().hoverScope().whenTrue(currencyDropdownDisabled, Modifier.hoverTooltip(stateOf("Other currencies coming soon!"), position = EssentialTooltip.Position.ABOVE)))
                        box(Modifier.widthAspect(1f).fillHeight().color(EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT).hoverColor(EssentialPalette.GRAY_BUTTON_HOVER).shadow().hoverScope()) {
                            icon(EssentialPalette.CANCEL_5X, Modifier.color(EssentialPalette.TEXT))
                        }.onLeftClick {
                            USound.playButtonPress()
                            close()
                        }
                    }
                }
                spacer(height = 13f)
                if_(coinsManager.pricing.isEmpty()) {
                    box(Modifier.childBasedWidth().height(168f)) {
                        box(Modifier.childBasedWidth(14f).childBasedHeight(12f).color(EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT)) {
                            text("Please try again later or contact support...", Modifier.color(EssentialPalette.TEXT).shadow(EssentialPalette.TEXT_SHADOW))
                        }
                    }
                }.`else` {
                    row(Modifier.fillWidth(), Arrangement.SpaceBetween) {
                        forEach(coinsManager.pricing) {
                            bundleBox(it)
                        }
                    }
                }
                spacer(height = 16f)
            }
        }
    }

    override fun LayoutScope.layoutModal() {
        container()
    }

    override fun afterInitialization() {
        super.afterInitialization()
        // Refresh prices when we open the modal to ensure we always have the latest prices
        // This is invisible to the user if the prices have not changed, so there's no harm in doing it
        state.coinsManager.refreshPricing()
    }

    companion object {
        fun open(state: WardrobeState, coinsNeeded: Int? = null) {
            platform.pushModal { create(it, state, coinsNeeded) }
        }

        fun create(modalManager: ModalManager, state: WardrobeState, coinsNeeded: Int? = null): Modal {
            return if (platform.disabledFeaturesManager.isFeatureDisabled(Feature.COIN_BUNDLE_PURCHASE)) {
                StoreDisabledModal(modalManager)
            } else {
                CoinsPurchaseModal(modalManager, state, coinsNeeded)
            }
        }
    }
}
