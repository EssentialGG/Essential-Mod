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
package gg.essential.gui.screenshot.editor.tools

import gg.essential.elementa.utils.ObservableList
import gg.essential.gui.screenshot.editor.ScreenshotCanvas
import gg.essential.gui.screenshot.editor.change.EditHistory
import gg.essential.gui.screenshot.editor.change.VectorStroke
import gg.essential.util.lwjgl3.api.nanovg.NanoVG
import java.awt.Color

/**
 * This tool works by storing points of where the cursor has been dragged on the canvas
 * then drawing a line between those points.
 */
class PenTool(private val editHistory: EditHistory, editableScreenshot: ScreenshotCanvas) : Tool(editableScreenshot) {
    var color: Color = Color.WHITE
    var width: Float = 1f

    // stores the last x position called from the mouseDrag event
    var previousMouseX = -1f

    // stores the last y position called from the mouseDrag event
    var previousMouseY = -1f

    // Stores the stroke that is currently being drawn
    var currentVectorStroke: PenVectorStroke? = null

    override fun enable() {
        editableScreenshot.onDraw = { mouseX, mouseY, mouseButton ->
            val relativeX = (mouseX - getLeft()).coerceIn(0f, getWidth())
            val relativeY = (mouseY - getTop()).coerceIn(0f, getHeight())
            // makes sure its inside the bounds of [uiImage]
            if (!(relativeX < 0 || relativeY < 0
                    || relativeX > editableScreenshot.getWidth() || relativeY > editableScreenshot.getHeight())
            ) {
                // left click is mouse button 0
                val LEFT_CLICK = 0
                if (mouseButton == LEFT_CLICK) {
                    // every frame during drag get previousMouse and draw a line from currentMouse
                    // if previous mouse is -1 then it is the first point therefore nothing is drawn
                    if (previousMouseX != -1f || previousMouseY != -1f) {
                        if (relativeX != previousMouseX || relativeY != previousMouseY) {
                            // add a line to draw from the previous mouse pointer to the current
                            // this mimics the function of a "pen"
                            currentVectorStroke?.list?.add(relativeX / getWidth() to relativeY / getHeight())
                        }
                    }
                    // set previous mouse pointers to the current mouse point
                    previousMouseX = relativeX
                    previousMouseY = relativeY
                }
            }
        }
        editableScreenshot.screenshotDisplay.onMouseClick {
            if (it.mouseButton != 0) {
                return@onMouseClick
            }
            currentVectorStroke = PenVectorStroke(color, width).also { editHistory.pushChange(it) }
        }
        editableScreenshot.screenshotDisplay.onMouseRelease {
            previousMouseX = -1f
            previousMouseY = -1f
            currentVectorStroke = null
        }
    }

    /**
     * impl of [VectorStroke]
     * handles drawing lines with nanovg and smooths with quadratic beziers
     */
    inner class PenVectorStroke(val colorObj: Color, val strokeWidth: Float) :
        VectorStroke(editableScreenshot, colorObj.rgb) {
        val list = ObservableList(ArrayList<Pair<Float, Float>>())

        init {
            list.addObserver { _, _ ->
                editableScreenshot.vectorEditingOverlay.markDirty()
            }
        }

        override fun render(vg: NanoVG, width: Float, height: Float, scale: Float) {
            vg.beginPath()
            vg.strokeWidth(strokeWidth * scale)
            vg.strokeColor(colorObj)
            list.firstOrNull()?.let { (x, y) -> vg.startPoint(x * width, y * height) }
            list.drop(1).zipWithNext { (x1, y1), (x2, y2) ->
                vg.quadBezierTo(
                    x1 * width, y1 * height,
                    x2 * width, y2 * height
                )
            }
            vg.stroke()
        }

    }
}