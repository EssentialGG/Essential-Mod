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

import gg.essential.cosmetics.BoneId
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.model.Side
import gg.essential.model.util.Color
import gg.essential.model.util.ColorAsRgbSerializer
import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable(with = CosmeticProperty.TheSerializer::class)
sealed class CosmeticProperty {

    @Deprecated(ID_DEPRECATION_MESSAGE)
    abstract val id: String?
    @Deprecated(ENABLED_DEPRECATION_MESSAGE)
    abstract val enabled: Boolean
    abstract val type: CosmeticPropertyType?

    @SerialName("__unknown__")
    @Serializable
    data class Unknown(
        @Deprecated(ID_DEPRECATION_MESSAGE)
        override val id: String?,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        @SerialName("__type") // see CosmeticProperty.TheSerializer
        val typeStr: String,
        val data: JsonObject,
    ) : CosmeticProperty() {
        @Transient
        override val type: CosmeticPropertyType? = null
    }

    @SerialName("ARMOR_HANDLING")
    @Serializable
    data class ArmorHandling(
        @Deprecated(ID_DEPRECATION_MESSAGE)
        override val id: String?,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty(), IdShouldBeSelf {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.ARMOR_HANDLING

        @Serializable
        data class Data(
            val head: Boolean = false,
            val arms: Boolean = false,
            val body: Boolean = false,
            val legs: Boolean = false,
        )
    }

    @SerialName("ARMOR_HANDLING_V2")
    @Serializable
    data class ArmorHandlingV2(
        @Deprecated(ID_DEPRECATION_MESSAGE)
        override val id: String?,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty(), IdShouldBeSelf {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.ARMOR_HANDLING_V2

        @Serializable
        data class Data(
            val conflicts: Map<BoneId, List<Int>> = mapOf()
        )
    }

    @SerialName("COSMETIC_BONE_HIDING")
    @Serializable
    data class CosmeticBoneHiding(
        @Suppress("OVERRIDE_DEPRECATION")
        override val id: String,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty(), IdIsTarget {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.COSMETIC_BONE_HIDING

        @Serializable
        data class Data(
            val head: Boolean = false,
            val arms: Boolean = false,
            val body: Boolean = false,
            val legs: Boolean = false,
        )
    }

    @SerialName("POSITION_RANGE")
    @Serializable
    data class PositionRange(
        @Deprecated(ID_DEPRECATION_MESSAGE)
        override val id: String?,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty() {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.POSITION_RANGE

        @Serializable
        data class Data(
            @SerialName("x_min") val xMin: Float? = null,
            @SerialName("x_max") val xMax: Float? = null,
            @SerialName("y_min") val yMin: Float? = null,
            @SerialName("y_max") val yMax: Float? = null,
            @SerialName("z_min") val zMin: Float? = null,
            @SerialName("z_max") val zMax: Float? = null,
        )
    }

    @SerialName("EXTERNAL_HIDDEN_BONE")
    @Serializable
    data class ExternalHiddenBone(
        @Suppress("OVERRIDE_DEPRECATION")
        override val id: String,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty(), IdIsTarget {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.EXTERNAL_HIDDEN_BONE

        @Serializable(with = ExternalHiddenBoneDataSerializer::class)
        data class Data(
            val hiddenBones: Set<String>,
        )

        object ExternalHiddenBoneDataSerializer : KSerializer<Data> {

            private val inner = JsonObject.serializer()
            override val descriptor = inner.descriptor

            override fun deserialize(decoder: Decoder): Data {
                val jsonObject: JsonObject = decoder.decodeSerializableValue(inner)

                val hiddenBones = mutableSetOf<String>()
                for ((key, value) in jsonObject) {
                    if (value is JsonPrimitive && value.boolean) {
                        hiddenBones.add(key)
                    }
                }

                return Data(hiddenBones)
            }

            override fun serialize(encoder: Encoder, value: Data) {
                val jsonObject = JsonObject(value.hiddenBones.associateWith { JsonPrimitive(true) })
                encoder.encodeSerializableValue(inner, jsonObject)
            }
        }
    }

    @SerialName("INTERRUPTS_EMOTE")
    @Serializable
    data class InterruptsEmote(
        @Deprecated(ID_DEPRECATION_MESSAGE)
        override val id: String?,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty() {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.INTERRUPTS_EMOTE

        @Serializable
        data class Data(
            /** If true, a change in player position will interrupt the emote. */
            @SerialName("MOVEMENT") val movement: Boolean = false,
            /** The [movement] condition will be ignored during the first [movementGraceTime] milliseconds. */
            @SerialName("MOVEMENT_ACTIVE_AFTER") val movementGraceTime: Double = 0.0,
            /** If true, attacking another entity will interrupt the emote. */
            @SerialName("ATTACK") val attack: Boolean = true,
            /** If true, being damaged will interrupt the emote. */
            @SerialName("DAMAGED") val damaged: Boolean = false,
            /** If true, the emote will be interrupt when the player is swinging their arm. */
            @SerialName("ARM_SWING") val armSwing: Boolean = true,
        )
    }
    @SerialName("ALL_OTHER_COSMETIC_OR_ITEM_HIDING")
    @Serializable
    data class HidesAllOtherCosmeticsOrItems(
        @Deprecated(ID_DEPRECATION_MESSAGE)
        override val id: String?,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty() {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.ALL_OTHER_COSMETIC_OR_ITEM_HIDING


        @Serializable
        data class Data(
            /** If true, all player cosmetics will be hidden. */
            @SerialName("ALL_COSMETICS") val hideAllCosmetics: Boolean = false,

            /** If true, player part cosmetics will be hidden. */
            @SerialName("HEAD_COSMETICS") val hideHeadCosmetics: Boolean = false,
            @SerialName("BODY_COSMETICS") val hideBodyCosmetics: Boolean = false,
            @SerialName("LEGS_COSMETICS") val hideLegCosmetics: Boolean = false,
            @SerialName("ARMS_COSMETICS") val hideArmCosmetics: Boolean = false,

            /** If true, player held items will be hidden. */
            @SerialName("HELD_ITEMS") val hideItems: Boolean = false,
        ) {
            fun hidesAnyCosmetics() : Boolean = hideAllCosmetics || hideHeadCosmetics || hideBodyCosmetics || hideLegCosmetics || hideArmCosmetics
        }
    }


    @SerialName("LOCKS_PLAYER_ROTATION")
    @Serializable
    data class LocksPlayerRotation(
        @Deprecated(ID_DEPRECATION_MESSAGE)
        override val id: String?,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty() {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.LOCKS_PLAYER_ROTATION

        @Serializable
        data class Data(
            /** If true, a change in player rotation will not affect the emote rotation. */
            @SerialName("ROTATION_LOCK") val rotationLock: Boolean = false,
        )
    }

    @SerialName("REQUIRES_UNLOCK_ACTION")
    @Serializable
    data class RequiresUnlockAction(
        @Deprecated(ID_DEPRECATION_MESSAGE)
        override val id: String?,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty() {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.REQUIRES_UNLOCK_ACTION

        @Serializable
        @JsonClassDiscriminator("ACTION_TYPE")
        sealed class Data {

            abstract val actionDescription: String

            @Serializable
            @SerialName("OPEN_LINK")
            data class OpenLink(
                @SerialName("ACTION_DESCRIPTION")
                override val actionDescription: String,
                @SerialName("LINK_ADDRESS") val linkAddress: String,
                @SerialName("LINK_SHORT") val linkShort: String,
            ): Data()

            @Serializable
            @SerialName("JOIN_SPS")
            data class JoinSps(
                @SerialName("ACTION_DESCRIPTION")
                override val actionDescription: String,
                @SerialName("REQUIRED_VERSION") val requiredVersion: String?,
            ): Data()

            @Serializable
            @SerialName("JOIN_SERVER")
            data class JoinServer(
                @SerialName("ACTION_DESCRIPTION")
                override val actionDescription: String,
                @SerialName("SERVER_ADDRESS") val serverAddress: String,
            ): Data()

        }
    }

    @SerialName("PREVIEW_RESET_TIME")
    @Serializable
    data class PreviewResetTime(
        @Deprecated(ID_DEPRECATION_MESSAGE)
        override val id: String?,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty() {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.PREVIEW_RESET_TIME

        @Serializable
        data class Data(
            val time: Double,
        )
    }

    @SerialName("LOCALIZATION")
    @Serializable
    data class Localization(
        @Deprecated(ID_DEPRECATION_MESSAGE)
        override val id: String?,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty() {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.LOCALIZATION

        @Serializable
        data class Data(
            val en_US: String,
        )
    }

    @SerialName("TRANSITION_DELAY")
    @Serializable
    data class TransitionDelay(
        @Deprecated(ID_DEPRECATION_MESSAGE)
        override val id: String?,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty() {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.TRANSITION_DELAY

        @Serializable
        data class Data(
            val time: Long,
        )
    }

    @SerialName("VARIANTS")
    @Serializable
    data class Variants(
        @Deprecated(ID_DEPRECATION_MESSAGE)
        override val id: String?,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty() {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.VARIANTS

        @Serializable
        data class Data(
            val variants: List<Variant>,
        )

        @Serializable
        data class Variant(
            val name: String,
            @Serializable(ColorAsRgbSerializer::class)
            val color: Color,
        )
    }

    @SerialName("DEFAULT_SIDE")
    @Serializable
    data class DefaultSide(
        @Deprecated(ID_DEPRECATION_MESSAGE)
        override val id: String?,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty() {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.DEFAULT_SIDE

        @Serializable
        data class Data(
            val side: Side,
        )
    }

    @SerialName("MUTUALLY_EXCLUSIVE")
    @Serializable
    data class MutuallyExclusive(
        @Deprecated(ID_DEPRECATION_MESSAGE)
        override val id: String?,
        @Deprecated(ENABLED_DEPRECATION_MESSAGE)
        override val enabled: Boolean,
        val data: Data
    ) : CosmeticProperty() {

        @Transient
        override val type: CosmeticPropertyType = CosmeticPropertyType.MUTUALLY_EXCLUSIVE

        @Serializable
        data class Data(
            val slots: Set<CosmeticSlot> = emptySet(),
        )
    }

    /**
     * Marks a property type which uses the [id] value in some way.
     * Any type which implements this must implement one of its sub-type, specifying the way in which it is used.
     */
    sealed interface UsesId
    /**
     * Marks a property type for which the [id] value should match the id of the cosmetic on which it is declared.
     * A diagnostics error is emitted when this is not the case.
     */
    sealed interface IdShouldBeSelf : UsesId
    /**
     * Marks a property type for which the [id] value declares the cosmetic it affects.
     */
    sealed interface IdIsTarget : UsesId

    object TheSerializer : FallbackPolymorphicSerializer<CosmeticProperty>(CosmeticProperty::class, "type", "__type", "__unknown__") {
        override val module = SerializersModule {
            polymorphic(CosmeticProperty::class) {
                subclass(Unknown::class, Unknown.serializer())
                subclass(ArmorHandling::class, ArmorHandling.serializer())
                subclass(ArmorHandlingV2::class, ArmorHandlingV2.serializer())
                subclass(CosmeticBoneHiding::class, CosmeticBoneHiding.serializer())
                subclass(PositionRange::class, PositionRange.serializer())
                subclass(ExternalHiddenBone::class, ExternalHiddenBone.serializer())
                subclass(InterruptsEmote::class, InterruptsEmote.serializer())
                subclass(RequiresUnlockAction::class, RequiresUnlockAction.serializer())
                subclass(PreviewResetTime::class, PreviewResetTime.serializer())
                subclass(Localization::class, Localization.serializer())
                subclass(TransitionDelay::class, TransitionDelay.serializer())
                subclass(Variants::class, Variants.serializer())
                subclass(DefaultSide::class, DefaultSide.serializer())
                subclass(MutuallyExclusive::class, MutuallyExclusive.serializer())
                subclass(HidesAllOtherCosmeticsOrItems::class, HidesAllOtherCosmeticsOrItems.serializer())
                subclass(LocksPlayerRotation::class, LocksPlayerRotation.serializer())
            }
        }
    }

    companion object {
        private const val ID_DEPRECATION_MESSAGE = "Only valid for a small sub-set of properties, see [UsesId]. Should be `\"UNUSED\"` for all others."
        private const val ENABLED_DEPRECATION_MESSAGE = "Use [Cosmetic.properties]/[Cosmetic.disabledProperties] instead of explicitly checking this property. It will be removed eventually."

        val json by lazy { // lazy to prevent initialization cycle in serializers
            Json {
                ignoreUnknownKeys = true
                serializersModule = TheSerializer.module
            }
        }

        fun fromJsonArray(json: String): List<CosmeticProperty> {
            var mutableJson = json
            // FIXME: Temporary workaround for typo
            mutableJson = mutableJson.replace("COSEMTIC", "COSMETIC")
            return this.json.decodeFromString(mutableJson)
        }

        fun fromJson(json: String): CosmeticProperty {
            var mutableJson = json
            // FIXME: Temporary workaround for typo
            mutableJson = mutableJson.replace("COSEMTIC", "COSMETIC")
            return this.json.decodeFromString(mutableJson)
        }

    }
}
