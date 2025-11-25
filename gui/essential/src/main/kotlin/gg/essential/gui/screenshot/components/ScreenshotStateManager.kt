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

import com.google.common.collect.MapMaker
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.withSetter
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.State as StateV2
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.RemoteScreenshot
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.network.connectionmanager.media.IScreenshotManager

/**
 * Manages states relating to the screenshot browser
 */
class ScreenshotStateManager(
    private val screenshotManager: IScreenshotManager,
) {
    private val metadataStateCache: MutableMap<ScreenshotId, StateV2<ClientScreenshotMetadata?>> = MapMaker().weakValues().makeMap()
    fun metadata(id: ScreenshotId): StateV2<ClientScreenshotMetadata?> =
        metadataStateCache.getOrPut(id) {
            memo { screenshotManager.screenshots().find { it.id == id }?.metadata }
        }

    /**
     * Gets the current favorite state for the screenshot identified by [id].
     * Updates to this state will be automatically forwarded to the [gg.essential.network.connectionmanager.media.ScreenshotManager]
     * to be persisted
     */
    fun getFavoriteState(id: ScreenshotId): MutableState<Boolean> =
        metadata(id)
            .map { it?.favorite ?: false }
            .withSetter { update ->
                val oldValue = getUntracked()
                val newValue = update(oldValue)
                if (oldValue == newValue) return@withSetter
                when (id) {
                    is LocalScreenshot -> screenshotManager.setFavorite(id.path, newValue)
                    is RemoteScreenshot -> screenshotManager.setFavorite(id.media, newValue)
                }
            }

    /**
     * Gets a text state that is returns the string that should be displayed
     * for the favorite action
     */
    fun getFavoriteTextState(id: ScreenshotId): State<String> {
        return mapFavoriteText(getFavoriteState(id))
    }

    fun mapFavoriteText(favorite: State<Boolean>): State<String> {
        return favorite.map {
            if (it) {
                "Remove Favorite"
            } else {
                "Favorite"
            }
        }
    }
}