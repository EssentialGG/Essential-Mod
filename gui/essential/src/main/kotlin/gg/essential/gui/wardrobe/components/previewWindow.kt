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

import gg.essential.api.gui.NotificationType
import gg.essential.api.gui.Slot
import gg.essential.config.EssentialConfig
import gg.essential.cosmetics.CosmeticId
import gg.essential.cosmetics.EquippedCosmetic
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.PixelConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.basicYConstraint
import gg.essential.elementa.dsl.boundTo
import gg.essential.elementa.dsl.coerceAtMost
import gg.essential.elementa.dsl.coerceIn
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.width
import gg.essential.elementa.events.UIClickEvent
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.CosmeticHoverOutlineEffect
import gg.essential.gui.common.CosmeticPreview
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.common.EmoteScheduler
import gg.essential.gui.common.UIPlayer
import gg.essential.gui.common.bindEffect
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.and
import gg.essential.gui.elementa.state.v2.combinators.component1
import gg.essential.gui.elementa.state.v2.combinators.component2
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.not
import gg.essential.gui.elementa.state.v2.combinators.or
import gg.essential.gui.elementa.state.v2.flatten
import gg.essential.gui.elementa.state.v2.isNotEmpty
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.onChange
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.elementa.state.v2.stateDelegatingTo
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.notification.Notifications
import gg.essential.gui.util.hoverScope
import gg.essential.gui.util.hoveredState
import gg.essential.gui.util.layoutSafePollingState
import gg.essential.gui.util.makeHoverScope
import gg.essential.gui.util.selfAndParents
import gg.essential.gui.wardrobe.EmoteWheelPage
import gg.essential.gui.wardrobe.Item
import gg.essential.gui.wardrobe.WardrobeCategory
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.modals.CoinsPurchaseModal
import gg.essential.gui.wardrobe.modals.PurchaseConfirmationModal
import gg.essential.gui.wardrobe.modals.StoreDisabledModal
import gg.essential.gui.wardrobe.purchaseEquippedCosmetics
import gg.essential.gui.wardrobe.purchaseSelectedBundle
import gg.essential.gui.wardrobe.purchaseSelectedEmote
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.CAPE_DISABLED_COSMETIC_ID
import gg.essential.mod.cosmetics.settings.CosmeticProperty
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.mod.cosmetics.settings.variant
import gg.essential.model.Side
import gg.essential.model.util.Color
import gg.essential.model.util.toJavaColor
import gg.essential.network.connectionmanager.coins.CoinsManager
import gg.essential.network.connectionmanager.cosmetics.AssetLoader
import gg.essential.network.connectionmanager.features.Feature
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMouse
import gg.essential.universal.USound
import gg.essential.util.Client
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.findChildOfTypeOrNull
import gg.essential.util.onLeftClick
import gg.essential.util.scrollGradient
import gg.essential.util.thenAcceptOnMainThread
import gg.essential.util.toState
import gg.essential.vigilance.utils.onLeftClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.round

fun LayoutScope.previewWindowTitleBar(state: WardrobeState, modifier: Modifier) {
    val regularContent = State { !state.isUsingConfigurator() && state.showingDiagnosticsFor() == null }
    val bundleSelected = state.selectedBundle.map { it != null }
    val emoteSelected = state.selectedEmote.map { it != null }
    val characterSelected = bundleSelected or !state.inEmoteWheel
    val characterColor = characterSelected.map { if (it) EssentialPalette.GRAY_BUTTON_HOVER else EssentialPalette.GRAY_BUTTON }
    val characterIconColor = characterSelected.map { if (it) EssentialPalette.TEXT_HIGHLIGHT else EssentialPalette.TEXT }
    val emoteWheelSelected = !characterSelected
    val emoteColor = emoteWheelSelected.map { if (it) EssentialPalette.GRAY_BUTTON_HOVER else EssentialPalette.GRAY_BUTTON }
    val emoteIconColor = emoteWheelSelected.map { if (it) EssentialPalette.TEXT_HIGHLIGHT else EssentialPalette.TEXT }
    val textWithHoverability = memo {
        val selectedBundle = state.selectedBundle()
        val selectedEmote = state.selectedEmote()
        val equippedOutfitItem = state.equippedOutfitItem()
        when {
            selectedBundle != null -> selectedBundle.name to false
            state.inEmoteWheel() -> "Wheel #${state.emoteWheelManager.selectedEmoteWheelIndex() + 1}" to false
            selectedEmote != null -> selectedEmote.name to false
            equippedOutfitItem != null -> equippedOutfitItem.name to true
            else -> "Unknown Outfit" to false
        }
    }
    val text = textWithHoverability.map { it.first }
    val isOutfitMenuOpen = mutableStateOf(false)
    val selectedEmoteHasSound = memo {
        val emote = state.selectedEmote() ?: return@memo false
        val variant = state.selectedPreviewingEquippedSettings()[emote.id]?.variant ?: emote.cosmetic.defaultVariantName

        val model = state.modelLoader.getModel(
            emote.cosmetic,
            variant,
            AssetLoader.Priority.High,
        ).toState()() ?: return@memo false

        model.soundData != null
    }
    var leftButton: UIComponent? = null
    var rightButton: UIComponent? = null
    val leftFlash = mutableStateOf(false)
    val rightFlash = mutableStateOf(false)
    val flashModifier = Modifier.outline(EssentialPalette.TEXT, 1f, true)

    fun LayoutScope.titleBarButton(modifier: Modifier, block: LayoutScope.() -> Unit = {}): UIComponent {
        return box(modifier.width(17f).heightAspect(1f).shadow(EssentialPalette.BLACK), block)
    }
    fun changeEmoteWheelOrOutfit(state: WardrobeState, offset: Int) {
        if (state.inEmoteWheel.get()) {
            state.changeEmoteWheel(offset)
        } else {
            state.changeOutfit(offset)
        }
    }

    val container = box(modifier) {
        val titleBarBox = box(Modifier.fillParent())
        val selectorModifier = BasicXModifier { (CenterConstraint() boundTo titleBarBox).coerceIn(0.pixels, 0.pixels(true)) }.then(BasicWidthModifier { 100.percent.coerceAtMost(115.pixels) })
        row(Modifier.fillWidth(padding = 10f), Arrangement.spacedBy(3f, FloatPosition.START)) {
            row(Arrangement.spacedBy(3f)) {
                if_(regularContent) {
                    titleBarButton(Modifier.color(characterColor).hoverColor(EssentialPalette.GRAY_BUTTON_HOVER).hoverTooltip("Character").hoverScope()) {
                        icon(EssentialPalette.CHARACTER_4X6, Modifier.color(characterIconColor).hoverColor(EssentialPalette.TEXT_HIGHLIGHT))
                    }.onLeftClick { handleClick(it) {
                        state.inEmoteWheel.set(false)
                    } }
                }
                if_(!bundleSelected and regularContent) {
                    titleBarButton(Modifier.color(emoteColor).hoverColor(EssentialPalette.GRAY_BUTTON_HOVER).hoverTooltip("Emote Wheel").hoverScope()) {
                        icon(EssentialPalette.EMOTE_WHEEL_5X, Modifier.color(emoteIconColor).hoverColor(EssentialPalette.TEXT_HIGHLIGHT))
                    }.onLeftClick { handleClick(it) {
                        state.inEmoteWheel.set(true)
                    } }
                }
            }
            box(Modifier.fillRemainingWidth()) {
                row(selectorModifier, Arrangement.spacedBy(3f, FloatPosition.CENTER)) {
                    if_(regularContent) {
                        if_(state.inEmoteWheel or (!emoteSelected and !bundleSelected)) {
                            leftButton = titleBarButton(Modifier.color(EssentialPalette.GRAY_BUTTON)
                                .hoverColor(EssentialPalette.GRAY_BUTTON_HOVER).whenTrue(leftFlash, flashModifier).hoverScope()) {
                                icon(EssentialPalette.ARROW_LEFT_4X7, Modifier.color(EssentialPalette.TEXT).hoverColor(EssentialPalette.TEXT_HIGHLIGHT))
                            }.onLeftClick { handleClick(it) { changeEmoteWheelOrOutfit(state, -1) } }
                        }
                        box(Modifier.fillRemainingWidth().alignVertical(Alignment.Center(true))) {
                            box(Modifier.height(14f).then(
                                // Set width to the text length + padding or 100 percent of the parent which ever is smallest
                                BasicWidthModifier { PixelConstraint(text.map { it.width() + 12f }.toV1(stateScope)).coerceAtMost(100.percent) }
                            ).then {
                                val hovered = hoveredState().toV2()
                                val hoverable = textWithHoverability.map { it.second }
                                makeHoverScope(memo { hoverable() && (hovered() || isOutfitMenuOpen()) })
                                ; {}
                            }) {
                                text(
                                    text,
                                    Modifier.shadow(EssentialPalette.TEXT_SHADOW_LIGHT).color(EssentialPalette.TEXT_HIGHLIGHT)
                                        .hoverColor(EssentialPalette.TEXT),
                                    centeringContainsShadow = false, truncateIfTooSmall = true, centerTruncatedText = true
                                )
                            }.onMouseClick { event ->
                                if (!textWithHoverability.getUntracked().second) {
                                    return@onMouseClick
                                }
                                val item = state.equippedOutfitItem.getUntracked() ?: return@onMouseClick
                                isOutfitMenuOpen.set(true)
                                displayOutfitOptions(item, state, event) { isOutfitMenuOpen.set(false) }
                            }
                        }
                        if_(state.inEmoteWheel or (!emoteSelected and !bundleSelected)) {
                            rightButton = titleBarButton(Modifier.color(EssentialPalette.GRAY_BUTTON)
                                .hoverColor(EssentialPalette.GRAY_BUTTON_HOVER).whenTrue(rightFlash, flashModifier).hoverScope()) {
                                icon(EssentialPalette.ARROW_RIGHT_4X7, Modifier.color(EssentialPalette.TEXT).hoverColor(EssentialPalette.TEXT_HIGHLIGHT))
                            }.onLeftClick { handleClick(it) { changeEmoteWheelOrOutfit(state, 1) } }
                        }
                    }
                }
                ifNotNull(state.showingDiagnosticsFor) { localPath ->
                    text("$localPath/", Modifier.shadow(EssentialPalette.TEXT_SHADOW_LIGHT).alignHorizontal(Alignment.Start), truncateIfTooSmall = true)
                }
            }
            if_({ state.showingDiagnosticsFor() != null }) {
                titleBarButton(Modifier.color(EssentialPalette.GRAY_BUTTON).hoverColor(EssentialPalette.GRAY_BUTTON_HOVER).hoverScope()) {
                    icon(EssentialPalette.CANCEL_7X)
                }.onLeftClick { click -> handleClick(click) { state.showingDiagnosticsFor.set(null) } }
            }
            if_({ state.cosmeticsManager.cosmeticsDataWithChanges != null && state.showingDiagnosticsFor() == null }) {
                val saveTooltip = state.cosmeticsManager.cosmeticsDataWithChanges!!.getUpdatesSummary()
                if_(state.isUsingConfigurator) {
                    val saveColor = saveTooltip.map { if (it == null) EssentialPalette.GRAY_BUTTON else EssentialPalette.YELLOW_BUTTON_HOVER }
                    box(modifier.width(17f).heightAspect(1f).color(saveColor).shadow(EssentialPalette.BLACK).hoverTooltip(saveTooltip.map { it ?: "No changes" }).hoverScope()) {
                        icon(EssentialPalette.SAVE_9X)
                    }.onLeftClick {
                        state.cosmeticsManager.cosmeticsDataWithChanges!!.writeChangesToLocalCosmeticData(state.cosmeticsManager.localCosmeticsData!!).thenAcceptOnMainThread {
                            Notifications.push("Cosmetics", "Data saved to file successfully")
                        }
                    }
                }

                val configuratorColor = memo {
                    when {
                        !state.isUsingConfigurator() && saveTooltip() != null -> EssentialPalette.YELLOW_BUTTON_HOVER
                        state.isUsingConfigurator() -> EssentialPalette.GRAY_BUTTON_HOVER
                        else -> EssentialPalette.GRAY_BUTTON
                    }
                }
                titleBarButton(Modifier.color(configuratorColor).hoverColor(EssentialPalette.GRAY_BUTTON_HOVER).hoverTooltip("Cosmetic editor").hoverScope()) {
                    icon(EssentialPalette.SETTINGS_9X7)
                }.onLeftClick { click -> handleClick(click) { state.editingMenuOpen.set { !it } } }
            }
            if_(!emoteSelected and !bundleSelected and !state.inEmoteWheel and regularContent) {
                outfitAddButton(state)
            }
            if_(selectedEmoteHasSound and !state.isUsingConfigurator and !state.inEmoteWheel) {
                titleBarButton(Modifier.color(EssentialPalette.GRAY_BUTTON).hoverColor(EssentialPalette.GRAY_BUTTON_HOVER).hoverScope()) {
                    icon({ if (EssentialConfig.playEmoteSoundsInWardrobe()) EssentialPalette.UNMUTE_10X7 else EssentialPalette.MUTE_10X7 },
                        Modifier.color(EssentialPalette.TEXT).hoverColor(EssentialPalette.TEXT_HIGHLIGHT))
                }.onLeftClick { click -> handleClick(click) { EssentialConfig.playEmoteSoundsInWardrobe.set { !it } } }
            }
        }
    }

    // Animation and behavior for dragging an emote over an emote wheel switch button
    var targetButton: UIComponent? = null
    var animationScope: CoroutineScope? = null

    fun resetDragAnimation() {
        leftFlash.set(false)
        rightFlash.set(false)
        animationScope?.cancel()
        animationScope = null
    }

    container.addUpdateFunc { _, _ ->
        if (leftButton == null || rightButton == null || state.draggingEmote.getUntracked() == null) {
            if (animationScope != null) {
                resetDragAnimation()
            }
            return@addUpdateFunc
        }

        // Get which button, if any, the emote is being dragged on top of
        val (mouseX, mouseY) = UMouse.Scaled.x.toFloat() to UMouse.Scaled.y.toFloat()
        val target = Window.of(container).hitTest(mouseX, mouseY).selfAndParents().firstOrNull { it == leftButton || it == rightButton }
        if (target != targetButton) {
            // Target changed, reset animation
            targetButton = target
            resetDragAnimation()
        }

        if (targetButton != null && animationScope == null) {
            val isLeftButton = target == leftButton

            animationScope = CoroutineScope(Dispatchers.Client)
            animationScope?.launch {
                delay(750)
                repeat(3) { _ ->
                    (if (isLeftButton) leftFlash else rightFlash).set { !it }
                    delay(125)
                }
                changeEmoteWheelOrOutfit(state, if (isLeftButton) -1 else 1)
                resetDragAnimation()
            }
        }
    }
}

fun LayoutScope.previewWindow(state: WardrobeState, modifier: Modifier, bottomDivider: UIComponent) {

    val purchaseBannerHeight = 45f
    val purchaseBannerWidth = 200f
    val bundleListWidth = 28f
    val bundleListPadding = 10f
    val playerModelWidth = 87f
    val playerModelHeight = 167f

    val maxPreviewWidth = purchaseBannerWidth + ((bundleListWidth + bundleListPadding) * 2f)
    val previewTooShort = mutableStateOf(false)
    val previewTooNarrow = mutableStateOf(false)
    val purchaseBannerLocked = previewTooShort or previewTooNarrow

    val colorOptions = state.editingCosmetic.map { it?.cosmetic?.property<CosmeticProperty.Variants>()?.data?.variants }
    val sides = state.editingCosmetic.map { editing ->
        editing?.let { item ->
            state.modelLoader.getModel(item.cosmetic, item.cosmetic.defaultVariantName, AssetLoader.Priority.Blocking)
                .join().sideOptions.takeUnless { it.isEmpty() }
        }
    }

    val previewing = memo {
        when {
            state.editingCosmetic() != null -> false
            state.selectedBundle() != null -> state.selectedBundle()?.id !in state.unlockedBundles()
            state.inEmoteWheel() -> false
            state.selectedEmote() != null -> true
            else -> state.equippedCosmeticsPurchasable().isNotEmpty() || false
        }
    }

    box(modifier) {
        val previewWindowContainer = containerDontUseThisUnlessYouReallyHaveTo
        val purchaseBannerPosition = box(Modifier.height(playerModelHeight).then(BasicYModifier { CenterConstraint() - 1.5.percent }))
        val previewContainer = box(BasicYModifier { CenterConstraint() - 1.5.percent }.whenTrue(state.inEmoteWheel, BasicYModifier { CenterConstraint() - 10.pixels })) {
            if_(!state.inEmoteWheel) {
                playerPreview(state, Modifier.width(playerModelWidth).height(playerModelHeight), previewWindowContainer)
            } `else` {
                EmoteWheelPage(state)(Modifier.childBasedSize())
            }
        }
        if_(!state.inEmoteWheel) {
            ifNotNull(state.editingCosmetic) { editing ->
                column(BasicYModifier { SiblingConstraint(10f) boundTo previewContainer }, Arrangement.spacedBy(10f)) {
                    ifNotNull(colorOptions) { colors ->
                        if (colors.isNotEmpty()) {
                            colorOptions(state, editing, colors)
                        }
                    }
                    ifNotNull(sides) { sides ->
                        if (sides.isNotEmpty()) {
                            sideOptions(state, editing, sides.toList().sorted())
                        }
                    }
                }
                editing.heightRange().let { range ->
                    if (range.first != null || range.second != null || range.third != null) {
                        heightSlider(state, editing, range, previewContainer)
                    }
                }
            }

            val (cosmetics, settings) = memo {
                val selectedBundle = state.selectedBundle()
                val selectedEmote = state.selectedEmote()
                val equippedOutfit = state.equippedOutfitItem()
                val previewingSetting = state.previewingSetting()

                val cosmetics = selectedBundle?.cosmetics
                    ?: selectedEmote?.cosmetic?.let { mapOf(CosmeticSlot.EMOTE to it.id) }
                    ?: equippedOutfit?.cosmetics
                    ?: emptyMap()
                val settings = selectedBundle?.settings
                    ?: ((equippedOutfit?.settings ?: emptyMap()) + previewingSetting.mapValues { listOf(it.value) })

                cosmetics.entries
                    .sortedBy { WardrobeCategory.slotOrder.indexOf(it.key) }
                    .map { it.value } to settings.mapValues { it.value.filterIsInstance<CosmeticSetting.Variant>() }
            }

            if_(cosmetics.map { it.isNotEmpty() }) {
                column(Modifier.fillHeight(padding = bundleListPadding).alignHorizontal(Alignment.End(bundleListPadding))) {
                    scrollable(Modifier.fillRemainingHeight(), vertical = true) {
                        sidebarItems(state, cosmetics.toListState(), settings, bundleListWidth, state.selectedBundle.map { it != null })
                    }.apply { scrollGradient(13.pixels) }
                    if_(purchaseBannerLocked) {
                        spacer(height = purchaseBannerHeight)
                    }
                }
            }
        }
        if_(previewing) {
            val purchaseBannerModifier = BasicYModifier { (SiblingConstraint(38f) boundTo purchaseBannerPosition).coerceAtMost(0.pixels(true) boundTo bottomDivider) }
                .whenTrue(purchaseBannerLocked, Modifier.fillWidth()).whenTrue(previewTooNarrow, Modifier.alignVertical(Alignment.End))
            box(purchaseBannerModifier.color(EssentialPalette.COMPONENT_BACKGROUND)) {
                purchaseBannerContent(state, Modifier.width(purchaseBannerWidth).height(purchaseBannerHeight))
            }.apply {
                layoutSafePollingState(false) { bottomDivider.getTop() <= getBottom() }.onSetValue(this) {
                    // The banner's only at the bottom when the preview's too short, but always at the bottom when the preview's too narrow
                    // which would cause previewTooShort to always be true, so we need to check for that
                    if (!previewTooNarrow.get()) previewTooShort.set(it)
                }
            }
        }
    }.onLeftClick {
        if (!state.inEmoteWheel.getUntracked()) {
            findChildOfTypeOrNull<UIPlayer>(true)?.mouseClick(it.absoluteX.toDouble(), it.absoluteY.toDouble(), it.mouseButton)
        }
    }.apply {
        state.inEmoteWheel.onChange(this) {
            state.editingCosmetic.set(null)
        }
        layoutSafePollingState(false) { getWidth() <= maxPreviewWidth }
            .onSetValue(this) { previewTooNarrow.set(it) }
    }
}

private fun LayoutScope.playerPreview(state: WardrobeState, modifier: Modifier, previewWindowContainer: UIComponent) {
    var instance: UIPlayer? = null
    if_(state.screenOpen, cache = false) {
        instance = playerPreviewInner(state, modifier, previewWindowContainer)
    } `else` {
        instance?.close()
        instance = null
    }
}

private fun LayoutScope.playerPreviewInner(state: WardrobeState, modifier: Modifier, previewWindowContainer: UIComponent): UIPlayer {
    val profile = state.selectedBundle.map { bundle ->
        bundle?.skin?.let { Pair(it.toMod(), null) }
    }

    val cosmeticsSource = mutableStateOf(stateOf(emptyMap<CosmeticSlot, EquippedCosmetic>()))

    val player = platform.newUIPlayer(
        camera = stateOf(null),
        profile = profile,
        cosmetics = cosmeticsSource.flatten(),
        sounds = EssentialConfig.playEmoteSoundsInWardrobe.map { if (it) 1f else 0f },
    ).apply {
        val settings = memo {
            state.selectedBundle()?.settings ?: state.selectedPreviewingEquippedSettings()
        }
        val emoteScheduler = EmoteScheduler(
            this,
            memo { state.selectedEmote()?.cosmetic },
            memo { state.selectedEmote()?.let { settings()[it.id] } },
        )
        cosmeticsSource.set(
            memo {
                val cosmeticIds = (state.selectedEmote()?.let { emote ->
                    if (emoteScheduler.emoteEquipped()) mapOf(CosmeticSlot.EMOTE to emote.id) else emptyMap()
                } ?: state.selectedBundle()?.cosmetics ?: state.equippedCosmeticsState()).toMutableMap()

                if (state.purchaseAnimationState()) {
                    cosmeticIds[CosmeticSlot.EMOTE] = state.purchaseConfirmationEmoteId
                }

                with(state) { resolveCosmeticIds(cosmeticIds, settings()) }
            }
        )

        val dragging = mutableStateOf(false)
        val hovered = stateDelegatingTo(stateOf<Cosmetic?>(null))

        val outlineCosmetic = stateBy {
            listOfNotNull(state.editingCosmetic()?.cosmetic, hovered().takeIf { !dragging() })
        }

        val cosmeticHoverEffect = CosmeticHoverOutlineEffect(EssentialPalette.GUI_BACKGROUND, outlineCosmetic)
        bindEffect(cosmeticHoverEffect, state.selectedBundle.map { it == null })

        hovered.rebind(cosmeticHoverEffect.hoveredCosmetic)

        val onLeftClick = onLeftClick@{
            if (state.selectedBundle.get() != null) {
                return@onLeftClick
            }

            val editingCosmeticId = state.editingCosmetic.get()?.cosmetic?.id
            state.editingCosmetic.set(null)

            val hoveredCosmetic = cosmeticHoverEffect.hoveredCosmetic.get() ?: return@onLeftClick

            USound.playButtonPress()
            if (UKeyboard.isShiftKeyDown()) {
                state.outfitManager.updateEquippedCosmetic(
                    state.outfitManager.selectedOutfitId.getUntracked() ?: return@onLeftClick,
                    hoveredCosmetic.type.slot,
                    if (hoveredCosmetic.type.slot == CosmeticSlot.CAPE) CAPE_DISABLED_COSMETIC_ID else null,
                )
                return@onLeftClick
            }

            val editable = hoveredCosmetic.property<CosmeticProperty.Variants>() != null
                || hoveredCosmetic.property<CosmeticProperty.PositionRange>() != null
                || state.modelLoader.getModel(
                hoveredCosmetic,
                hoveredCosmetic.defaultVariantName,
                AssetLoader.Priority.Blocking,
            ).join().isContainsSideOption
            val item = Item.CosmeticOrEmote(hoveredCosmetic)

            if (hoveredCosmetic.id != editingCosmeticId) {
                if (editable) {
                    state.editingCosmetic.set(item)
                }
                state.highlightItem.set(item.itemId)
            }
        }

        var potentialClick: UIClickEvent? = null
        onLeftClick { click ->
            click.stopPropagation()
            potentialClick = click
        }
        onMouseDrag { x, y, _ ->
            val click = potentialClick ?: return@onMouseDrag
            if (abs(click.relativeX - x) > 1 || abs(click.relativeY - y) > 1) {
                potentialClick = null
                dragging.set(true)
            }
        }
        onMouseRelease {
            dragging.set(false)
            if (potentialClick != null) {
                // Need to delay because the component tree may not be modified from `onMouseRelease`
                Window.enqueueRenderOperation {
                    // Need to reset potentialClick so onMouseRelease fired via other clicks doesn't run onLeftClick()
                    potentialClick = null
                    onLeftClick()
                }
            }
        }
    }(modifier)

    return player
}

private fun LayoutScope.purchaseBannerContent(state: WardrobeState, modifier: Modifier): UIComponent {
    return purchaseBannerContentOld(state, modifier)
}

private fun LayoutScope.purchaseBannerContentOld(state: WardrobeState, modifier: Modifier): UIComponent {

    fun purchaseCallback(success: Boolean) {
        if (success) {
            if (state.selectedBundle.getUntracked() != null || state.selectedEmote.getUntracked() != null) {
                state.selectedItem.set(null)
            }
            state.triggerPurchaseAnimation()
        } else {
            Notifications.push("Purchase failed", "Please try again later or contact our support.") {
                type = NotificationType.ERROR
                withCustomComponent(Slot.ICON, EssentialPalette.REPORT_10X7.create())
            }
        }
    }

    val cost = state.currentCategoryTotalCost
    val buttonModifier = Modifier.width(94f).height(23f).color(EssentialPalette.BLUE_BUTTON).shadow(EssentialPalette.BLACK)

    return row(modifier) {
        spacer(width = 4f)
        box(Modifier.fillRemainingWidth()) {
            row(Arrangement.spacedBy(5f)) {
                text(
                    cost.map { if (it == 0) "FREE" else CoinsManager.COIN_FORMAT.format(it) },
                    Modifier.color(EssentialPalette.TEXT).shadow(EssentialPalette.BLACK),
                    centeringContainsShadow = false
                )

                if_({ cost() > 0 }) {
                    icon(EssentialPalette.COIN_7X)
                }
            }
        }
        box(buttonModifier.hoverColor(EssentialPalette.BLUE_BUTTON_HOVER).hoverScope()) {
            text(
                cost.map { if (it == 0) "Claim" else "Purchase" },
                Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(EssentialPalette.TEXT_SHADOW),
                centeringContainsShadow = false
            )
        }.onLeftClick { click ->
            handleClick(click) {
                if (platform.disabledFeaturesManager.isFeatureDisabled(Feature.COSMETIC_PURCHASE)) {
                    platform.pushModal { StoreDisabledModal(it) }
                    return@handleClick
                }

                val bundle = state.selectedBundle.getUntracked()
                val emote = state.selectedEmote.getUntracked()
                if (cost.getUntracked() == 0) {
                    if (bundle != null) {
                        // If the user already owns all the items in a bundle, we can claim it without having to
                        // ask first.
                        state.purchaseSelectedBundle { purchaseCallback(it) }
                        return@handleClick
                    }
                    if (emote != null) {
                        state.purchaseSelectedEmote { successful ->
                            if (successful) {
                                state.inEmoteWheel.set(true)
                                state.highlightItem.set(emote.itemId)
                            }
                            purchaseCallback(successful)
                        }
                        return@handleClick
                    }
                }

                if (state.coins.get() < cost.get()) {
                    CoinsPurchaseModal.open(state, cost.getUntracked())
                } else {
                    platform.pushModal { manager ->
                        val purchaseAction: () -> Unit = {
                            when {
                                bundle != null -> state.purchaseSelectedBundle { purchaseCallback(it) }
                                emote != null -> state.purchaseSelectedEmote { successful ->
                                    if (successful) {
                                        state.inEmoteWheel.set(true)
                                        state.highlightItem.set(emote.itemId)
                                    }
                                    purchaseCallback(successful)
                                }
                                else -> state.purchaseEquippedCosmetics { purchaseCallback(it) }
                            }
                        }
                        when {
                            bundle != null -> PurchaseConfirmationModal.forBundle(manager, bundle, state, purchaseAction)
                            emote != null -> PurchaseConfirmationModal.forItem(manager, emote, state, purchaseAction)
                            else -> PurchaseConfirmationModal.forEquippedItemsPurchasable(manager, state, purchaseAction)
                        }
                    }
                }
            }
        }
        spacer(width = 22f)
    }
}

private fun LayoutScope.colorOptions(state: WardrobeState, item: Item.CosmeticOrEmote, variants: List<CosmeticProperty.Variants.Variant>) {
    val selectedVariant = state.getVariant(item).map { it ?: variants[0].name }
    row {
        variants.forEachIndexed { index, variant ->
            val selected = selectedVariant.map { it == variant.name }
            val outlineColor = if (variant.color != Color.WHITE) EssentialPalette.WHITE else EssentialPalette.GRAY_BUTTON_HOVER_OUTLINE
            row(Modifier.hoverScope()) {
                if (index > 0) {
                    spacer(width = 1f)
                }
                box(Modifier.width(17f).heightAspect(1f).shadow(EssentialPalette.BLACK).color(EssentialPalette.GRAY_BUTTON).hoverColor(EssentialPalette.GRAY_BUTTON_HOVER)) {
                    box(Modifier.width(9f).heightAspect(1f).shadow(EssentialPalette.TEXT_SHADOW).color(variant.color.toJavaColor()).whenTrue(selected, Modifier.outline(outlineColor, 1f)))
                }.onLeftClick { handleClick(it) { state.setVariant(item, variant.name) } }
                if (index < variants.size - 1) {
                    spacer(width = 2f)
                }
            }.hoverScope().onSetValue { handleVariantHover(variant, item, state, it) }
        }
    }
}

private fun LayoutScope.sideOptions(state: WardrobeState, item: Item.CosmeticOrEmote, sides: List<Side>) {
    val selectedSide = state.getSelectedSide(item).map { it ?: item.cosmetic.defaultSide ?: sides[0] }
    val anyHovered = mutableStateOf(false)
    row(Modifier.width(86f).height(17f).hoverScope()) {
        sides.forEach { side ->
            val color = stateBy { if (!anyHovered() && selectedSide() == side) EssentialPalette.GRAY_BUTTON_HOVER else EssentialPalette.GRAY_BUTTON }
            column(
                Modifier.fillWidth(0.5f).fillHeight().shadow(EssentialPalette.BLACK).whenHovered(Modifier.color(EssentialPalette.GRAY_BUTTON_HOVER), Modifier.color(color)).hoverScope(),
                Arrangement.spacedBy(0f, FloatPosition.CENTER)
            ) {
                spacer(height = 1f) // Extra pixel for text shadow
                text(side.displayName, Modifier.shadow(EssentialPalette.TEXT_SHADOW))
            }.onLeftClick { handleClick(it) { state.setSelectedSide(item, side) } }
        }
    }.hoverScope().onSetValue { anyHovered.set(it) }
}

private fun LayoutScope.heightSlider(state: WardrobeState, item: Item.CosmeticOrEmote, range: Triple<IntRange?, IntRange?, IntRange?>, playerModel: UIComponent) {
    val itemPos = state.getSelectedPosition(item)
    val selectedHeight = when {
        range.first != null -> itemPos.get()?.first?.toInt() ?: 0
        range.second != null -> itemPos.get()?.second?.toInt() ?: 0
        else -> itemPos.get()?.third?.toInt() ?: 0
    }
    val heightRange = (range.first ?: range.second ?: range.third)?.reversed() ?: return
    val numNotches = heightRange.count()
    val numSections = numNotches - 1

    val endHeight = 9f
    val middleHeight = 16f
    val barHeight = (endHeight * 2) + ((numNotches - 2) * middleHeight)
    val hitboxPadding = 2f
    val hitboxWidth = 12f
    val notchSize = 2f

    val fraction = mutableStateOf(heightRange.indexOf(selectedHeight) / numSections.toFloat())
    val height = fraction.map { heightRange.elementAt((it * numSections).toInt()) }
    val mouseHeld = BasicState(false)
    val barColor = Modifier.shadow(EssentialPalette.BLACK).color(EssentialPalette.SCROLLBAR).hoverColor(EssentialPalette.TEXT_DISABLED)
    val sliderPosSize = BasicYModifier { basicYConstraint { it.parent.getTop() + fraction.get() * (it.parent.getHeight() - it.getHeight()) } }.width(8f).height(4f)
    val sliderTooltip = Modifier.hoverTooltip(height.map { "$it" }, position = EssentialTooltip.Position.RIGHT)
    val position = BasicXModifier { SiblingConstraint(10f) boundTo playerModel }.then(BasicYModifier { (0.percent boundTo playerModel) - 10.pixels })

    fun updateSliderFraction(mouseY: Float) {
        // Mouse Y as a fraction of barHeight rounded to closest notch position
        fraction.set(round(((mouseY - hitboxPadding) / barHeight).coerceIn(0f..1f) * numSections) / numSections)
    }

    box(position.childBasedMaxSize().hoverScope()) {
        column {
            box(Modifier.width(hitboxWidth).height(endHeight + hitboxPadding)) {
                box(Modifier.alignHorizontal(Alignment.Start(hitboxPadding)).alignVertical(Alignment.Start(hitboxPadding)).width(notchSize).heightAspect(1f).then(barColor))
            }
            repeat(numNotches - 2) {
                box(Modifier.width(hitboxWidth).height(middleHeight)) {
                    box(Modifier.alignHorizontal(Alignment.Start(hitboxPadding)).alignVertical(Alignment.Center).width(notchSize).heightAspect(1f).then(barColor))
                }
            }
            box(Modifier.width(hitboxWidth).height(endHeight + hitboxPadding)) {
                box(Modifier.alignHorizontal(Alignment.Start(hitboxPadding)).alignVertical(Alignment.End(hitboxPadding)).width(notchSize).heightAspect(1f).then(barColor))
            }
        }
        box(Modifier.alignHorizontal(Alignment.Center).alignVertical(Alignment.Start(hitboxPadding)).width(4f).height(barHeight).then(barColor))
        box(sliderPosSize.shadow(EssentialPalette.BLACK).color(EssentialPalette.OTHER_BUTTON_ACTIVE).hoverColor(EssentialPalette.GRAY_BUTTON_HOVER_OUTLINE).then(sliderTooltip))
    }.onLeftClick {
        handleClick(it) {
            mouseHeld.set(true)
            updateSliderFraction(it.relativeY)
        }
    }.onMouseRelease {
        mouseHeld.set(false)
    }.apply {
        onMouseDrag { _, mouseY, _ ->
            if (mouseHeld.get()) {
                updateSliderFraction(mouseY)
            }
        }
        height.onSetValue(this) {
            state.setSelectedPosition(
                item,
                when {
                    range.first != null -> Triple(it.toFloat(), itemPos.get()?.second ?: 0f, itemPos.get()?.third ?: 0f)
                    range.second != null -> Triple(itemPos.get()?.first ?: 0f, it.toFloat(), itemPos.get()?.third ?: 0f)
                    else -> Triple(itemPos.get()?.first ?: 0f, itemPos.get()?.second ?: 0f, it.toFloat())
                },
            )
        }
    }
}

private fun LayoutScope.sidebarItems(
    state: WardrobeState,
    cosmetics: ListState<CosmeticId>,
    settings: State<Map<CosmeticId, List<CosmeticSetting>>>,
    width: Float,
    isBundle: State<Boolean>
) {
    sidebarItemsOld(state, cosmetics, settings, width, isBundle)
}

private fun LayoutScope.sidebarItemsOld(
    state: WardrobeState,
    cosmetics: ListState<CosmeticId>,
    settings: State<Map<CosmeticId, List<CosmeticSetting>>>,
    width: Float,
    isBundle: State<Boolean>
) {
    val resolvedCosmetics = memo {
        val allCosmetics = state.rawCosmetics()
        cosmetics().mapNotNull { id -> allCosmetics.find { it.id == id } }
    }.toListState()

    column(Modifier.childBasedMaxWidth(1f).childBasedHeight(1f), Arrangement.spacedBy(4f, FloatPosition.CENTER)) {
        val previewModifier = Modifier.width(width - 2f).heightAspect(1f).color(EssentialPalette.GRAY_BUTTON).shadow(EssentialPalette.BLACK)
        forEach(resolvedCosmetics) { cosmetic ->
            val unlocked = state.unlockedCosmetics.map { it.contains(cosmetic.id) }
            val visible = isBundle or !unlocked

            if_(visible) {
                box(previewModifier.hoverTooltip(cosmetic.displayName, position = EssentialTooltip.Position.LEFT).hoverScope()) {
                    sidebarCosmeticPreview(cosmetic, settings.map { it[cosmetic.id] ?: emptyList() })

                    val iconModifier = Modifier.alignHorizontal(Alignment.End(1f)).alignVertical(Alignment.End(1f))
                    if_(unlocked) {
                        icon(EssentialPalette.CHECKMARK_7X5, iconModifier.color(EssentialPalette.GREEN))
                    } `else` {
                        if (cosmetic.requiresUnlockAction()) {
                            icon(EssentialPalette.LOCK_HOLLOW_7X9, iconModifier.color(EssentialPalette.LOCKED_ICON))
                        } else {
                            icon(EssentialPalette.SHOPPING_CART_8X7, iconModifier.color(EssentialPalette.CART_ACTIVE))
                        }
                    }
                }
            }
        }
    }
}

private fun LayoutScope.sidebarCosmeticPreview(cosmetic: Cosmetic, settings: State<List<CosmeticSetting>>) {
    CosmeticPreview(cosmetic, settings)(Modifier.fillParent(padding = 1f))
}

private fun Item.CosmeticOrEmote.heightRange(): Triple<IntRange?, IntRange?, IntRange?> {
    val data = this.cosmetic.property<CosmeticProperty.PositionRange>()?.data
    return Triple(
        data?.xMax?.let { data.xMin?.toInt()?.rangeTo(it.toInt()) },
        data?.yMax?.let { data.yMin?.toInt()?.rangeTo(it.toInt()) },
        data?.zMax?.let { data.zMin?.toInt()?.rangeTo(it.toInt()) },
    )
}

private fun handleClick(clickEvent: UIClickEvent, action: () -> Unit) {
    clickEvent.stopPropagation()
    USound.playButtonPress()
    action()
}
