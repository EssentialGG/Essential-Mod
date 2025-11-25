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
package gg.essential.gui.friends.message.screenshot

import com.sparkuniverse.toolbox.chat.model.Channel
import gg.essential.connectionmanager.common.packet.Packet
import gg.essential.connectionmanager.common.packet.chat.ServerChatChannelMessagePacket
import gg.essential.elementa.components.Window
import gg.essential.gui.elementa.state.v2.MutableListState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.clear
import gg.essential.gui.elementa.state.v2.combinators.and
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.not
import gg.essential.gui.elementa.state.v2.mutableListStateOf
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.friends.state.SocialStates
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.RemoteScreenshot
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.ScreenshotUploadToast.ToastProgress
import gg.essential.media.model.Media
import gg.essential.media.model.MediaVariant
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class ScreenshotAttachmentManager(
    val channel: Channel,
    val isScreenOpen: State<Boolean>,
    val socialStates: SocialStates,
) {

    val maxSelectAmount = 10

    val isPickingScreenshots = mutableStateOf(false)
    val selectedImages: MutableListState<ScreenshotId> = mutableListStateOf()
    val hasSelectedImages = selectedImages.map { it.isNotEmpty() }
    val isConfirmingScreenshots = hasSelectedImages and !isPickingScreenshots
    val isUploading = mutableStateOf(false)

    val totalProgressPercentage = mutableStateOf(0)
    private val initialProgress: ToastProgress = ToastProgress.Step(0)
    private val progresses = mutableMapOf<ScreenshotId, ToastProgress>()

    fun uploadAndSend() {

        val screenshotManager = platform.screenshotManager

        val uploadTasks = mutableMapOf<ScreenshotId, CompletableFuture<Media>>()

        for (screenshotId in selectedImages.get()) {
            when (screenshotId) {
                is LocalScreenshot -> {
                    progresses[screenshotId] = initialProgress
                    uploadTasks[screenshotId] = screenshotManager.upload(
                        screenshotId.path,
                        screenshotManager.screenshotMetadataManager.getOrCreateMetadata(screenshotId.path)
                    ) {
                        Window.enqueueRenderOperation {
                            progresses[screenshotId] = it
                        }
                    }
                }

                is RemoteScreenshot -> {
                    uploadTasks[screenshotId] = CompletableFuture.completedFuture(screenshotId.media)
                }
            }
        }

        isUploading.set(true)
        selectedImages.clear()

        CompletableFuture.allOf(*uploadTasks.values.toTypedArray<CompletableFuture<Media>>())
            .whenCompleteAsync({ _, _ ->
                val embeds = mutableListOf<MediaVariant>()
                for ((id, task) in uploadTasks) {

                    val media = try {
                        task.get()
                    } catch (e: ExecutionException) {
                        LOGGER.error("Unable to upload image ${id.name}", e.cause)
                        // Set progress to failed unless it already is, in which case it'll likely have a better message
                        if ((progresses[id] as? ToastProgress.Complete)?.success != false) {
                            progresses[id] = ToastProgress.Complete("Failed: An unknown error occurred", false)
                        }
                        continue
                    }

                    progresses[id] = ToastProgress.Complete("Screenshot was uploaded.", true)
                    val embed = media.variants["embed"]
                    if (embed == null) {
                        LOGGER.error("Unable to share screenshot ${media.id} to channel as it's missing its embed.")
                        continue
                    }
                    embeds.add(embed)
                }

                if (embeds.isEmpty()) {
                    return@whenCompleteAsync
                }

                val message = embeds.joinToString("\n") { it.url }

                socialStates.messages.sendMessage(
                    channel.id,
                    message
                ) { response: Optional<Packet> ->
                    if (!response.isPresent || response.get() !is ServerChatChannelMessagePacket) {
                        LOGGER.error("Failed to send message of ${embeds.size} screenshot(s) to channel.")
                    }
                }
            }, Window::enqueueRenderOperation)
    }

    fun updateProgress() {
        if (progresses.isEmpty()) {
            isUploading.set(false)
            return
        }
        val totalPercentage = progresses.values.sumOf { (it as? ToastProgress.Step)?.completionPercent ?: 100 }
        totalProgressPercentage.set(totalPercentage / progresses.size)
        isUploading.set(totalProgressPercentage.get() < 100)

        if (!isUploading.get()) {
            clearProgress()
        }
    }

    private fun clearProgress() {
        progresses.clear()
        totalProgressPercentage.set(0)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ScreenshotAttachmentManager::class.java)
    }
}