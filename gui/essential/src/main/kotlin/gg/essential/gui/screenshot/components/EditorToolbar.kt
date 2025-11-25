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
package gg.essential.gui.screenshot.components

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.elementa.utils.withAlpha
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.effect.CheckerboardBackgroundEffect
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.common.or
import gg.essential.gui.common.shadow.ShadowEffect
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.elementa.state.v2.State as StateV2
import gg.essential.gui.screenshot.editor.ScreenshotCanvas
import gg.essential.gui.screenshot.editor.change.EditHistory
import gg.essential.gui.screenshot.editor.tools.PenTool
import gg.essential.gui.util.hoveredState
import gg.essential.universal.UGraphics
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMatrixStack
import gg.essential.universal.USound
import gg.essential.universal.shader.BlendState
import gg.essential.util.bindHoverEssentialTooltip
import gg.essential.util.centered
import gg.essential.gui.util.hoveredState
import gg.essential.lib.gson.Gson
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.vertex.UBufferBuilder
import gg.essential.universal.vertex.UVertexConsumer
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.HSBColor
import gg.essential.vigilance.utils.onLeftClick
import org.intellij.lang.annotations.Language
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.io.IOException
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class EditorToolbar(
    isScreenOpen: StateV2<Boolean>,
    private val canvas: ScreenshotCanvas,
    private val editHistory: EditHistory,
    private val active: State<Boolean>
) : UIContainer() {

    private val penTool = PenTool(editHistory, canvas)

    private val colorPicker by ScreenshotColorPicker(
        penTool,
        active
    ).constrain {
        x = SiblingConstraint(3f)
        y = 0.pixels(alignOpposite = true)
    } childOf this

    private val strokeWidthComponent by StrokeWidthComponent(penTool).constrain {
        x = SiblingConstraint(3f)
        y = 0.pixels(alignOpposite = true)
    } childOf this

    private val undo by IconButton(EssentialPalette.UNDO_9X, tooltipText = "Undo").constrain {
        x = SiblingConstraint(3f)
        y = 0.pixels(alignOpposite = true)
        width = AspectConstraint(1f)
        height = 100.percent
    }.rebindEnabled(editHistory.undoEnabled.toV1(this)).onActiveClick {
        undo()
    } childOf this

    private val redo by IconButton(EssentialPalette.REDO_9X, tooltipText = "Redo").constrain {
        x = SiblingConstraint(3f)
        y = 0.pixels(alignOpposite = true)
        width = AspectConstraint(1f)
        height = 100.percent
    }.rebindEnabled(editHistory.redoEnabled.toV1(this)).onActiveClick {
        redo()
    } childOf this


    init {
        penTool.enable()
        constrain {
            height = 17.pixels
            width = ChildBasedSizeConstraint()
        }

        effect(this) {
            if (!isScreenOpen()) colorPicker.saveColors()
        }
    }

    override fun afterInitialization() {
        super.afterInitialization()

        Window.of(this).onKeyType { _, keyCode ->
            if (active.get()) {
                if (UKeyboard.isKeyComboCtrlZ(keyCode)) {
                    undo()
                } else if (UKeyboard.isKeyComboCtrlY(keyCode)) {
                    redo()
                }
            }
        }
    }

    private fun undo() {
        editHistory.undo()
    }

    private fun redo() {
        editHistory.redo()
    }
}

class StrokeWidthComponent(val penTool: PenTool) : UIBlock(EssentialPalette.COMPONENT_BACKGROUND) {

    private val container by UIContainer().centered().constrain {
        width = ChildBasedSizeConstraint()
        height = 100.percent
    } childOf this

    private val selectedWidth = BasicState(3)

    init {
        constrain {
            width = ChildBasedSizeConstraint() + 4.pixels
            height = 100.percent
        }

        for (i in 1..5) {
            Stoke(i) childOf container
        }

        selectedWidth.onSetValueAndNow {
            penTool.width = it.toFloat()
        }

        effect(ShadowEffect(Color.BLACK))
    }

    inner class Stoke(val width: Int) : UIContainer() {

        private val selected = selectedWidth.map { it == width }
        private val hovered = hoveredState()

        private val block by UIBlock().centered().constrain {
            width = this@Stoke.width.pixels
            height = 100.percent - 4.pixels
        } childOf this

        init {
            constrain {
                x = SiblingConstraint(1f)
                y = CenterConstraint()
                width = ChildBasedSizeConstraint() + 2.pixels // Increase hitbox a little to make it easier to click
                height = 100.percent - 2.pixels
            }

            onLeftClick {
                selectedWidth.set(width)
            }

            selected.onSetValue {
                if (it) {
                    USound.playButtonPress()
                }
            }

            bindHoverEssentialTooltip(BasicState("Brush Size: $width"))

            block.setColor(selected.zip(hovered).map { (selected, hovered) ->
                if (selected) {
                    EssentialPalette.TEXT_HIGHLIGHT
                } else if (hovered) {
                    EssentialPalette.TEXT
                } else {
                    EssentialPalette.BUTTON_HIGHLIGHT
                }
            }.toConstraint())
        }
    }
}

class ScreenshotColorPicker(
    private val penTool: PenTool,
    active: State<Boolean>,
) : UIContainer() {

    private val showMenu = BasicState(false)
    private val hue = BasicState(0f)
    private val saturation = BasicState(1f)
    private val brightness = BasicState(1f)
    private val alpha = BasicState(1f)
    private val hueSaturationSide = 69f

    private val hueColorList: List<Color> =
        (0..hueSaturationSide.toInt()).map { i -> Color(Color.HSBtoRGB(i / hueSaturationSide, 1f, 0.9f)) }
    private val componentCrossDimension =
        9f // The other side of the rectangle that isn't of equal side fo [hueSaturationSide]

    private lateinit var currentColorState: State<HSBColor>

    private val hsb = hue.zip(saturation).zip(brightness)

    private val collapsedBlock by UIBlock().constrain {
        y = 0.pixels(alignOpposite = true)
        width = AspectConstraint(1f)
        height = 100.percent
    }.onLeftClick {
        showMenu.set { !it }
    } childOf this

    // color bound in init
    private val currentColor by UIBlock().centered().constrain {
        width = 100.percent - 6.pixels
        height = 100.percent - 6.pixels
    } childOf collapsedBlock effect CheckerboardBackgroundEffect()

    private val buttonConnector by UIBlock(EssentialPalette.BUTTON_HIGHLIGHT).constrain {
        x = 0.pixels boundTo collapsedBlock
        y = SiblingConstraint(0f, alignOpposite = true) boundTo collapsedBlock
        width = 100.percent boundTo collapsedBlock
        height = 3.pixels
    }.apply { isFloating = true }.onMouseClick { it.stopPropagation() }.bindParent(collapsedBlock, showMenu)

    private val selectorBlock by UIBlock(EssentialPalette.BUTTON_HIGHLIGHT).constrain {
        x = 0.pixels boundTo collapsedBlock
        y = SiblingConstraint(0f, alignOpposite = true) boundTo buttonConnector
        width = ChildBasedSizeConstraint() + 4.pixels
        height = ChildBasedSizeConstraint() + 4.pixels
    }.apply { isFloating = true }.onMouseClick { it.stopPropagation() }.bindParent(collapsedBlock, showMenu)

    private val selectorContent by UIBlock(EssentialPalette.COMPONENT_BACKGROUND).centered().constrain {
        width = ChildBasedSizeConstraint() + 6.pixels
        height = ChildBasedSizeConstraint() + 6.pixels
    } childOf selectorBlock

    private val selectorInnerContent by UIContainer().centered().constrain {
        width = ChildBasedMaxSizeConstraint()
        height = ChildBasedSizeConstraint()
    } childOf selectorContent

    private val hueSaturationBrightnessRow by UIContainer().constrain {
        width = ChildBasedSizeConstraint()
        height = ChildBasedMaxSizeConstraint()
    } childOf selectorInnerContent


    private val saturationBrightnessContainer by UIBlock(EssentialPalette.BUTTON).constrain {
        width = hueSaturationSide.pixels
        height = AspectConstraint(1f)
    } childOf hueSaturationBrightnessRow

    private val alphaContainer by UIBlock(EssentialPalette.BUTTON).constrain {
        y = SiblingConstraint(5f)
        height = componentCrossDimension.pixels
        width = hueSaturationSide.pixels
    } childOf selectorInnerContent

    private val colorHistoryBlock by UIBlock(EssentialPalette.BUTTON).constrain {
        y = SiblingConstraint(5f)
        height = componentCrossDimension.pixels
        width = ChildBasedSizeConstraint() + 2.pixels
    } childOf selectorInnerContent

    private val colorHistoryContainer by UIContainer().centered().constrain {
        width = ChildBasedSizeConstraint()
        height = 100.percent - 2.pixels
    } childOf colorHistoryBlock

    private val hueContainer by UIBlock(EssentialPalette.BUTTON).constrain {
        x = SiblingConstraint(5f)
        height = hueSaturationSide.pixels
        width = componentCrossDimension.pixels
    } childOf hueSaturationBrightnessRow

    private val alphaContentContainer by UIContainer().centered().constrain {
        width = 100.percent() - 4.pixels
        height = 100.percent() - 4.pixels
    } childOf alphaContainer

    private val alphaArea by object : UIComponent() {
        override fun draw(matrixStack: UMatrixStack) {
            beforeDraw(matrixStack)
            drawAlpha(matrixStack, this)

            super.draw(matrixStack)
        }
    }.constrain {
        width = 100.percent
        height = 100.percent
    } childOf alphaContentContainer effect CheckerboardBackgroundEffect()

    private val alphaSlider by UIContainer().constrain {
        x = RelativeConstraint().bindValue(alpha).coerceIn(0.pixels, 100.percent - 1.pixels)
        y = CenterConstraint()
        width = 1.pixel
        height = 100.percent
    } effect OutlineEffect(EssentialPalette.TEXT_HIGHLIGHT, 1f) childOf alphaArea


    private val hueArea by object : UIComponent() {
        override fun draw(matrixStack: UMatrixStack) {
            beforeDraw(matrixStack)
            drawHueLine(matrixStack, this)

            super.draw(matrixStack)
        }
    }.centered().constrain {
        width = 100.percent() - 4.pixels
        height = 100.percent() - 4.pixels
    } childOf hueContainer

    private val hueSlider by UIContainer().constrain {
        x = CenterConstraint()
        y = RelativeConstraint().bindValue(hue).coerceIn(0.pixels, 100.percent - 1.pixels)
        width = 100.percent
        height = 1.pixel
    } effect OutlineEffect(EssentialPalette.TEXT_HIGHLIGHT, 1f) childOf hueArea

    private val saturationBrightnessArea by object : UIComponent() {
        override fun draw(matrixStack: UMatrixStack) {
            beforeDraw(matrixStack)
            drawColorPicker(matrixStack, this)

            super.draw(matrixStack)
        }
    }.centered().constrain {
        width = 100.percent() - 4.pixels
        height = 100.percent() - 4.pixels
    } childOf saturationBrightnessContainer

    private val saturationBrightnessSelection by UIContainer().constrain {
        x = RelativeConstraint().bindValue(saturation).coerceIn(0.pixels, 100.percent - 1.pixels)
        y = RelativeConstraint().bindValue(brightness.map { 1 - it }).coerceIn(0.pixels, 100.percent - 1.pixel)
        width = 1.pixel
        height = 1.pixel
    } childOf saturationBrightnessArea effect OutlineEffect(EssentialPalette.TEXT_HIGHLIGHT, 1f)

    private val activeColorIndex = BasicState(0)

    init {

        createMouseDragListener(hueContainer).onSetValue { (_, yPercent) ->
            hue.set(yPercent)
        }

        createMouseDragListener(alphaContainer).onSetValue { (xPercent, _) ->
            alpha.set(xPercent)
        }

        createMouseDragListener(saturationBrightnessContainer).onSetValue { (xPercent, yPercent) ->
            saturation.set(xPercent)
            brightness.set(1 - yPercent)
        }

        collapsedBlock.setColor(
            EssentialPalette.getButtonColor(collapsedBlock.hoveredState() or showMenu).toConstraint()
        )
        constrain {
            height = 100.percent
            width = ChildBasedMaxSizeConstraint()
        }

        for ((index, colorPreset) in loadEditorColors().withIndex()) {
            val colorChoice = ColorChoice(BasicState(colorPreset), index)
            colorChoice childOf colorHistoryContainer
        }

        activeColorIndex.onSetValueAndNow { index ->

            val colorChoice = colorHistoryContainer.childrenOfType(ColorChoice::class.java)[index]

            currentColorState = colorChoice.containedColor

            val color = currentColorState.get()

            hue.set(color.hue)
            saturation.set(color.saturation)
            brightness.set(color.brightness)
        }

        hsb.zip(alpha).onSetValueAndNow { (hsb, alpha) ->
            val (hs, brightness) = hsb
            val (hue, saturation) = hs

            val color = HSBColor(hue, saturation, brightness, alpha)
            currentColorState.set(color)
            penTool.color = color.toColor()
            currentColor.setColor(color.toColor())
        }


        // When the editor is no longer active, we must close the menu because it is bound to Window
        active.onSetValue {
            if (!it) {
                showMenu.set(false)
            }
        }

        effect(ShadowEffect(Color.BLACK))
    }

    fun saveColors() {
        saveEditorColors(
            colorHistoryContainer.childrenOfType(ColorChoice::class.java).map { it.containedColor.get() }
        )
    }

    private fun drawColorPicker(matrixStack: UMatrixStack, component: UIComponent) {
        val left = component.getLeft().toDouble()
        val top = component.getTop().toDouble()
        val right = component.getRight().toDouble()
        val bottom = component.getBottom().toDouble()

        setupDraw()
        val graphics = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR)
        drawVertex(graphics, matrixStack, left, top, Color(0, 255, 0))
        drawVertex(graphics, matrixStack, left, bottom, Color(0, 0, 0))
        drawVertex(graphics, matrixStack, right, bottom, Color(255, 0, 0))
        drawVertex(graphics, matrixStack, right, top, Color(255, 255, 0))
        graphics.build()?.drawAndClose(PIPELINE_SATURATION_VALUE_QUAD) {
            uniform("u_Hue", hue.get())
        }
        cleanupDraw()
    }

    private fun drawAlpha(matrixStack: UMatrixStack, component: UIComponent) {
        val left = component.getLeft().toDouble()
        val top = component.getTop().toDouble()
        val right = component.getRight().toDouble()
        val bottom = component.getBottom().toDouble()

        setupDraw()

        val graphics = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR)
        drawVertex(graphics, matrixStack, left, top, currentColorState.get().toColor().withAlpha(0))
        drawVertex(graphics, matrixStack, left, bottom, currentColorState.get().toColor().withAlpha(0))
        drawVertex(graphics, matrixStack, right, bottom, currentColorState.get().toColor().withAlpha(255))
        drawVertex(graphics, matrixStack, right, top, currentColorState.get().toColor().withAlpha(255))
        graphics.build()?.drawAndClose(PIPELINE)
        cleanupDraw()
    }

    private fun drawHueLine(matrixStack: UMatrixStack, component: UIComponent) {
        val left = component.getLeft().toDouble()
        val top = component.getTop().toDouble()
        val right = component.getRight().toDouble()
        val height = component.getHeight().toDouble()

        setupDraw()
        val graphics = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR)

        var first = true
        for ((i, color) in hueColorList.withIndex()) {
            val yPos = top + (i.toFloat() * height / hueSaturationSide)
            if (!first) {
                drawVertex(graphics, matrixStack, left, yPos, color)
                drawVertex(graphics, matrixStack, right, yPos, color)
            }

            drawVertex(graphics, matrixStack, right, yPos, color)
            drawVertex(graphics, matrixStack, left, yPos, color)

            first = false
        }

        graphics.build()?.drawAndClose(PIPELINE)
        cleanupDraw()
    }

    private fun setupDraw() {
        UGraphics.shadeModel(GL11.GL_SMOOTH)
    }

    private fun cleanupDraw() {
        UGraphics.shadeModel(GL11.GL_FLAT)
    }

    private fun drawVertex(graphics: UVertexConsumer, matrixStack: UMatrixStack, x: Double, y: Double, color: Color) {
        graphics
            .pos(matrixStack, x, y, 0.0)
            .color(
                color.red.toFloat() / 255f,
                color.green.toFloat() / 255f,
                color.blue.toFloat() / 255f,
                color.alpha.toFloat() / 255f
            )
            .endVertex()
    }

    private fun createMouseDragListener(component: UIComponent): State<Pair<Float, Float>> {
        var mouseHeld = false
        val basicState = BasicState(0f to 0f)


        fun updateState(mouseX: Float, mouseY: Float) {

            val child = component.children[0]
            val offsetX = child.getLeft() - component.getLeft()
            val offsetY = child.getTop() - component.getTop()

            basicState.set(
                ((mouseX - offsetX) / child.getWidth()).coerceIn(0f..1f) to ((mouseY - offsetY) / child.getHeight()).coerceIn(
                    0f..1f
                )
            )
        }

        component.onLeftClick {
            USound.playButtonPress()
            mouseHeld = true
            updateState(it.relativeX, it.relativeY)
        }
        component.onMouseRelease {
            mouseHeld = false
        }
        component.onMouseDrag { mouseX, mouseY, _ ->
            if (!mouseHeld) return@onMouseDrag

            updateState(mouseX, mouseY)
        }
        return basicState
    }


    inner class ColorChoice(val containedColor: State<HSBColor>, val index: Int) : UIBlock() {
        val active = activeColorIndex.map { it == index }

        private val colorBlock by UIBlock(containedColor.map { it.toColor() }).centered().constrain {
            width = 100.percent - 2.pixels
            height = 100.percent - 2.pixels
        } childOf this

        init {
            constrain {
                x = SiblingConstraint(2f)
                width = AspectConstraint(1f)
                height = 100.percent
            }
            setColor(active.zip(hoveredState()).map { (active, hovered) ->
                if (active) {
                    EssentialPalette.TEXT_HIGHLIGHT
                } else if (hovered) {
                    EssentialPalette.GRAY_OUTLINE
                } else {
                    EssentialPalette.COMPONENT_BACKGROUND
                }
            }.toConstraint())

            onLeftClick {
                // Only play the sound if the stroke is going to be selected
                if (activeColorIndex.get() != index) {
                    USound.playButtonPress()
                }
                activeColorIndex.set { index }
            }
        }
    }

    companion object {
        private val PIPELINE = URenderPipeline.builderWithDefaultShader(
            "essential:screenshot_picker",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_COLOR,
        ).apply {
            blendState = BlendState.NORMAL
        }.build()

        private val PIPELINE_SATURATION_VALUE_QUAD = run {
            @Language("GLSL")
            val vertSource = """
                #version 110
                
                void main() {
                    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
                    gl_FrontColor = gl_Color;
                }
            """.trimIndent()
            @Language("GLSL")
            val fragSource = """
                #version 110
                
                uniform float u_Hue;
                
                // From https://stackoverflow.com/a/17897228 (Licence: WTFPL)
                // All components are in the range [0…1], including hue.
                vec3 hsv2rgb(vec3 c) {
                    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
                    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
                    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
                }
                
                void main() {
                    vec3 hsv = vec3(u_Hue, gl_Color.r, gl_Color.g);
                    gl_FragColor = vec4(hsv2rgb(hsv), 1.0);
                }
            """.trimIndent()
            URenderPipeline.builderWithLegacyShader(
                "essential:screenshot_color_picker_saturation_value",
                UGraphics.DrawMode.QUADS,
                UGraphics.CommonVertexFormats.POSITION_COLOR,
                vertSource,
                fragSource,
            ).build()
        }


        private val editorStateFile = platform.essentialBaseDir / "screenshot-editor.json"

        private val DEFAULT_COLORS = listOf(HSBColor(0xD32121), HSBColor(0xEAB600), HSBColor(0x3B8A2F), HSBColor(0x0085FF), HSBColor(0x000000))
        private var savedColors: List<HSBColor>? = null

        private fun saveEditorColors(colors: List<HSBColor>) {
            if (savedColors == colors) {
                return
            }
            savedColors = colors
            try {
                editorStateFile.writeText(Gson().toJson(colors))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        private fun loadEditorColors(): List<HSBColor> {
            if (savedColors == null) {
                savedColors = try {
                    if (editorStateFile.exists()) {
                        Gson().fromJson(editorStateFile.readText(), Array<HSBColor>::class.java).toList()
                    } else {
                        null
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    null
                }?.takeUnless { it.size != 5 } ?: DEFAULT_COLORS
            }
            return savedColors!!
        }
    }
}