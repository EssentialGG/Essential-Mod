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
package gg.essential.minecraftauth.xbox.response

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// There are more keys associated with this object, but we don't need them.
// https://minecraft.wiki/w/Microsoft_authentication#Obtain_XSTS_token_for_Minecraft:~:text=The%20endpoint%20can%20return%20a%20401
@Serializable
data class XboxLiveAuthenticationErrorResponse(
    @SerialName("XErr")
    val errorCode: XboxLiveErrorCode
)

// https://minecraft.wiki/w/Microsoft_authentication#Obtain_XSTS_token_for_Minecraft:~:text=Noted%20XErr%20codes%20and%20their%20meanings%3A
@Serializable(with = XboxErrorSerializer::class)
sealed interface XboxLiveErrorCode {
    companion object {
        fun fromCode(code: Long) = when (code) {
            2148916227 -> AccountIsBannedFromXbox
            2148916233 -> NoXboxLiveAccount
            2148916235 -> XboxLiveBannedInCountry
            2148916236, 2148916237 -> RequiresAdultVerification
            2148916238 -> AccountIsAChild
            2148916229 -> MultiplayerNotAllowed
            2148916234 -> TermsOfServiceNotAccepted

            else -> Unknown(code)
        }
    }

    // The `toString` implementations below are just to make the exception messages prettier, users of the API should
    // catch the error which uses these, and communicate the issue to the user appropriately.

    /**
     * This Microsoft account is banned from Xbox.
     */
    object AccountIsBannedFromXbox : XboxLiveErrorCode {
        override fun toString() = "This Microsoft Account is banned from Xbox."
    }

    /**
     * This Microsoft account doesn't have an Xbox account linked to it.
     */
    object NoXboxLiveAccount : XboxLiveErrorCode {
        override fun toString() = "This Microsoft Account does not have an Xbox Live account associated with it."
    }

    /**
     * The account is from a country where Xbox Live is not available/banned
     */
    object XboxLiveBannedInCountry : XboxLiveErrorCode {
        override fun toString() = "Xbox Live is unavailable in your country/region."
    }

    /**
     * The account needs adult verification on the Xbox website, usually only occurs in South Korea.
     */
    object RequiresAdultVerification : XboxLiveErrorCode {
        override fun toString() = "You need to verify that you are an adult on the Xbox website before using Xbox Live Services."
    }

    /**
     * The account is a child (under 18) and cannot proceed unless the account is added to a Family by an adult.
     */
    object AccountIsAChild : XboxLiveErrorCode {
        override fun toString() = "Your account is a child (under 18), and cannot use Xbox Live Services until added to a Family."
    }

    /**
     * Multiplayer is not allowed for this Xbox Live Account, and needs to be enabled by a guardian.
     */
    object MultiplayerNotAllowed : XboxLiveErrorCode {
        override fun toString() = "Your parent needs to grant permission for your account to play multiplayer games."
    }

    /**
     * The user needs to accept the Xbox terms of service before they can use the account.
     */
    object TermsOfServiceNotAccepted : XboxLiveErrorCode {
        override fun toString() = "Your account has not accepted the Xbox Live terms of service."
    }

    /**
     * Some other Xbox error code that we don't know about, Microsoft makes you sign an NDA to get documentation about
     * the Xbox Live API and Xbox Secure Token Service, so ¯\_(ツ)_/¯
     */
    data class Unknown(val value: Long) : XboxLiveErrorCode {
        override fun toString() = "An unknown error occurred: $value"
    }
}

object XboxErrorSerializer : KSerializer<XboxLiveErrorCode> {
    override val descriptor: SerialDescriptor = Long.serializer().descriptor

    override fun deserialize(decoder: Decoder) = XboxLiveErrorCode.fromCode(decoder.decodeLong())

    override fun serialize(encoder: Encoder, value: XboxLiveErrorCode) {
        throw NotImplementedError("XboxError cannot be serialized.")
    }
}