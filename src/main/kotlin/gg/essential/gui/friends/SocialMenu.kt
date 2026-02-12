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
package gg.essential.gui.friends

import com.sparkuniverse.toolbox.chat.model.Channel
import gg.essential.Essential
import gg.essential.api.gui.GuiRequiresTOS
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.gui.InternalEssentialGUI
import gg.essential.gui.common.ContextOptionMenu
import gg.essential.gui.common.bindConstraints
import gg.essential.gui.common.constraints.CenterPixelConstraint
import gg.essential.gui.elementa.essentialmarkdown.EssentialMarkdown
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.friends.message.SocialMenuActions
import gg.essential.gui.friends.message.SocialMenuState
import gg.essential.gui.friends.modals.BlockConfirmationModal
import gg.essential.gui.friends.modals.ConfirmJoinModal
import gg.essential.gui.friends.modals.FriendRemoveConfirmationModal
import gg.essential.gui.friends.previews.ChannelPreview
import gg.essential.gui.friends.state.SocialStateManager
import gg.essential.gui.friends.state.SocialStates
import gg.essential.gui.friends.tabs.ChatTab
import gg.essential.gui.friends.tabs.FriendsTab
import gg.essential.gui.friends.title.SocialTitleManagementActions
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.modals.communityRulesModal
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.sendOutgoingSpsInviteNotification
import gg.essential.gui.notification.warning
import gg.essential.gui.util.onItemAdded
import gg.essential.universal.UMinecraft
import gg.essential.util.*
import gg.essential.util.GuiUtil.launchModalFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class SocialMenu(
    channelIdToOpen: Long? = null
): InternalEssentialGUI(
    ElementaVersion.V6,
    "Social",
    discordActivityDescription = "Messaging friends",
), GuiRequiresTOS, SocialMenuActions {

    private val connectionManager = Essential.getInstance().connectionManager
    private val referenceHolder = ReferenceHolderImpl()

    val isScreenOpen = screenOpen

    val socialStateManager = SocialStateManager(connectionManager)

    var selectedTab = mutableStateOf(Tab.CHAT)

    private val socialMenuState = object : SocialMenuState, SocialStates by socialStateManager {
        override val tab: State<Tab>
            get() = selectedTab
    }

    val tabsSelector by TabsSelector(
        selectedTab,
        connectionManager.socialMenuNewFriendRequestNoticeManager.unseenFriendRequestCount()
    ).constrain {
        width = 215.pixels.coerceAtMost(50.percent).coerceAtLeast(ChildBasedSizeConstraint())
        height = 27.pixels
    } childOf content

    val chatTab by ChatTab(
        selectedTab,
        socialStateManager,
        this,
        tabsSelector,
        titleBar,
        rightDivider,
        isScreenOpen,
    )
    val friendsTab by FriendsTab(
        selectedTab,
        socialStateManager,
        this,
        connectionManager.socialMenuNewFriendRequestNoticeManager,
        tabsSelector,
        rightDivider,
    )
    private val titleManagementActions by SocialTitleManagementActions(
        selectedTab,
        socialStateManager,
        this,
    ).constrain {
        y = CenterPixelConstraint()
    }.bindConstraints(selectedTab) {
        x = if (it == Tab.CHAT) {
            10.pixels(alignOpposite = true) boundTo tabsSelector
        } else {
            10.pixels(alignOpposite = true)
        }
    } childOf titleBar

    private val tabs = listOf(chatTab, friendsTab)

    /**
     * The channel ID of the channel that was open when messenger states reset.
     */
    private var channelToRestore: Long? = null

    init {
        titleManagementActions.search.textContent.onSetValue { username ->
            tabs.forEach {
                it.search(username)
            }
        }
        chatTab.populate()
        friendsTab.populate()

        content.layout {
            bind(selectedTab) { tab ->
                when (tab) {
                    Tab.CHAT -> chatTab()
                    Tab.FRIENDS -> friendsTab()
                }
            }
        }

        if (channelIdToOpen != null) {
            val preview = chatTab[connectionManager.chatManager.mergeAnnouncementChannel(channelIdToOpen)]
            if (preview != null) {
                openMessageScreen(preview)
            } else {
                Essential.logger.error("Unable to find channel with ID $channelIdToOpen")
            }
        } else {
            chatTab.openTopChannel()
        }

        socialStateManager.messengerStates.registerResetListener {
            channelToRestore = chatTab.currentMessageView.get()?.preview?.channel?.id
        }

        socialStateManager.messengerStates.getObservableChannelList().onItemAdded {
            if (it.id == channelToRestore) {

                channelToRestore = null

                // In order to ensure this functions correctly, two delays is required. The order that
                // observers are called is not guaranteed the Java spec, and the call to openMessageScreen
                // must be after the chatTab has processed channels after connect.
                Window.enqueueRenderOperation {
                    Window.enqueueRenderOperation {
                        openMessageScreen(it)
                    }
                }
            }
        }

        effect(referenceHolder) {
            val rulesManager = connectionManager.rulesManager
            if (isScreenOpen() && rulesManager.hasRules() && !rulesManager.acceptedRules) {
                launchModalFlow {
                    if (!communityRulesModal(rulesManager, UMinecraft.getSettings().language)) {
                        // FIXME this double withContext is a workaround for EM-3456
                        withContext(Dispatchers.IO) {
                            withContext(Dispatchers.Client) {
                                restorePreviousScreen()
                            }
                        }
                    }
                }
            }
        }
    }

    // Prevents multiple EssentialMarkdown components from having a selection at the same time
    // Linear: EM-1973
    override fun onMouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) {
        if (mouseButton != 0 || selectedTab.get() != Tab.CHAT) {
            super.onMouseClicked(mouseX, mouseY, mouseButton)
            return
        }

        // A general floating check would be better, but Elementa has no way to do that at the moment.
        val isHittingContextMenu = window
            .hitTest(mouseX.toFloat(), mouseY.toFloat())
            .findParentOfTypeOrNull<ContextOptionMenu>() != null

        if (!isHittingContextMenu) {
            chatTab.findChildrenOfType<EssentialMarkdown>(recursive = true).forEach {
                it.clearSelection()
            }
        }

        super.onMouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun updateGuiScale() {
        newGuiScale = GuiUtil.getGuiScale()
        super.updateGuiScale()
    }

    fun openMessageScreen(preview: ChannelPreview) {
        chatTab.openMessage(preview)
        selectedTab.set(Tab.CHAT)
    }

    override fun openMessageScreen(channel: Channel) {
        chatTab[channel.id]?.let { openMessageScreen(it) }
    }

    override fun openMessageScreen(user: UUID) {
        val channelPreview = chatTab[user]
        if (channelPreview != null) {
            openMessageScreen(channelPreview)
        }
    }

    override fun showManagementDropdown(
        preview: ChannelPreview,
        position: ContextOptionMenu.Position,
        extraOptions: List<ContextOptionMenu.Item>,
        onClose: () -> Unit
    ) = showManagementDropdown(window, socialMenuState, this, preview, position, extraOptions, onClose)

    override fun showUserDropdown(
        user: UUID,
        position: ContextOptionMenu.Position,
        extraOptions: List<ContextOptionMenu.Item>,
        onClose: () -> Unit
    ) = showUserDropdown(window, socialMenuState, this, user, position, extraOptions, onClose)

    override fun joinSessionWithConfirmation(user: UUID) {
        if (UMinecraft.getWorld() != null) {
            if (!socialStateManager.statusStates.joinSession(user)) {
                Notifications.warning("World invite expired", "")
            }
            return
        }

        UUIDUtil.getName(user).thenAcceptOnMainThread {
            val isSps = connectionManager.spsManager.remoteSessions.any { it.hostUUID == user }
            GuiUtil.pushModal { manager ->
                ConfirmJoinModal(manager, it, isSps).onPrimaryAction {
                    if (!socialStateManager.statusStates.joinSession(user)) {
                        Notifications.warning("World invite expired", "")
                    }
                }
            }
        }
    }

    override fun invitePlayers(users: Set<UUID>, name: String) {
        val currentServerData = UMinecraft.getMinecraft().currentServerData

        val spsManager = connectionManager.spsManager
        if (spsManager.localSession != null) {
            spsManager.reinviteUsers(users)
            sendOutgoingSpsInviteNotification(name)
        } else if (currentServerData != null) {
            connectionManager.socialManager.reinviteFriendsOnServer(currentServerData.serverIP, users)
        }
    }

    override fun blockOrUnblock(uuid: UUID) {
        UUIDUtil.getName(uuid).thenAcceptOnMainThread {
            val block = !socialStateManager.relationships.isBlocked(uuid)
            val blockText = if (block) {
                "Block"
            } else {
                "Unblock"
            }
            GuiUtil.pushModal { manager ->
                BlockConfirmationModal(manager, it, blockText).onPrimaryAction {
                    if (block) {
                        socialStateManager.relationshipStates.blockPlayer(uuid, true)
                    } else {
                        socialStateManager.relationshipStates.unblockPlayer(uuid, true)
                    }
                }
            }
        }
    }

    override fun addOrRemoveFriend(uuid: UUID) {
        UUIDUtil.getName(uuid).thenAcceptOnMainThread {
            if (socialStateManager.relationships.isFriend(uuid)) {
                GuiUtil.pushModal { manager ->
                    FriendRemoveConfirmationModal(manager, it).onPrimaryAction {
                        socialStateManager.relationshipStates.removeFriend(uuid, false)
                    }
                }
            } else {
                socialStateManager.relationshipStates.addFriend(uuid, false)
            }

        }
    }

    companion object {

        @JvmStatic
        fun getInstance(): SocialMenu? {
            return GuiUtil.openedScreen() as? SocialMenu
        }
    }
}
