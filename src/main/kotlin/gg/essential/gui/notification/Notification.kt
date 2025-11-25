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
package gg.essential.gui.notification

import gg.essential.api.gui.NotificationType
import gg.essential.api.gui.Slot
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.AnimationComponent
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.pixels
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.layoutdsl.layout
import gg.essential.universal.USound
import gg.essential.gui.util.hoveredState
import gg.essential.util.thenAcceptOnMainThread
import gg.essential.vigilance.utils.onLeftClick
import java.util.concurrent.CompletableFuture

class Notification(
    val text: String,
    val title: String,
    private val timerEnabled: State<Boolean>,
    val duration: Float = 4f,
    val onClick: () -> Unit = {},
    val onClosed: () -> Unit = {},
    val trimTitle: Boolean = false,
    val trimMessage: Boolean = false,
    dismissNotification: CompletableFuture<Void?>,
    dismissNotificationInstantly: CompletableFuture<Void?>,
    val persistent: Boolean,
    val components: Map<Slot, UIComponent> = mutableMapOf(),
    val uniqueId: Any? = null,
    val type: NotificationType = NotificationType.GENERAL,
) : HighlightedBlock(
    backgroundColor = EssentialPalette.TOAST_BACKGROUND,
    highlightColor = EssentialPalette.TOAST_BORDER,
    highlightHoverColor = EssentialPalette.TOAST_BORDER_HOVER,
    clickBehavior = ClickBehavior.STAY_HIGHLIGHTED
) {
    private val hovered = hoveredState()

    private val timer = UIBlock(hovered.map {
        if (it) {
            EssentialPalette.NEW_TOAST_PROGRESS_HOVER
        } else {
            EssentialPalette.NEW_TOAST_PROGRESS
        }
    }).constrain {
        x = 0.pixels
        y = 0.pixels(alignOpposite = true)
        width = 0.pixels
        height = 3.pixels
    }

    private val timerAnimation = timer.makeAnimation()

    private var dragging = false

    private var dragStart = 0f

    private var draggedPixels = BasicState(0f)

    private var couldBeAClick = true

    private var didTriggerAction = false

    val content by UIContainer().apply {
            layout { notificationContent(title, text, type, trimTitle, trimMessage, components, persistent) }
        }.constrain {
        x = 7.pixels
        y = 7.pixels
        width = 100.percent - 14.pixels
        height = ChildBasedSizeConstraint()
    } childOf contentContainer

    init {
        constrain {
            x = 0.pixels(alignOpposite = true, alignOutside = true)
            y = SiblingConstraint(alignOpposite = true) - 5.pixels
            width = 175.pixels
            height = ChildBasedSizeConstraint()
        }

        parentContainer.constrain {
            width = RelativeConstraint()
            height = ChildBasedMaxSizeConstraint() + 2.pixels
        }

        contentContainer.constrain {
            height = ChildBasedSizeConstraint() + 11.pixels
        }

        timer childOf contentContainer

        onLeftClick {
            dragging = true
            clickAction(it)
            dragStart = it.relativeX
            couldBeAClick = true
            clicked = false // We don't yet know! If this was a click it will be handled on release.
        }

        onMouseRelease {
            if (dragging) {
                dragging = false

                val draggedPercentage = (draggedPixels.get() / 170 * 100).coerceIn(0f..100f)

                when {
                    couldBeAClick -> {
                        if (didTriggerAction) return@onMouseRelease

                        // Was a click
                        onClick()
                        clicked = true
                        didTriggerAction = true
                        USound.playButtonPress()
                        animateCompleteTimerNow()
                    }
                    draggedPercentage < 25 -> springBack()
                    else -> animateOutFast()
                }
            }
        }

        onMouseDrag { mouseX, _, _ ->
            if (dragging) {
                draggedPixels.set { (it + mouseX - dragStart).coerceAtLeast(0f) }
                val draggedPercentage = (draggedPixels.get() / 170 * 100).coerceIn(0f..100f)
                if (draggedPercentage > 2) {
                    couldBeAClick = false
                }
            }
        }
        dismissNotification.thenAcceptOnMainThread {
            animateCompleteTimerNow()
            animateOut()
        }

        dismissNotificationInstantly.thenAcceptOnMainThread {
            dismissInstantly()
        }

        if (persistent) {
            IconButton(EssentialPalette.CANCEL_5X, buttonShadow = false).constrain {
                x = 5.pixels(alignOpposite = true)
                y = 5.pixels
                width = 11.pixels
                height = AspectConstraint()
                color = EssentialPalette.TOAST_BACKGROUND.toConstraint()
            }.onLeftClick {
                animateOut()
                USound.playButtonPress()
            } childOf parentContainer
        }
    }

    private fun animateCompleteTimerNow() {
        if (persistent) {
            animateOut()
            return
        }
        val widthAnim = timerAnimation.width as? AnimationComponent<*> ?: return
        val percentComplete = widthAnim.getPercentComplete()
        timer.setWidth(RelativeConstraint(percentComplete))
        widthAnim.stopIfSupported()

        timer.animate {
            setWidthAnimation(Animations.OUT_EXP, (1f - percentComplete) * 1.25f, RelativeConstraint())
            onComplete {
                animateOut()
            }
        }
    }

    fun dismissInstantly() {
        parent.removeChild(this@Notification)
        onClosed()
    }

    override fun highlight() {
        if (clicked)
            return
        super.highlight()
    }

    override fun unhighlight() {
        if (clicked)
            return
        super.unhighlight()
    }

    fun animateIn() {
        animate {
            setXAnimation(Animations.OUT_EXP, 0.5f, 2.pixels(alignOpposite = true) + draggedPixels.pixels())
            onComplete {
                animateTimer()
            }
        }
    }

    private fun animateTimer() {
        // If the toast is persistent, don't animate the timer
        if (persistent) {
            return
        }

        timerAnimation
            .setWidthAnimation(Animations.LINEAR, duration, RelativeConstraint(), 0.5f)
            .begin()
            .onComplete {
                animateOut()
            }

        (timerEnabled and !hoveredState()).onSetValueAndNow { enabled ->
            if (enabled) {
                timerAnimation.width.resumeIfSupported()
            } else {
                timerAnimation.width.pauseIfSupported()
            }
        }
    }

    private fun animateOut() {
        animate {
            setXAnimation(Animations.IN_EXP, 0.5f, 0.pixels(alignOpposite = true, alignOutside = true), 0.5f)

            onComplete {
                animate {
                    setHeightAnimation(Animations.OUT_EXP, 0.25f, 0.pixels)

                    onComplete {
                        dismissInstantly()
                    }
                }
            }
        }
    }

    private fun animateOutFast() {
        animate {
            setXAnimation(Animations.OUT_EXP, 0.5f, 0.pixels(alignOpposite = true, alignOutside = true))

            onComplete {
                animate {
                    setHeightAnimation(Animations.OUT_EXP, 0.25f, 0.pixels)

                    onComplete {
                        parent.removeChild(this@Notification)
                        onClosed()
                    }
                }
            }
        }
    }

    private fun springBack() {
        animate {
            setXAnimation(Animations.OUT_EXP, 0.5f, 2.pixels(alignOpposite = true))
            onComplete {
                draggedPixels.set(0f)
                setX(2.pixels(alignOpposite = true) + draggedPixels.pixels())
            }
        }
    }

    override fun isHovered(): Boolean {
        return gg.essential.api.utils.GuiUtil.getOpenedScreen() != null && super.isHovered()
    }
}
