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
package gg.essential.gui.friends.message

import com.sparkuniverse.toolbox.chat.enums.ChannelType
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.common.constraints.CenterPixelConstraint
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.friends.state.SocialStates
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.heightAspect
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.util.hoveredState
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import java.util.*


class MessageTitleBar(
    private val messageScreen: MessageScreen,
    private val socialStates: SocialStates,
    private val socialMenuActions: SocialMenuActions,
) : UIContainer() {

    private val preview = messageScreen.preview

    private val text by EssentialUIText().bindText(preview.titleState.toV1(this)).constrain {
        x = 10.pixels
        y = CenterPixelConstraint()
    } childOf this

    private val memberContainer = UIContainer().constrain {
        x = SiblingConstraint(11f)
        y = CenterPixelConstraint()
        width = ChildBasedSizeConstraint()
        height = RelativeConstraint()
    }.bindParent(this, !preview.channel.isAnnouncement().state())

    private val dropdownOpen = BasicState(false)

    private val managementButton by IconButton(EssentialPalette.BURGER_7X5).constrain {
        x = 10.pixels(alignOpposite = true)
        y = CenterPixelConstraint()
        width = 17.pixels
        height = AspectConstraint()
    }.bindParent(this, !preview.channel.isAnnouncement().state())

    init {

        constrain {
            x = CopyConstraintFloat() boundTo messageScreen
            height = 100.percent
            width = 100.percent boundTo messageScreen
        }
        managementButton.setColor(
            EssentialPalette.getButtonColor(
                managementButton.hoveredState() or dropdownOpen,
                BasicState(true)
            ).toConstraint()
        )


        // Member container is not bound if this is an announcement channel
        memberContainer.bindChildren(
            socialStates.messages.getObservableMemberList(preview.channel.id),
            filter = { it != USession.activeNow().uuid }
        ) { uuid ->
            Member(uuid).also { member ->
                member.bindHoverEssentialTooltip(memo {
                    if (member.isMemberSuspendedState()) {
                        "Temporarily suspended from Essential social features"
                    } else {
                        UuidNameLookup.nameState(uuid, "Loading...")()
                    }
                }.toV1(member))
            }
        }

        // Only activates if not an announcement
        managementButton.onLeftClick {
            it.stopPropagation()
            dropdownOpen.set(true)

            socialMenuActions.showManagementDropdown(preview, ContextOptionMenu.Position(managementButton, true)) {
                dropdownOpen.set(false)
            }
        }

        val otherUser = preview.otherUser
        if (otherUser != null) {
            val activityState = socialStates.activity.getActivityState(otherUser)

            var imageIcon = EssentialPalette.JOIN_ARROW_10X5

            imageIcon = EssentialPalette.JOIN_ARROW_5X

            val button by IconButton(imageIcon, "Join Game").constrain {
                x = SiblingConstraint(3f, alignOpposite = true)
                y = CenterPixelConstraint()
            }.bindParent(this, activityState.map { it.isJoinable() })
            button.onLeftClick {
                USound.playButtonPress()
                socialMenuActions.joinSessionWithConfirmation(otherUser)
            }

            button
                .setLayout(IconButton.Layout.ICON_FIRST)
                .rebindIconColor(EssentialPalette.TEXT_HIGHLIGHT.state())
                .rebindTextColor(EssentialPalette.TEXT_HIGHLIGHT.state())
                .setDimension(IconButton.Dimension.FitWithPadding(18f, 10f))
                .setColor(
                    button.hoveredState()
                        .map { if (it) EssentialPalette.BLUE_BUTTON_HOVER else EssentialPalette.BLUE_BUTTON }
                        .toConstraint()
                )
        }

        if (!preview.channel.isAnnouncement() && preview.channel.type != ChannelType.GROUP_DIRECT_MESSAGE) {

            if (ServerType.current()?.supportsInvites == true) {

                val invited = BasicState(false)

                val textState = BasicState("Invite to Game")

                var imageIcon = EssentialPalette.ENVELOPE_10X7

                imageIcon = EssentialPalette.ENVELOPE_9X7

                val button by IconButton(imageIcon, textState).constrain {
                    x = SiblingConstraint(3f, alignOpposite = true)
                    y = CenterPixelConstraint()
                } childOf this
                button.onLeftClick {
                    if (!invited.get()) {
                        USound.playButtonPress()
                        invited.set(true)
                        socialMenuActions.invitePlayers(preview.channel.members, preview.titleState.getUntracked())
                    }
                }
                button.onMouseLeave {
                    invited.set(false)
                }

                button
                    .setLayout(IconButton.Layout.ICON_FIRST)
                    .rebindIconColor(EssentialPalette.TEXT_HIGHLIGHT.state())
                    .rebindTextColor(EssentialPalette.TEXT_HIGHLIGHT.state())
                    .setDimension(IconButton.Dimension.FitWithPadding(18f, 10f))
                    .setColor(
                        button.hoveredState()
                            .map { if (it) EssentialPalette.BLUE_BUTTON_HOVER else EssentialPalette.BLUE_BUTTON }
                            .toConstraint()
                    )

            }
        }
    }

    inner class Member(private val member: UUID) : UIContainer() {

        val isMemberSuspendedState = socialStates.isSuspended(member)

        init {
            constrain {
                width = ChildBasedSizeConstraint()
                height = ChildBasedSizeConstraint()
                x = SiblingConstraint(3f)
                y = CenterPixelConstraint()
            }

            layout {
                val headModifier = Modifier.width(16f).heightAspect(1f).shadow()
                if_(isMemberSuspendedState) {
                    headGrayscale(headModifier)
                } `else` {
                    head(headModifier)
                }
            }

            this.onMouseClick {
                if (it.mouseButton > 1) {
                    return@onMouseClick
                }
                if (member != USession.activeNow().uuid) {
                    val options = mutableListOf<ContextOptionMenu.Item>()
                    if (preview.channel.createdInfo.by == USession.activeNow().uuid && preview.channel.type == ChannelType.GROUP_DIRECT_MESSAGE) {
                        options.add(ContextOptionMenu.Option("Remove from group", image = EssentialPalette.CANCEL_5X) {
                            socialStates.messages.removeUser(preview.channel.id, member)
                        })
                    }
                    socialMenuActions.showUserDropdown(member, ContextOptionMenu.Position(this, false), options) {

                    }
                }
            }
        }

        private fun LayoutScope.head(modifier: Modifier = Modifier) {
            CachedAvatarImage.create(member)(modifier)
        }

        private fun LayoutScope.headGrayscale(modifier: Modifier = Modifier) {
            CachedAvatarImage.create(member, grayscale = true)(modifier)
        }
    }
}
