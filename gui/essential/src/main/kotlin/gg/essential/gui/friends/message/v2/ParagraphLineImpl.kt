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
package gg.essential.gui.friends.message.v2

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.constraints.MarkdownContentWidthConstraint
import gg.essential.gui.common.modal.OpenLinkModal
import gg.essential.gui.elementa.essentialmarkdown.EssentialMarkdown
import gg.essential.gui.elementa.essentialmarkdown.drawables.HeaderDrawable
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.friends.message.MessageUtils
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.hoverTooltip
import gg.essential.gui.layoutdsl.image
import gg.essential.gui.layoutdsl.layoutAsBox
import gg.essential.gui.layoutdsl.shadow
import gg.essential.util.hiddenChildOf
import gg.essential.util.isAnnouncement
import java.net.URI
import java.net.URISyntaxException

class ParagraphLineImpl(
    private val wrapper: MessageWrapper,
    messageContent: String,
) : ParagraphLine(
    messageContent,
    wrapper,
) {

    private val message = wrapper.message
    private val announcementMessage = message.channel.isAnnouncement()
    private val cleanedText = messageContent

    private val markdownConfig = when {
        message.sendState == SendState.Failed -> MessageUtils.failedMessageMarkdownConfig
        message.sendState is SendState.Blocked -> MessageUtils.blockedMessageMarkdownConfig
        message.channel.isAnnouncement() -> MessageUtils.fullMarkdownConfig
        wrapper.sentByClient -> MessageUtils.outgoingMessageMarkdownConfig
        else -> MessageUtils.incomingMessageMarkdownConfig
    }

    private val retryButton by IconButton(EssentialPalette.RETRY_7X, "", "Retry").constrain {
        x = 0.pixels(alignOpposite = true)
        y = CenterConstraint()
        width = 17.pixels
        height = AspectConstraint()
    }.onActiveClick {
        wrapper.retrySend()
    }.bindParent(this, BasicState(wrapper.message.sendState == SendState.Failed && wrapper.sentByClient))

    // This component contains the same content as visibleMessageComponent
    // and is hidden from the component tree to prevent rendering. It is used
    // to calculate the width of the message container allowing for a much simpler
    // constraint setup around the relationship between this ParagraphLineImpl, messageContainer,
    // and visibleMessageComponent
    private val invisibleMessageSizeComponent by EssentialMarkdown(
        cleanedText,
        markdownConfig,
    ).constrain {
        width = (100.percent - MessageUtils.messagePadding.pixels).coerceAtLeast(
            if (announcementMessage) 60.pixels else 8.pixels
        )
    } hiddenChildOf this

    private val messageContainer by UIContainer().constrain {
        x = CenterConstraint()
        y = 9.pixels
        height = ChildBasedSizeConstraint()
        width = MarkdownContentWidthConstraint() boundTo invisibleMessageSizeComponent
    } childOf bubble

    private val visibleMessageComponent by EssentialMarkdown(
        cleanedText,
        markdownConfig,
    ).constrain {
        width = 100.percent
        color = when (message.sendState) {
            SendState.Confirmed -> {
                if (wrapper.sentByClient) {
                    EssentialPalette.SENT_MESSAGE_TEXT
                } else {
                    EssentialPalette.RECEIVED_MESSAGE_TEXT
                }
            }
            SendState.Sending -> EssentialPalette.PENDING_MESSAGE_TEXT
            SendState.Failed -> EssentialPalette.FAILED_MESSAGE_TEXT
            is SendState.Blocked -> EssentialPalette.TEXT_DISABLED
        }.toConstraint()
    } childOf messageContainer

    private val blockedInfo by UIBlock().constrain {
        x = bubble.constraints.x - 21.pixels
        y = CenterConstraint()
        width = 17.pixels
        height = AspectConstraint()
        color = EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT.toConstraint()
    }.layoutAsBox(if (message.sendState is SendState.Blocked) Modifier.hoverScope().hoverTooltip((message.sendState as SendState.Blocked).tooltipMessage, position = EssentialTooltip.Position.ABOVE) else Modifier) {
        image(EssentialPalette.ROUND_WARNING_7X, Modifier.color(EssentialPalette.RED).shadow(EssentialPalette.TEXT_SHADOW))
    }.bindParent(this, stateOf(wrapper.message.sendState is SendState.Blocked))

    init {
        constrain {
            width = MessageUtils.getMessageWidth(announcementMessage)
            height = ChildBasedMaxSizeConstraint()
        }

        visibleMessageComponent.addUpdateFunc { _, _ ->
            visibleMessageComponent.drawables.filterIsInstance<HeaderDrawable>().forEach {
                it.dividerWidth = visibleMessageComponent.maxTextLineWidth.toDouble()
            }
        }

        visibleMessageComponent.onLinkClicked { event ->
            try {
                OpenLinkModal.openUrl(URI.create(event.url))
                event.stopImmediatePropagation()
            } catch (e: URISyntaxException) {
                // Ignored, if the link is invalid we just do nothing
            }
        }

        bubble.onMouseClick {
            if (it.mouseButton != 1) {
                return@onMouseClick
            }
            wrapper.openOptionMenu(it, this@ParagraphLineImpl)
        }

    }

    override val selectedText: String
        get() = visibleMessageComponent.drawables.selectedText(false)

    override fun beginHighlight() {
        bubble.animate {
            setColorAnimation(Animations.OUT_EXP, highlightAnimationTransitionDuration, EssentialPalette.MESSAGE_HIGHLIGHT.toConstraint())
        }
    }

    override fun releaseHighlight() {
        bubble.animate {
            setColorAnimation(Animations.IN_EXP, highlightAnimationTransitionDuration, colorState.toConstraint())
        }
    }
}
