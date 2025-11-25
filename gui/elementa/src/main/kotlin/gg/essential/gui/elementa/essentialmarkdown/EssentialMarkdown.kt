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
package gg.essential.gui.elementa.essentialmarkdown

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.HeightConstraint
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.events.UIEvent
import gg.essential.gui.elementa.essentialmarkdown.drawables.BlockquoteDrawable
import gg.essential.gui.elementa.essentialmarkdown.drawables.DrawableList
import gg.essential.gui.elementa.essentialmarkdown.drawables.HeaderDrawable
import gg.essential.gui.elementa.essentialmarkdown.drawables.ListDrawable
import gg.essential.gui.elementa.essentialmarkdown.drawables.ParagraphDrawable
import gg.essential.gui.elementa.essentialmarkdown.selection.Cursor
import gg.essential.gui.elementa.essentialmarkdown.selection.Selection
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.universal.UDesktop
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMatrixStack
import gg.essential.vigilance.utils.onLeftClick
import org.apache.logging.log4j.LogManager
import java.awt.Color

/**
 * Component that parses a string as Markdown and renders it.
 *
 * This component's width and height must be non-child-related
 * constraints. This is because the actual text rendering is
 * done with direct render calls instead of through the component
 * hierarchy.
 */
class EssentialMarkdown(
    private val text: State<String>,
    config: MarkdownConfig = MarkdownConfig(),
    private val disableSelection: Boolean = false,
) : UIComponent() {

    @JvmOverloads
    constructor(
        text: String,
        config: MarkdownConfig = MarkdownConfig(),
        disableSelection: Boolean = false,
    ) : this(stateOf(text), config, disableSelection)

    private val configState = mutableStateOf(config)
    val config: MarkdownConfig
        get() = configState.get()

    val drawables = DrawableList(this, emptyList())
    var sectionOffsets: Map<String, Float> = emptyMap()
        private set
    private var baseX: Float = -1f
    private var baseY: Float = -1f
    private lateinit var lastValues: ConstraintValues
    private var maxHeight: HeightConstraint = Int.MAX_VALUE.pixels()
    private var cursor: Cursor<*>? = null
    private var selection: Selection? = null
    private var canDrag = false
    private var needsInitialLayout = true
    private val linkClickListeners = mutableListOf<EssentialMarkdown.(LinkClickEvent) -> Unit>()

    var maxTextLineWidth = 0f
        private set

    private var layoutFailed = false

    init {
        onLeftClick {
            val xShift = getLeft() - baseX
            val yShift = getTop() - baseY
            cursor =
                drawables.cursorAt(it.absoluteX - xShift, it.absoluteY - yShift, dragged = false, it.mouseButton)

            selection?.remove()
            selection = null
            releaseWindowFocus()
        }
        if (!disableSelection) {
            onMouseClick {
                canDrag = true
            }

            onMouseRelease {
                canDrag = false
            }

            onMouseDrag { mouseX, mouseY, mouseButton ->
                if (mouseButton != 0 || !canDrag)
                    return@onMouseDrag

                val x = baseX + mouseX.coerceIn(0f, getWidth())
                val y = baseY + mouseY.coerceIn(0f, getHeight())

                val otherEnd = drawables.cursorAt(x, y, dragged = true, mouseButton)

                if (cursor == otherEnd)
                    return@onMouseDrag

                selection?.remove()
                selection = Selection.fromCursors(cursor!!, otherEnd)
                grabWindowFocus()
            }

            onKeyType { _, keyCode ->
                if (selection != null && keyCode == UKeyboard.KEY_C && UKeyboard.isCtrlKeyDown()) {
                    UDesktop.setClipboardString(drawables.selectedText(UKeyboard.isShiftKeyDown()))
                }
            }
        }
        configState.onSetValue(this) {
            reparse()
            layout()
        }
        text.onSetValue(this) {
            reparse()
            layout()
        }
    }

    fun setMaxHeight(maxHeight: HeightConstraint) = apply {
        this.maxHeight = maxHeight
    }

    /**
     * Parses the text into a markdown tree. This is called everytime
     * that the text of this component changes, and is always followed
     * by a call to layout().
     */
    private fun reparse() {
        drawables.setDrawables(MarkdownRenderer(text.get(), this, config).render())
    }

    /**
     * This method is responsible for laying out the markdown tree.
     *
     * @see Drawable.layout
     */
    fun layout() {
        try {
            layoutFailed = false

            baseX = getLeft()
            baseY = getTop()
            var currY = baseY
            val width = getWidth()

            drawables.forEach {
                currY += it.layout(baseX, currY, width).height
            }

            sectionOffsets = drawables.filterIsInstance<HeaderDrawable>().associate { it.id to it.y }

            setHeight((currY - baseY).coerceAtMost(maxHeight.getHeight(this)).pixels())

            maxTextLineWidth = drawables.maxOfOrNull { drawable ->
                when (drawable) {
                    is ParagraphDrawable -> drawable.maxTextLineWidth
                    is HeaderDrawable -> drawable.children.filterIsInstance<ParagraphDrawable>().maxOfOrNull { it.maxTextLineWidth } ?: 0f
                    is ListDrawable -> drawable.maxTextLineWidth
                    is BlockquoteDrawable -> drawable.maxTextLineWidth
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            LogManager.getLogger().error("Failed to layout markdown", e)
            layoutFailed = true
        }
    }

    init {
        addUpdateFunc { _, _ -> update() }
    }

    fun update() {
        if (needsInitialLayout) {
            needsInitialLayout = false
            reparse()
            layout()
            lastValues = constraintValues()
        }

        // Re-layout if important constraint values have changed
        val currentValues = constraintValues()
        if (currentValues != lastValues)
            layout()
        lastValues = currentValues
    }

    /**
     * Updates the MarkdownConfig this component uses.
     */
    fun updateConfig(config: MarkdownConfig)  {
        configState.set(config)
    }

    fun clearSelection() {
        selection?.remove()
        selection = null
    }

    override fun draw(matrixStack: UMatrixStack) {
        if (needsInitialLayout) {
            update()
        }
        beforeDraw(matrixStack)

        if (layoutFailed) {
            getFontProvider().drawString(matrixStack, "Failed to render markdown", Color(0xCC2929), getLeft(), getTop(), 10f, 1f)
            super.draw(matrixStack)
            return
        }

        val drawState = DrawState(getLeft() - baseX, getTop() - baseY)
        val parentWindow = Window.of(this)

        drawables.forEach {

            if (!parentWindow.isAreaVisible(
                    it.layout.left.toDouble() + drawState.xShift, it.layout.top.toDouble() + drawState.yShift,
                    it.layout.right.toDouble() + drawState.xShift, it.layout.bottom.toDouble() + drawState.yShift
            )) return@forEach

//            if (elementaDebug) {
//                drawDebugOutline(
//                    matrixStack,
//                    it.layout.left.toDouble() + drawState.xShift,
//                    it.layout.top.toDouble() + drawState.yShift,
//                    it.layout.right.toDouble() + drawState.xShift,
//                    it.layout.bottom.toDouble() + drawState.yShift,
//                    this
//                )
//            }

            it.draw(matrixStack, drawState)
        }
        if (!disableSelection)
            selection?.draw(matrixStack, drawState) ?: cursor?.draw(matrixStack, drawState)

        super.draw(matrixStack)
    }

    private fun constraintValues() = ConstraintValues(
        getWidth(),
        getTextScale(),
    )

    fun onLinkClicked(block: EssentialMarkdown.(LinkClickEvent) -> Unit) {
        linkClickListeners.add(block)
    }

    internal fun fireLinkClickEvent(event: LinkClickEvent): Boolean {
        for (listener in linkClickListeners) {
            this.listener(event)

            if (event.propagationStoppedImmediately) return false
        }
        return !event.propagationStopped
    }

    class LinkClickEvent internal constructor(val url: String) : UIEvent()

    /**
     * This class stores the values of the important constraints of this
     * component. If these values change between frames, we need to do a
     * complete re-layout of the entire markdown tree.
     */
    data class ConstraintValues(
        val width: Float,
        val textScale: Float,
    )

    companion object {
        // TODO: Remove
        const val DEBUG = false
    }
}
