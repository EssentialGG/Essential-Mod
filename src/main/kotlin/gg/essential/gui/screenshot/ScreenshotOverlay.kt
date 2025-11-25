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
package gg.essential.gui.screenshot

import gg.essential.Essential
import gg.essential.config.EssentialConfig
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.constraints.XConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.animate
import gg.essential.elementa.dsl.basicHeightConstraint
import gg.essential.elementa.dsl.basicWidthConstraint
import gg.essential.elementa.dsl.basicXConstraint
import gg.essential.elementa.dsl.basicYConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.max
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.percentOfWindow
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.elementa.dsl.times
import gg.essential.elementa.dsl.toConstraint
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.elementa.utils.invisible
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.ImageLoadCallback
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.common.or
import gg.essential.gui.common.shadow.ShadowIcon
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.fillHeight
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.layoutAsColumn
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.modals.ensurePrerequisites
import gg.essential.gui.overlay.Layer
import gg.essential.gui.overlay.LayerPriority
import gg.essential.gui.screenshot.ScreenshotOverlay.animating
import gg.essential.gui.screenshot.components.ScreenshotBrowser
import gg.essential.gui.screenshot.components.shareScreenshotModal
import gg.essential.gui.screenshot.constraints.AspectPreservingFillConstraint
import gg.essential.gui.screenshot.toast.ScreenshotPreviewAction
import gg.essential.gui.screenshot.toast.ScreenshotPreviewActionSlot
import gg.essential.gui.util.hoveredState
import gg.essential.universal.UResolution
import gg.essential.universal.USound
import gg.essential.util.GuiUtil
import gg.essential.util.Multithreading
import gg.essential.util.bindEssentialTooltip
import gg.essential.util.centered
import gg.essential.util.div
import gg.essential.util.times
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color
import java.io.File

object ScreenshotOverlay {
    var animating = true

    fun pauseAll() {
        animating = false
    }

    fun resumeAll() {
        animating = true
    }

    /** see [Layer.rendered] */
    private var rendered: Boolean = true
        set(value) {
            field = value
            instance?.layer?.rendered = value
        }

    fun hide() {
        rendered = false
    }

    fun show() {
        rendered = true
    }

    internal var instance: ScreenshotOverlayInstance? = null
    private fun instance(): ScreenshotOverlayInstance {
        return instance ?: run {
            val layer = GuiUtil.addLayer(LayerPriority.Notifications)
            layer.rendered = rendered
            layer.window.addUpdateFunc { _, _ ->
                if (layer.window.children.isEmpty()) {
                    GuiUtil.removeLayer(layer)
                    instance = null
                }
            }
            ScreenshotOverlayInstance(layer)
        }.also { instance = it }
    }

    fun push(file: File) = instance().push(file)
    fun hasActiveNotifications(): Boolean = instance?.layer?.window?.children?.isNotEmpty() ?: false
    fun clearScreenshot(screenshotToast: ScreenshotToast) = instance?.clearScreenshot(screenshotToast)
    fun delete(file: File) = instance?.delete(file)
}

internal class ScreenshotOverlayInstance(val layer: Layer) {
    private val window = layer.window

    fun push(file: File) {
        pushToast(ScreenshotPreviewToast(file))
    }

    private fun pushToast(toast: ScreenshotToast) {
        // Drop new screenshots if too many are taken in a short period
        if (window.children.size > 3) {
            return
        }
        toast childOf window

        if (window.children.size > 3) {
            clearScreenshot(window.children.first() as ScreenshotToast)
        }
    }

    fun clearScreenshot(screenshotToast: ScreenshotToast) {
        val index = window.children.indexOf(screenshotToast)
        screenshotToast.animateAway {
            window.childrenOfType(ScreenshotToast::class.java).forEach {
                if (index < window.children.indexOf(it)) {
                    it.animateUp()
                }
            }
        }
    }

    /**
     * Called when a file is deleted by the user so that the
     * toast can be removed from the UI.
     */
    fun delete(file: File) {
        val component = window.children.firstOrNull {
            it is ScreenshotPreviewToast && it.file.name == file.name
        } ?: return
        clearScreenshot(component as ScreenshotToast)
    }
}

private const val TOP_PADDING = 5f
private const val RIGHT_PADDING = 5f

open class ScreenshotToast : UIContainer() {
    private var animatingAway = false

    init {
        constrain {
            width = 10.percentOfWindow * 2
            x = RIGHT_PADDING.pixels(alignOpposite = true)
            y = max(SiblingConstraint(TOP_PADDING), TOP_PADDING.pixels)
        }
    }

    fun animateAway(callback: () -> Unit) {
        if (animatingAway) {
            return
        }
        animatingAway = true

        val targetConstraint: XConstraint = 100.percent

        // Elementa will never execute the `onComplete` block of an animation if
        // another animation is started before the first finishes. However, the
        // animation will visually reach 100% and stay there. Therefore, we
        // use this work around to ensure that the callback is executed and
        // this component is removed from the window's children when the animation
        // reaches 100%.
        //
        // This issue becomes apparent if the screenshot above this one finishes
        // animating away while this one is animating away because both screenshots
        // were taken in quick succession.
        addUpdateFunc { _, _ ->
            if (getLeft() == targetConstraint.getXPosition(this)) {
                callback()
                Window.of(this@ScreenshotToast).removeChild(this@ScreenshotToast)
            }
        }

        animate {
            setXAnimation(Animations.IN_EXP, 0.5f, targetConstraint)
        }
    }


    fun animateUp() {
        // Once the previous sibling is removed from the component tree, SiblingConstraint
        // will instantly update the Y value it returns. As a result, the "start" value
        // that is interpolated between changes and the animation doesn't work as desired.
        // Therefore, we fix the Y starting position wherever the component is now to use that
        // as the starting location for the animation.
        setY(getTop().pixels)

        animate {
            setYAnimation(Animations.OUT_EXP, 0.5f, max(SiblingConstraint(TOP_PADDING), TOP_PADDING.pixels))
        }
    }

}

class ScreenshotPreviewToast(val file: File) : ScreenshotToast() {

    private val screenshotId = LocalScreenshot(file.toPath())
    private val aspectRatio = BasicState(UResolution.scaledWidth / UResolution.scaledHeight.toFloat())
    private var timeMsRemaining = -1
    private val hovered = hoveredState()
    private val favoriteState = BasicState(false)
    private val favoriteIcon = favoriteState.map {
        if (it) {
            EssentialPalette.HEART_FILLED_9X
        } else {
            EssentialPalette.HEART_EMPTY_9X
        }
    }
    private val favoriteTooltip = favoriteState.map {
        if (it) {
            "Remove Favorite"
        } else {
            "Favorite"
        }
    }
    private val background by UIBlock(Color.WHITE).constrain {
        width = AspectPreservingFillConstraint(aspectRatio)
        height = 100.percent
    } childOf this

    private val image by UIImage.ofFile(file).constrain {
        color = Color.WHITE.invisible().toConstraint()
        width = AspectPreservingFillConstraint(aspectRatio)
        height = 100.percent
    } childOf this

    private val hoverComponent by UIBlock(Color(0, 0, 0, 100)).constrain {
        width = 100.percent
        height = 100.percent
    }.bindParent(this, hovered)

    init {
        constrain {
            height = ChildBasedSizeConstraint()
        }

        val topLeft by getManageActionForSlot(ScreenshotPreviewActionSlot.TOP_LEFT)
        val topRight by getManageActionForSlot(ScreenshotPreviewActionSlot.TOP_RIGTH)
        val bottomLeft by getManageActionForSlot(ScreenshotPreviewActionSlot.BOTTOM_LEFT)
        val bottomRight by getManageActionForSlot(ScreenshotPreviewActionSlot.BOTTOM_RIGHT)

        val halfHeightModifier = Modifier.fillWidth().fillHeight(0.5f)
        val halfWidthModifier = Modifier.fillWidth(0.5f).fillHeight()

        hoverComponent.layoutAsColumn {
            row(halfHeightModifier) {
                topLeft(halfWidthModifier)
                topRight(halfWidthModifier)
            }
            row(halfHeightModifier) {
                bottomLeft(halfWidthModifier)
                bottomRight(halfWidthModifier)
            }
        }

        aspectRatio.onSetValueAndNow {
            setHeight(AspectConstraint(1 / it))
        }

        favoriteState.onSetValue {
            Essential.getInstance().connectionManager.screenshotManager.setFavorite(file.toPath(), it)
        }

        hovered.onSetValue {
            if (it) {
                ScreenshotOverlay.pauseAll()
            } else {
                ScreenshotOverlay.resumeAll()
            }
        }
        enableEffect(OutlineEffect(EssentialPalette.TEXT, 1f))
    }

    override fun afterInitialization() {
        super.afterInitialization()
        // Delayed because there is a race condition between the class initializing and the image loading.
        // If the image loads first, then the animate call inside the block will fail because this component
        // is not yet part of a widow.
        image.supply(ImageLoadCallback {
            aspectRatio.set(this.width / this.height.toFloat())
            image.animate {
                setColorAnimation(Animations.LINEAR, 0.5f, Color.WHITE.toConstraint())
            }
            timeMsRemaining = EssentialConfig.screenshotToastDuration.seconds * 1000
        })
    }

    private fun getManageActionForSlot(slot: ScreenshotPreviewActionSlot): UIComponent {
        return when (slot.action) {
            ScreenshotPreviewAction.COPY_PICTURE -> {
                ManageAction("Copy Picture", EssentialPalette.COPY_9X).onLeftClick {
                    Multithreading.runAsync {
                        copyScreenshotToClipboard(file.toPath())
                    }
                }
            }

            ScreenshotPreviewAction.COPY_LINK -> {
                ManageAction("Copy Link", EssentialPalette.LINK_8X7).onLeftClick {
                    clear()

                    val connectionManager = Essential.getInstance().connectionManager

                    val upload: () -> Unit = { connectionManager.screenshotManager.uploadAndCopyLinkToClipboard(file.toPath()) }

                    GuiUtil.launchModalFlow {
                        ensurePrerequisites()
                        upload()
                    }
                }
            }

            ScreenshotPreviewAction.FAVORITE -> {
                ManageAction(favoriteTooltip, favoriteIcon).apply {
                    imageColor.rebind((hovered or favoriteState).map { if (it) EssentialPalette.TEXT_RED else EssentialPalette.TEXT })
                }.onLeftClick {
                    favoriteState.set { !it }
                }
            }

            ScreenshotPreviewAction.DELETE -> {
                ManageAction("Delete", EssentialPalette.TRASH_9X).apply {
                    imageColor.rebind(hovered.map { if (it) EssentialPalette.TEXT_RED else EssentialPalette.TEXT })
                }.onLeftClick {
                    Essential.getInstance().connectionManager.screenshotManager.handleDelete(file, false)
                }
            }

            ScreenshotPreviewAction.SHARE -> {
                ManageAction("Send to Friends", EssentialPalette.SOCIAL_10X).onLeftClick {
                    GuiUtil.launchModalFlow { shareScreenshotModal(screenshotId) }
                }
            }

            ScreenshotPreviewAction.EDIT -> {
                ManageAction("Edit", EssentialPalette.EDIT_10X7).onLeftClick {
                    clear()
                    GuiUtil.openScreen { ScreenshotBrowser(file.toPath()) }
                }
            }
        }
    }

    inner class ManageAction(
        tooltip: State<String>,
        icon: State<ImageFactory>,
    ) : UIBlock() {

        constructor(tooltip: String, icon: ImageFactory) : this(
            BasicState(tooltip),
            BasicState(icon)
        )

        val hovered = hoveredState()

        private val unscaled by ShadowIcon(icon, BasicState(true))

        private val imageContainer by UIContainer().centered().constrain {
            width = basicWidthConstraint { it.getHeight() / unscaled.getHeight() * unscaled.getWidth() }
            // For purposes of calculating scale factor, the image icon is assumed to be
            // 7 pixels tall. That way, all icons will have the same scale
            height = 25.percent / 7.pixels * basicHeightConstraint {
                unscaled.getHeight().coerceAtLeast(1f)
            }
        } childOf this

        val imageColor = EssentialPalette.getTextColor(hovered).map { it }

        init {

            bindEssentialTooltip(hoveredState(), tooltip, windowPadding = RIGHT_PADDING)

            setColor(hovered.map {
                if (it) {
                    Color(255, 255, 255, 50)
                } else {
                    Color(0, 0, 0, 0)
                }
            }.toConstraint())

            onLeftClick {
                USound.playButtonPress()
            }
            icon.onSetValueAndNow { factory ->
                imageContainer.clearChildren()
                // Main image
                val image by factory.create().constrain {
                    width = 100.percent
                    height = 100.percent
                    color = imageColor.toConstraint()
                }  // Parent defined after shadow to avoid shadow being drawn on top of image

                // Shadow
                factory.create().constrain {
                    width = 100.percent
                    height = 100.percent
                    x = basicXConstraint {
                        image.getLeft() + image.getWidth() / unscaled.getWidth()
                    }
                    y = basicYConstraint {
                        image.getTop() + image.getHeight() / unscaled.getHeight()
                    }
                    color = EssentialPalette.TEXT_SHADOW.toConstraint()
                } childOf imageContainer

                image childOf imageContainer
            }
        }
    }

    private fun clear() {
        ScreenshotOverlay.clearScreenshot(this)
    }

    init {
        addUpdateFunc { _, dtMs -> update(dtMs) }
    }

    private fun update(dtMs: Int) {
        if (animating && timeMsRemaining > 0) {
            timeMsRemaining -= dtMs
            if (timeMsRemaining <= 0) {
                clear()
            }
        }
    }

}
