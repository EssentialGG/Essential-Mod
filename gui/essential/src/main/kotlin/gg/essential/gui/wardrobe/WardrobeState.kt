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

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import gg.essential.cosmetics.CosmeticBundleId
import gg.essential.cosmetics.CosmeticCategoryId
import gg.essential.cosmetics.CosmeticId
import gg.essential.cosmetics.CosmeticTypeId
import gg.essential.cosmetics.EquippedCosmetic
import gg.essential.cosmetics.FeaturedPageCollectionId
import gg.essential.cosmetics.ImplicitOwnershipId
import gg.essential.cosmetics.diagnose
import gg.essential.cosmetics.events.AnimationEventType
import gg.essential.cosmetics.isAvailable
import gg.essential.elementa.UIComponent
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.Observer
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.zip
import gg.essential.gui.elementa.state.v2.filter
import gg.essential.gui.elementa.state.v2.filterNotNull
import gg.essential.gui.elementa.state.v2.mapEach
import gg.essential.gui.elementa.state.v2.mapEachNotNull
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.onChange
import gg.essential.gui.elementa.state.v2.set
import gg.essential.gui.elementa.state.v2.setAll
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.elementa.state.v2.stateUsingSystemTime
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.gui.elementa.state.v2.withSystemTime
import gg.essential.gui.elementa.state.v2.zipWithEachElement
import gg.essential.gui.state.Sale
import gg.essential.gui.util.layoutSafePollingState
import gg.essential.gui.wardrobe.Item.Companion.toItem
import gg.essential.mod.cosmetics.CosmeticCategory
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.featured.FeaturedPageCollection
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.model.Side
import gg.essential.network.connectionmanager.coins.CoinsManager
import gg.essential.network.connectionmanager.cosmetics.AssetLoader
import gg.essential.network.connectionmanager.skins.SkinsManager
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.gui.util.pollingStateV2
import gg.essential.mod.cosmetics.featured.FeaturedItem
import gg.essential.mod.cosmetics.featured.FeaturedItemRow
import gg.essential.mod.cosmetics.settings.CosmeticSettings
import gg.essential.mod.cosmetics.settings.setting
import gg.essential.network.connectionmanager.cosmetics.EmoteWheelManager
import gg.essential.network.connectionmanager.cosmetics.ICosmeticsManager
import gg.essential.network.connectionmanager.cosmetics.ModelLoader
import gg.essential.network.connectionmanager.cosmetics.OutfitManager
import gg.essential.network.connectionmanager.cosmetics.WardrobeSettings
import gg.essential.network.connectionmanager.notices.CosmeticNotices
import gg.essential.network.connectionmanager.notices.NoticeBannerManager
import gg.essential.universal.UResolution
import gg.essential.util.Client
import gg.essential.util.EssentialSounds
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WardrobeState(
    initialCategory: WardrobeCategory?,
    val screenOpen: State<Boolean>,
    val component: UIComponent,
    val cosmeticsManager: ICosmeticsManager,
    val modelLoader: ModelLoader,
    val settings: WardrobeSettings,
    val outfitManager: OutfitManager,
    val skinsManager: SkinsManager,
    val emoteWheelManager: EmoteWheelManager,
    val coinsManager: CoinsManager,
    val saleState: ListState<Sale>,
    val cosmeticNotices: CosmeticNotices,
    val bannerManager: NoticeBannerManager,
    private val guiScale: State<Int>,
) {
    val diagnosticsEnabled = cosmeticsManager.localCosmeticsData != null

    val inEmoteWheel = mutableStateOf(false)

    val search = mutableStateOf("")
    val filterSort = mutableStateOf(FilterSort.Default)

    val coins = coinsManager.coins

    val coinsSpent = coinsManager.coinsSpent

    val areCoinsVisuallyFrozen = coinsManager.areCoinsVisuallyFrozen

    val unlockedCosmetics = mutableStateOf(cosmeticsManager.unlockedCosmetics.get()).apply {
        cosmeticsManager.unlockedCosmetics.onSetValue(component) { this.set(it) }
    }

    val cosmeticsData = cosmeticsManager.cosmeticsData

    val rawTypes = cosmeticsManager.cosmeticsData.types

    val rawCategories = cosmeticsManager.cosmeticsData.categories

    val rawBundles = cosmeticsManager.cosmeticsData.bundles

    val rawFeaturedPageCollections = cosmeticsManager.cosmeticsData.featuredPageCollections

    val rawImplicitOwnerships = cosmeticsManager.cosmeticsData.implicitOwnerships

    val rawCosmetics = cosmeticsManager.cosmeticsData.cosmetics

    private val availableCosmetics = stateUsingSystemTime { now ->
        val unlockedCosmetics = unlockedCosmetics()
        rawCosmetics().filter {
            (it.isAvailable(now) && "HIDDEN" !in it.tags) || it.id in unlockedCosmetics
        }
    }.toListState()

    val cosmetics = if (diagnosticsEnabled) {
        val cosmeticsWithDiagnostics = availableCosmetics.mapEach { cosmetic ->
            val diagnosticsState = diagnose(modelLoader, cosmetic)
            memo {
                val diagnostics = diagnosticsState()
                when {
                    diagnostics == null -> cosmetic.copy(diagnostics = null)
                    diagnostics.isEmpty() -> cosmetic.copy(diagnostics = emptyList())
                    else -> cosmetic.copy(diagnostics = diagnostics)
                }
            }
        }
        memo { cosmeticsWithDiagnostics().map { it() } }.toListState()
    } else {
        availableCosmetics
    }

    // Categories are no longer used for limited time stuff, we are no longer using availability stuff, so we should ignore it
    val categories = rawCategories.zip(cosmetics).map { (categories, cosmetics) ->
        categories.filterTo(mutableListOf()) { category ->
            cosmetics.any { it.categories.containsKey(category.id) }
        }
    }

    val allCategories = memo { buildList<WardrobeCategory> {
        if (diagnosticsEnabled && memo { cosmetics().any { it.diagnostics?.isNotEmpty() == true } }()) {
            add(WardrobeCategory.Diagnostics)
        }

        add(WardrobeCategory.FeaturedRefresh)
        add(WardrobeCategory.Outfits)
        add(WardrobeCategory.Skins)

        add(WardrobeCategory.Cosmetics)
        add(WardrobeCategory.Emotes)

        for (category in categories()) {
            if (category.isHidden()) continue
            val parent = if (category.isEmoteCategory()) WardrobeCategory.Emotes else WardrobeCategory.Cosmetics
            add(WardrobeCategory.CosmeticCategorySubCategory(category, parent))
        }

        sortBy { it.order }
    }}.toListState()

    val currentCategory = mutableStateOf(initialCategory ?: allCategories.get().first { it !is WardrobeCategory.SubCategory })

    val types = rawTypes

    val bundles = rawBundles

    // We currently support only one layout, so we pick one from the available ones
    // We use the raw list state, so that in the case we only have expired pages, we keep showing them until we get new ones
    val featuredPageCollection = memo {
        val pagesWithAvailability = withSystemTime { now ->
            rawFeaturedPageCollections().map { it to it.isAvailable(now) }
        }
        pagesWithAvailability.minWithOrNull(
            compareByDescending<Pair<FeaturedPageCollection, Boolean>> { it.second } // Prioritize available ones (includes no availability)
                .thenByDescending { it.first.availability != null } // Prioritize ones that do have an explicit range
        )?.first
    }

    private val isOwnedOnly = filterSort.map { it == FilterSort.Owned }
    private val filter = isOwnedOnly.zip(unlockedCosmetics).map { (isOwnedOnly, unlockedCosmetics) ->
        filter@{ cosmetic: Cosmetic ->
            return@filter !isOwnedOnly || cosmetic.id in unlockedCosmetics
        }
    }

    private val filteredCosmetics = cosmetics.zipWithEachElement(filter) { cosmetic, filter ->
        cosmetic.takeIf(filter)
    }.filterNotNull()

    val cosmeticItems = filteredCosmetics.mapEach { Item.CosmeticOrEmote(it) }
    val bundleItems = bundles.mapEach { it.toItem() }
    val outfitItems = outfitManager.outfits.mapEachNotNull { outfit ->
        val skin = outfit.skin
        val skinId = outfit.skinId
        if (skin == null || skinId == null) {
            null
        } else {
            Item.OutfitItem(outfit.id, outfit.name, skinId, skin, outfit.equippedCosmetics, outfit.cosmeticSettings, outfit.createdAt, outfit.favoritedSince)
        }
    }
    val skinItems = skinsManager.skinsOrdered.mapEach { skin ->
        Item.SkinItem(skin.id, skin.name, skin.skin, skin.createdAt, skin.lastUsedAt, skin.favoritedSince)
    }

    private fun <T : Item> ListState<T>.filteredBySearch() =
        zipWithEachElement(search) { item, search ->
            if (item.name.contains(search, ignoreCase = true)) {
                item
            } else {
                null
            }
        }.filterNotNull()

    val visibleCosmeticItems = cosmeticItems.filteredBySearch()
    val visibleBundleItems = bundleItems.filteredBySearch()
    val visibleOutfitItems = outfitItems.filteredBySearch()
    val visibleSkinItems = skinItems.filteredBySearch()

    val bundlePurchaseInProgress: MutableState<String?> = mutableStateOf(null)

    val unlockedBundles = stateBy {
        val unlockedCosmetics = unlockedCosmetics()
        val outfitItems = outfitItems()

        bundles()
            .filter { bundle ->
                val bundleCosmeticsUnlocked = bundle.cosmetics.values.all { it in unlockedCosmetics }

                // Future proofing: Eventually, infra should include bundle IDs within `ServerCosmeticsUserUnlockedPacket`
                // (which is trickled down to unlockedCosmetics).
                // https://discord.com/channels/887304453500325900/887707778125299772/1184809493490565191
                bundle.id in unlockedCosmetics
                    || bundle.id == bundlePurchaseInProgress() && bundleCosmeticsUnlocked
                    || outfitItems.any { outfit ->
                        ((outfit.name == bundle.name && outfit.skin == bundle.skin?.toMod()) || outfit.cosmetics == bundle.cosmetics)
                    } && bundleCosmeticsUnlocked
            }
            .map { it.id }
    }

    val visibleCosmetics = visibleCosmeticItems.mapEach { it.cosmetic }

    /**
     * When set, the main view is scrolled to have the item, with the same itemId,
     * in view and the item outline is highlighted for a moment.
     * Will be automatically un-set shortly after it is set.
     */
    val highlightItem = mutableStateOf<ItemId?>(null)

    /** The cosmetic for which the color/height/side/etc. components are displayed in the preview window. */
    val editingCosmetic = mutableStateOf<Item.CosmeticOrEmote?>(null)

    /** The current variants of cosmetics mapped to their outfits. These may or may not be saved to the current outfit. **/
    private val cosmeticVariantStates = mutableMapOf<String, MutableMap<CosmeticId, MutableState<CosmeticSetting.Variant?>>>()

    /** The current sides of cosmetics mapped to their outfits. These may or may not be saved to the current outfit. **/
    private val cosmeticSideStates = mutableMapOf<String, MutableMap<CosmeticId, MutableState<CosmeticSetting.Side?>>>()

    /** The current positions of cosmetics mapped to their outfits. These may or may not be saved to the current outfit. **/
    private val cosmeticPositionStates = mutableMapOf<String, MutableMap<CosmeticId, MutableState<CosmeticSetting.PlayerPositionAdjustment?>>>()

    val selectedItem = mutableStateOf<Item?>(null)

    /** Currently selected / being-previewed bundle. */
    val selectedBundle: State<Item.Bundle?> = memo { selectedItem() as? Item.Bundle }.apply {
        onChange(component) {
            if (it != null) {
                editingCosmetic.set(null)
                inEmoteWheel.set(false)
            }
        }
    }

    /** Currently selected emote */
    val selectedEmote: State<Item.CosmeticOrEmote?> = memo {
        (selectedItem() as? Item.CosmeticOrEmote)?.takeIf { it.cosmetic.type.slot == CosmeticSlot.EMOTE && it.id !in unlockedCosmetics() }
    }.apply {
        onChange(component) {
            if (it != null) {
                editingCosmetic.set(null)
            }
        }
    }

    /**
     * Hovered but not yet confirmed cosmetic setting(s) (e.g. color variant).
     * Applies only to [editingCosmetic] and player preview.
     */
    val previewingSetting = mutableStateOf<Map<CosmeticId, CosmeticSetting>>(emptyMap())

    /** The currently previewed, selected, and equipped cosmetic settings for the currently selected outfit. */
    val selectedPreviewingEquippedSettings = component.pollingStateV2 {
        val outfitId = outfitManager.selectedOutfitId.get() ?: return@pollingStateV2 ImmutableMap.of()
        val previewMap = previewingSetting.get()
        val variantMap = cosmeticVariantStates[outfitId].orEmpty()
        val sideMap = cosmeticSideStates[outfitId].orEmpty()
        val positionMap = cosmeticPositionStates[outfitId].orEmpty()
        val currentMap = outfitManager.getOutfit(outfitId)?.cosmeticSettings
        val cosmeticIds = (previewMap.keys.asSequence() + variantMap.keys.asSequence() + sideMap.keys.asSequence()
            + positionMap.keys.asSequence() + currentMap?.keys?.asSequence().orEmpty()).distinct()
        ImmutableMap.copyOf(cosmeticIds.associateWith { cosmeticId ->
            ImmutableList.copyOf(listOfNotNull(
                previewMap[cosmeticId],
                variantMap[cosmeticId]?.get(),
                sideMap[cosmeticId]?.get(),
                positionMap[cosmeticId]?.get(),
            ) + currentMap?.get(cosmeticId).orEmpty())
        })
    }

    /** Show purchase animation if true. **/
    val purchaseAnimationState = mutableStateOf(false)

    /** The [DraggedEmoteInfo] data for the emote currently being dragged or null if no emote is being dragged. */
    val draggingEmote = mutableStateOf<DraggedEmote?>(null).apply {
        onChange(component) { inEmoteWheel.set(true) }
    }

    val draggingOntoOccupiedEmoteSlot = memo {
        val dragged = draggingEmote()
        dragged?.to != null && dragged.to != dragged.from &&
                emoteWheelManager.getEmoteWheel(dragged.to.emoteWheelId)?.slots?.getOrNull(dragged.to.slotIndex) != null
    }

    val equippedOutfitId = outfitManager.selectedOutfitId

    val equippedOutfitItem: State<Item.OutfitItem?> = stateBy {
        val equippedId = equippedOutfitId() ?: return@stateBy null
        outfitItems().firstOrNull { it.id == equippedId }
    }

    val equippedCosmeticsState = component.pollingStateV2 {
        val equippedCosmetics = outfitManager.getSelectedOutfit()?.equippedCosmetics?.toMutableMap() ?: mutableMapOf()
        equippedCosmetics.remove(CosmeticSlot.EMOTE) // only show emotes if they are explicitly previewing them
        equippedCosmetics
    }.apply {
        onSetValueAndNow(component) { equipped ->
            // Reset editing cosmetic when unequipped
            if (editingCosmetic.get()?.cosmetic?.id !in equipped.values) {
                editingCosmetic.set(null)
            }
            // Commit selected settings when equipped
            val outfitId = equippedOutfitId.get() ?: return@onSetValueAndNow
            val settingsMap = outfitManager.getOutfit(outfitId)?.cosmeticSettings ?: emptyMap()
            for (cosmeticId in equipped.values) {
                val settings = settingsMap[cosmeticId]?.toMutableList() ?: mutableListOf()
                cosmeticVariantStates[outfitId]?.get(cosmeticId)?.let { state ->
                    settings.removeIf { it is CosmeticSetting.Variant }
                    state.get()?.let { settings.add(it) }
                }
                cosmeticSideStates[outfitId]?.get(cosmeticId)?.let { state ->
                    settings.removeIf { it is CosmeticSetting.Side }
                    state.get()?.let { settings.add(it) }
                }
                cosmeticPositionStates[outfitId]?.get(cosmeticId)?.let { state ->
                    settings.removeIf { it is CosmeticSetting.PlayerPositionAdjustment }
                    state.get()?.let { settings.add(it) }
                }
                outfitManager.updateOutfitCosmeticSettings(outfitId, cosmeticId, settings)
            }
        }
    }

    private fun Observer.getUnownedCosmetics(itemsToCheck: List<CosmeticId>, predicate: (Item.CosmeticOrEmote) -> Boolean): List<Item.CosmeticOrEmote> {
        val unlockedCosmetics = unlockedCosmetics()
        val cosmetics = rawCosmetics()
        return itemsToCheck.asSequence()
            .filter { it !in unlockedCosmetics }
            .mapNotNull { id -> cosmetics.find { it.id == id } }
            .map { Item.CosmeticOrEmote(it) }
            .filter(predicate)
            .toList()
    }

    val equippedCosmeticsPurchasable = memo {
        getUnownedCosmetics(equippedCosmeticsState().values.toList()) { it.isPurchasable }
    }.toListState()

    val equippedCosmeticsTotalCost = getTotalCost(equippedCosmeticsPurchasable)

    val itemIdToCategoryMap: MutableMap<CosmeticId, WardrobeCategory> = mutableMapOf()

    private val currentFeaturedPageCollection = if (cosmeticsManager.cosmeticsDataWithChanges != null) {
        // If we have cosmetic overrides, we display the collection we are currently editing, if it exists
        memo {
            currentlyEditingFeaturedPageCollection() ?: featuredPageCollection()
        }
    } else {
        featuredPageCollection
    }

    private val columnCount = component.layoutSafePollingState {
        getWardrobeGridColumnCount()
    }

    private val columnCountFeaturedPage = memo {
        val mainColumnCount = columnCount()
        currentFeaturedPageCollection()?.getClosestLayoutOrNull(mainColumnCount)?.key ?: mainColumnCount
    }

    fun getColumnCount(category: WardrobeCategory?): State<Int> {
        return if (category is WardrobeCategory.FeaturedRefresh) columnCountFeaturedPage else columnCount
    }

    val featuredPageLayout = memo {
        val pageCollection = currentFeaturedPageCollection()
        val columns = columnCountFeaturedPage()
        if (pageCollection == null || pageCollection.pages.isEmpty()) {
            Pair(true, null)
        } else {
            Pair(false, pageCollection.getClosestLayoutOrNull(columns))
        }
    }

    val featuredPageItems: ListState<String> = memo {
        val layoutStateObject = featuredPageLayout()

        if (layoutStateObject.first) {
            return@memo listOf()
        }

        layoutStateObject.second?.value?.rows?.filterIsInstance<FeaturedItemRow>()?.flatMap { it.items }?.mapNotNull {
            when (it) {
                is FeaturedItem.Bundle -> it.bundle
                is FeaturedItem.Cosmetic -> it.cosmetic
                is FeaturedItem.Empty -> null
            }
        } ?: listOf()
    }.toListState()

    val currentCategoryTotalCost = memo {
        val bundle = selectedBundle()
        val emote = selectedEmote()
        when {
            bundle != null -> bundle.getCost(this@WardrobeState)() ?: 0
            emote != null -> emote.getCost(this@WardrobeState)() ?: 0
            else -> equippedCosmeticsTotalCost()
        }
    }

    val purchaseConfirmationEmoteId = "ESSENTIAL_PURCHASE_CONFIRMATION"

    val wardrobeOpenTime = System.currentTimeMillis()

    init {
        // Register purchase confirmation emote
        cosmeticsManager.infraCosmeticsData.requestCosmeticsIfMissing(listOf(purchaseConfirmationEmoteId))
    }

    fun Observer.resolveCosmeticIds(map: Map<CosmeticSlot, CosmeticId>, settings: Map<String, CosmeticSettings>): ImmutableMap<CosmeticSlot, EquippedCosmetic> {
        if (map.isEmpty()) {
            return ImmutableMap.of()
        }
        val cosmetics = rawCosmetics()
        return ImmutableMap.copyOf(map.mapNotNull { (slot, id) ->
            val cosmetic = cosmetics.find { it.id == id }
            if (cosmetic != null) {
                slot to EquippedCosmetic(cosmetic, settings[id] ?: emptyList())
            } else {
                cosmeticsManager.infraCosmeticsData.requestCosmeticsIfMissing(listOf(id))
                null
            }
        }.toMap())
    }

    fun getTotalCost(items: ListState<Item>): State<Int> {
        val costs = items.map { it.toSet().toList() }.toListState().mapEach { it.getCost(this) }
        return stateBy { costs().sumOf { it() ?: 0 } }
    }

    // Local cosmetics editing

    val editingMenuOpen = mutableStateOf(false)

    val currentlyEditingCosmeticId = mutableStateOf<CosmeticId?>(null)
    val currentlyEditingCosmeticBundleId = mutableStateOf<CosmeticBundleId?>(null)
    val currentlyEditingCosmeticTypeId = mutableStateOf<CosmeticTypeId?>(null)
    val currentlyEditingCosmeticCategoryId = mutableStateOf<CosmeticCategoryId?>(null)
    val currentlyEditingFeaturedPageCollectionId = mutableStateOf<FeaturedPageCollectionId?>(null)
    val currentlyEditingImplicitOwnershipId = mutableStateOf<ImplicitOwnershipId?>(null)

    val currentlyEditingCosmetic = stateBy { currentlyEditingCosmeticId()?.let { id -> cosmetics().find { it.id == id } } }
    val currentlyEditingCosmeticBundle = stateBy { currentlyEditingCosmeticBundleId()?.let { id -> bundles().find { it.id == id } } }
    val currentlyEditingCosmeticType = stateBy { currentlyEditingCosmeticTypeId()?.let { id -> types().find { it.id == id } } }
    val currentlyEditingCosmeticCategory = stateBy { currentlyEditingCosmeticCategoryId()?.let { id -> rawCategories().find { it.id == id } } }
    val currentlyEditingFeaturedPageCollection = stateBy { currentlyEditingFeaturedPageCollectionId()?.let { id -> rawFeaturedPageCollections().find { it.id == id } } }
    val currentlyEditingImplicitOwnership = stateBy { currentlyEditingImplicitOwnershipId()?.let { id -> rawImplicitOwnerships().find { it.id == id } } }

    val showingDiagnosticsFor = mutableStateOf<String?>(null) // value is LOCAL_PATH of cosmetic

    val isUsingConfigurator = memo {
        editingMenuOpen() || listOfNotNull(
            showingDiagnosticsFor(),
            currentlyEditingCosmetic(),
            currentlyEditingCosmeticBundle(),
            currentlyEditingCosmeticType(),
            currentlyEditingCosmeticCategory(),
            currentlyEditingFeaturedPageCollection(),
        ).isNotEmpty()
    }

    fun changeOutfit(offset: Int) {
        val outfits = outfitManager.outfits.get()
        val n = outfits.size
        val outfitIndex = outfits.indexOfFirst { it.id == equippedOutfitId.get() }
        val outfit = outfits[(outfitIndex + offset + n) % n]
        outfitManager.setSelectedOutfit(outfit.id)
    }

    fun changeEmoteWheel(offset: Int) {
        emoteWheelManager.shiftSelectedEmoteWheel(offset)
    }

    fun setFavorite(item: Item, favorite: Boolean) {
        when (item) {
            is Item.OutfitItem -> outfitManager.setFavorite(item.id, favorite)
            is Item.SkinItem -> skinsManager.setFavoriteState(item.id, favorite)
            else -> {}
        }
    }

    fun setVariant(item: Item.CosmeticOrEmote, variant: String?) {
        val outfitId = equippedOutfitId.get() ?: return

        val setting = variant?.let { CosmeticSetting.Variant(item.cosmetic.id, true, CosmeticSetting.Variant.Data(variant)) }
        cosmeticVariantStates.getOrPut(outfitId, ::mutableMapOf).getOrPut(item.cosmetic.id) { mutableStateOf(setting) }.set(setting)

        if (item.cosmetic.id in equippedCosmeticsState.get().values) {
            val newSettings = outfitManager.getOutfit(outfitId)?.cosmeticSettings?.get(item.cosmetic.id)?.toMutableList() ?: mutableListOf()
            newSettings.removeIf { it is CosmeticSetting.Variant }
            setting?.let { newSettings.add(it) }
            outfitManager.updateOutfitCosmeticSettings(outfitId, item.cosmetic.id, newSettings)
        }
    }

    fun getVariant(item: Item.CosmeticOrEmote): State<String?> {
        return stateBy {
            val outfitId = equippedOutfitId() ?: return@stateBy null
            cosmeticVariantStates.getOrPut(outfitId, ::mutableMapOf)
                .getOrPut(item.cosmetic.id) {
                    val settings = outfitManager.getOutfit(outfitId)?.cosmeticSettings?.get(item.cosmetic.id)
                    mutableStateOf(settings?.setting<CosmeticSetting.Variant>())
                }
                .invoke()
                ?.data?.variant
        }
    }

    fun setSelectedSide(item: Item.CosmeticOrEmote, side: Side?) {
        val outfitId = equippedOutfitId.get() ?: return

        val setting = side?.let { CosmeticSetting.Side(item.cosmetic.id, true, CosmeticSetting.Side.Data(side)) }
        cosmeticSideStates.getOrPut(outfitId, ::mutableMapOf).getOrPut(item.cosmetic.id) { mutableStateOf(setting) }.set(setting)

        if (item.cosmetic.id in equippedCosmeticsState.get().values) {
            val newSettings = outfitManager.getOutfit(outfitId)?.cosmeticSettings?.get(item.cosmetic.id)?.toMutableList() ?: mutableListOf()
            newSettings.removeIf { it is CosmeticSetting.Side }
            setting?.let { newSettings.add(it) }
            outfitManager.updateOutfitCosmeticSettings(outfitId, item.cosmetic.id, newSettings)
        }
    }

    fun getSelectedSide(item: Item.CosmeticOrEmote): State<Side?> {
        return stateBy {
            val outfitId = equippedOutfitId() ?: return@stateBy null
            cosmeticSideStates.getOrPut(outfitId, ::mutableMapOf)
                .getOrPut(item.cosmetic.id) {
                    val settings = outfitManager.getOutfit(outfitId)?.cosmeticSettings?.get(item.cosmetic.id)
                    mutableStateOf(settings?.setting<CosmeticSetting.Side>())
                }
                .invoke()
                ?.data?.side
        }
    }

    fun setSelectedPosition(item: Item.CosmeticOrEmote, position: Triple<Float, Float, Float>?) {
        val outfitId = equippedOutfitId.get() ?: return

        val setting = position?.let {
            CosmeticSetting.PlayerPositionAdjustment(item.cosmetic.id, true, CosmeticSetting.PlayerPositionAdjustment.Data(position.first, position.second, position.third))
        }
        cosmeticPositionStates.getOrPut(outfitId, ::mutableMapOf).getOrPut(item.cosmetic.id) { mutableStateOf(setting) }.set(setting)

        if (item.cosmetic.id in equippedCosmeticsState.get().values) {
            val newSettings = outfitManager.getOutfit(outfitId)?.cosmeticSettings?.get(item.cosmetic.id)?.toMutableList() ?: mutableListOf()
            newSettings.removeIf { it is CosmeticSetting.PlayerPositionAdjustment }
            setting?.let { newSettings.add(it) }
            outfitManager.updateOutfitCosmeticSettings(outfitId, item.cosmetic.id, newSettings)
        }
    }

    fun getSelectedPosition(item: Item.CosmeticOrEmote): State<Triple<Float, Float, Float>?> {
        return stateBy {
            val outfitId = equippedOutfitId() ?: return@stateBy null
            cosmeticPositionStates.getOrPut(outfitId, ::mutableMapOf)
                .getOrPut(item.cosmetic.id) {
                    val settings = outfitManager.getOutfit(outfitId)?.cosmeticSettings?.get(item.cosmetic.id)
                    mutableStateOf(settings?.setting<CosmeticSetting.PlayerPositionAdjustment>())
                }
                .invoke()
                ?.data?.let { Triple(it.x, it.y, it.z) }
        }
    }

    @OptIn(DelicateCoroutinesApi::class) // FIXME should use screen scope or something like that
    fun triggerPurchaseAnimation() {
        if (purchaseAnimationState.get()) {
            return
        }
        val purchaseEmote = rawCosmetics.get().find { it.id == purchaseConfirmationEmoteId }
        if (purchaseEmote == null) {
            LOGGER.warn("Unable to find purchase confirmation animation.")
            return
        }
        // Find out animation time for purchase confirmation emote
        val bedrockModel = modelLoader.getModel(
            purchaseEmote, purchaseEmote.defaultVariantName, AssetLoader.Priority.Blocking
        ).join()
        val emoteTime = bedrockModel.animationEvents
            .filter { it.type == AnimationEventType.EMOTE }
            .maxOfOrNull { it.getTotalTime(bedrockModel) }
            ?.toDouble()?.seconds
            ?: Duration.ZERO

        // Delay purchase sound to go with the animation (animation is delayed by 0.3s)
        GlobalScope.launch(Dispatchers.Client) {
            delay(300.milliseconds)
            EssentialSounds.playPurchaseConfirmationSound()
        }

        GlobalScope.launch(Dispatchers.Client) {
            purchaseAnimationState.set(true)
            try {
                delay(emoteTime)
            } finally {
                purchaseAnimationState.set(false)
            }
        }
    }

    private fun getWardrobeGridColumnCount(): Int {
        val scale = guiScale.getUntracked().coerceIn(1, 4) // Just in-case we haven't updated it yet
        val width = UResolution.viewportWidth

        // Ranges provided in Scaling figma (from issue EM-2058)
        return when (scale) {
            1 -> when (width) {
                in 0..899 -> 3
                in 900..1049 -> 4
                in 1050..1439 -> 5
                else -> 5
            }

            2 -> when (width) {
                in 1440..1719 -> 3
                in 1720..1999 -> 4
                in 2000..2209 -> 5
                else -> 5
            }

            3 -> when (width) {
                in 2210..2549 -> 3
                in 2550..2915 -> 4
                in 2916..2944 -> 5
                else -> 5
            }

            4 -> when (width) {
                in 2945..3416 -> 3
                in 3417..3887 -> 4
                else -> 5
            }

            else -> 5
        }
    }

    enum class FilterSort(
        val displayName: String,
        comparator: Comparator<CosmeticWithSortInfo>,
    ) : Comparator<CosmeticWithSortInfo> by comparator {
        Default("All Items", sortByOrderInCollection.then(sortBySortWeight).then(sortByPriority).then(sortByName)),
        Alphabetical("A-Z", sortByName.then(Default)),
        Owned("Owned Only", Default),
        Price("Price", sortByPrice.then(Default)),
    }

    data class CosmeticWithSortInfo(val cosmetic: Cosmetic, val owned: Boolean, val price: Int?, val collection: CosmeticCategory?)

    /** The [emoteWheelId] and [slotIndex] for an emote in an emote wheel. */
    data class EmoteSlotId(val emoteWheelId: String, val slotIndex: Int)
    /**
     *  The [EmoteSlotId] for an emote being dragged.
     *  A null value for [from] indicates the emote originated from the Wardrobe container.
     *  A null value for [to] indicates the emote's destination is the Wardrobe container and that it should be removed.
     *  A same value for [from] and [to] indicate the emote currently has no destination and that it should not be removed.
     */
    data class DraggedEmote(
        val emoteId: CosmeticId? = null,
        val from: EmoteSlotId? = null,
        val to: EmoteSlotId? = null,
        val clickOffset: Pair<Float, Float> = Pair(0f, 0f),
        val onInstantLeftClick: () -> Unit = {},
    )

    companion object {
        private val LOGGER = LoggerFactory.getLogger(WardrobeState::class.java)

        private val sortByOrderInCollection: Comparator<CosmeticWithSortInfo> =
            compareBy { it.cosmetic.categories[it.collection?.id] ?: 0 }
        private val sortBySortWeight: Comparator<CosmeticWithSortInfo> =
            compareBy { it.cosmetic.defaultSortWeight }
        private val sortByPriority: Comparator<CosmeticWithSortInfo> =
            compareBy { it.cosmetic.categories["popular"] ?: 0 }
        private val sortByName: Comparator<CosmeticWithSortInfo> =
            compareBy { it.cosmetic.displayName }
        private val sortByReleaseDate: Comparator<CosmeticWithSortInfo> =
            compareBy { it.cosmetic.availableAfter }
        private val sortByPrice: Comparator<CosmeticWithSortInfo> =
            compareBy { it.price ?: 0 }
    }
}
