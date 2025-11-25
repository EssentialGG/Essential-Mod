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

import com.sparkuniverse.toolbox.chat.model.Channel
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.dsl.basicColorConstraint
import gg.essential.elementa.dsl.basicHeightConstraint
import gg.essential.elementa.dsl.coerceAtMost
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.utils.roundToRealPixels
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.input.UIMultilineTextInput
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.friends.message.screenshot.ScreenshotAttacher
import gg.essential.gui.friends.message.screenshot.ScreenshotAttachmentManager
import gg.essential.gui.friends.message.screenshot.ScreenshotPicker
import gg.essential.gui.friends.message.screenshot.screenshotAttachmentUploadBox
import gg.essential.gui.friends.message.v2.ClientMessage
import gg.essential.gui.friends.state.SocialStates
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.warning
import gg.essential.universal.ChatColor
import gg.essential.universal.UKeyboard
import gg.essential.universal.USound
import gg.essential.util.UuidNameLookup
import gg.essential.util.scrollGradient
import gg.essential.vigilance.utils.onLeftClick

class MessageInput(
    channel: Channel,
    isScreenOpen: State<Boolean>,
    socialStates: SocialStates,
    private val channelName: State<String>,
    private val replyTo: MutableState<ClientMessage?>,
    private val editingMessage: MutableState<ClientMessage?>,
    private val scrollToMessage: (ClientMessage) -> Unit,
    private val onSendAction: (String) -> Unit,
    private val onEditAction: (String) -> Unit,
) : UIContainer() {

    private val isReplying = replyTo.map { it != null }
    private val isEditing = editingMessage.map { it != null }
    private val actionBarData: State<Pair<String, ImageFactory>?> = memo {
        if (replyTo() != null) {
            "Replying to" to EssentialPalette.REPLY_7X5
        } else if (editingMessage() != null) {
            "Editing Message" to EssentialPalette.PENCIL_7x7
        } else {
            null
        }
    }

    private var stashedMessage: String? = ""

    private val usernameState = memo { replyTo()?.sender?.let(UuidNameLookup::nameState)?.invoke() ?: "Nobody" }

    private val message = mutableStateOf("")
    private val messageLength = memo { message().length }

    private val input = UIMultilineTextInput(shadowColor = EssentialPalette.TEXT_SHADOW_LIGHT).apply {
        effect(this) {
            placeholder = "Message ${channelName()}"
        }
        linePadding = 3f
        setColor(basicColorConstraint {
            if (getText().isEmpty()) {
                EssentialPalette.TEXT
            } else {
                EssentialPalette.TEXT_HIGHLIGHT
            }
        })
        onKeyType { keyChar, keyCode ->
            // Cancel reply on escape
            if (keyCode == UKeyboard.KEY_ESCAPE) {
                if (isReplying.get()) {
                    replyTo.set(null)
                } else if (isEditing.get()) {
                    editingMessage.set(null)
                } else {
                    Window.of(this).keyTypedListeners.forEach { it(keyChar, keyCode) }
                }
            }

            if (UKeyboard.isEnterKey(keyCode)) {

                if (!UKeyboard.isShiftKeyDown()) {
                    handleSendMessage()
                }

            }

            message.set(this@apply.getText())
        }
    }

    // FIXME: Kotlin emits invalid bytecode if this is `val`, see https://youtrack.jetbrains.com/issue/KT-48757
    private var scrollComponent: ScrollComponent

    private var cachedCursorRelativeYPosition = 0f

    val screenshotAttachmentManager = ScreenshotAttachmentManager(channel, isScreenOpen, socialStates)

    init {

        effect(this) {
            if (replyTo() != null) {
                grabFocus()
            }
        }

        effect(this) {
            val message = editingMessage()
            if (message != null) {
                grabFocus()
                if (stashedMessage == null) {
                    stashedMessage = input.getText()
                }
                input.setText(message.contents)
            } else {
                stashedMessage?.let { text -> input.setText(text) }
                stashedMessage = null
            }
        }

        var screenshotPicker: ScreenshotPicker? = null

        screenshotAttachmentManager.isPickingScreenshots.onSetValue(this) {
            Window.enqueueRenderOperation {
                if (screenshotAttachmentManager.isPickingScreenshots.get()) {
                    screenshotPicker?.grabWindowFocus()
                } else {
                    screenshotPicker?.releaseWindowFocus()
                    grabFocus()
                }
            }
        }

        fun Modifier.limitHeight() = this then {
            val originalHeightConstraint = constraints.height
            constraints.height = originalHeightConstraint.coerceAtMost(134.pixels)
            return@then { constraints.height = originalHeightConstraint }
        }

        fun Modifier.limitScreenshotPickerHeight() = this then BasicHeightModifier {
            basicHeightConstraint { component ->
                this@MessageInput.parent.getHeight() * 0.85f - component.parent.children
                    .filter { it != component }.map { it.getHeight() }.sum()
            }
        }

        fun Modifier.maxSiblingHeight() = this then BasicHeightModifier {
            basicHeightConstraint { it.parent.children.maxOfOrNull { child -> if (child === it) 0f else child.getHeight() } ?: 1f }
        }

        val scrollBar: UIComponent

        val replyColorModifier = Modifier.color(EssentialPalette.TEXT_MID_GRAY).shadow(EssentialPalette.TEXT_SHADOW)
        val replyUsernameColorModifier = Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(EssentialPalette.TEXT_SHADOW)
        val replyBarColor = screenshotAttachmentManager.isConfirmingScreenshots.map {
            if (it) EssentialPalette.COMPONENT_BACKGROUND
            else EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT
        }

        this.layout(Modifier.fillWidth().childBasedHeight().alignVertical(Alignment.End)) {
            column(Modifier.fillWidth()) {

                if_(screenshotAttachmentManager.isPickingScreenshots) {
                    screenshotPicker = ScreenshotPicker(screenshotAttachmentManager)(
                        Modifier.fillWidth().limitScreenshotPickerHeight()
                    )
                }

                // Everything else
                column(Modifier.fillWidth(padding = 7f)) {

                    if_(screenshotAttachmentManager.isUploading, cache = false) {
                        box(Modifier.childBasedHeight(7f).alignHorizontal(Alignment.End)) {
                            screenshotAttachmentUploadBox(screenshotAttachmentManager)
                        }
                    }

                    //Reply/Edit banner
                    ifNotNull(actionBarData) { (actionBarMessage, actionBarIcon) ->
                        row(Modifier.fillWidth().height(17f).color(replyBarColor), Arrangement.SpaceBetween) {
                            row(Modifier.alignVertical(Alignment.Center(true))) {
                                spacer(width = 10f)
                                icon(actionBarIcon, replyColorModifier)
                                spacer(width = 4f)
                                text(actionBarMessage, replyColorModifier)
                                if_(isReplying) {
                                    spacer(width = 5f)
                                    text(usernameState, replyUsernameColorModifier)
                                }
                            }
                            row {
                                //Make a wider box for easier targeting
                                box(Modifier.childBasedSize(3f).hoverScope()) {
                                    image(
                                        EssentialPalette.CANCEL_5X,
                                        Modifier
                                            .color(EssentialPalette.TEXT).hoverColor(EssentialPalette.TEXT_HIGHLIGHT)
                                            .hoverTooltip("Cancel"),
                                    )
                                }.onLeftClick {
                                    if (isReplying.get()) {
                                        replyTo.set(null)
                                    } else if (isEditing.get()) {
                                        editingMessage.set(null)
                                    }
                                    USound.playButtonPress()
                                }
                                spacer(width = 8f)
                            }

                        }.onLeftClick {
                            val message = replyTo.get() ?: editingMessage.get()
                            message?.let { scrollToMessage(it) }
                        }
                    }

                    // Extra content/banners go here?

                    if_(screenshotAttachmentManager.isConfirmingScreenshots) {
                        ScreenshotAttacher(screenshotAttachmentManager)()
                    }

                    // Main input field
                    box(Modifier.fillWidth().childBasedMaxHeight().color(EssentialPalette.COMPONENT_BACKGROUND)) {
                        row(Modifier.fillWidth()) {
                            // put a spacer here, where the icon below will be
                            spacer(width = 37f)
                            scrollComponent = scrollable(Modifier.fillRemainingWidth().limitHeight(), vertical = true) {
                                box(Modifier.fillWidth().childBasedHeight(10f)) {
                                    input(Modifier.fillWidth())
                                }
                            }
                            spacer(width = 67f)
                            box(Modifier.width(2f).maxSiblingHeight()) {
                                scrollBar = box(Modifier.fillWidth().color(EssentialPalette.SCROLLBAR))
                            }
                        }
                        scrollGradient(scrollComponent, true, 30f, maxGradient = 153)
                        scrollGradient(scrollComponent, false, 30f, maxGradient = 153)
                        // Place the icon last, so it is above the gradients
                        IconButton(EssentialPalette.PICTURES_SHORT_9X7, tooltipText = "Attach Pictures")
                            .setDimension(IconButton.Dimension.Fixed(17f, 17f))(
                            Modifier
                                .width(17f).height(17f)
                                .alignHorizontal(Alignment.Start(10f)).alignVertical(Alignment.Start(6f))
                                .color(EssentialPalette.GRAY_BUTTON)
                        ).onActiveClick {
                            screenshotAttachmentManager.isPickingScreenshots.set { !it }
                        }
                        characterLimit(Modifier
                            .alignHorizontal(Alignment.End(11f))
                            .alignVertical(Alignment.End(10f))
                        )
                    }.onLeftClick {
                        screenshotAttachmentManager.isPickingScreenshots.set(false)
                    }
                }.onLeftClick {
                    grabFocus()
                }
                spacer(height = 7f)
            }
        }

        scrollComponent.setVerticalScrollBarComponent(scrollBar, hideWhenUseless = true)
    }

    init {
        addUpdateFunc { _, _ -> adjustScroll() }
    }

    private fun adjustScroll() {
        val cursorPositionRelativeToInput = input.cursor.toScreenPos().second.roundToRealPixels()

        if (cursorPositionRelativeToInput == cachedCursorRelativeYPosition) {
            return
        }

        cachedCursorRelativeYPosition = cursorPositionRelativeToInput

        val cursorScreenPosition = cursorPositionRelativeToInput + input.getTop()

        val topBorder = scrollComponent.getTop() + 30f
        val bottomBorder = scrollComponent.getBottom() - input.cursorHeight - 30f

        if (topBorder >= bottomBorder) {
            return // Return if the input is too small to do this
        }

        val coercedCursorScreenPosition = cursorScreenPosition.coerceIn(topBorder, bottomBorder)
        val offsetRequired = coercedCursorScreenPosition - cursorScreenPosition

        scrollComponent.scrollTo(verticalOffset = scrollComponent.verticalOffset + offsetRequired, smoothScroll = false)
    }

    fun grabFocus() {
        input.grabWindowFocus()
    }

    private fun handleSendMessage() {
        var text = input.getText()

        val charLimit = MESSAGE_CHAR_LIMIT
        if (text.length > charLimit) {
            Notifications.warning("Too many characters", "You have exceeded the\n$charLimit character limit.")
            return
        }
        //Keep calling the replacement until there are no more changes
        //&sect&sect;; will pass the filter because on the first pass
        // &sect; is replaced but still leaves &sect;
        var previous = text
        var modified = false
        do {
            for (colorSymbol in colorSymbols) {
                text = text.replace(colorSymbol, "")
            }
            if (text != previous) {
                previous = text
                modified = true
            } else {
                modified = false
            }
        } while (modified)

        if (isEditing.get()) {
            onEditAction(text)
        } else {
            onSendAction(text)
            screenshotAttachmentManager.uploadAndSend()
            input.setText("")
        }
    }

    private fun LayoutScope.characterLimit(modifier: Modifier = Modifier) {
        if_({ messageLength() >= MESSAGE_CHAR_WARNING }) {
            text({ "${MESSAGE_CHAR_LIMIT - messageLength()}" }, Modifier
                .color { if (messageLength() > MESSAGE_CHAR_LIMIT) EssentialPalette.TEXT_WARNING else EssentialPalette.TEXT }
                .shadow { EssentialPalette.BLACK }
                .hoverScope()
                .hoverTooltip({ if (messageLength() <= MESSAGE_CHAR_LIMIT) "Remaining characters" else "Message is too long" }, position = EssentialTooltip.Position.ABOVE)
                .then(modifier)
            )
        }
    }

    override fun afterInitialization() {
        grabFocus()
    }

    companion object {

        private const val MESSAGE_CHAR_LIMIT = 2500
        private const val MESSAGE_CHAR_WARNING = 2000

        val colorSymbols = listOf("&sect;", "&#xA7", "&#167", "${ChatColor.COLOR_CHAR}")
    }

}
