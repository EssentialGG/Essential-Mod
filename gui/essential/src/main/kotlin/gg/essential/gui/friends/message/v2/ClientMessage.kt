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

import com.sparkuniverse.toolbox.chat.model.Channel
import gg.essential.cosmetics.CosmeticId
import gg.essential.gui.friends.message.MessageUtils
import gg.essential.gui.friends.message.MessageUtils.handleMarkdownUrls
import gg.essential.mod.Model
import gg.essential.mod.Skin
import okhttp3.HttpUrl
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URL
import java.time.Instant
import java.util.*

data class ClientMessage(
    val id: Long,
    val channel: Channel,
    val sender: UUID,
    val contents: String,
    val sendState: SendState,
    val replyTo: MessageRef?,
    val lastEditTime: Long?,
    val createdAt: Long,
) {
    val sendTime: Instant = Instant.ofEpochMilli(createdAt)
    val sent = sendState == SendState.Confirmed
    val parts: List<Part> = buildList {
        val cleanedText = contents.handleMarkdownUrls()
        var strippedText = cleanedText

        fun stripUrl(url: String) {
            val bracketedUrl = "<$url>"
            strippedText = if (strippedText.indexOf(bracketedUrl) != -1) {
                strippedText.replace(bracketedUrl, "")
            } else {
                strippedText.replace(url, "")
            }
        }

        // Replace no embed urls with "" so they are not parsed for embeds
        MessageUtils.SCREENSHOT_URL_REGEX.findAll(MessageUtils.URL_NO_EMBED_REGEX.replace(cleanedText, "")).forEach {
            val match = it.value.removeSuffix(">")

            try {
                add(Part.Image(URL(match)))
                stripUrl(match)
            } catch (e: MalformedURLException) {
                LOGGER.debug("Ignoring invalid URL:", e)
            }
        }

        MessageUtils.URL_REGEX.findAll(MessageUtils.URL_NO_EMBED_REGEX.replace(cleanedText, "")).forEach {
            val match = it.value.removeSuffix(">")

            HttpUrl.parse(match)?.let { url ->
                if (url.host() == "essential.gg" && url.pathSegments().size > 1) {
                    if (url.pathSegments()[0] == "gift") {
                        add(Part.Gift(url.pathSegments()[1]))
                        stripUrl(match)
                    } else if (url.pathSegments()[0] == "skin" && url.pathSegments().size > 2) {
                        add(Part.Skin(Skin(url.pathSegments()[2], Model.byVariantOrDefault(url.pathSegments()[1]))))
                        stripUrl(match)
                    }
                }
            }
        }

        if (strippedText.isNotBlank()) {
            add(0, Part.Text(MessageUtils.URL_NO_EMBED_REGEX.replace(cleanedText, "<$1>"))) // Strip outer < > from no embed urls
        }
    }

    sealed interface Part {
        data class Text(val content: String) : Part
        data class Image(val url: URL) : Part
        data class Gift(val id: CosmeticId) : Part
        data class Skin(val skin: gg.essential.mod.Skin) : Part
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClientMessage::class.java)
    }
}

sealed interface SendState {

    data object Sending : SendState

    data object Confirmed : SendState

    data object Failed : SendState

    data class Blocked(val reason: String) : SendState {

        val toastMessage = when(reason) {
            CONTAINS_NON_WHITELISTED_DOMAIN -> "You cannot share this link"
            else -> "You cannot send this message"
        }

        val tooltipMessage = when(reason) {
            CONTAINS_NON_WHITELISTED_DOMAIN -> "Message not sent: This link is not allowed"
            else -> "Message not sent: This isn't allowed"
        }

        companion object {

            private const val CONTAINS_NON_WHITELISTED_DOMAIN = "CONTAINS_NON_WHITELISTED_DOMAIN"
        }
    }
}