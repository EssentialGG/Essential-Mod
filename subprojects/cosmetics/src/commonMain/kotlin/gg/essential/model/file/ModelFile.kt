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
@file:UseSerializers(Vector3AsFloatArraySerializer::class)

package gg.essential.model.file

import gg.essential.model.Side
import gg.essential.model.Vector3
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class ModelFile(
    @SerialName("format_version")
    val formatVersion: String,
    @SerialName("minecraft:geometry")
    val geometries: List<Geometry> = emptyList(),
) {
    @Serializable
    data class Geometry(
        val description: Description,
        val bones: List<Bone> = emptyList(),
    )

    @Serializable
    data class Description(
        val identifier: String,
        @SerialName("texture_width") val textureWidth: Int,
        @SerialName("texture_height") val textureHeight: Int,
        @SerialName("texture_translucent") val textureTranslucent: Boolean = false,
        @SerialName("visible_bounds_width") val visibleBoundsWidth: Float,
        @SerialName("visible_bounds_height") val visibleBoundsHeight: Float,
        @SerialName("visible_bounds_offset") val visibleBoundsOffset: Vector3,
    )

    @Serializable
    data class Bone(
        val name: String,
        val parent: String? = null,
        val pivot: Vector3 = Vector3(),
        val rotation: Vector3 = Vector3(),
        val mirror: Boolean = false,
        val side: Side? = null,
        val cubes: List<Cube> = emptyList(),
        val locators: Map<String, Vector3> = emptyMap(),
    )

    @Serializable
    data class Cube(
        val origin: Vector3,
        val size: Vector3,
        val uv: Uvs,
        val mirror: Boolean? = null,
        val inflate: Float = 0f,
    )

    @Serializable(with = UvsSerializer::class)
    sealed class Uvs {
        @Serializable(with = UvBoxSerializer::class)
        data class Box(val uv: List<Float>) : Uvs()

        @Serializable
        data class PerFace(
            val north: UvFace? = null,
            val east: UvFace? = null,
            val south: UvFace? = null,
            val west: UvFace? = null,
            val up: UvFace? = null,
            val down: UvFace? = null,
        ) : Uvs()
    }

    @Serializable
    data class UvFace(
        val uv: List<Float>,
        @SerialName("uv_size")
        val size: List<Float>,
    )
}

private class Vector3AsFloatArraySerializer : KSerializer<Vector3> {
    private val inner = FloatArraySerializer()
    override val descriptor = inner.descriptor
    override fun deserialize(decoder: Decoder): Vector3 =
        decoder.decodeSerializableValue(inner).let { (x, y, z) -> Vector3(x, y, z) }
    override fun serialize(encoder: Encoder, value: Vector3) =
        encoder.encodeSerializableValue(inner, floatArrayOf(value.x, value.y, value.z))
}

private class UvsSerializer : JsonContentPolymorphicSerializer<ModelFile.Uvs>(ModelFile.Uvs::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out ModelFile.Uvs> =
        when (element) {
            is JsonObject -> ModelFile.Uvs.PerFace.serializer()
            else -> ModelFile.Uvs.Box.serializer()
        }
}

private class UvBoxSerializer : KSerializer<ModelFile.Uvs.Box> {
    private val inner = ListSerializer(Float.serializer())
    override val descriptor = inner.descriptor
    override fun deserialize(decoder: Decoder): ModelFile.Uvs.Box = ModelFile.Uvs.Box(inner.deserialize(decoder))
    override fun serialize(encoder: Encoder, value: ModelFile.Uvs.Box) = inner.serialize(encoder, value.uv)
}
