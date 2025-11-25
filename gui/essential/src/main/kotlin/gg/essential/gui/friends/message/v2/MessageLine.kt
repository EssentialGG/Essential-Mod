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

import gg.essential.config.EssentialConfig
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.CopyConstraintColor
import gg.essential.elementa.dsl.*
import gg.essential.elementa.events.UIClickEvent
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.FadeEffect
import gg.essential.gui.common.or
import gg.essential.gui.elementa.state.v2.combinators.or
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateDelegatingTo
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.friends.message.MessageUtils
import gg.essential.gui.util.hoveredState
import gg.essential.mod.Skin
import gg.essential.util.*
import java.net.URL
import java.time.Instant
import java.util.concurrent.TimeUnit

fun getGuiScaleOffset(): Float {
    return if (EssentialConfig.enlargeSocialMenuChatMetadata) {
        0f
    } else {
        -1f
    }
}

/**
 * Parent type for all components directly added to the scroller for a channel.
 * Current implementations are [MessageWrapper] and [UnreadDivider].
 */
sealed class MessengerElement : UIContainer()

/**
 * Divides messages (such as date and new messages) from each other.
 */
sealed class Divider(
    val timeStamp: Instant,
) : MessengerElement()

/**
 * A divider that is used to separate previously viewed messages from new, unread messages
 */
abstract class UnreadDivider(
    timeStamp: Instant,
) : Divider(timeStamp)

/**
 * A divider that is used to separate messages by date
 */
abstract class DateDivider(
    timeStamp: Instant,
    val unread: State<Boolean>,
) : Divider(timeStamp)

/**
 * The message line is the parent component for all content displayed in a [MessageWrapper]
 * relating to a specific message. For example, content can be [ParagraphLine], [ImageEmbed],
 * or [OutfitEmbed]
 */
sealed class MessageLine(
    val messageWrapper: MessageWrapper,
) : UIContainer() {

    /**
     * Called to highlight this component briefly. A default implementation is provided
     * that calls [beginHighlight] and [releaseHighlight] with a hold time controlled by
     * [highlightAnimationHoldTime]
     */
    open fun flashHighlight() {
        beginHighlight()
        delay(((highlightAnimationTransitionDuration + highlightAnimationHoldTime) * 1000).toLong()) {
            releaseHighlight()
        }
    }

    /**
     * Called to highlight this component until [releaseHighlight] is called
     */
    abstract fun beginHighlight()

    /**
     * Called to indicate this component should no longer be highlighted and return
     * to its normal color
     */
    abstract fun releaseHighlight()

    companion object {
        /**
         * The amount of time in seconds that the highlight animation should take to transition from
         * oen state to the other
         */
        const val highlightAnimationTransitionDuration = 0.25f

        /**
         * The amount of time in seconds that the highlight animation should hold before
         * transitioning back to the normal color
         */
        const val highlightAnimationHoldTime = 0.25f

    }
}


/**
 * Wrapper class around message. All instances of [MessageLine] pertaining
 * to [message] will be within this component.
 */
abstract class MessageWrapper(
    val message: ClientMessage,
): MessengerElement() {

    val showTimestamp = mutableStateOf(true)
    val sender = message.sender
    val sentByClient = sender == USession.activeNow().uuid
    val sendTime = message.sendTime
    val channelType = message.channel.type
    val sendingMessageAlpha = 0.7f

    protected val dropdownOpen = mutableStateOf(false)
    protected val actionButtonHovered = stateDelegatingTo(mutableStateOf(false))

    val appearHovered = actionButtonHovered or dropdownOpen

    init {
        if (message.sendState == SendState.Sending) {
            effect(FadeEffect(EssentialPalette.GUI_BACKGROUND, sendingMessageAlpha))
        }
    }

    abstract fun delete()

    abstract fun addComponent(line: MessageLine)

    fun previousSiblingIsSameSenderWithinAMinute(): Boolean {
        val siblings = parent.children
        val minute = TimeUnit.MINUTES.toMillis(1)
        val indexInParent = siblings.indexOf(this) - 1
        if (indexInParent in siblings.indices) {
            val comp = siblings[indexInParent]
            return (comp is MessageWrapper && comp.message.sender == message.sender && (message.sendTime.toEpochMilli() - comp.message.sendTime.toEpochMilli() <= minute))

        }
        return false
    }

    /**
     * Called when the user right-clicks on the message or one of the message's components
     */
    abstract fun openOptionMenu(event: UIClickEvent, component: MessageLine)

    /**
     * Called when the user clicks on the context of a reply and the message screen
     * scrolls to this component. This should briefly highlight the component
     */
    abstract fun flashHighlight()

    /**
     * Called when the user clicks on the retry button for a failed message.
     * This should remove this component from the message screen and re-send the message
     */
    abstract fun retrySend()
}

sealed class MessageBubble(wrapper: MessageWrapper) : MessageLine(wrapper) {

    private val verticalPadding = 16f
    private val horizontalPadding = MessageUtils.messagePadding
    private val hoveredState = BasicState(false).map { it }

    val colorState = EssentialPalette.getMessageColor(hoveredState or wrapper.appearHovered.toV1(this), wrapper.sentByClient, wrapper.message.sendState).map { it }

    val bubble by UIBlock().constrain {
        x = 0.pixels(alignOpposite = wrapper.sentByClient)
        width = ChildBasedMaxSizeConstraint() + horizontalPadding.pixels()
        height = ChildBasedMaxSizeConstraint() + verticalPadding.pixels()
        color = colorState.toConstraint()
    }.apply {
        hoveredState.rebind(hoveredState())
    } childOf this

    val tail by UIBlock().constrain {
        x = 0.pixels(alignOpposite = wrapper.sentByClient, alignOutside = true)
        y = 0.pixels(alignOpposite = true)
        width = MessageUtils.dotWidth.pixels()
        height = AspectConstraint()
        color = CopyConstraintColor() boundTo bubble
    }.apply {
        if (wrapper.message.sendState == SendState.Sending) {
            effect(FadeEffect(EssentialPalette.GUI_BACKGROUND, wrapper.sendingMessageAlpha))
        }
    } childOf bubble
}

/**
 * Contains text sent by a user
 */
abstract class ParagraphLine(
    val messageContent: String,
    wrapper: MessageWrapper,
) : MessageBubble(wrapper) {
    abstract val selectedText: String
}


/**
 * An embed that displays an image located at [url]
 */
abstract class ImageEmbed(
    val url: URL,
    wrapper: MessageWrapper,
) : MessageLine(wrapper) {

    abstract fun copyImageToClipboard()

    abstract fun saveImageToScreenshotBrowser()

}

abstract class GiftEmbed(
    wrapper: MessageWrapper,
) : MessageBubble(wrapper)

abstract class SkinEmbed(
    val skin: Skin,
    wrapper: MessageWrapper,
) : MessageBubble(wrapper)
