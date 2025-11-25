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
package gg.essential.model.backend

import gg.essential.model.util.UVertexConsumer

interface RenderBackend {
    fun createTexture(name: String, width: Int, height: Int): Texture
    fun deleteTexture(texture: Texture)

    fun blitTexture(dst: Texture, ops: Iterable<BlitOp>)

    suspend fun readTexture(name: String, bytes: ByteArray): Texture

    interface Texture {
        val width: Int
        val height: Int
    }

    fun interface CommandQueue {
        fun submit(texture: Texture, translucent: Boolean, emissive: Boolean, render: (UVertexConsumer) -> Unit)
    }
    fun interface VertexConsumerProvider {
        fun provide(texture: Texture, emissive: Boolean, block: (UVertexConsumer) -> Unit)
    }

    data class BlitOp(val src: Texture, val srcX: Int, val srcY: Int, val destX: Int, val destY: Int, val width: Int, val height: Int)
}
