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
package gg.essential.model

import gg.essential.model.backend.RenderBackend
import gg.essential.model.file.ParticleEffectComponents
import gg.essential.model.file.ParticlesFile

data class ParticleEffect(
    val file: String,
    val identifier: String,
    val material: ParticlesFile.Material,
    val components: ParticleEffectComponents,
    val curves: Map<String, ParticlesFile.Curve>,
    val events: Map<String, ParticlesFile.Event>,
) {

    // used as a key to group particles by material and texture for render
    // take care to only call once per usage as this value will vary with the input texture
    fun renderPass(textureSource: () -> RenderBackend.Texture?): RenderPass? {
        return textureSource()?.let { RenderPass(material, it) }
    }
    data class RenderPass(val material: ParticlesFile.Material, val texture: RenderBackend.Texture)
}
