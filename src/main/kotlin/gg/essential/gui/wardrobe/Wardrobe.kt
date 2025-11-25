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

import gg.essential.Essential
import gg.essential.api.gui.GuiRequiresTOS
import gg.essential.config.EssentialConfig
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.EssentialPalette
import gg.essential.gui.InternalEssentialGUI
import gg.essential.gui.common.ContextOptionMenu
import gg.essential.gui.common.EssentialCollapsibleSearchbar
import gg.essential.gui.common.EssentialDropDown
import gg.essential.gui.common.OutlineButtonStyle
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.common.sendCosmeticsDisabledNotification
import gg.essential.gui.common.sendCosmeticsHiddenNotification
import gg.essential.gui.common.sendEmotesDisabledNotification
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.util.pollingStateV2
import gg.essential.gui.wardrobe.components.*
import gg.essential.gui.wardrobe.configuration.*
import gg.essential.gui.wardrobe.configuration.DiagnosticsMenu
import gg.essential.gui.wardrobe.configuration.FeaturedPageCollectionConfiguration
import gg.essential.gui.wardrobe.configuration.ImplicitOwnershipConfiguration
import gg.essential.gui.wardrobe.modals.SkinModal
import gg.essential.network.connectionmanager.telemetry.TelemetryManager
import gg.essential.universal.UKeyboard
import gg.essential.universal.UResolution
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import kotlin.math.min

class Wardrobe(
    initialCategory: WardrobeCategory? = null,
    initialEmoteWheel: Boolean = false,
) : InternalEssentialGUI(
    ElementaVersion.V6,
    "Wardrobe",
    discordActivityDescription = "Customizing their character",
), GuiRequiresTOS {

    val connectionManager = Essential.getInstance().connectionManager
    val cosmeticsManager = connectionManager.cosmeticsManager
    val skinsManager = connectionManager.skinsManager
    val outfitManager = connectionManager.outfitManager
    val emoteWheelManager = connectionManager.emoteWheelManager
    val coinsManager = connectionManager.coinsManager

    private val guiScaleState = mutableStateOf(newGuiScale)

    val state = WardrobeState(
        initialCategory,
        screenOpen,
        window,
        cosmeticsManager,
        cosmeticsManager.modelLoader,
        cosmeticsManager.wardrobeSettings,
        outfitManager,
        skinsManager,
        emoteWheelManager,
        coinsManager,
        connectionManager.saleNoticeManager.saleState.map {
            it.filter { it.discountPercent > 0 } // Sales with 0% discount are used to display on the main menu and should be ignored here
        }.toListState(),
        connectionManager.cosmeticNotices,
        connectionManager.noticeBannerManager,
        guiScaleState,
    )
    init {
        state.inEmoteWheel.set(initialEmoteWheel)

        var previousColumnCount = -1
        effect(window) {
            val columnCount = state.featuredPageLayout().second?.key ?: return@effect
            if (previousColumnCount != columnCount) {
                connectionManager.telemetryManager.enqueue(ClientTelemetryPacket("WARDROBE_LAYOUT", mapOf("columnCount" to columnCount)))
                previousColumnCount = columnCount
            }
        }
    }

    private val searchbar = EssentialCollapsibleSearchbar(activateOnType = false).apply {
        textContent.onSetValueAndNow { state.search.set(it) }
    }

    private var previousCategory: WardrobeCategory? = null

    private val configurationMenu by lazy { ConfigurationMenu(state) }
    private val cosmeticConfiguration by lazy { CosmeticConfiguration(state) }
    private val cosmeticCategoryConfiguration by lazy { CosmeticCategoryConfiguration(state) }
    private val cosmeticTypeConfiguration by lazy { CosmeticTypeConfiguration(state) }
    private val cosmeticBundleConfiguration by lazy { CosmeticBundleConfiguration(state) }
    private val featuredPageCollectionConfiguration by lazy { FeaturedPageCollectionConfiguration(state) }
    private val implicitOwnershipConfiguration by lazy { ImplicitOwnershipConfiguration(state) }

    private val currentConfigurationComponent = stateBy {
        val diagnosticsFor = state.showingDiagnosticsFor()
        if (diagnosticsFor != null) {
            return@stateBy DiagnosticsMenu(state, diagnosticsFor)
        }

        if (state.cosmeticsManager.cosmeticsDataWithChanges == null) {
            return@stateBy null
        }
        when {
            state.currentlyEditingCosmetic() != null -> cosmeticConfiguration
            state.currentlyEditingCosmeticBundle() != null -> cosmeticBundleConfiguration
            state.currentlyEditingCosmeticType() != null -> cosmeticTypeConfiguration
            state.currentlyEditingCosmeticCategory() != null -> cosmeticCategoryConfiguration
            state.currentlyEditingFeaturedPageCollection() != null -> featuredPageCollectionConfiguration
            state.currentlyEditingImplicitOwnership() != null -> implicitOwnershipConfiguration
            state.editingMenuOpen() -> configurationMenu
            else -> null
        }
    }

    private val mainContainerWidthState = stateDelegatingTo(stateOf(0f))

    private val collapseTopBarButtonsWidth = 350f

    init {
        lateinit var mainContainer: UIComponent
        val sidebarScroller: ScrollComponent
        val mainContent: WardrobeContainer

        effect(content) {
            val category = state.currentCategory()

            // Clear and collapse search bar when switching between parent categories
            // & Deselect current item when changing categories
            if (category.superCategory == category && previousCategory?.superCategory != category) {
                searchbar.textContentV2.set("")
                searchbar.collapse()

                state.selectedItem.set(null)
            }

            previousCategory = category
        }

        content.layout {
            val divider = Modifier.fillHeight().width(outlineThickness).color(EssentialPalette.COMPONENT_BACKGROUND)

            row(Modifier.fillParent()) {
                sidebarScroller = wardrobeSidebar(state, Modifier.width(sidebarWidth).fillHeight())

                box(divider) {
                    scrollbar(sidebarScroller)
                }

                mainContainer = column(Modifier.fillHeight().then(BasicWidthModifier(::mainWidthConstraint))) {
                    mainContent = WardrobeContainer(state)(Modifier.fillWidth().fillRemainingHeight())
                }

                box(divider) {
                    scrollbar(mainContent.scroller)
                }

                column(Modifier.fillHeight().fillRemainingWidth()) {
                    bind(currentConfigurationComponent) { configurationComponent ->
                        if (configurationComponent != null) {
                            configurationComponent(Modifier.fillWidth().fillRemainingHeight())
                        } else {
                            previewWindow(state, Modifier.fillRemainingWidth().fillHeight().effect { ScissorEffect() }, bottomDivider)
                        }
                    }
                }
            }
        }

        titleBar.layout {
            val divider = Modifier.fillHeight().width(outlineThickness).color(EssentialPalette.COMPONENT_HIGHLIGHT)

            row(Modifier.fillParent()) {
                spacer(width = sidebarWidth)

                box(divider)

                box(Modifier.width(mainContainer)) {
                    row(Modifier.fillWidth(padding = 10f), Arrangement.SpaceBetween) {
                        row(Arrangement.spacedBy(5f)) {
                            text(state.currentCategory.map { it.superCategory.fullName }, centeringContainsShadow = false)
                            if_(state.currentCategory.map { it is WardrobeCategory.Outfits }) {
                                text(
                                    stateBy {
                                        val max = cosmeticsManager.wardrobeSettings.outfitsLimit()
                                        val curr = outfitManager.outfits().size
                                        "[$curr/$max]"
                                    },
                                    Modifier.color(EssentialPalette.TEXT_MID_GRAY)
                                        .shadow(EssentialPalette.TEXT_SHADOW_LIGHT),
                                    centeringContainsShadow = false
                                )
                            }
                            if_(state.currentCategory.map { it is WardrobeCategory.Skins }) {
                                text(
                                    stateBy {
                                        val max = cosmeticsManager.wardrobeSettings.skinsLimit()
                                        val curr = skinsManager.skins().size
                                        "[$curr/$max]"
                                    },
                                    Modifier.color(EssentialPalette.TEXT_MID_GRAY)
                                        .shadow(EssentialPalette.TEXT_SHADOW_LIGHT),
                                    centeringContainsShadow = false
                                )
                            }
                        }
                        row(Modifier.height(17f), Arrangement.spacedBy(6f)) {
                            row(Modifier.fillHeight(), Arrangement.spacedBy(3f)) {
                                if_(state.currentCategory.map { it.superCategory is WardrobeCategory.ParentCategory }) {
                                    filterSortDropDown(Modifier.fillHeight())
                                }
                                if_(state.currentCategory.map { !(it is WardrobeCategory.FeaturedRefresh) }) {
                                    searchbar()
                                }
                            }
                            if_(state.currentCategory.map { it is WardrobeCategory.Skins }) {
                                addSkinButton()
                            } `else` {
                                coinsButton(state)
                            }
                        }
                    }
                }

                box(divider)

                previewWindowTitleBar(state, Modifier.fillRemainingWidth().fillHeight())
            }
        }

        if (EssentialConfig.disableEmotes) {
            sendEmotesDisabledNotification()
        }
        if (EssentialConfig.disableCosmetics) {
            sendCosmeticsDisabledNotification()
        }
        if (EssentialConfig.ownCosmeticsHidden) {
            sendCosmeticsHiddenNotification()
        }

        mainContainerWidthState.rebind(window.pollingStateV2 { mainContainer.getWidth() })

        state.currentCategory.map { it.superCategory }.onSetValueAndNow(content) { category ->
            when (category) {
                is WardrobeCategory.Emotes -> {
                    state.inEmoteWheel.set(true)
                }
                else -> {
                    state.inEmoteWheel.set(false)
                }
            }
        }

        state.highlightItem.onSetValue(mainContainer) {
            if (it == null) return@onSetValue

            searchbar.textContentV2.set("")
            searchbar.collapse()
        }

        // Clear the default key handler and override so that hitting escape doesn't unconditionally close the UI
        window.keyTypedListeners.removeFirst()
        window.onKeyType { typedChar, keyCode ->
            when {
                keyCode == UKeyboard.KEY_ESCAPE && hasUnownedItems() -> displayCartWarningModal()
                //#if MC>=11602
                //$$ keyCode == UKeyboard.KEY_ESCAPE -> restorePreviousScreen()
                //#endif
                Essential.getInstance().keybindingRegistry.toggleCosmetics.isKeyCode(keyCode)
                        && !EssentialConfig.disableCosmetics -> {
                    EssentialConfig.ownCosmeticsVisibleStateWithSource.set { !it.first to EssentialConfig.CosmeticsVisibilitySource.UserWithNotification }
                }
                else -> {
                    defaultKeyBehavior(typedChar, keyCode)
                }
            }
        }
    }

    private fun LayoutScope.scrollbar(scroller: ScrollComponent) {
        val bar: UIComponent

        box(Modifier.fillWidth().height(scroller).alignVertical(Alignment.Start)) {
            bar = box(Modifier.fillWidth().color(EssentialPalette.SCROLLBAR))
        }

        scroller.setVerticalScrollBarComponent(bar, true)
    }

    private fun LayoutScope.filterSortDropDown(modifier: Modifier = Modifier) {
        val dropDown: UIComponent

        dropDown = EssentialDropDown(
            state.filterSort.get(),
            mutableListStateOf(*WardrobeState.FilterSort.values().map { EssentialDropDown.Option(it.displayName, it) }.toTypedArray()),
            compact = mainContainerWidthState.map { it < collapseTopBarButtonsWidth },
        )
        dropDown.selectedOption.onSetValue(dropDown) { state.filterSort.set(it.value) }

        dropDown(Modifier.fillHeight().then(modifier))
    }

    private fun LayoutScope.addSkinButton(modifier: Modifier = Modifier) {
        val limitState = stateBy {
            state.skinItems().size >= state.settings.skinsLimit()
        }
        val buttonModifier = Modifier.width(69f).height(17f).shadow().hoverScope()
            .whenTrue(
                limitState,
                Modifier.color(EssentialPalette.BLUE_BUTTON_DISABLED).hoverTooltip("Skin limit reached"),
                Modifier.color(EssentialPalette.COINS_BLUE).hoverColor(EssentialPalette.COINS_BLUE_HOVER)
            ) then modifier
        box(buttonModifier) {
            text("+ Add Skin", Modifier.alignVertical(Alignment.Center(true)).shadow(EssentialPalette.TEXT_SHADOW))
        }.onLeftClick {
            if (limitState.get()) return@onLeftClick
            USound.playButtonPress()
            ContextOptionMenu.create(
                ContextOptionMenu.Position(this, false),
                Window.of(this),
                ContextOptionMenu.Option("Select File", EssentialPalette.UPLOAD_9X) {
                    GuiUtil.pushModal { SkinModal.upload(it, state) }
                },
                ContextOptionMenu.Option("By URL", EssentialPalette.LINK_10X7) {
                    GuiUtil.pushModal { SkinModal.fromURL(it, state) }
                },
                ContextOptionMenu.Option("Username", EssentialPalette.EMOTES_7X) {
                    GuiUtil.pushModal { SkinModal.steal(it, state) }
                }
            )
        }
    }

    override fun onScreenClose() {
        Multithreading.runAsync {
            cosmeticsManager.capeManager.flushCapeUpdates()
        }
        Multithreading.runAsync {
            Essential.getInstance().skinManager.flushChanges(false)
        }
        outfitManager.flushSelectedOutfit(false)
        emoteWheelManager.flushSelectedEmoteWheel(false)

        val outfit = outfitManager.getSelectedOutfit()
        if (outfit != null) {
            val skin = connectionManager.skinsManager.getSkin(outfit.skinId ?: "").get()
            if (skin != null) {
                connectionManager.skinsManager.updateLastUsedAtState(skin.id)
            }
        }

        outfitManager.cleanUpUnusedSettingsOnOutfits()

        Essential.getInstance().connectionManager.noticesManager.flushDismissNotices()

        super.onScreenClose()
    }

    private fun displayCartWarningModal() {
        GuiUtil.pushModal { manager ->
            val modal = UnownedItemsEquippedModal(manager)

            modal.onPrimaryAction {
                restorePreviousScreen()
            }

            connectionManager.telemetryManager.clientActionPerformed(TelemetryManager.Actions.CART_NOT_EMPTY_WARNING)

            modal
        }
    }

    class UnownedItemsEquippedModal(manager: ModalManager) : ConfirmDenyModal(manager, false) {
        init {
            configure {
                titleText = "Unowned items equipped..."
                titleTextColor = EssentialPalette.MODAL_WARNING
                contentText = "Unowned items will\nnot be visible in-game."
                contentTextColor = EssentialPalette.TEXT
                primaryButtonText = "Okay!"
                primaryButtonStyle = OutlineButtonStyle.GRAY.defaultStyle
                primaryButtonHoverStyle = OutlineButtonStyle.GRAY.hoveredStyle
                cancelButtonText = "Back"
            }
        }
    }

    private fun hasUnownedItems(): Boolean {
        return state.equippedCosmeticsState.getUntracked().any { it.value !in state.unlockedCosmetics.getUntracked() }
    }

    override fun backButtonPressed() {
        if (hasUnownedItems()) {
            displayCartWarningModal()
        } else {
            super.backButtonPressed()
        }
    }

    private fun mainWidthConstraint(): WidthConstraint {
        return basicWidthConstraint {
            val columnWidth = cosmeticWidth + cosmeticXSpacing
            val columnCount = state.getColumnCount(state.currentCategory.getUntracked())
            return@basicWidthConstraint cosmeticXSpacing + columnWidth * columnCount.getUntracked().coerceAtLeast(1)
        }
    }

    override fun updateGuiScale() {
        newGuiScale = getWardrobeGuiScale()
        guiScaleState.set(newGuiScale)
        super.updateGuiScale()
    }

    // Provides a gui scale from 1 to 4, based on design spec
    // When changing this, make sure to also change the method just below
    private fun getWardrobeGuiScale(): Int {
        val width = UResolution.viewportWidth
        val height = UResolution.viewportHeight

        // Ranges provided in Scaling figma (from issue EM-2058)
        val widthScale = when (width) {
            in 0..1439 -> 1
            in 1440..2209 -> 2
            in 2210..2944 -> 3
            else -> 4
        }
        // above ranges multiplied by 9 / 16 (weren't provided, so I made it follow 16:9 ratio)
        val heightScale = when (height) {
            in 0..809 -> 1
            in 810..1242 -> 2
            in 1243..1656 -> 3
            else -> 4
        }
        return min(widthScale, heightScale)
    }

    override fun afterInitialization() {
        coinsManager.tryClaimingWelcomeCoins()
    }

    companion object {
        private const val sidebarWidth = 90f

        @JvmStatic
        fun getInstance(): Wardrobe? = GuiUtil.openedScreen() as? Wardrobe
    }
}
