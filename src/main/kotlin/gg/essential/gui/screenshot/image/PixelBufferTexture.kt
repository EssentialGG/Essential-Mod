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
package gg.essential.gui.screenshot.image

import gg.essential.gui.screenshot.downsampling.ErrorImage
import gg.essential.gui.screenshot.downsampling.PixelBuffer
import gg.essential.universal.UMinecraft
import net.minecraft.client.renderer.GlStateManager

import net.minecraft.client.resources.IResourceManager
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import java.nio.IntBuffer

//#if MC>=12105
//$$ import com.mojang.blaze3d.textures.GpuTexture
//#endif

//#if MC>=12105
//$$ import com.mojang.blaze3d.opengl.GlStateManager
//$$ import com.mojang.blaze3d.textures.TextureFormat
//$$ import net.minecraft.client.texture.GlTexture
//#endif

//#if MC<=11202
import net.minecraft.client.renderer.texture.AbstractTexture
//#else
//$$ import net.minecraft.client.renderer.texture.Texture
//#endif

//#if MC>=11600 && MC<12105
//$$ import com.mojang.blaze3d.platform.GlStateManager
//#endif

/**
 * Uploads the contents of a PixelBuffer to OpenGL
 */
class PixelBufferTexture(debugLabel: String, image: PixelBuffer) :
//#if MC<=11202
    AbstractTexture() {
    //#else
    //$$ Texture() {
    //#endif

    // Whether this texture's underlying image had an error during loading
    // To be used in ScreenshotBrowser for alternate behavior
    val error = image is ErrorImage

    val imageWidth: Int = image.getWidth()
    val imageHeight: Int = image.getHeight()

    init {
        if(image !is ErrorImage) {
            //#if MC>=12106
            //$$ glTexture = RenderSystem.getDevice().createTexture(debugLabel, GpuTexture.USAGE_TEXTURE_BINDING, TextureFormat.RGBA8, imageWidth, imageHeight, 1, 1)
            //#elseif MC>=12105
            //$$ glTexture = RenderSystem.getDevice().createTexture(debugLabel, TextureFormat.RGBA8, imageWidth, imageHeight, 1)
            //#else
            // Note: Must allocate via GlStateManager because the vanilla method also deallocates via GlStateManager
            //       and GlStateManager does some internal counting on newer versions.
            //#if MC>=11600
            //$$ glTextureId = GlStateManager.genTexture()
            //#else
            glTextureId = GlStateManager.generateTexture()
            //#endif
            //#endif
        }
    }

    fun upload(image: PixelBuffer) {
        if (image !is ErrorImage) {
            // We need to support both uploading on the main thread and in another async context
            if (UMinecraft.getMinecraft().isCallingFromMinecraftThread) {
                //#if MC>=12105
                //$$ GlStateManager._bindTexture((glTexture as GlTexture).glId)
                //#else
                GlStateManager.bindTexture(glTextureId)
                //#endif
                // Minecraft changes these values in some places, for example NativeImage#upload
                glPixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0)
                glPixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0)
                glPixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0)
            } else {
                //#if MC>=12105
                //$$ GL11.glBindTexture(GL11.GL_TEXTURE_2D, (glTexture as GlTexture).glId)
                //#else
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTextureId)
                //#endif
            }

            val buffer = image.prepareDirectBuffer()
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA,
                imageWidth,
                imageHeight,
                0,
                GL11.GL_RGBA,
                GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                null as IntBuffer?
            )

            // Extra GL call should not be required but is because of a suspected graphics driver bug
            // This acts as a workaround fix for the texture not uploading and rendering correctly
            GL11.glTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                0,
                0,
                imageWidth,
                imageHeight,
                GL11.GL_RGBA,
                GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                buffer
            )

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        }
    }

    //#if MC<12104
    //Impl handled by constructor to avoid retaining complete image
    override fun loadTexture(resourceManager: IResourceManager) {
    }
    //#endif


}

fun glPixelStore(pname: Int, param: Int) {
    // FIXME remap bug: Doesn't seem to be a way to do this with mapping overrides, since GlStateManager is overridden to RenderSystem
    //#if MC>=11600
    //$$ GlStateManager.pixelStore(pname, param)
    //#elseif MC>=11200
    GlStateManager.glPixelStorei(pname, param)
    //#else
    //$$ GL11.glPixelStorei(pname, param)
    //#endif
}
