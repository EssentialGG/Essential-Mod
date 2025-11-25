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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.InputStream
import java.security.MessageDigest

@Serializable(with = Sha256.Serializer::class)
class Sha256(val bytes: ByteArray) {
    init {
        require(bytes.size == BYTES)
    }

    // TODO use stdlib once HexFormat becomes stable
    // constructor(hexStr: String) : this(hexStr.hexToByteArray())
    constructor(hexStr: String) : this(hexStr.chunked(2).map { it.toInt(16).toByte() }.toByteArray())

    val hexStr: String
        // TODO use stdlib once HexFormat becomes stable
        // get() = bytes.toHexString()
        get() = bytes.joinToString("") { "%02x".format(it) }

    override fun toString(): String {
        return "sha256-$hexStr"
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Sha256) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    internal object Serializer : KSerializer<Sha256> {
        private val inner = String.serializer()
        override val descriptor: SerialDescriptor = inner.descriptor
        override fun deserialize(decoder: Decoder) = Sha256(decoder.decodeSerializableValue(inner))
        override fun serialize(encoder: Encoder, value: Sha256) = encoder.encodeSerializableValue(inner, value.hexStr)
    }

    companion object {
        const val BITS = 256
        const val BYTES = BITS / 8

        fun compute(block: MessageDigest.() -> Unit): Sha256 =
            Sha256(MessageDigest.getInstance("SHA-256").apply(block).digest())

        val EMPTY = compute {}

        fun compute(data: ByteArray) =
            if (data.isEmpty()) EMPTY else compute { update(data) }

        fun compute(data: InputStream) = compute {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val chunkSize = data.read(buffer)
                if (chunkSize < 0) break
                update(buffer, 0, chunkSize)
            }
        }

    }
}
