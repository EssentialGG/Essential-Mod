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

import gg.essential.universal.UGraphics
import gg.essential.util.image.GpuTexture
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_NEAREST
import org.lwjgl.opengl.GL11.GL_RGBA
import org.lwjgl.opengl.GL11.GL_RGBA8
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11.glTexImage2D
import org.lwjgl.opengl.GL11.glTexParameteri
import org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT32
import org.lwjgl.opengl.GL30.GL_DEPTH24_STENCIL8
import org.lwjgl.opengl.GL30.GL_DEPTH_STENCIL
import org.lwjgl.opengl.GL30.GL_UNSIGNED_INT_24_8
import java.nio.ByteBuffer

//#if MC>=11600
//$$ import com.mojang.blaze3d.platform.GlStateManager
//#else
import net.minecraft.client.renderer.GlStateManager
//#endif

class OwnedGlGpuTexture(
    width: Int,
    height: Int,
    private val format: GpuTexture.Format,
) : GlGpuTexture(format) {
    override var glId: Int = -1
        private set
    override var width: Int = width
        private set
    override var height: Int = height
        private set

    init {
        init()
    }

    override fun resize(width: Int, height: Int) {
        if (this.width == width && this.height == height && this.glId != -1) {
            return
        }
        this.width = width
        this.height = height

        delete()
        init()
    }

    private fun init() {
        // Note: Must allocate via GlStateManager because we must use it to deallocate as well (see [delete])
        //       and GlStateManager does some internal counting on newer versions.
        //#if MC>=11600
        //$$ glId = GlStateManager.genTexture()
        //#else
        glId = GlStateManager.generateTexture()
        //#endif

        UGraphics.configureTexture(glId) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexImage2D(
                GL_TEXTURE_2D,
                0,
                when (format) {
                    GpuTexture.Format.RGBA8 -> GL_RGBA8
                    GpuTexture.Format.DEPTH24_STENCIL8 -> GL_DEPTH24_STENCIL8
                    GpuTexture.Format.DEPTH32 -> GL_DEPTH_COMPONENT32
                },
                width,
                height,
                0,
                when (format) {
                    GpuTexture.Format.RGBA8 -> GL_RGBA
                    GpuTexture.Format.DEPTH24_STENCIL8 -> GL_DEPTH_STENCIL
                    GpuTexture.Format.DEPTH32 -> GL_DEPTH_COMPONENT
                },
                when (format) {
                    GpuTexture.Format.RGBA8 -> GL_UNSIGNED_BYTE
                    GpuTexture.Format.DEPTH24_STENCIL8 -> GL_UNSIGNED_INT_24_8
                    GpuTexture.Format.DEPTH32 -> GL_FLOAT
                },
                null as ByteBuffer?,
            )
        }
    }

    override fun delete() {
        if (glId != -1) {
            // Note: Must use GlStateManager to deallocate as otherwise the caching in its `bindTexture` can break!
            //#if MC>=11600
            //$$ GlStateManager.deleteTexture(glId)
            //#else
            GlStateManager.deleteTexture(glId)
            //#endif
            glId = -1
        }
    }
}
