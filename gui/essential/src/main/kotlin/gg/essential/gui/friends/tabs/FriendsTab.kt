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
package gg.essential.gui.friends.tabs

import gg.essential.elementa.UIComponent
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.FillConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.CopyConstraintFloat
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.bindChildren
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.friends.Tab
import gg.essential.gui.friends.message.SocialMenuActions
import gg.essential.gui.friends.previews.*
import gg.essential.gui.friends.state.PlayerActivity
import gg.essential.gui.friends.state.SocialStates
import gg.essential.network.connectionmanager.notices.SocialMenuNewFriendRequestNoticeManager
import gg.essential.util.scrollGradient
import kotlin.Comparator

class FriendsTab(
    selectedTab: State<Tab>,
    private val socialStates: SocialStates,
    private val socialMenuActions: SocialMenuActions,
    private val friendRequestNoticeManager: SocialMenuNewFriendRequestNoticeManager,
    private val tabsSelector: UIComponent,
    private val rightDivider: UIComponent,
) : TabComponent(Tab.FRIENDS, selectedTab) {

    private val horizontalDivider by UIBlock(EssentialPalette.COMPONENT_BACKGROUND).constrain {
        y = SiblingConstraint() boundTo tabsSelector
        width = 100.percent
        height = rightDivider.getWidth().pixels
    } childOf this

    private val sectionContainer by UIContainer().constrain {
        y = SiblingConstraint()
        width = 100.percent
        height = FillConstraint(useSiblings = false)
    } childOf this

    private val pendingSorter: Comparator<UIComponent> = compareBy(
        { (it as PendingUserEntry).incoming },
        {
            -(socialStates.relationships.getPendingRequestTime((it as PendingUserEntry).user)?.toEpochMilli()
                ?: 0)
        }
    )

    private val friendSorter: Comparator<UIComponent> = compareBy<UIComponent> {
        val activity = socialStates.activity.getActivity((it as BasicUserEntry).user)
        if (activity.isJoinable()) {
            return@compareBy 0L
        }
        when (activity) {
            is PlayerActivity.Multiplayer -> 0L
            is PlayerActivity.SPSSession -> if (activity.isJoinable()) 0L else 1L
            is PlayerActivity.OnlineWithDescription -> 1L
            PlayerActivity.Online -> 2L
            is PlayerActivity.Offline -> 3L
        }
    }.thenBy { (it as BasicUserEntry).usernameState.get() }

    private val blockedSorter: Comparator<UIComponent> = compareBy {
        (it as BasicUserEntry).usernameState.get()
    }

    private val friendSection by Section(UserEntryType.FRIEND) childOf sectionContainer
    private val firstDivider by createDivider(friendSection) childOf sectionContainer
    private val pendingSection by Section(UserEntryType.PENDING) childOf sectionContainer
    private val secondDivider by createDivider(pendingSection) childOf sectionContainer
    private val blockedSection by Section(UserEntryType.BLOCKED) childOf sectionContainer
    private val thirdScrollArea by UIContainer().constrain {
        y = CopyConstraintFloat() boundTo blockedSection
        height = CopyConstraintFloat() boundTo blockedSection
        width = 100.percent
    }.bindParent(rightDivider, active).also {
        blockedSection.setupScrollbar(it)
    }

    override val userLists: List<ScrollComponent> = listOf(
        friendSection.scrollList, pendingSection.scrollList, blockedSection.scrollList
    )

    private fun createDivider(section: Section): UIBlock {
        return UIBlock(EssentialPalette.COMPONENT_BACKGROUND).constrain {
            x = SiblingConstraint()
            width = rightDivider.getWidth().pixels
            height = 100.percent boundTo section
        }.also {
            section.setupScrollbar(it)
        }
    }

    override fun populate() {
        val relationshipStates = socialStates.relationships

        friendSection.scrollList.bindChildren(
            relationshipStates.getObservableFriendList(),
            comparator = friendSorter
        ) {
            FriendUserEntry(it, socialStates, socialMenuActions, friendSection)
        }

        blockedSection.scrollList.bindChildren(
            relationshipStates.getObservableBlockedList(),
            comparator = blockedSorter
        ) {
            BlockedUserEntry(it, socialMenuActions, blockedSection)
        }

        pendingSection.scrollList.bindChildren(
            relationshipStates.getObservableIncomingRequests(),
            comparator = pendingSorter
        ) {
            PendingUserEntry(it, true, socialStates, socialMenuActions, friendRequestNoticeManager, pendingSection)
        }

        pendingSection.scrollList.bindChildren(
            relationshipStates.getObservableOutgoingRequests(),
            comparator = pendingSorter
        ) {
            PendingUserEntry(it, false, socialStates, socialMenuActions, friendRequestNoticeManager, pendingSection)
        }
    }

    override fun sortUserLists() {
        friendSection.scrollList.sortChildren(friendSorter)
        blockedSection.scrollList.sortChildren(blockedSorter)
        pendingSection.scrollList.sortChildren(pendingSorter)
    }

    private inner class Section(private val type: UserEntryType) : UIContainer(), SortListener {
        private val text by EssentialUIText(type.sectionTitle).constrain {
            x = CenterConstraint()
            y = 8.pixels
            color = EssentialPalette.TEXT_HIGHLIGHT.toConstraint()
        } childOf this

        val scrollList by ScrollComponent(type.emptyText).constrain {
            x = CenterConstraint()
            y = SiblingConstraint(10f)
            width = 100.percent - 20.pixels
            height = FillConstraint(useSiblings = false)
        }.apply {
            emptyText.setColor(EssentialPalette.TEXT)
        } childOf this scrollGradient 20.pixels

        init {
            constrain {
                x = SiblingConstraint()
                width = (100.percent - (rightDivider.getWidth().pixels * 2)) / 3
                height = 100.percent
            }
        }

        fun setupScrollbar(parent: UIComponent) {
            val scrollbar = UIBlock(EssentialPalette.SCROLLBAR).constrain {
                width = 100.percent
            } childOf parent

            scrollList.setVerticalScrollBarComponent(scrollbar, hideWhenUseless = true)
        }

        override fun sort() {
            val filter: java.util.Comparator<UIComponent> = when (type) {
                UserEntryType.FRIEND -> friendSorter
                UserEntryType.PENDING -> pendingSorter
                UserEntryType.BLOCKED -> blockedSorter
            }
            scrollList.sortChildren(filter)
        }

    }

    private enum class UserEntryType(
        val sectionTitle: String,
        val emptyText: String,
    ) {
        FRIEND("Friend List", "No Friends"),
        PENDING("Friend Requests", "No Friend Requests"),
        BLOCKED("Blocked Players", "No Players Blocked")
    }
}