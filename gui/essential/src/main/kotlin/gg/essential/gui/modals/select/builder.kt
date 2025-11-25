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
package gg.essential.gui.modals.select

import com.sparkuniverse.toolbox.chat.enums.ChannelType
import com.sparkuniverse.toolbox.chat.model.Channel
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.IconButton
import gg.essential.gui.elementa.essentialmarkdown.EssentialMarkdown
import gg.essential.gui.elementa.essentialmarkdown.MarkdownConfig
import gg.essential.gui.elementa.essentialmarkdown.ParagraphConfig
import gg.essential.gui.elementa.essentialmarkdown.TextConfig
import gg.essential.gui.elementa.essentialmarkdown.URLConfig
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.StateByScope
import gg.essential.gui.elementa.state.v2.combinators.and
import gg.essential.gui.elementa.state.v2.combinators.contains
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.not
import gg.essential.gui.elementa.state.v2.filter
import gg.essential.gui.elementa.state.v2.mapEach
import gg.essential.gui.elementa.state.v2.mapList
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.friends.state.PlayerActivity
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.modals.select.component.playerAvatar
import gg.essential.gui.modals.select.component.playerName
import gg.essential.gui.overlay.ModalFlow
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.util.hoveredState
import gg.essential.gui.util.toStateV2List
import gg.essential.universal.USound
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.USession
import gg.essential.util.UuidNameLookup
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color
import java.util.*

typealias SectionFilterBlock<T> = (T, State<String>) -> State<Boolean>
typealias SectionLayoutBlock<T> = LayoutScope.(selected: MutableState<Boolean>, item: T) -> Unit

private typealias HoveredToColorStateBlock = StateByScope.(hovered: State<Boolean>) -> Color
private typealias ModalSettingsBlock<T> = SelectModal<T>.() -> Unit

data class Section<T, S : Any>(
    val title: String,
    val map: (S) -> T,
    val identifiers: ListState<S>,
    val filter: SectionFilterBlock<S>,
    val layout: SectionLayoutBlock<S>,
)

// Not everything is used in the builder at the moment, but it will be once we start to use it more.
@Suppress("UNUSED_PARAMETER", "MemberVisibilityCanBePrivate")
class SelectModalBuilder<T>(
    val title: String,
    val modalSimpleName: String,
) {
    private val socialStates by lazy { platform.createSocialStates() }
    private val relationshipStates by lazy { socialStates.relationships }
    private val messageStates by lazy { socialStates.messages }
    private val statusStates by lazy { socialStates.activity }

    private val sections = mutableListOf<Section<T, *>>()
    internal val defaultUserRow: SectionLayoutBlock<UUID>
        get() = { selected, uuid ->
            box(Modifier.fillParent()) {
                row(Modifier.fillParent(padding = 4f)) {
                    playerEntry(selected, uuid)
                    defaultAddRemoveButton(selected)
                }
            }.onLeftClick { event ->
                USound.playButtonPress()
                event.stopPropagation()
                selected.set { !it }
            }
        }

    internal val defaultGroupRow: SectionLayoutBlock<Long>
        get() = { selected, id ->
            box(Modifier.fillParent()) {
                row(Modifier.fillParent(padding = 4f)) {
                    groupEntry(selected, id)
                    defaultAddRemoveButton(selected)
                }
            }.onLeftClick { event ->
                USound.playButtonPress()
                event.stopPropagation()
                selected.set { !it }
            }
        }

    internal val defaultUserOrGroupRow: SectionLayoutBlock<Channel>
        get() = { selected, channel ->
            val otherUser =
                if (channel.type != ChannelType.DIRECT_MESSAGE) null
                else channel.members.find { it != USession.activeNow().uuid }
            box(Modifier.fillParent()) {
                row(Modifier.fillParent(padding = 4f)) {
                    if (otherUser == null) {
                        groupEntry(selected, channel.id)
                    } else {
                        playerEntry(selected, otherUser)
                    }
                    defaultAddRemoveButton(selected)
                }
            }.onLeftClick { event ->
                USound.playButtonPress()
                event.stopPropagation()
                selected.set { !it }
            }
        }

    private var initiallySelected = setOf<T>()

    private var modalSettings = mutableListOf<ModalSettingsBlock<T>>()

    var selectTooltip: String? = null
    var deselectTooltip: String? = null

    /**
     * If you want to force the user to click an action button to dismiss the modal instead of clicking out of its bounds
     */
    var requiresButtonPress: Boolean = false

    /**
     * If you want to force the user to select at least one entry before clicking continue
     */
    var requiresSelection: Boolean = true

    /**
     * Adds shadows all entries
     */
    var shadowsOnEntries: Boolean = true

    /** Displays when there are no entries available in the modal. Will not be displayed if there are entries available, but they are filtered out by search. */
    var whenEmpty: (LayoutScope.() -> Unit)? = null

    var extraContent: (LayoutScope.() -> Unit)? = null

    private val emptyTextMarkdownConfig = MarkdownConfig(
        paragraphConfig = ParagraphConfig(centered = true),
        textConfig = TextConfig(EssentialPalette.TEXT_DISABLED, shadowColor = EssentialPalette.COMPONENT_BACKGROUND),
        urlConfig = URLConfig(EssentialPalette.TEXT_DISABLED, EssentialPalette.TEXT_MID_GRAY, true),
    )

    fun emptyText(text: String) {
        emptyText(stateOf(text))
    }

    fun emptyText(text: State<String>) = apply {
        whenEmpty = {
            column {
                spacer(height = 3f)
                EssentialMarkdown(text, emptyTextMarkdownConfig)(Modifier.width(130f)).onLinkClicked(platform.essentialUriListener)
            }
        }
    }

    fun emptyTextNoFriends() = emptyText("You haven't added any friends yet. You can add them in the social menu.")

    fun LayoutScope.playerEntry(selected: MutableState<Boolean>, uuid: UUID) {
        row(Modifier.fillRemainingWidth(), Arrangement.spacedBy(7f, FloatPosition.START)) {
            playerAvatar(uuid, shadowColor = EssentialPalette.BLACK, modifier = Modifier.width(8f).heightAspect(1f))
            playerName(uuid, modifier = Modifier.alignVertical(Alignment.Start(1f)).shadow(EssentialPalette.BLACK))
        }
    }

    fun LayoutScope.groupEntry(selected: MutableState<Boolean>, id: Long) {
        row(Modifier.fillRemainingWidth(), Arrangement.spacedBy(7f)) {
            image(EssentialPalette.groupIconForChannel(id), Modifier.shadow(EssentialPalette.BLACK))
            row(Modifier.fillRemainingWidth().alignVertical(Alignment.Start(1f)), Arrangement.spacedBy(float = FloatPosition.START)) {
                text(messageStates.getTitle(id), truncateIfTooSmall = true, modifier = Modifier.shadow(EssentialPalette.BLACK))
            }
        }
    }

    fun onlinePlayers(map: (UUID) -> T, block: SectionLayoutBlock<UUID> = defaultUserRow) {
        val onlineList = filterFriendsByActivity { it !is PlayerActivity.Offline }
        users("Online", map, onlineList, block)
    }

    fun offlinePlayers(map: (UUID) -> T, block: SectionLayoutBlock<UUID> = defaultUserRow) {
        val offlineList = filterFriendsByActivity { it is PlayerActivity.Offline }
        users("Offline", map, offlineList, block)
        if (whenEmpty == null) {
            emptyTextNoFriends()
        }
    }

    fun friends(map: (UUID) -> T, block: SectionLayoutBlock<UUID> = defaultUserRow) {
        val friendsList = relationshipStates.getObservableFriendList().toStateV2List().mapList { list -> list.filter { !socialStates.isSuspended(it)() } }
        users("Friends", map, friendsList, block)
        if (whenEmpty == null) {
            emptyTextNoFriends()
        }
    }

    fun users(
        title: String,
        map: (UUID) -> T,
        state: ListState<UUID>,
        block: SectionLayoutBlock<UUID> = defaultUserRow,
    ) {
        section(
            title = title,
            map = map,
            identifiers = state,
            searchFilter = { identifier, searchText ->
                UuidNameLookup.nameState(identifier).contains(searchText, ignoreCase = true)
            },
            block = block
        )
    }

    fun groups(
        map: (Long) -> T,
        block: SectionLayoutBlock<Long> = defaultGroupRow,
    ) {
        val groupsState = messageStates.getObservableChannelList()
            .toStateV2List()
            .filter { it.type == ChannelType.GROUP_DIRECT_MESSAGE }
            .mapEach { it.id }

        section(
            title = "Groups",
            map = map,
            identifiers = groupsState,
            searchFilter = { identifier, searchText ->
                messageStates.getTitle(identifier).contains(searchText, ignoreCase = true)
            },
            block = block,
        )
    }

    fun friendsAndGroups(
        map: (Channel) -> T,
        block: SectionLayoutBlock<Channel> = defaultUserOrGroupRow,
    ) {
        val channelList = messageStates.getObservableChannelList().toStateV2List().mapList { list ->
                list.filter {
                    if (it.type == ChannelType.DIRECT_MESSAGE) {
                        val other = it.getOtherUser()
                        other == null || !socialStates.isSuspended(other)()
                    } else {
                        it.type == ChannelType.GROUP_DIRECT_MESSAGE
                    }
                }
            }
        val friendsAndGroupsState =
            stateBy {
                // Adapted from ChatTab
                channelList().sortedWith(
                    compareBy<Channel>(
                        { messageStates.getUnreadChannelState(it.id)() },
                        { (messageStates.getLatestMessage(it.id)()?.createdAt ?: it.joinedAt) },
                    ).reversed()
                )
            }

        section(
            title = "Friends & Groups",
            map = map,
            identifiers = friendsAndGroupsState.toListState(),
            searchFilter = { identifier, searchText ->
                messageStates.getTitle(identifier.id).contains(searchText, ignoreCase = true)
            },
            block = block,
        )

        if (whenEmpty == null) {
            emptyTextNoFriends()
        }
    }

    fun <S : Any> section(
        title: String,
        map: (S) -> T,
        identifiers: ListState<S>,
        searchFilter: SectionFilterBlock<S>,
        block: SectionLayoutBlock<S> = { _, _ -> },
    ) {
        val section = Section(title, map, identifiers, searchFilter, block)
        sections.add(section)
    }

    fun modalSettings(block: ModalSettingsBlock<T>) = apply {
        modalSettings.add(block)
    }

    fun setInitiallySelected(vararg items: T) {
        initiallySelected = items.toSet()
    }

    fun LayoutScope.defaultAddRemoveButton(selected: MutableState<Boolean>) {
        val tooltipState = selected
            .map { if (it) deselectTooltip else selectTooltip }
            .map { it ?: "" }

        iconButton(
            modifier = Modifier.width(9f),
            icon = selected.map { if (it) EssentialPalette.CANCEL_5X else EssentialPalette.PLUS_5X },
            color = { EssentialPalette.TEXT },
            backgroundColor = { hovered ->
                if (hovered()) EssentialPalette.TEXT_DISABLED else EssentialPalette.BUTTON_HIGHLIGHT
            },
            tooltip = tooltipState,
        ).onLeftClick {
            selected.set(!selected.get())
        }
    }

    fun LayoutScope.iconButton(
        modifier: Modifier = Modifier,
        icon: State<ImageFactory>,
        tooltip: State<String>? = null,
        color: HoveredToColorStateBlock? = null,
        backgroundColor: HoveredToColorStateBlock? = null,
    ): IconButton {
        val button = IconButton(imageFactory = icon.get(), textShadow = false, iconShadow = false)
        val hoveredState = button.hoveredState().toV2()

        button.rebindIcon(icon.toV1(button))

        // These are optional, therefore we only want to rebind/set if they are non-null
        tooltip?.let { button.rebindTooltipText(tooltip.toV1(button)) }
        color?.let { button.rebindIconColor(stateBy { it(hoveredState) }.toV1(button)) }
        backgroundColor?.let { button.setColor(stateBy { it(hoveredState) }.toV1(button).toConstraint()) }

        return button(Modifier.heightAspect(1f).then(modifier))
    }

    fun build(modalManager: ModalManager) = SelectModal(
        modalManager,
        modalSimpleName,
        sections,
        requiresButtonPress,
        requiresSelection,
        shadowsOnEntries,
        initiallySelected,
        whenEmpty,
        extraContent,
    ).apply {
        titleText = title
        modalSettings.forEach(this::apply)
    }

    fun filterFriendsByActivity(predicate: (PlayerActivity) -> Boolean): ListState<UUID> {
        val mappedFriends = relationshipStates.getObservableFriendList()
            .toStateV2List()
            .mapEach { uuid ->
                uuid to statusStates.getActivityState(uuid).map { predicate(it) }
            }

        return stateBy {
            mappedFriends().mapNotNull { (uuid, isActivity) ->
                if (isActivity()) {
                    if (!socialStates.isSuspended(uuid)()) {
                        uuid
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        }.toListState()
    }
}

fun SelectModalBuilder<UUID>.onlinePlayers(block: SectionLayoutBlock<UUID> = defaultUserRow) =
    onlinePlayers({ it }, block)

fun SelectModalBuilder<UUID>.offlinePlayers(block: SectionLayoutBlock<UUID> = defaultUserRow) =
    offlinePlayers({ it }, block)

fun SelectModalBuilder<UUID>.friends(block: SectionLayoutBlock<UUID> = defaultUserRow) =
    friends({ it }, block)

fun SelectModalBuilder<UUID>.users(
    title: String,
    state: ListState<UUID>,
    block: SectionLayoutBlock<UUID> = defaultUserRow,
) =
    users(title, { it }, state, block)

fun SelectModalBuilder<Long>.groups(block: SectionLayoutBlock<Long> = defaultGroupRow) =
    groups({ it }, block)

fun SelectModalBuilder<Channel>.friendsAndGroups(block: SectionLayoutBlock<Channel> = defaultUserOrGroupRow) =
    friendsAndGroups({ it }, block)

/**
 * @param title: The title text for your modal
 */
fun <T> selectModal(
    modalManager: ModalManager,
    title: String,
    modalSimpleName: String,
    block: SelectModalBuilder<T>.() -> Unit = {}
) = SelectModalBuilder<T>(title, modalSimpleName)
    .apply(block)
    .build(modalManager)

/**
 * @param title: The title text for your modal
 */
suspend fun <T> ModalFlow.selectModal(
    title: String,
    modalSimpleName: String,
    block: SelectModalBuilder<T>.() -> Unit = {}
): Set<T>? = awaitModal { continuation ->
    SelectModalBuilder<T>(title, modalSimpleName)
        .modalSettings {
            onPrimaryAction { result -> replaceWith(continuation.resumeImmediately(result)) }
            onCancel { button -> if (button) replaceWith(continuation.resumeImmediately(null)) }
        }
        .apply(block)
        .build(modalManager)
}

private fun Channel.getOtherUser(): UUID? =
    if (type == ChannelType.DIRECT_MESSAGE) members.firstOrNull { it != USession.activeNow().uuid } else null
