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
package gg.essential.model.file

import gg.essential.model.SoundCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject

@Serializable
data class SoundDefinitionsFile(
    @SerialName("sound_definitions")
    val definitions: Map<String, Definition>,
) {
    @Serializable
    data class Definition(
        val category: SoundCategory,
        // Note: Min distance is not currently implemented. Minecraft uses a fixed 0 for all sounds.
        @SerialName("min_distance")
        val minDistance: Float = 0f,
        @SerialName("max_distance")
        val maxDistance: Float = 16f,
        /**
         * When set to `true`, the sound will stay at the position it was emitted, otherwise it will follow the locator
         * it is bound to (or its emitter if no explicit locator is set).
         */
        @SerialName("fixed_position")
        val fixedPosition: Boolean = false,
        val sounds: List<@Serializable(with = SoundObjectOrNameSerializer::class) Sound>,
    )

    @Serializable
    data class Sound(
        val name: String,
        val stream: Boolean = false,
        val interruptible: Boolean = false,
        val volume: Float = 1f,
        val pitch: Float = 1f,
        val looping: Boolean = false,
        @SerialName("is3D")
        val directional: Boolean = true,
        val weight: Int = 1,
    )

    private class SoundObjectOrNameSerializer : JsonTransformingSerializer<Sound>(Sound.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement = if (element is JsonPrimitive) {
            buildJsonObject { put("name", element) }
        } else element

        override fun transformSerialize(element: JsonElement): JsonElement {
            element as JsonObject
            return if (element.size == 1) element.getValue("name") else element
        }
    }
}
