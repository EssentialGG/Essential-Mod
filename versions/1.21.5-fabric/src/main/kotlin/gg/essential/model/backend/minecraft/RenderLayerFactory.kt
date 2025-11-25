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
package gg.essential.model.backend.minecraft

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import gg.essential.model.ParticleEffect
import gg.essential.model.backend.minecraft.MinecraftRenderBackend.MinecraftTexture
import gg.essential.model.file.ParticlesFile.Material.Add
import gg.essential.model.file.ParticlesFile.Material.Blend
import gg.essential.model.file.ParticlesFile.Material.Cutout
import gg.essential.util.ModLoaderUtil
//#if FABRIC
import net.irisshaders.iris.api.v0.IrisApi
import net.irisshaders.iris.api.v0.IrisProgram
//#endif
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier
import net.minecraft.util.TriState

/** see usage in [MinecraftRenderBackend] for further context
 * prior to 1.21.5 we used a wrapper [RenderLayer] class to handle the creation of these render layers, however with each
 * version mojang has made it more difficult to create custom render layers, so we now use the actual [RenderLayer] classes
 * and builders directly.
 *
 * This is also required in 1.21.6+ for compatibility with Iris shaders, as they now require additional implementation if
 * we had continued using [RenderLayer] wrapper classes rather than building and using a [RenderLayer.MultiPhase] normally
 */
abstract class RenderLayerFactory : RenderLayer("dummy", 0, false, false, {}, {}) {
    companion object {

        //#if FABRIC || FORGE
        private fun RenderPipeline.toBuilder(): RenderPipeline.Builder = RenderPipeline.builder().copyFrom(this)

        private fun RenderPipeline.Builder.copyFrom(pipeline: RenderPipeline): RenderPipeline.Builder = apply {
            withLocation(pipeline.location)
            withVertexShader(pipeline.vertexShader)
            withFragmentShader(pipeline.fragmentShader)
            pipeline.shaderDefines.values.forEach { (key, value) ->
                if ("." in value) withShaderDefine(key, value.toFloat())
                else withShaderDefine(key, value.toInt())
            }
            pipeline.shaderDefines.flags.forEach { withShaderDefine(it) }
            pipeline.samplers.forEach { sampler ->
                withSampler(sampler)
            }
            pipeline.uniforms.forEach { uniform ->
                withUniform(uniform.name(), uniform.type())
            }
            withDepthTestFunction(pipeline.depthTestFunction)
            withPolygonMode(pipeline.polygonMode)
            withCull(pipeline.isCull)
            if (pipeline.blendFunction.isPresent) {
                withBlend(pipeline.blendFunction.get())
            } else {
                withoutBlend()
            }
            withColorWrite(pipeline.isWriteColor, pipeline.isWriteAlpha)
            withDepthWrite(pipeline.isWriteDepth)
            withColorLogic(pipeline.colorLogic)
            withVertexFormat(pipeline.vertexFormat, pipeline.vertexFormatMode)
            withDepthBias(pipeline.depthBiasScaleFactor, pipeline.depthBiasConstant)
        }
        //#endif

        //#if FABRIC
        // Note: Cannot pass `program` directly because Iris might not be installed
        private fun RenderPipeline.assignIrisProgram(program: () -> () -> IrisProgram): RenderPipeline = apply {
            if (ModLoaderUtil.isModLoaded("iris")) {
                // Separate method for class loading reasons
                fun doIt() {
                    IrisApi.getInstance().assignPipeline(this, program()())
                }
                doIt()
            }
        }
        //#endif

        fun createEmissiveArmorLayer(texture: Identifier): RenderLayer =
            createRenderLayer(
                "armor_translucent_emissive",
                DEFAULT_BUFFER_SIZE,
                true,
                true,
                RenderPipelines.ENTITY_EYES,
                MultiPhaseParameters.builder()
                    .texture(texture, false)
                    .set(VIEW_OFFSET_Z_LAYERING, Layering::class.java)
                    .build(true)
            )

        private val entityTranslucentCullPipeline = RenderPipelines.ENTITY_TRANSLUCENT.toBuilder().withCull(true).build()
            //#if FABRIC
            .assignIrisProgram {{ IrisProgram.ENTITIES_TRANSLUCENT }}
            //#endif

        fun createEntityTranslucentCullLayer(texture: Identifier): RenderLayer =
            createRenderLayer(
                "entity_translucent_cull",
                DEFAULT_BUFFER_SIZE,
                true,
                true,
                entityTranslucentCullPipeline,
                MultiPhaseParameters.builder()
                    .texture(texture, false)
                    .set(ENABLE_OVERLAY_COLOR, Overlay::class.java)
                    .set(ENABLE_LIGHTMAP, Lightmap::class.java)
                    .build(true)
            )

        private val particleAdditivePipeline = RenderPipelines.TRANSLUCENT_PARTICLE.toBuilder()
            .withBlend(BlendFunction.LIGHTNING)
            .build()
            //#if FABRIC
            .assignIrisProgram {{ IrisProgram.PARTICLES_TRANSLUCENT }}
            //#endif

        //#if MC>=12109
        //$$ private val PARTICLES_TARGET = Target("particles") {
        //$$     val mc = net.minecraft.client.MinecraftClient.getInstance()
        //$$     mc.worldRenderer.particlesFramebuffer ?: mc.framebuffer
        //$$ }
        //#endif

        fun createParticleLayer(renderPass: ParticleEffect.RenderPass): RenderLayer {
            val texture = (renderPass.texture as MinecraftTexture).identifier
            val pipeline = when (renderPass.material) {
                Cutout -> RenderPipelines.OPAQUE_PARTICLE
                Blend -> RenderPipelines.TRANSLUCENT_PARTICLE
                Add -> particleAdditivePipeline
            }

            val builder = MultiPhaseParameters.builder()
                .texture(texture, false)
                .set(ENABLE_LIGHTMAP, Lightmap::class.java)

            if (renderPass.material != Cutout) builder.set(PARTICLES_TARGET, Target::class.java) // only diff between cutout and translucent


            return createRenderLayer(
                renderPass.material.name.lowercase() + "_particle",
                DEFAULT_BUFFER_SIZE,
                false,
                false,
                pipeline,
                builder.build(false)
            )
        }

        // [RenderLayer.MultiPhase] is inaccessible, so we use reflection to invoke the static method "of(): RenderLayer.MultiPhase"
        private fun createRenderLayer(name: String, size: Int, hasCrumbling: Boolean, translucent: Boolean, pipeline: RenderPipeline, params: MultiPhaseParameters) =
            RenderLayer::class.java.declaredMethods
                .first { RenderLayer::class.java.isAssignableFrom(it.returnType)
                    && it.parameterTypes.contentEquals(arrayOf(
                        String::class.java,
                        Int::class.javaPrimitiveType,
                        Boolean::class.javaPrimitiveType,
                        Boolean::class.javaPrimitiveType,
                        RenderPipeline::class.java,
                        MultiPhaseParameters::class.java
                    ))
                }
                .apply { isAccessible = true }
                .invoke(null, name, size, hasCrumbling, translucent, pipeline, params)
                    as RenderLayer // is [RenderLayer.MultiPhase]


        private fun MultiPhaseParameters.Builder.texture(texture: Identifier, mipmap: Boolean = false): MultiPhaseParameters.Builder =
            //#if MC>=12106
            //$$ set(Texture(texture, mipmap), TextureBase::class.java)
            //#else
            set(Texture(texture, TriState.FALSE, mipmap), TextureBase::class.java)
            //#endif

        // Return values and parameter types are inaccessible via invoker/accessor, so we use reflection to invoke the methods
        private fun <T> MultiPhaseParameters.Builder.set(arg: T, clss: Class<T>): MultiPhaseParameters.Builder =
             MultiPhaseParameters.Builder::class.java.declaredMethods
                    .first { it.returnType == MultiPhaseParameters.Builder::class.java
                            && it.parameterTypes.contentEquals(arrayOf(clss)) }
                    .apply { isAccessible = true }
                    .invoke(this, arg) as MultiPhaseParameters.Builder

        // Return value is inaccessible via invoker/accessor, so we use reflection to invoke the method
        private fun MultiPhaseParameters.Builder.build(affectsOutline: Boolean): MultiPhaseParameters =
            MultiPhaseParameters.Builder::class.java.declaredMethods
                .first { it.returnType == MultiPhaseParameters::class.java
                        && it.parameterTypes.contentEquals(arrayOf(Boolean::class.java)) }
                .apply { isAccessible = true }
                .invoke(this, affectsOutline) as MultiPhaseParameters
    }
}