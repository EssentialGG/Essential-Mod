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
package gg.essential.gui.sps.options

import gg.essential.Essential
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.elementa.utils.getStringSplitToWidth
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.common.input.StateTextInput
import gg.essential.gui.common.modal.DangerConfirmationEssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.common.shadow.EssentialUIWrappedText
import gg.essential.gui.common.shadow.ShadowIcon
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.util.hoveredState
import gg.essential.mixins.transformers.server.integrated.LanConnectionsAccessor
import gg.essential.universal.USound
import gg.essential.upnp.UPnPPrivacy
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import net.minecraft.client.Minecraft
import java.util.*

class SpsOption(
    val information: SettingInformation,
) : UIBlock(EssentialPalette.BUTTON_HIGHLIGHT) {

    private val background by UIBlock(EssentialPalette.COMPONENT_BACKGROUND).centered().constrain {
        width = 100.percent - (outlineWidth * 2).pixels
        height = max(ChildBasedMaxSizeConstraint() + (contentPadding * 2).pixels, (40 - 2 * outlineWidth).pixels)
    } childOf this

    private val paddedContent by UIContainer().constrain {
        width = 100.percent - (contentPadding * 2).pixels
        height = ChildBasedMaxSizeConstraint()
    }.centered() childOf background

    private val actionContent by UIContainer().constrain {
        x = 0.pixels(alignOpposite = true)
        y = CenterConstraint()
        width = ChildBasedSizeConstraint()
        height = ChildBasedMaxSizeConstraint()
    } childOf paddedContent

    private val textContent by UIContainer().constrain {
        width = FillConstraint() - 8.pixels // Add some extra padding so text can't be right next to actions
        height = ChildBasedSizeConstraint()
        y = CenterConstraint()
    } childOf paddedContent

    init {
        constrain {
            width = 100.percent
            height = ChildBasedSizeConstraint() + (outlineWidth * 2).pixels
            y = SiblingConstraint(7f)
        }

        when (information) {
            is SettingInformation.SettingWithDescription -> {
                val titleLine by UIContainer().constrain {
                    width = 100.percent
                    height = ChildBasedMaxSizeConstraint()
                } childOf textContent

                val title by EssentialUIWrappedText(information.title).constrain {
                    width = 100.percent
                } childOf titleLine

                val pinState = information.pinState
                if (pinState != null) {
                    title.setColor(pinState.pinned.map {
                        if (it) {
                            EssentialPalette.ITEM_PINNED
                        } else {
                            EssentialPalette.TEXT_HIGHLIGHT
                        }
                    }.toConstraint())
                    title.bindShadowColor(pinState.pinned.map { if (it) EssentialPalette.BLUE_SHADOW else EssentialPalette.TEXT_SHADOW_LIGHT })
                    val pinIcon by ShadowIcon(EssentialPalette.UNPINNED_8X, true).constrain {
                        x = basicXConstraint {
                            val textScale = title.getTextScale()
                            val firstLine = getStringSplitToWidth(
                                title.getText(),
                                title.getWidth(),
                                textScale,
                                ensureSpaceAtEndOfLines = false,
                                fontProvider = title.getFontProvider()
                            ).first().trimEnd()
                            title.getLeft() + firstLine.width(textScale, fontProvider) / textScale
                        } + 5.pixels

                        y = 0.pixels boundTo title
                    }.apply {
                        val hovered = hoveredState()
                        rebindIcon(hovered.or(pinState.pinned).map {
                            if (it) {
                                EssentialPalette.PINNED_8X
                            } else {
                                EssentialPalette.UNPINNED_8X
                            }
                        })
                        rebindPrimaryColor(pinState.pinned.zip(hovered).map { (pinned, hovered) ->
                            if (hovered) {
                                EssentialPalette.TEXT_HIGHLIGHT
                            } else if (pinned) {
                                EssentialPalette.ITEM_PINNED
                            } else {
                                EssentialPalette.TEXT
                            }
                        })
                        rebindShadowColor(pinState.pinned.map { if (it) EssentialPalette.BLUE_SHADOW else EssentialPalette.TEXT_SHADOW_LIGHT })
                        bindHoverEssentialTooltip(pinState.pinned.map {
                            if (it) {
                                "Unpin"
                            } else {
                                "Pin"
                            }
                        })
                        onLeftClick {
                            USound.playButtonPress()
                            pinState.pinned.set { !it }
                        }
                    } childOf paddedContent
                }

                val description by EssentialUIWrappedText(information.description, shadowColor = EssentialPalette.TEXT_SHADOW_LIGHT).constrain {
                    width = 100.percent
                    y = SiblingConstraint(5f)
                    color = EssentialPalette.TEXT.toConstraint()
                } childOf textContent
            }

            is SettingInformation.SettingWithOptionalTooltip -> {
                val title by EssentialUIText(information.title).constrain {
                    y = CenterConstraint()
                } childOf textContent

                if (information.tooltip != null) {
                    val infoBlock by HoverableInfoBlock(BasicState(information.tooltip)).constrain {
                        x = SiblingConstraint(5f)
                        y = CenterConstraint() boundTo title
                    } childOf textContent
                }
            }

            is SettingInformation.Player -> {

                val playerInfo by UIContainer().constrain {
                    y = CenterConstraint()
                    width = ChildBasedSizeConstraint()
                    height = ChildBasedMaxSizeConstraint()
                } childOf textContent

                val playerHead by CachedAvatarImage.create(information.uuid).constrain {
                    y = CenterConstraint()
                    width = 24.pixels
                    height = AspectConstraint(1f)
                } childOf playerInfo


                val title by EssentialUIText().bindText(UUIDUtil.getNameAsState(information.uuid, "Loading..."))
                    .constrain {
                        x = SiblingConstraint(7f)
                        y = 2.pixels
                    } childOf playerInfo

                val onSession = Essential.getInstance().connectionManager.spsManager.getOnlineState(information.uuid).toV1(this)

                val descriptionText = if (information.uuid == UUIDUtil.getClientUUID()) {
                    BasicState("Host")
                } else {
                    onSession.map {
                        if (it) {
                            "Playing"
                        } else {
                            "Invited"
                        }
                    }
                }

                val description by EssentialUIText().bindText(descriptionText).constrain {
                    x = CopyConstraintFloat() boundTo title
                    y = SiblingConstraint(5f)
                    color = onSession.map {
                        if (it) {
                            EssentialPalette.MESSAGE_SENT
                        } else {
                            EssentialPalette.TEXT
                        }
                    }.toConstraint()
                } childOf playerInfo
            }
        }
    }

    companion object {
        private const val contentPadding = 9
        const val outlineWidth = 1

        /**
         * Creates an SPS option with the text content defined by [settingInformation]
         * with a dropdown of [options] initialized to [initialSelection]
         */
        fun <T> createDropdownOption(
            settingInformation: SettingInformation,
            initialSelection: T,
            options: ListState<EssentialDropDown.Option<T>>,
            onSelect: (T) -> Unit
        ): SpsOption {
            return SpsOption(settingInformation).apply {
                // A new container is used here with a fixed height so that
                // the height of actionContent is not affected by the dropdown expanding
                val dropdownContainer by UIContainer().constrain {
                    width = ChildBasedSizeConstraint()
                    height = 17.pixels
                } childOf actionContent

                val dropdown by EssentialDropDown(initialSelection, options) childOf dropdownContainer

                dropdown.selectedOption.onSetValue(dropdown) { onSelect(it.value) }
            }
        }

        /**
         * Creates an SPS option with the text content defined by [settingInformation]
         * with a toggle initially set to [initialValue] that calls [onSetValue] when toggled.
         */
        fun createToggleOption(
            settingInformation: SettingInformation,
            enabledState: State<Boolean>,
        ): SpsOption {
            return SpsOption(settingInformation).apply {
                val toggle by FullEssentialToggle(
                    enabledState.toV2()
                ) childOf actionContent
            }
        }

        /**
         * Creates an SPS option with the text content defined by [settingInformation]
         * with a number input initially set to [initialSelection] that calls [onSetValue] when changed.
         */
        fun createNumberOption(
            settingInformation: SettingInformation,
            numberState: State<Int>
        ): SpsOption {
            return SpsOption(settingInformation).apply {
                val outlineBlock by UIBlock(EssentialPalette.BUTTON_HIGHLIGHT).constrain {
                    width = ChildBasedSizeConstraint() + 2.pixels
                    height = 17.pixels
                } childOf actionContent

                val contentBlock by UIBlock(EssentialPalette.COMPONENT_BACKGROUND).constrain {
                    width = ChildBasedSizeConstraint() + 6.pixels
                    height = 100.percent - 2.pixels
                }.centered() childOf outlineBlock

                val input by StateTextInput(
                    numberState.toV2(),
                    mutable = true,
                    textPadding = 0f,
                    maxLength = 11,
                    formatToText = { it.toString() },
                ) {
                    try {
                        it.toInt()
                    } catch (e: NumberFormatException) {
                        throw StateTextInput.ParseException()
                    }
                }.centered() childOf contentBlock

            }
        }

        /**
         * Creates an SPS option with the content defined by [player]
         * with kick and op buttons
         */
        fun createPlayerOption(
            player: SettingInformation.Player,
            cheatsEnabled: State<Boolean>,
        ): SpsOption {
            return SpsOption(player).apply {
                val connectionManager = Essential.getInstance().connectionManager
                val spsManager = connectionManager.spsManager
                val isHost = BasicState(player.uuid == UUIDUtil.getClientUUID())
                val onlineState = spsManager.getOnlineState(player.uuid)

                fun removePlayerFromSession() {
                    spsManager.updateInvitedUsers(spsManager.invitedUsers - player.uuid)
                    // Updating the invited users is not sufficient to kick this player if the privacy is set to friends
                    if (onlineState.getUntracked() && spsManager.localSession!!.privacy == UPnPPrivacy.FRIENDS) {
                        val integratedServer = Minecraft.getMinecraft().integratedServer ?: return
                        integratedServer.executor.execute {

                            (integratedServer.playerList as LanConnectionsAccessor).playerEntityList.firstOrNull {
                                it.uniqueID == player.uuid
                            }?.let { player ->
                                //#if MC>=11200
                                player.connection?.disconnect(textLiteral("The host kicked you from the session"))
                                //#else
                                //$$ player.playerNetServerHandler.kickPlayerFromServer(
                                //$$     "The host kicked you from the session"
                                //$$ )
                                //#endif
                            }

                        }
                    }
                    hide(instantly = true)
                }

                val removeButton by IconButton(EssentialPalette.CANCEL_5X).constrain {
                    x = 0.pixels(alignOpposite = true)
                    y = CenterConstraint()
                    width = 17.pixels
                    height = AspectConstraint(1f)
                }.apply {
                    rebindIconColor(hoveredState().map {
                        if (it) {
                            EssentialPalette.RED
                        } else {
                            EssentialPalette.TEXT
                        }
                    })
                    rebindTooltipText(onlineState.map {
                        if (it) {
                            "Remove"
                        } else {
                            "Cancel invite"
                        }
                    }.toV1(this))
                }.onLeftClick {
                    if (onlineState.get()) {
                        UUIDUtil.getName(player.uuid).thenAcceptOnMainThread { username ->
                            GuiUtil.pushModal { manager ->
                                SPSRemovePlayerModal(manager, username).onPrimaryAction {
                                    removePlayerFromSession()
                                }
                            }
                        }
                    } else {
                        removePlayerFromSession()
                    }
                }.bindParent(actionContent, !isHost)

                val isOp = BasicState(player.uuid in spsManager.oppedPlayers)

                cheatsEnabled.onSetValue {
                    isOp.set(player.uuid in spsManager.oppedPlayers)
                }

                val opButton by IconButton(EssentialPalette.OP_7X5).constrain {
                    width = CopyConstraintFloat() boundTo removeButton
                    height = AspectConstraint(1f)
                    x = SiblingConstraint(3f, alignOpposite = true)
                    y = CenterConstraint()
                }.apply {
                    rebindIconColor(isOp.zip(hoveredState()).zip(isHost).map { (opHovered, host) ->
                        val (op, hovered) = opHovered
                        if (hovered && !host) {
                            EssentialPalette.MESSAGE_SENT_HOVER
                        } else if (op || host) {
                            EssentialPalette.MESSAGE_SENT
                        } else {
                            EssentialPalette.TEXT
                        }
                    })

                    rebindTooltipText(isOp.zip(isHost).map { (op, host) ->
                        if (host) {
                            "The host is always OP when cheats are enabled"
                        } else if (op) {
                            "Remove Operator"
                        } else {
                            "Make Operator"
                        }
                    })

                    onLeftClick {
                        isOp.set { !it }
                    }

                }.bindParent(actionContent, cheatsEnabled)

                isOp.onSetValue {
                    if (it) {
                        spsManager.updateOppedPlayers(spsManager.oppedPlayers + player.uuid)
                    } else {
                        spsManager.updateOppedPlayers(spsManager.oppedPlayers - player.uuid)
                    }
                }
            }
        }
    }
}

sealed class SettingInformation {

    /**
     * Returns true when the option containing this setting
     * should be displayed given [searchText]
     */
    abstract fun matchesFilter(searchText: String): Boolean

    data class Player(val uuid: UUID) : SettingInformation() {

        override fun matchesFilter(searchText: String): Boolean {
            return UUIDUtil.getName(uuid).getNow(null)?.contains(searchText, true) ?: false
        }
    }

    /**
     * A setting that has a title and a description that can be pinned by supplying a non-null value for [pinState]
     */
    data class SettingWithDescription(
        val title: String,
        val description: String,
        val pinState: PinState?,
    ) : SettingInformation() {

        override fun matchesFilter(searchText: String): Boolean {
            return title.contains(searchText, ignoreCase = true) || description.contains(searchText, ignoreCase = true)
        }
    }

    data class SettingWithOptionalTooltip(val title: String, val tooltip: String? = null) : SettingInformation() {

        override fun matchesFilter(searchText: String): Boolean {
            return title.contains(searchText, ignoreCase = true)
        }
    }
}

/**
 * Keeps track of whether the current setting is pinned.
 */
data class PinState(
    /** Denotes whether the component this setting is for is the pinned version. */
    val isPinnedComponent: Boolean,
    /** Denotes whether the setting is pinned.*/
    val pinned: State<Boolean>,
)

class SPSRemovePlayerModal(manager: ModalManager, username: String) : DangerConfirmationEssentialModal(
    manager,
    "Remove",
    requiresButtonPress = false
) {
    init {
        configure {
            titleText = "Remove"
            contentText = "Are you sure you want to kick ${username}?"
        }
    }
}
