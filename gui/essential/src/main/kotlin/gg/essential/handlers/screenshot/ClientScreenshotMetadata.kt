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
package gg.essential.handlers.screenshot

import com.sparkuniverse.toolbox.util.DateTime
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.components.ScreenshotProperties
import gg.essential.gui.screenshot.getImageTime
import gg.essential.lib.gson.annotations.SerializedName
import gg.essential.media.model.Media
import gg.essential.media.model.MediaLocationMetadata
import gg.essential.media.model.MediaLocationType
import gg.essential.sps.SpsAddress
import gg.essential.util.USession
import java.nio.file.Path
import java.util.*

data class ClientScreenshotMetadata(
    val authorId: UUID,
    val time: DateTime,
    val checksum: String,
    val editTime: DateTime?,
    val locationMetadata: Location,
    val favorite: Boolean,
    val edited: Boolean,
    /**
     * Contains all known media ids of this screenshot.
     * Note that some, or even all, of these may belong to different user accounts, so it might not be possible to modify them
     * as the current user. If one of the ids is owned by the current user, [ownedMediaId] will contain that id.
     */
    val mediaIds: Set<String>,
    /** @see mediaIds */
    val ownedMediaId: String?,
) {
    constructor(media: Media) : this(
        media.metadata.authorId,
        media.metadata.time,
        media.id, // we can't know the checksum for remote media, so just use something unique
        null, // unknown for remote media
        Location(media.metadata.locationMetadata),
        media.metadata.isFavorite,
        media.metadata.isEdited,
        setOf(media.id),
        media.id,
    )

    fun withoutMediaId(mediaId: String) = copy(mediaIds = mediaIds - mediaId, ownedMediaId = ownedMediaId?.takeUnless { it == mediaId })
    fun withMediaId(mediaId: String) = copy(mediaIds = mediaIds + mediaId, ownedMediaId = mediaId)
    fun withFavorite(favorite: Boolean) = copy(favorite = favorite)

    data class Location(
        @SerializedName("type", alternate = ["a"])
        val type: Type,
        @SerializedName("identifier", alternate = ["b"])
        val identifier: String?,
    ) {

        constructor(metadata: MediaLocationMetadata) : this(
            Type.fromNetworkType(metadata.type),
            metadata.spsHost?.let { SpsAddress(it).toString() } ?: metadata.identifier,
        )

        enum class Type {
            SINGLE_PLAYER,
            SHARED_WORLD,
            MULTIPLAYER,
            MENU,
            UNKNOWN;

            fun toNetworkType(): MediaLocationType {
                return when (this) {
                    SINGLE_PLAYER -> MediaLocationType.SINGLE_PLAYER
                    SHARED_WORLD -> MediaLocationType.SHARED_WORLD
                    MULTIPLAYER -> MediaLocationType.MULTIPLAYER
                    MENU -> MediaLocationType.MENU
                    UNKNOWN -> MediaLocationType.UNKNOWN
                }
            }

            companion object {
                fun fromNetworkType(type: MediaLocationType): Type {
                    return when (type) {
                        MediaLocationType.SINGLE_PLAYER -> SINGLE_PLAYER
                        MediaLocationType.SHARED_WORLD -> SHARED_WORLD
                        MediaLocationType.MULTIPLAYER -> MULTIPLAYER
                        MediaLocationType.MENU -> MENU
                        MediaLocationType.UNKNOWN -> UNKNOWN
                    }
                }
            }
        }
    }

    companion object {
        fun createUnknown(path: Path, checksum: String) =
            createUnknown(getImageTime(path, null, false), checksum)

        fun createUnknown(id: ScreenshotId, checksum: String) =
            createUnknown(getImageTime(ScreenshotProperties(id, null), false), checksum)

        fun createUnknown(time: DateTime, checksum: String) =
            ClientScreenshotMetadata(
                USession.activeNow().uuid,
                time,
                checksum,
                null,
                Location(ClientScreenshotMetadata.Location.Type.UNKNOWN, "Unknown"),
                favorite = false,
                edited = false,
                mediaIds = emptySet(),
                ownedMediaId = null,
            )
    }
}