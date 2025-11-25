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
package gg.essential.gui.wardrobe.configuration

import gg.essential.elementa.components.Window
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.ContextOptionMenu
import gg.essential.gui.common.EssentialDropDown
import gg.essential.gui.common.OutlineButtonStyle
import gg.essential.gui.common.input.StateTextInput
import gg.essential.gui.common.input.essentialDoubleInput
import gg.essential.gui.common.input.essentialFloatInput
import gg.essential.gui.common.input.essentialISODateInput
import gg.essential.gui.common.input.essentialIntInput
import gg.essential.gui.common.input.essentialLongInput
import gg.essential.gui.common.input.essentialManagedNullableISODateInput
import gg.essential.gui.common.input.essentialNullableISODateInput
import gg.essential.gui.common.input.essentialNullableStringInput
import gg.essential.gui.common.input.essentialStringInput
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.EssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.util.focusedState
import gg.essential.mod.EssentialAsset
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.image.bitmap.Bitmap
import gg.essential.util.image.bitmap.fromOrThrow
import gg.essential.util.lwjgl3.api.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.io.path.Path

object ConfigurationUtils {

    private val LOGGER = LoggerFactory.getLogger(ConfigurationUtils::class.java)

    // 6x6 white png used as a default icon
    private const val BLANK_IMAGE_URI =
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAYAAAAGCAIAAABvrngfAAAAAXNSR0IArs4c6QAAAMZlWElmTU0AKgAAAAgABgESAAMAAAABAAEAAAEaAAUAAAABAAAAVgEbAAUAAAABAAAAXgEoAAMAAAABAAIAAAExAAIAAAAVAAAAZodpAAQAAAABAAAAfAAAAAAAAABIAAAAAQAAAEgAAAABUGl4ZWxtYXRvciBQcm8gMy40LjMAAAAEkAQAAgAAABQAAACyoAEAAwAAAAEAAQAAoAIABAAAAAEAAAAGoAMABAAAAAEAAAAGAAAAADIwMjM6MTA6MTcgMTc6MTM6MjkA7laYZgAAAAlwSFlzAAALEwAACxMBAJqcGAAAA65pVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IlhNUCBDb3JlIDYuMC4wIj4KICAgPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICAgICAgPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIKICAgICAgICAgICAgeG1sbnM6dGlmZj0iaHR0cDovL25zLmFkb2JlLmNvbS90aWZmLzEuMC8iCiAgICAgICAgICAgIHhtbG5zOmV4aWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20vZXhpZi8xLjAvIgogICAgICAgICAgICB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iPgogICAgICAgICA8dGlmZjpZUmVzb2x1dGlvbj43MjAwMDAvMTAwMDA8L3RpZmY6WVJlc29sdXRpb24+CiAgICAgICAgIDx0aWZmOlhSZXNvbHV0aW9uPjcyMDAwMC8xMDAwMDwvdGlmZjpYUmVzb2x1dGlvbj4KICAgICAgICAgPHRpZmY6UmVzb2x1dGlvblVuaXQ+MjwvdGlmZjpSZXNvbHV0aW9uVW5pdD4KICAgICAgICAgPHRpZmY6T3JpZW50YXRpb24+MTwvdGlmZjpPcmllbnRhdGlvbj4KICAgICAgICAgPGV4aWY6UGl4ZWxZRGltZW5zaW9uPjY8L2V4aWY6UGl4ZWxZRGltZW5zaW9uPgogICAgICAgICA8ZXhpZjpQaXhlbFhEaW1lbnNpb24+NjwvZXhpZjpQaXhlbFhEaW1lbnNpb24+CiAgICAgICAgIDx4bXA6TWV0YWRhdGFEYXRlPjIwMjMtMTAtMTdUMTc6MTQ6NTIrMDI6MDA8L3htcDpNZXRhZGF0YURhdGU+CiAgICAgICAgIDx4bXA6Q3JlYXRlRGF0ZT4yMDIzLTEwLTE3VDE3OjEzOjI5KzAyOjAwPC94bXA6Q3JlYXRlRGF0ZT4KICAgICAgICAgPHhtcDpDcmVhdG9yVG9vbD5QaXhlbG1hdG9yIFBybyAzLjQuMzwveG1wOkNyZWF0b3JUb29sPgogICAgICA8L3JkZjpEZXNjcmlwdGlvbj4KICAgPC9yZGY6UkRGPgo8L3g6eG1wbWV0YT4K5w86hAAAABZJREFUCB1j/P//PwMqYELlgnjUFAIAUd0DCc8FRvYAAAAASUVORK5CYII="
    private const val BLANK_IMAGE_HASH = "bf6d320d5b75603be8f5756f5644d094"
    val blankImageEssentialAsset = EssentialAsset(BLANK_IMAGE_URI, BLANK_IMAGE_HASH)

    @OptIn(DelicateCoroutinesApi::class) // FIXME
    fun chooseIcon(future: CompletableFuture<EssentialAsset?> = CompletableFuture<EssentialAsset?>()): CompletableFuture<EssentialAsset?> {
        var selectingIcon = true

        val modal = platform.pushModal { manager ->
            EssentialModal(manager, requiresButtonPress = false).configure {
                this.titleText = "Select Icon"
                titleTextColor = EssentialPalette.TEXT_HIGHLIGHT
                primaryButtonText = "Cancel"
            }.configureLayout {
                it.layout {
                    spacer(height = 10f)
                }
            }.onPrimaryOrDismissAction {
                // I don't think there's a way to close the file selector remotely. We just have to ignore what they select if they cancel
                selectingIcon = false
                future.complete(null)
            }
        }

        GlobalScope.launch(Dispatchers.IO) runAsync@{
            val result = platform.lwjgl3.get(TinyFd::class.java).openFileDialog(
                "Choose Icon",
                null,
                listOf("*.png"),
                "image files",
                false
            )
            // Ignore the result if they cancelled from the modal?
            if (!selectingIcon) return@runAsync

            val path = if (result == null) null else Path(result)

            if (path == null) {
                // no path selected, so just close the modal
                Window.enqueueRenderOperation {
                    future.complete(null)
                    modal.replaceWith(null)
                }
                return@runAsync
            }
            try {
                val bytes = Files.readAllBytes(path)
                Bitmap.fromOrThrow(bytes.inputStream())
                Window.enqueueRenderOperation {
                    modal.replaceWith(null)
                    val url = "data:;base64," + Base64.getEncoder().encodeToString(bytes)
                    val checksum = DigestUtils.md5Hex(bytes)
                    future.complete(EssentialAsset(url, checksum))
                }
            } catch (e: Exception) {
                LOGGER.info("Error parsing icon file!", e)
                Window.enqueueRenderOperation {
                    modal.replaceWith(ConfirmDenyModal(modal.modalManager, requiresButtonPress = false).configure {
                        titleText = "Select File Error"
                        titleTextColor = EssentialPalette.TEXT_WARNING
                        contentText = "Failed to parse image"
                        contentTextColor = EssentialPalette.TEXT_MID_GRAY
                        primaryButtonText = "Retry"
                        primaryButtonStyle = OutlineButtonStyle.GRAY.defaultStyle
                        primaryButtonHoverStyle = OutlineButtonStyle.GRAY.hoveredStyle
                    }.onPrimaryAction {
                        chooseIcon(future)
                    }.configureLayout {
                        it.layout {
                            spacer(height = 10f)
                        }
                    })
                }
            }
        }
        return future
    }

    fun LayoutScope.divider() = box(Modifier.fillWidth().height(2f).color(EssentialPalette.LIGHT_DIVIDER))

    fun LayoutScope.navButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, action: () -> Unit) = navButton(stateOf(text), modifier, enabled, action)

    fun LayoutScope.navButton(text: State<String>, modifier: Modifier = Modifier, enabled: Boolean = true, action: () -> Unit) =
        menuButton(text, Modifier.fillWidth(padding = 10f) then modifier, action = action).apply {
            rebindEnabled(stateOf(enabled).toV1(stateScope))
        }

    fun LayoutScope.labeledRow(label: String, arrangement: Arrangement = Arrangement.SpaceBetween, inputComponent: LayoutScope.() -> Unit) {
        row(Modifier.fillWidth(), arrangement) {
            text(label, truncateIfTooSmall = true)
            inputComponent()
        }
    }

    fun <T> LayoutScope.labeledInputRow(label: String, arrangement: Arrangement = Arrangement.SpaceBetween, inputComponent: LayoutScope.() -> StateTextInput<T>): StateTextInput<T> {
        val input: StateTextInput<T>
        row(Modifier.fillWidth(), arrangement) {
            text(label, truncateIfTooSmall = true)
            input = inputComponent()
        }
        return input
    }

    fun LayoutScope.labeledBooleanInputRow(
        label: String,
        initialValue: Boolean,
        inputModifier: Modifier = Modifier,
        horizontalArrangement: Arrangement = Arrangement.SpaceBetween,
        onSetValue: (Boolean) -> Unit
    ) {
        labeledListInputRow(
            label,
            initialValue,
            listStateOf(EssentialDropDown.Option("True", true), EssentialDropDown.Option("False", false)),
            inputModifier,
            horizontalArrangement,
            onSetValue
        )
    }

    fun LayoutScope.labeledIntInputRow(
        label: String,
        state: MutableState<Int>,
        inputModifier: Modifier = Modifier,
        horizontalArrangement: Arrangement = Arrangement.SpaceBetween,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
    ) = labeledInputRow(label, horizontalArrangement) {
        essentialIntInput(state, inputModifier, min, max)
    }

    fun LayoutScope.labeledLongInputRow(
        label: String,
        state: MutableState<Long>,
        inputModifier: Modifier = Modifier,
        horizontalArrangement: Arrangement = Arrangement.SpaceBetween,
        min: Long = Long.MIN_VALUE,
        max: Long = Long.MAX_VALUE,
    ) = labeledInputRow(label, horizontalArrangement) {
        essentialLongInput(state, inputModifier, min, max)
    }

    fun LayoutScope.labeledFloatInputRow(
        label: String,
        state: MutableState<Float>,
        inputModifier: Modifier = Modifier,
        horizontalArrangement: Arrangement = Arrangement.SpaceBetween,
        min: Float = Float.NEGATIVE_INFINITY,
        max: Float = Float.POSITIVE_INFINITY,
    ) = labeledInputRow(label, horizontalArrangement) {
        essentialFloatInput(state, inputModifier, min, max)
    }

    fun LayoutScope.labeledDoubleInputRow(
        label: String,
        state: MutableState<Double>,
        inputModifier: Modifier = Modifier,
        horizontalArrangement: Arrangement = Arrangement.SpaceBetween,
        min: Double = Double.NEGATIVE_INFINITY,
        max: Double = Double.POSITIVE_INFINITY,
    ) = labeledInputRow(label, horizontalArrangement) {
        essentialDoubleInput(state, inputModifier, min, max)
    }

    fun LayoutScope.labeledStringInputRow(
        label: String,
        state: MutableState<String>,
        inputModifier: Modifier = Modifier,
        horizontalArrangement: Arrangement = Arrangement.SpaceBetween,
    ) = labeledInputRow(label, horizontalArrangement) {
        essentialStringInput(state, inputModifier)
    }

    fun LayoutScope.labeledNullableStringInputRow(
        label: String,
        state: MutableState<String?>,
        inputModifier: Modifier = Modifier,
        horizontalArrangement: Arrangement = Arrangement.SpaceBetween,
    ) = labeledInputRow(label, horizontalArrangement) {
        essentialNullableStringInput(state, inputModifier)
    }

    fun LayoutScope.labeledManagedNullableISODateInputRow(
        label: String,
        state: MutableState<Instant?>,
        inputModifier: Modifier = Modifier,
        horizontalArrangement: Arrangement = Arrangement.SpaceBetween,
    ) = labeledInputRow(label, horizontalArrangement) {
        essentialManagedNullableISODateInput(state, inputModifier)
    }

    fun LayoutScope.labeledNullableISODateInputRow(
        label: String,
        state: MutableState<Instant?>,
        inputModifier: Modifier = Modifier,
        horizontalArrangement: Arrangement = Arrangement.SpaceBetween,
    ) = labeledInputRow(label, horizontalArrangement) {
        essentialNullableISODateInput(state, inputModifier)
    }

    fun LayoutScope.labeledISODateInputRow(
        label: String,
        state: MutableState<Instant>,
        inputModifier: Modifier = Modifier,
        horizontalArrangement: Arrangement = Arrangement.SpaceBetween,
    ) = labeledInputRow(label, horizontalArrangement) {
        essentialISODateInput(state, inputModifier)
    }

    fun <E : Enum<E>> LayoutScope.labeledEnumInputRow(
        label: String,
        initialValue: E,
        inputModifier: Modifier = Modifier,
        enumFilter: (ListState<EssentialDropDown.Option<E>>) -> ListState<EssentialDropDown.Option<E>> = { it },
        horizontalArrangement: Arrangement = Arrangement.SpaceBetween,
        onSetValue: (E) -> Unit
    ) {
        labeledListInputRow(
            label,
            initialValue,
            enumFilter(stateOf(initialValue.declaringJavaClass.enumConstants.map { EssentialDropDown.Option(it.name, it) }).toListState()),
            inputModifier,
            horizontalArrangement,
            onSetValue
        )
    }

    fun <T> LayoutScope.labeledListInputRow(
        label: String,
        initialValue: T,
        optionsList: List<T>,
        nameMapper: (T) -> String,
        inputModifier: Modifier = Modifier,
        horizontalArrangement: Arrangement = Arrangement.SpaceBetween,
        onSetValue: (T) -> Unit
    ) {
        labeledListInputRow(label, initialValue, stateOf(optionsList.map { EssentialDropDown.Option(nameMapper(it), it) }).toListState(), inputModifier, horizontalArrangement, onSetValue)
    }

    fun <T> LayoutScope.labeledListInputRow(
        label: String,
        initialValue: T,
        optionsList: ListState<T>,
        nameMapper: (T) -> String,
        inputModifier: Modifier = Modifier,
        horizontalArrangement: Arrangement = Arrangement.SpaceBetween,
        onSetValue: (T) -> Unit
    ) {
        labeledListInputRow(label, initialValue, optionsList.mapEach { EssentialDropDown.Option(nameMapper(it), it) }, inputModifier, horizontalArrangement, onSetValue)
    }

    fun <T> LayoutScope.labeledListInputRow(
        label: String,
        initialValue: T,
        optionsList: ListState<EssentialDropDown.Option<T>>,
        inputModifier: Modifier = Modifier,
        horizontalArrangement: Arrangement = Arrangement.SpaceBetween,
        onSetValue: (T) -> Unit
    ) {
        labeledRow(label, horizontalArrangement) {
            val dropDown = EssentialDropDown(initialValue, optionsList)
            dropDown.selectedOption.onSetValue(stateScope) { onSetValue(it.value) }
            dropDown(inputModifier)
        }
    }

    fun LayoutScope.addAutoCompleteMenu(
        input: StateTextInput<*>,
        items: ListState<Pair<String, String>>,
    ) {
        val minChars = items.map { if (it.size > 20) 2 else 0 }
        val options = memo {
            val text = input.textState()
            if (text.length < minChars()) return@memo listOf()
            return@memo items().filter { it.first.contains(text, ignoreCase = true) || it.second.contains(text, ignoreCase = true) }
        }.toListState().mapEach { ContextOptionMenu.Option("${it.second} (${it.first})", null) { input.setText(it.first) } }
        var menu: ContextOptionMenu? = null
        input.focusedState().onChange(stateScope) { focused ->
            menu?.close()
            menu = null
            if (focused) {
                menu = ContextOptionMenu.create(input, options)
            }
        }
        // Fixes menu staying around if manually entering cosmetic
        input.state.onChange(stateScope) {
            menu?.close()
            menu = null
        }
    }

}
