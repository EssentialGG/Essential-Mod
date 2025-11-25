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
package gg.essential.util

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.components.image.BlurHashImage
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.layoutdsl.*
import gg.essential.mod.EssentialAsset
import gg.essential.network.connectionmanager.cosmetics.AssetLoader
import gg.essential.network.connectionmanager.skins.PlayerSkinLookup
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.*
import java.util.concurrent.CompletableFuture.completedFuture
import javax.imageio.ImageIO
import kotlin.io.inputStream

object CachedAvatarImage {
    private val fallbackImage = BlurHashImage("U9QuA+e8vyu48wVsVYkq_~tlP9Z~Y7pIyXVX")
    private val fallbackImageFactory = ImageFactory { UIImage(completedFuture(null), fallbackImage) }

    private object PlayerHeadAssetType : AssetLoader.AssetType<ImageFactory>({ _, bytes ->
        parseAsset(bytes)
    })

    private object PlayerHeadGrayscaleAssetType : AssetLoader.AssetType<ImageFactory>({ _, bytes ->
        parseAsset(bytes) { image ->
            for (x in 0..< image.width) {
                for (y in 0..< image.height) {
                    val rgb = Color(image.getRGB(x, y))
                    val grey = ((rgb.red * 0.2126f + rgb.green * 0.7152f + rgb.blue * 0.0722f) * 0.7f).toInt()
                    image.setRGB(x, y, Color(grey, grey, grey).rgb)
                }
            }
        }
    })

    private fun parseAsset(bytes: ByteArray, modifier: (BufferedImage) -> Unit = {}): ImageFactory {
        // Parse skin
        val skinImage = ImageIO.read(bytes.inputStream())

        // Extract head and compose hat layer on top
        val headImage = BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
        val graphics = headImage.graphics
        graphics.drawImage(skinImage, 0, 0, 8, 8, 8, 8, 16, 16, null)
        graphics.drawImage(skinImage, 0, 0, 8, 8, 40, 8, 48, 16, null)

        // Apply modifier
        modifier(headImage)

        // Create a UIImage from it (so it gets uploaded to gpu memory, and is then immediately ready when needed, and
        // so all users can share the same gpu memory instead of uploading it multiple times)
        return UIImage(completedFuture(headImage))
            .toImageFactory(fallbackImage) // use fallback image until gpu upload is done
    }

    /**
     * Creates a [UIComponent] that will contain the head of the given user's skin for use as an avatar image.
     *
     * This component also gives a solid shadow when required by wrapping the UIImage in a UIContainer.
     *
     * The image will be cached for half an hour. If the cached image has expired, it will still be used as the loading
     * image until a new one has been retrieved.
     * If no old image is available (or not yet read from disk), a [BlurHashImage] is used as fallback.
     *
     * If the image needs to be modified, for example, to apply a [ColorConvertOp], a unary operator is provided to
     * allow this to be done once the image future completes.
     */
    @JvmStatic
    @JvmOverloads
    fun create(
        uuid: UUID,
        priority: AssetLoader.Priority = AssetLoader.Priority.High,
        grayscale: Boolean = false,
    ): UIComponent {
        val skinState = PlayerSkinLookup.getSkin(uuid)
        val imageFutureState = memo {
            val skin = skinState() ?: return@memo stateOf(null)
            val asset = platform.assetLoader.getAsset(
                EssentialAsset(skin.url, skin.hash.padStart(64, '0')),
                priority,
                if (grayscale) PlayerHeadGrayscaleAssetType else PlayerHeadAssetType,
            )
            asset.parsedOrNull.toState()
        }
        val imageState = State { imageFutureState()() ?: fallbackImageFactory }
        val uiContainer = UIContainer()
        uiContainer.layout(Modifier.width(8f).heightAspect(1f)) {
            image(imageState, Modifier.fillParent())
        }
        return uiContainer
    }
}
