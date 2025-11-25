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

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.TextureFormat

// Note: Copied from UniversalCraft, changes here should probably be applied over there as well.
internal class TemporaryTextureAllocator(
    private val allCleanedUp: () -> Unit = {},
) : AutoCloseable {
    // When we allocate a texture, we need to hold on to it until the next frame so MC's gui renderer can use it
    private val usedAllocations = mutableListOf<TextureAllocation>()
    // We hold on to it for an additionally frame so we can re-use it instead of having to re-allocate one each frame
    private val reusableAllocations = mutableListOf<TextureAllocation>()

    fun allocate(width: Int, height: Int): TextureAllocation {
        var texture = reusableAllocations.removeLastOrNull()

        if (texture != null && (texture.width != width || texture.height != height)) {
            texture.close()
            texture = null
        }

        if (texture == null) {
            texture = TextureAllocation(width, height)
        }

        val device = RenderSystem.getDevice()
        device.createCommandEncoder().clearColorAndDepthTextures(texture.texture, 0, texture.depthTexture, 1.0)

        usedAllocations.add(texture)

        return texture
    }

    fun free(allocation: TextureAllocation) {
        if (usedAllocations.remove(allocation)) {
            reusableAllocations.add(allocation)
        }
    }

    fun nextFrame() {
        reusableAllocations.forEach { it.close() }
        reusableAllocations.clear()
        reusableAllocations.addAll(usedAllocations)
        usedAllocations.clear()

        if (reusableAllocations.isEmpty()) {
            allCleanedUp()
        }
    }

    override fun close() {
        nextFrame()
        nextFrame()
        assert(usedAllocations.isEmpty())
        assert(reusableAllocations.isEmpty())
    }

    class TextureAllocation(
        val width: Int,
        val height: Int,
    ) : AutoCloseable {
        private val gpuDevice = RenderSystem.getDevice()

        var texture = gpuDevice.createTexture(
            { "Pre-rendered texture" },
            GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_TEXTURE_BINDING,
            TextureFormat.RGBA8,
            width,
            height,
            1,
            1
        ).apply { setTextureFilter(FilterMode.NEAREST, false) }
        var depthTexture = gpuDevice.createTexture(
            { "Pre-rendered depth texture" },
            GpuTexture.USAGE_RENDER_ATTACHMENT,
            TextureFormat.DEPTH32,
            width,
            height,
            1,
            1
        )

        var textureView = gpuDevice.createTextureView(texture)
        var depthTextureView = gpuDevice.createTextureView(depthTexture)

        override fun close() {
            depthTextureView.close()
            textureView.close()
            depthTexture.close()
            texture.close()
        }
    }
}
