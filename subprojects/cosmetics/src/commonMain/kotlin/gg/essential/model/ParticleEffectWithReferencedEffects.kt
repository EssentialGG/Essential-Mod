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

// represents an instance of [ParticleEffect] via a reference to the contained effect map
// this resolves circular references in equality checks
data class ParticleEffectWithReferencedEffects(
    private val particleEffectId: String,
    /** All effects referenced by events of this effect. May contain more events than actually referenced. */
    val referencedEffects: Map<String, ParticleEffect>, // MUST contain particleEffectId
    /** All sounds referenced by events of this effect. May contain more sounds than actually referenced. */
    val referencedSounds: Map<String, SoundEffect>,
) {
    val particleEffect: ParticleEffect = referencedEffects.getValue(particleEffectId)

    fun getOtherParticleByReference(id: String): ParticleEffectWithReferencedEffects? {
        return if (id in referencedEffects) copy(particleEffectId = id) else null
    }
}
