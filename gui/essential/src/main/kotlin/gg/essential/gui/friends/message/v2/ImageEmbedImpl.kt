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

import com.sparkuniverse.toolbox.chat.enums.ChannelType
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.common.modal.OpenLinkModal
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.friends.message.MessageUtils
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.screenshot.constraints.AspectPreservingFillConstraint
import gg.essential.gui.screenshot.copyScreenshotToClipboard
import gg.essential.gui.screenshot.saveImageAsScreenshot
import gg.essential.gui.util.hoveredState
import gg.essential.gui.util.stateBy
import gg.essential.universal.ChatColor
import gg.essential.universal.UKeyboard
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.vigilance.utils.onLeftClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile

class ImageEmbedImpl(
    url: URL,
    wrapper: MessageWrapper,
) : ImageEmbed(url, wrapper) {

    private val coroutineScope = CoroutineScope(Dispatchers.Client)

    private val loadingState = BasicState(false)
    private var loadedImage: BufferedImage? = null
    private var loadingJob: Job? = null

    private val aspectRatio = BasicState(16 / 9f)
    private val highlightedState = BasicState(false)
    private val focusedView by FocusedView()

    private val loadingEmbed by UIBlock(EssentialPalette.LIGHT_DIVIDER).constrain {
        width = RelativeConstraint()
        height = AspectConstraint(9 / 16f)
    }.bindParent(this, loadingState)

    private val loadingIcon by LoadingIcon(2.0) childOf loadingEmbed

    init {

        constrain {
            x = 10.pixels(alignOpposite = wrapper.sentByClient)
            y = SiblingConstraint(4f)
            width = MessageUtils.getMessageWidth(wrapper.channelType == ChannelType.ANNOUNCEMENT)
            height = ChildBasedMaxSizeConstraint()
        }

        if (loadingJob == null) {
            prepareAndLoad()
        }

    }


    private fun prepareAndLoad() {
        loadingState.set(true)

        val localPaths = MessageUtils.SCREENSHOT_URL_REGEX.find(url.toString())
            ?.let { platform.screenshotManager.getUploadedLocalPathsCache(it.groupValues[1]) }
            ?: emptyList()

        loadingJob?.cancel()
        loadingJob = coroutineScope.launch {
            val bufferedImage = withContext(Dispatchers.IO) {
                localPaths.firstNotNullOfOrNull { fetchLocalImage(it) } ?: fetchRemoteImage()
            }
            val uiImage = bufferedImage?.let { loadUIImage(it) }
            loadImage(bufferedImage, uiImage)
        }
    }

    override fun copyImageToClipboard() {
        val loadedImage = loadedImage
        if (loadedImage != null && loadedImage != noImage) {
            coroutineScope.launch(Dispatchers.IO) {
                val tempFile = Files.createTempFile("essential-screenshot", "png").toFile()
                ImageIO.write(loadedImage, "png", tempFile)
                withContext(Dispatchers.Client) {
                    copyScreenshotToClipboard(tempFile.toPath())
                }
                tempFile.delete()
            }
        }
    }

    override fun saveImageToScreenshotBrowser() {
        val loadedImage = loadedImage
        if (loadedImage != null && loadedImage != noImage) {
            coroutineScope.launch {
                saveImageAsScreenshot(loadedImage)
            }
        }
    }

    override fun beginHighlight() {
        highlightedState.set(true)
    }

    override fun releaseHighlight() {
        highlightedState.set(false)
    }

    private suspend fun download(): BufferedImage? {
        val original = httpGetToBytes(url.toString())

        try {
            ImageIO.read(ByteArrayInputStream(original))?.let { return it }
        } catch (e: Exception) {
            LOGGER.debug("Error parsing image", e)
        }

        // Follow metadata if present
        val responseString = String(original)
        val embedUrlMatcher = MessageUtils.imageEmbedRegex.find(responseString) ?: return noImage
        val embedUrl = embedUrlMatcher.groups["url"]?.value ?: return noImage
        if (!MessageUtils.URL_REGEX.matches(embedUrl)) {
            return noImage
        }

        val embedUrlBytes = httpGetToBytes(embedUrl)

        return try {
            ImageIO.read(ByteArrayInputStream(embedUrlBytes))
        } catch (e: Exception) {
            LOGGER.debug("Error parsing image", e)
            null
        }
    }

    /**
     * Attempts to fetch local image and returns BufferedImage if successful
     */
    private fun fetchLocalImage(localPath: Path): BufferedImage? {
        return try {
            if (!localPath.isRegularFile()) {
                LOGGER.debug("Local image path does not point to a file: {}", localPath)
                return null
            }
            localPath.inputStream().use { ImageIO.read(it) }
        } catch (e: Exception) {
            LOGGER.error("Error loading local image using path: $localPath", e)
            null
        }
    }

    /**
     * Attempts to download remote image and returns BufferedImage if successful
     */
    private suspend fun fetchRemoteImage(): BufferedImage? {
        return try {
            download()
        } catch (e: IOException) {
            LOGGER.debug("Error downloading image", e)
            null
        }
    }

    private fun loadImage(loadedImage: BufferedImage?, uiImage: UIImage?) {
        run maybeLoadUIImage@{
            loadingState.set(false)

            this.loadedImage = loadedImage

            if (loadedImage == noImage) {
                return@maybeLoadUIImage
            }

            val imageContainer by UIContainer().apply {
                layout(Modifier.fillWidth()
                    .then(stateBy { Modifier.heightAspect( (1/aspectRatio()).coerceAtMost(9/16f)) })
                ) {}
            } childOf this

            if (uiImage == null) {
                FailedEmbed() childOf imageContainer
            } else {
                loadedImage?.let { aspectRatio.set(it.width / it.height.toFloat()) }

                displayImageEmbed(imageContainer, uiImage)
            }

        }
    }

    private var previewing = false


    private fun getWindow(): Window {
        return Window.of(this)
    }

    inner class FocusedView : UIContainer() {

        private val block = UIBlock(Color(0, 0, 0, 0)).constrain {
            height = 100.percent
            width = 100.percent
        } childOf this

        private val floatImageContainer by UIContainer().constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 1.pixel
            height = 1.pixel
        } childOf this

        private val floatImage by generateEmptyImage().centered().constrain {
            width = AspectPreservingFillConstraint(aspectRatio)
            height = AspectPreservingFillConstraint(aspectRatio)
        }.apply {
            textureMagFilter = UIImage.TextureScalingMode.LINEAR
            textureMinFilter = UIImage.TextureScalingMode.LINEAR
            onKeyType { _, keyCode ->
                if (keyCode == UKeyboard.KEY_ESCAPE) {
                    exitPreview()
                }
            }
            onMouseClick {
                grabWindowFocus()
                it.stopImmediatePropagation()
            }
        } childOf floatImageContainer

        private val openOriginal by EssentialUIText().constrain {
            y = SiblingConstraint(3f) boundTo floatImage
            x = 0.pixels boundTo floatImage
            color = Color(0, 0, 0, 0).toConstraint()
        }.apply {
            bindText(hoveredState().map {
                if (it) {
                    "${ChatColor.UNDERLINE}"
                } else {
                    ""
                } + "Open Original"
            })
        }.onLeftClick {
            OpenLinkModal.openUrl(url.toURI())
        } childOf this

        init {
            constrain {
                width = 100.percent
                height = 100.percent
            }
            onLeftClick {
                exitPreview()
            }
        }

        fun setup(image: UIImage) {
            image.supply(floatImage)
        }

        fun enterPreview() {
            if (previewing) {
                return
            }

            previewing = true
            getWindow().addChild(this)
            floatImage.grabWindowFocus()
            block.animate {
                setColorAnimation(Animations.OUT_EXP, animationTime, Color(0, 0, 0, 200).toConstraint())
            }
            openOriginal.animate {
                setColorAnimation(Animations.OUT_EXP, animationTime, EssentialPalette.TEXT_HIGHLIGHT.toConstraint())
            }
            floatImageContainer.animate {
                setWidthAnimation(Animations.OUT_EXP, animationTime, 75.percentOfWindow)
                setHeightAnimation(Animations.OUT_EXP, animationTime, 75.percentOfWindow)
            }
        }

        private fun exitPreview(callback: () -> Unit = {}) {
            releaseWindowFocus()

            floatImageContainer.animate {
                setWidthAnimation(Animations.OUT_EXP, animationTime, 1.pixel)
                setHeightAnimation(Animations.OUT_EXP, animationTime, 1.pixel)
            }
            openOriginal.animate {
                setColorAnimation(Animations.OUT_EXP, animationTime, Color(0, 0, 0, 0).toConstraint())
            }

            block.animate {
                setColorAnimation(Animations.OUT_EXP, animationTime, Color(0, 0, 0, 0).toConstraint()).onComplete {
                    callback()
                    previewing = false
                    this@FocusedView.hide(instantly = true)
                }
            }
        }

        private fun generateEmptyImage(): UIImage {
            return UIImage(CompletableFuture.completedFuture(BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)))
        }
    }

    inner class FailedEmbed : UIBlock(EssentialPalette.getMessageColor(hoveredState() or messageWrapper.appearHovered.toV1(this@ImageEmbedImpl), false, SendState.Failed)) {
        init {
            layoutAsBox(Modifier.childBasedWidth().childBasedHeight()) {
                column(Modifier.childBasedWidth(10f).childBasedHeight(8f), Arrangement.spacedBy(0f, FloatPosition.CENTER)) {
                    spacer(height = 1f) // button shadow
                    row(Arrangement.spacedBy(10f)) {
                        text("Image failed to load.", Modifier.shadow(EssentialPalette.TEXT_SHADOW))
                        retryButton(Modifier.width(17f).heightAspect(1f))
                    }
                }
            }
        }
    }

    private fun displayImageEmbed(container: UIContainer, image: UIImage) {
        image.apply {
            textureMagFilter = UIImage.TextureScalingMode.LINEAR
            textureMinFilter = UIImage.TextureScalingMode.LINEAR
            focusedView.setup(this)
        }.onLeftClick {
            focusedView.enterPreview()
        }.onMouseClick {
            if (it.mouseButton != 1) return@onMouseClick
            messageWrapper.openOptionMenu(it, this@ImageEmbedImpl)
        }

        val borderModifier = Modifier.color(EssentialPalette.MESSAGE_HIGHLIGHT)
        val sideBorderModifier = borderModifier.width(3f).fillHeight()
        val topBottomBorderModifier = borderModifier.height(3f).fillWidth()

        container.layout {
            box(
                Modifier.then(BasicHeightModifier { AspectPreservingFillConstraint(aspectRatio) })
                    .then(BasicWidthModifier { AspectPreservingFillConstraint(aspectRatio) })
                    .alignHorizontal(if (messageWrapper.sentByClient) Alignment.End else Alignment.Start)
            ) {
                image(Modifier.fillParent())
                if_(highlightedState) {
                    box(sideBorderModifier.alignHorizontal(Alignment.Start))
                    box(sideBorderModifier.alignHorizontal(Alignment.End))
                    box(topBottomBorderModifier.alignVertical(Alignment.Start))
                    box(topBottomBorderModifier.alignVertical(Alignment.End))
                }
            }
        }
    }

    private fun LayoutScope.retryButton(modifier: Modifier = Modifier) {
        IconButton(EssentialPalette.RETRY_7X)(modifier.shadow(Color.BLACK).onLeftClick {
            this@ImageEmbedImpl.clearChildren()
            prepareAndLoad()
        }).apply {
            setColor(hoveredState().map { if (it) EssentialPalette.BUTTON else EssentialPalette.COMPONENT_BACKGROUND }.toConstraint())
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ImageEmbedImpl::class.java)

        private const val animationTime = 0.25f

        // Used to denote that no image should be displayed. This is returned by download()
        // when the web page is successfully fetched, but no image is found.
        private val noImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    }
}
