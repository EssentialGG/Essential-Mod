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
package gg.essential.mod.cosmetics.settings

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable(with = CosmeticSetting.TheSerializer::class)
sealed class CosmeticSetting {


    abstract val id: String?
    @Deprecated("unused")
    abstract val enabled: Boolean
    abstract val type: CosmeticSettingType?

    @SerialName("__unknown__")
    @Serializable
    data class Unknown(
        override val id: String?,
        @Deprecated("unused")
        override val enabled: Boolean,
        @SerialName("__type") // see CosmeticSetting.TheSerializer
        val typeStr: String,
        val data: JsonObject,
    ) : CosmeticSetting() {
        @Transient
        override val type: CosmeticSettingType? = null
    }


    @SerialName("PLAYER_POSITION_ADJUSTMENT")
    @Serializable
    data class PlayerPositionAdjustment(
        override val id: String?,
        @Deprecated("unused")
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticSetting() {

        @Transient
        override val type: CosmeticSettingType = CosmeticSettingType.PLAYER_POSITION_ADJUSTMENT

        @Serializable
        data class Data(
            val x: Float = 0f,
            val y: Float = 0f,
            val z: Float = 0f,
        )
    }

    @SerialName("SIDE")
    @Serializable
    data class Side(
        override val id: String?,
        @Deprecated("unused")
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticSetting() {

        @Transient
        override val type: CosmeticSettingType = CosmeticSettingType.SIDE

        // Side defaults to LEFT because of old settings failing to parse otherwise.
        // You should probably always provide your own value to this constructor. See comments about default sides in Side.
        @Serializable
        data class Data(
            @SerialName("SIDE")
            @Serializable(with = gg.essential.model.Side.UpperCase::class)
            val side: gg.essential.model.Side = gg.essential.model.Side.LEFT,
        )
    }

    @SerialName("VARIANT")
    @Serializable
    data class Variant(
        override val id: String?,
        @Deprecated("unused")
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticSetting() {

        @Transient
        override val type: CosmeticSettingType = CosmeticSettingType.VARIANT

        @Serializable
        data class Data(
            val variant: String,
        )
    }

    @SerialName("ANIMATION_VARIANT")
    @Serializable
    data class AnimationVariant(
        override val id: String?,
        @Deprecated("unused")
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticSetting() {

        @Transient
        override val type: CosmeticSettingType = CosmeticSettingType.ANIMATION_VARIANT

        @Serializable
        data class Data(
            val animationVariant: String? = null,
        )
    }

    object TheSerializer : FallbackPolymorphicSerializer<CosmeticSetting>(CosmeticSetting::class, "type", "__type", "__unknown__") {
        override val module = SerializersModule {
            polymorphic(CosmeticSetting::class) {
                subclass(Unknown::class, Unknown.serializer())
                subclass(PlayerPositionAdjustment::class, PlayerPositionAdjustment.serializer())
                subclass(Side::class, Side.serializer())
                subclass(Variant::class, Variant.serializer())
                subclass(AnimationVariant::class, AnimationVariant.serializer())
            }
        }
    }

    companion object {
        val json by lazy { // lazy to prevent initialization cycle in serializers
            Json {
                ignoreUnknownKeys = true
                serializersModule = TheSerializer.module
            }
        }

        fun fromJsonArray(json: String): List<CosmeticSetting> {
            return this.json.decodeFromString(json)
        }

        fun fromJson(json: String): CosmeticSetting {
            return this.json.decodeFromString(json)
        }
    }

}
