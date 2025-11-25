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
package gg.essential.gui.screenshot.editor

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.UINanoVG
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.onChange
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.RemoteScreenshot
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.editor.change.CropChange
import gg.essential.gui.screenshot.editor.change.EditHistory
import gg.essential.gui.screenshot.editor.change.VectorStroke
import gg.essential.gui.screenshot.image.ScreenshotImage
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.network.connectionmanager.media.IScreenshotManager
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution
import gg.essential.util.UIdentifier
import gg.essential.util.animateColor
import gg.essential.util.lwjgl3.api.nanovg.NanoVG
import gg.essential.vigilance.gui.VigilancePalette
import kotlinx.coroutines.Dispatchers
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Can be improved by abstracting cropping functions to a cropping [Tool] class
 */
class ScreenshotCanvas(
    val screenshot: State<UIdentifier?>,
    val editHistory: EditHistory,
) : UIContainer() {
    var onDraw: UIContainer.(Float, Float, Int) -> Unit = { _, _, _ -> }

    var mouseButton = -1

    private val padding = 2

    // screenshot [UIImage] which is currently being edited
    val screenshotDisplay = object : UIContainer() {

        override fun mouseMove(window: Window) {
            if (mouseButton != -1) {
                val (x, y) = getMousePosition()
                onDraw(x, y, mouseButton)
            }

            super.mouseMove(window)
        }

        override fun draw(matrixStack: UMatrixStack) {
            vectorEditingOverlay.draw(matrixStack)
            super.draw(matrixStack)
        }

    }.onMouseClick { event ->
        mouseButton = event.mouseButton
        event.stopPropagation()
    }.onMouseRelease {
        mouseButton = -1
    }.constrain {
        height = 100.percent() - (padding * 2).pixels()
        width = 100.percent() - (padding * 2).pixels()
        x = padding.pixels()
        y = padding.pixels()
    } childOf this


    val background = UIBlock(VigilancePalette.getModalBackground()).constrain {
        height = 100.percent()
        width = 100.percent()
    } childOf screenshotDisplay

    val draggingCrop = mutableStateOf<Crop?>(null)
    val cropSettings: State<Crop> = memo {
        draggingCrop()?.let { return@memo it }
        editHistory.history()
            .asReversed()
            .firstNotNullOfOrNull { (it as? CropChange)?.new }
            ?: Crop.DEFAULT
    }

    val retainedImage by UIContainer().constrain {
        x = basicXConstraint { it.parent.getLeft() + it.parent.getWidth() * cropSettings.getUntracked().left }
        y = basicYConstraint { it.parent.getTop() + it.parent.getHeight() * cropSettings.getUntracked().top }
        width = basicWidthConstraint { it.parent.getWidth() * cropSettings.getUntracked().width }
        height = basicHeightConstraint { it.parent.getHeight() * cropSettings.getUntracked().height }
    } childOf screenshotDisplay effect ScissorEffect()


    // anything drawn is displayed via this component
    // tools such as [PenTool] mutate its image and update it
    val vectorEditingOverlay: VectorEditingOverlay = VectorEditingOverlay(screenshot).constrain {
        x = 0.pixels boundTo screenshotDisplay
        y = 0.pixels boundTo screenshotDisplay
        height = 100.percent() boundTo screenshotDisplay
        width = 100.percent() boundTo screenshotDisplay
    } childOf retainedImage


    init {
        CropAlignment.values().forEach { alignment ->
            ImageCropItem(alignment) childOf this
        }
    }


    /**
     * NanoVG based editing overlay which handles drawing all edits as well as drawing the parts of the screenshot retained from cropping. [UINanoVG]
     */
    open inner class VectorEditingOverlay(val image: State<UIdentifier?>) : UINanoVG() {
        private val screenshotImage = ScreenshotImage(image)
        var scale = 1f

        init {
            image.onChange(this) { markDirty() }
            editHistory.history.onChange(this) { markDirty() }
        }

        constructor(veo: VectorEditingOverlay) : this(veo.image)

        override fun draw(matrixStack: UMatrixStack) {
            matrixStack.push()
            matrixStack.translate(getLeft(), getTop(), 0f)
            screenshotImage.renderImage(matrixStack, Color.WHITE, getWidth().toDouble(), getHeight().toDouble())
            matrixStack.pop()
            super.draw(matrixStack)
        }

        override fun renderVG(matrixStack: UMatrixStack, vg: NanoVG, width: Float, height: Float) {
            editHistory.history.getUntracked().filterIsInstance<VectorStroke>().forEach { vs ->
                vs.render(vg, width, height, scale)
            }
        }
    }

    /**
     * Exports the screenshot currently being edited to a file
     * If [temp] is true, the output is a temporary file.
     * Otherwise, the output is stored inside the screenshot folder
     * and the caller may refresh the view on completion.
     */
    fun exportImage(
        source: ScreenshotId,
        screenshotManager: IScreenshotManager,
        temp: Boolean,
        favorite: Boolean = false,
    ): CompletableFuture<File> {

        // Accessed early because it's needed on another thread and may have been reset by then
        val cropSettings = cropSettings.getUntracked()

        val completableFuture = CompletableFuture<File>()

        val screenshot = source.open().use {
            try {
                ImageIO.read(it) ?: throw IOException("Failed to load original image from $source")
            } catch (e: Exception) {
                completableFuture.completeExceptionally(e)
                return completableFuture
            }
        }

        val fullWidth = screenshot.width
        val fullHeight = screenshot.height
        val drawableWidth = screenshotDisplay.getWidth().toInt() * UResolution.scaleFactor.toInt()

        val buffer = BufferUtils.createFloatBuffer(fullWidth * fullHeight * 4)
        val veoCopy = object : VectorEditingOverlay(vectorEditingOverlay) {
            override fun render(matrixStack: UMatrixStack, width: Float, height: Float) {
                super.render(matrixStack, width, height)

                GL11.glReadPixels(
                    0,
                    0,
                    width.toInt(),
                    height.toInt(),
                    GL11.GL_RGBA,
                    GL11.GL_FLOAT,
                    buffer
                )
            }
        }
        veoCopy.scale = fullWidth / drawableWidth.toFloat()
        veoCopy.drawFrameBuffer(fullWidth / UResolution.scaleFactor, fullHeight / UResolution.scaleFactor)
        veoCopy.delete()
        // Fork as soon as we can to avoid freezing the main thread
        Dispatchers.IO.dispatch(EmptyCoroutineContext) {
            buffer.rewind()
            val imgData = (0 until buffer.limit()).map { i ->
                (buffer.get(i) * 255f).toInt()
            }.chunked(fullWidth * 4) { chunk ->
                chunk.chunked(4) { list ->
                    (((list[3] and 0xFF) shl 24) or
                        ((list[0] and 0xFF) shl 16) or
                        ((list[1] and 0xFF) shl 8) or
                        (list[2] and 0xFF))
                }
            }.reversed().flatten().toIntArray()
            val image = BufferedImage(
                fullWidth,
                fullHeight,
                BufferedImage.TYPE_INT_ARGB
            )
            image.setRGB(0, 0, fullWidth, fullHeight, imgData, 0, fullWidth)
            screenshot.graphics.drawImage(image, 0, 0, null)


            val left = (fullWidth * cropSettings.left).toInt()
            val right = (fullWidth * cropSettings.right).toInt()
            val top = (fullHeight * cropSettings.top).toInt()
            val bottom = (fullHeight * cropSettings.bottom).toInt()

            val croppedImage = screenshot.getSubimage(left, top, right - left, bottom - top)
            if (temp) {
                val tempFile = File.createTempFile("screenshot", null)
                ImageIO.write(croppedImage, "png", tempFile)
                completableFuture.complete(tempFile)
            } else {
                completableFuture.complete(
                    screenshotManager.handleScreenshotEdited(
                        source,
                        when (source) {
                            is LocalScreenshot -> screenshotManager.screenshotMetadataManager.getOrCreateMetadata(source.path)
                            is RemoteScreenshot -> ClientScreenshotMetadata(source.media)
                        },
                        croppedImage,
                        favorite,
                    )
                )
            }
        }

        return completableFuture
    }

    inner class ImageCropItem(val alignment: CropAlignment) : UIContainer() {

        //Accounts for position inside the element that we are dragging from
        var xDragOffset = 0f
        var yDragOffset = 0f

        init {
            constrain {
                height = 15.pixels()
                width = 15.pixels()
                x = if (alignment.centerX) {
                    CenterConstraint()
                } else {
                    (-padding).pixels(alignOpposite = alignment.alignOpX)
                } boundTo retainedImage
                y = if (alignment.centerY) {
                    CenterConstraint()
                } else {
                    (-padding).pixels(alignOpposite = alignment.alignOpY)
                } boundTo retainedImage
            }

            if (alignment.corner) {
                addChild {
                    UIBlock(EssentialPalette.TEXT).constrain {
                        x = (if (alignment.alignOpX) 0 else 0).pixels(alignOpposite = false, alignOutside = false)
                        y = 0.pixels(alignOpposite = alignment.alignOpY, alignOutside = false)
                        width = 15.pixels()
                        height = 2.pixels()
                    }
                }
                addChild {
                    UIBlock(EssentialPalette.TEXT).constrain {
                        x = 0.pixels(alignOpposite = alignment.alignOpX, alignOutside = false)
                        y = (if (alignment.alignOpY) 0 else 0).pixels(alignOpposite = false, alignOutside = false)
                        width = 2.pixels()
                        height = 15.pixels()
                    }
                }
            } else {
                addChild {
                    UIBlock(EssentialPalette.TEXT).constrain {
                        x = 0.pixels(alignOpposite = alignment.alignOpX, alignOutside = false)
                        y = 0.pixels(alignOpposite = alignment.alignOpY, alignOutside = false)
                        height = (if (alignment.centerY) 15 else 2).pixels()
                        width = (if (alignment.centerX) 15 else 2).pixels()
                    }
                }
            }

            onMouseEnter {
                children.forEach { it.animateColor(EssentialPalette.TEXT_HIGHLIGHT) }
            }
            onMouseLeave {
                children.forEach { it.animateColor(EssentialPalette.TEXT) }
            }
            var currDragStart: Crop? = null
            onMouseClick {
                currDragStart = cropSettings.getUntracked()
                draggingCrop.set(currDragStart)
                xDragOffset = it.relativeX + if (alignment.alignOpX) padding else -padding
                yDragOffset = it.relativeY + if (alignment.alignOpY) padding else -padding
            }
            onMouseRelease {
                val oldCrop = currDragStart ?: return@onMouseRelease
                val newCrop = draggingCrop.getUntracked() ?: return@onMouseRelease
                if (oldCrop != newCrop) {
                    editHistory.pushChange(CropChange(oldCrop, newCrop))
                }
                draggingCrop.set(null)
                currDragStart = null
            }
            onMouseDrag { _, _, _ ->
                if (currDragStart != null) {
                    //When dragging the position items, we want to take the position inside the item into account
                    val (mouseX, mouseY) = getMousePosition()
                    val relativeX = mouseX - screenshotDisplay.getLeft()
                    val relativeY = mouseY - screenshotDisplay.getTop()

                    val adjustedMouseX =
                        if (alignment.alignOpX) (relativeX + (getWidth() - xDragOffset)) else relativeX - xDragOffset
                    val adjustedMouseY =
                        if (alignment.alignOpY) (relativeY + (getHeight() - yDragOffset)) else relativeY - yDragOffset

                    val (x, y) = getRelativeMousePosition(adjustedMouseX, adjustedMouseY)

                    val minSize = .1f
                    var crop = draggingCrop.getUntracked()!!
                    for (side in alignment.sides) {
                        crop = when (side) {
                            CropAlignment.TOP_CENTER -> crop.copy(top = y.coerceAtMost(crop.bottom - minSize))
                            CropAlignment.RIGHT_CENTER -> crop.copy(right = x.coerceAtLeast(crop.left + minSize))
                            CropAlignment.BOTTOM_CENTER -> crop.copy(bottom = y.coerceAtLeast(crop.top + minSize))
                            CropAlignment.LEFT_CENTER -> crop.copy(left = x.coerceAtMost(crop.right - minSize))
                            else -> throw AssertionError("unreachable")
                        }
                    }
                    draggingCrop.set(crop)
                    //We changed a state that can change the position of this item,
                    //Therefore, we want to call animationFrame to invalidate the cached
                    //x and y
                    animationFrame()
                }
            }
        }
    }

    /**
     * Returns the x / y percentage [mouseX] and [mouseY] are inside of [screenshotDisplay]
     */
    fun getRelativeMousePosition(mouseX: Float, mouseY: Float): Pair<Float, Float> {
        val width = screenshotDisplay.getWidth()
        val height = screenshotDisplay.getHeight()
        return (mouseX / width).coerceIn(0f..1f) to (mouseY / height).coerceIn(0f..1f)
    }

    enum class CropAlignment(
        val alignOpX: Boolean,
        val alignOpY: Boolean,
        val centerX: Boolean = false,
        val centerY: Boolean = false,
        val corner: Boolean
    ) {
        TOP_LEFT(alignOpX = false, alignOpY = false, corner = true),
        TOP_CENTER(alignOpX = false, alignOpY = false, centerX = true, corner = false),
        TOP_RIGHT(alignOpX = true, alignOpY = false, corner = true),
        RIGHT_CENTER(alignOpX = true, alignOpY = false, centerY = true, corner = false),
        BOTTOM_RIGHT(alignOpX = true, alignOpY = true, corner = true),
        BOTTOM_CENTER(alignOpX = false, alignOpY = true, centerX = true, corner = false),
        BOTTOM_LEFT(alignOpX = false, alignOpY = true, corner = true),
        LEFT_CENTER(alignOpX = false, alignOpY = false, centerY = true, corner = false);

        val sides: List<CropAlignment>
            get() = when (this) {
                TOP_LEFT -> listOf(TOP_CENTER, LEFT_CENTER)
                TOP_RIGHT -> listOf(TOP_CENTER, RIGHT_CENTER)
                BOTTOM_LEFT -> listOf(BOTTOM_CENTER, LEFT_CENTER)
                BOTTOM_RIGHT -> listOf(BOTTOM_CENTER, RIGHT_CENTER)
                else -> listOf(this)
            }
    }

    data class Crop(val left: Float, val right: Float, val top: Float, val bottom: Float) {
        val width = right - left
        val height = bottom - top

        companion object {
            val DEFAULT = Crop(0f, 1f, 0f, 1f)
        }
    }
}