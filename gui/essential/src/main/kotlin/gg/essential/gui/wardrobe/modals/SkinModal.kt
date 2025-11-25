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
package gg.essential.gui.wardrobe.modals

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.OutlineButtonStyle
import gg.essential.gui.common.modal.CancelableInputModal
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.EssentialModal
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.common.modal.UsernameInputModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.notification.Notifications
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.sendCheckmarkNotification
import gg.essential.gui.skin.preprocessSkinImage
import gg.essential.gui.wardrobe.Item
import gg.essential.gui.wardrobe.ItemId
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.components.openWardrobeWithHighlight
import gg.essential.handlers.MojangSkinManager
import gg.essential.image.PNGFile
import gg.essential.mod.Model
import gg.essential.mod.Skin
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.image.bitmap.Bitmap
import gg.essential.util.image.bitmap.from
import gg.essential.util.image.bitmap.toTexture
import gg.essential.util.lwjgl3.api.*
import gg.essential.vigilance.utils.onLeftClick
import kotlinx.coroutines.Dispatchers
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.Path
import kotlin.io.path.inputStream

class SkinModal private constructor(
    modalManager: ModalManager,
    skin: Skin?,
    skinOverride: UIdentifier? = null,
    buttonText: String,
    initialName: String,
    defaultModel: Model,
    onConfirm: (String, Model) -> Unit,
) : CancelableInputModal(modalManager, "Skin Name", initialName, maxLength = 22) {

    constructor(modalManager: ModalManager, skin: Skin, buttonText: String, initialName: String, onConfirm: (String, Model) -> Unit) : this(modalManager, skin, null, buttonText, initialName, skin.model, onConfirm)

    constructor(modalManager: ModalManager, skinOverride: UIdentifier, buttonText: String, initialName: String, defaultModel: Model, onConfirm: (String, Model) -> Unit) : this(modalManager, null, skinOverride, buttonText, initialName, defaultModel, onConfirm)

    private val modelState = mutableStateOf(skin?.model ?: defaultModel)


    init {
        configure {
            primaryButtonText = buttonText
            modalWidth = 192f

            // Remove all spacing, so we can set it all in one place below
            contentTextSpacingState.rebind(BasicState(0f))
            spacer.setHeight(0.pixels)
            customContent.setY(SiblingConstraint())
            bottomSpacer.setHeight(0.pixels)

            // Allow dynamic resizing
            setMaxInputWidth(200.pixels)
            setDynamicInputContainerWidth(8.pixels, 90.pixels)

            onPrimaryActionWithValue {
                onConfirm(it, modelState.get())
            }

        }

        fun LayoutScope.modelSelector(model: Model) {
            val isSelected = modelState.map { it == model }

            val tickColor = isSelected.map { if (it) EssentialPalette.LINK else EssentialPalette.BUTTON_HIGHLIGHT }
            val tickColorHover = isSelected.map { if (it) EssentialPalette.LINK_HIGHLIGHT else EssentialPalette.SCROLLBAR }

            val name = when (model) {
                Model.STEVE -> "Wide"
                Model.ALEX -> "Slim"
            }

            row(Modifier.width(38f).height(14f).hoverScope(), Arrangement.spacedBy(0f, FloatPosition.CENTER)) {
                box(Modifier.width(9f).heightAspect(1f).color(EssentialPalette.COMPONENT_BACKGROUND).hoverColor(EssentialPalette.BUTTON_HIGHLIGHT).shadow()) {
                    image(EssentialPalette.RADIO_TICK_5X, Modifier.color(tickColor).hoverColor(tickColorHover))
                }
                spacer(width = 5f)
                text(name, Modifier.color(EssentialPalette.TEXT_MID_GRAY).shadow(EssentialPalette.BLACK))
            }.onLeftClick {
                USound.playButtonPress()
                modelState.set(model)
            }
        }

        val skinHash = skin?.hash ?: "f91f0820500c414d308c5678594631b917e51e06a31fedaacac5dad1a44a49d8"

        val emulatedPlayer = platform.newUIPlayer(
            camera = stateOf(null),
            profile = modelState.map { Pair(Skin(skinHash, it), null)},
            cosmetics = stateOf(emptyMap()),
            skinTextureOverride = skinOverride,
        )

        val previewWrapper by UIContainer().constrain {
            x = CenterConstraint()
            y = SiblingConstraint()
        } childOf customContent

        previewWrapper.layout(Modifier.childBasedSize()) {
            column {
                spacer(height = 17f)
                emulatedPlayer(Modifier.height(116f))
                spacer(height = 17f)
                row(Arrangement.spacedBy(6f)) {
                    modelSelector(Model.STEVE)
                    modelSelector(Model.ALEX)
                }
                spacer(height = 20f)
            }
        }
    }

    class SelectFileModal(modalManager: ModalManager) : EssentialModal(modalManager, requiresButtonPress = false) {
        init {
            configure {
                titleText = "Select File"
                titleTextColor = EssentialPalette.TEXT_HIGHLIGHT
                contentText = "Choose a skin file in the file browser."
                contentTextColor = EssentialPalette.TEXT_MID_GRAY
                primaryButtonText = "Cancel"
            }
            configureLayout {
                it.layout {
                    spacer(height = 10f)
                }
            }
        }
    }

    class IncorrectFileModal(modalManager: ModalManager, errorText: String) : ConfirmDenyModal(modalManager, requiresButtonPress = false) {
        init {
            titleText = "Incorrect File Format"
            titleTextColor = EssentialPalette.TEXT_WARNING
            contentText = errorText
            contentTextColor = EssentialPalette.TEXT
            primaryButtonText = "Select New File"
            primaryButtonStyle = OutlineButtonStyle.GRAY.defaultStyle
            primaryButtonHoverStyle = OutlineButtonStyle.GRAY.hoveredStyle

            contentTextSpacingState.rebind(BasicState(17f))
            spacer.setHeight(13.pixels)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SkinModal::class.java)

        fun add(modalManager: ModalManager, skin: Skin, initialName: String) = SkinModal(modalManager, skin, "Add Skin", initialName) { name, model ->
            val updatedSkin = skin.copy(model = model)
            platform.skinsManager.addSkin(name, updatedSkin).whenComplete { skin, throwable ->
                if (skin != null) {
                    sendCheckmarkNotification("Skin has been added.") {
                        openWardrobeWithHighlight(ItemId.SkinItem(skin.id))
                    }
                } else {
                    LOGGER.warn("Error adding skin!", throwable)
                    Notifications.push("Error adding skin", "An unexpected error has occurred. Try again.")
                }
            }
        }

        fun edit(modalManager: ModalManager, skin: Item.SkinItem) = SkinModal(modalManager, skin.skin, "Save", skin.name) { name, model ->
            val skinsManager = platform.skinsManager
            // Because the update packets are separate, we have to do each one separately
            if (skin.name != name) {
                skinsManager.renameSkin(skin.id, name)
            }
            if (skin.skin.model != model) {
                skinsManager.setSkinModel(skin.id, model)
            }
        }

        fun steal(modalManager: ModalManager, state: WardrobeState) = UsernameInputModal(modalManager, "") { uuid, name, modal ->
            modal.primaryButtonEnableStateOverride.set(false)
            modal.clearError()
            val skin = MojangSkinManager.getTextureProperty(uuid)?.first?.propertyToSkin()
            if (skin != null) {
                modal.replaceWith(add(modalManager, skin, initialName = state.skinsManager.getNextIncrementalSkinName()))
            } else {
                modal.primaryButtonEnableStateOverride.set(true)
                modal.setError("Invalid username")
            }
        }.configure {
            titleText = "Username"
            contentText = "Enter a Minecraft username\nto add their skin to your library."
            primaryButtonText = "Steal"
        }

        /**
         * Uploads skin file and adds new skin to the Wardrobe.
         * Mojang skin uploads are reverted immediately to keep the skin unselected.
         */
        fun addFile(modalManager: ModalManager, path: Path, skinOverride: UIdentifier, initialName: String, defaultModel: Model) = SkinModal(modalManager, skinOverride, "Add Skin", initialName, defaultModel) { name, model ->
            val mojangSkinManager = platform.mojangSkinManager
            val accessToken = USession.activeNow().token
            val oldSkin = mojangSkinManager.activeSkin
            val skin = mojangSkinManager.uploadSkin(accessToken, model, path.toFile(), false)

            if (skin == null) {
                Notifications.push("Skin Upload", "Skin upload failed!")
                return@SkinModal
            }

            val oldActiveSkin = oldSkin.join()
            mojangSkinManager.changeSkin(accessToken, oldActiveSkin.model, oldActiveSkin.url)
            mojangSkinManager.flushChanges(false)

            platform.skinsManager.addSkin(name, skin).whenComplete { skinItem, throwable ->
                if (skinItem != null) {
                    sendCheckmarkNotification("Skin has been added.")
                } else {
                    LOGGER.warn("Error adding skin!", throwable)
                    Notifications.push("Error adding skin", "An unexpected error has occurred. Try again.")
                }
            }
        }

        fun fromURL(modalManager: ModalManager, state: WardrobeState) = CancelableInputModal(modalManager, "", "").configure {
            titleText = "By URL"
            contentText = "Enter a link to a skin file to add the\nskin to your library."
            primaryButtonText = "Next"

            setInputContainerWidthConstraint(159.pixels)
        }.apply {
            primaryButtonAction = {
                clearError()
                val url = inputTextState.get()
                primaryButtonEnableStateOverride.set(false)
                Dispatchers.IO.dispatch(EmptyCoroutineContext) runAsync@{
                    val tempFilePath = Files.createTempFile("skin-by-url", ".png")
                    try {
                        httpGetToFileBlocking(url, tempFilePath)
                    } catch (e: Exception) {
                        LOGGER.warn("Error downloading skin file from $url!", e)
                        Window.enqueueRenderOperation {
                            primaryButtonEnableStateOverride.set(true)
                            when (e) {
                                is MalformedURLException, is IllegalArgumentException -> setError("Invalid URL")
                                else -> setError("Page could not be loaded")
                            }
                        }
                        return@runAsync
                    }
                    val (image, errorText) = checkFile(tempFilePath, true)

                    Window.enqueueRenderOperation {
                        primaryButtonEnableStateOverride.set(true)
                        if (image == null) {
                            setError(errorText)
                        } else {
                            replaceWith(addFile(modalManager, tempFilePath, imageToLocation(image), state.skinsManager.getNextIncrementalSkinName(), getDetectedModel(image)))
                        }
                    }
                }
            }
        }

        // Added this one here too, so they're all in one place
        fun upload(modalManager: ModalManager, state: WardrobeState): Modal {
            var selectingSkin = true

            val modal = SelectFileModal(modalManager).onPrimaryOrDismissAction {
                // I don't think there's a way to close the file selector remotely. We just have to ignore what they select if they cancel
                selectingSkin = false
            }

            Dispatchers.IO.dispatch(EmptyCoroutineContext) runAsync@{
                val result = platform.lwjgl3.get(TinyFd::class.java).openFileDialog(
                    "Choose Skin",
                    null,
                    listOf("*.png"),
                    "image files",
                    false
                )
                // Ignore the result if they cancelled from the modal?
                if (!selectingSkin) {
                    return@runAsync
                }
                val path = if (result == null) null else Path(result)

                if (path == null) {
                    // no path selected, so just close the modal
                    Window.enqueueRenderOperation { modal.replaceWith(null) }
                    return@runAsync
                }
                val (image, errorText) = checkFile(path, false)
                Window.enqueueRenderOperation {
                    modal.replaceWith(
                        if (image == null) {
                            IncorrectFileModal(modalManager, errorText).also {
                                it.onPrimaryAction { it.replaceWith(upload(modal.modalManager, state)) }
                            }
                        } else {
                            addFile(modal.modalManager, path, imageToLocation(image), state.skinsManager.getNextIncrementalSkinName(), getDetectedModel(image))
                        }
                    )
                }
            }

            return modal
        }

        private fun checkFile(path: Path, loadedFromURL: Boolean): Pair<Bitmap?, String> {
            try {
                if (!PNGFile.hasValidSignature(path)) {
                    return Pair(null, "This PNG file is broken" + if (!loadedFromURL) "." else "")
                }

                val image = path.inputStream().use { inputStream -> Bitmap.from(inputStream) }
                    ?: return Pair(null, "This PNG file is broken" + if (!loadedFromURL) "." else "")

                // Support 64x32 for older skins
                if ((image.height != 64 && image.height != 32) || image.width != 64) {
                    return Pair(null, if (loadedFromURL) "Incorrect size, must be 64x64 or 64x32 pixels" else "Skin file must be a PNG image with a resolution of 64x64 or 64x32 pixels.")
                }

                return Pair(image, "")
            } catch (e: Exception) {
                LOGGER.info("Error parsing skin file!", e)
                return Pair(null, "Unexpected error" + if (!loadedFromURL) "." else "")
            }
        }

        private fun imageToLocation(image: Bitmap): UIdentifier {
            return platform.registerCosmeticTexture("uploaded-skin", preprocessSkinImage(image).toTexture())
        }

        private fun getDetectedModel(image: Bitmap): Model {
            return if (image[50, 16].a == 0.toUByte()) Model.ALEX else Model.STEVE
        }

    }

}
