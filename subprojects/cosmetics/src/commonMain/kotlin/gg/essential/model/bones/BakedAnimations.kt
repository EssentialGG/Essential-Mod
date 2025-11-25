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
package gg.essential.model.bones

import gg.essential.model.BoneId
import gg.essential.model.Bones
import gg.essential.model.util.Quaternion

class BakedAnimations(
    val bakedBones: List<BakedBone>,
    /** Entity rotation in world space. Valid only if there is a [BakedBone.worldGimbal]. */
    val entityRotation: Quaternion,
) {
    class BakedBone(val boneId: BoneId) {
        var animOffsetX = 0f
        var animOffsetY = 0f
        var animOffsetZ = 0f
        var animRotX = 0f
        var animRotY = 0f
        var animRotZ = 0f
        var animScaleX = 1f
        var animScaleY = 1f
        var animScaleZ = 1f
        var gimbal = false
        var worldGimbal = false
    }

    fun apply(bones: Bones) {
        for (baked in bakedBones) {
            val bone = bones[baked.boneId]
            bone.animOffsetX = baked.animOffsetX
            bone.animOffsetY = baked.animOffsetY
            bone.animOffsetZ = baked.animOffsetZ
            bone.animRotX = baked.animRotX
            bone.animRotY = baked.animRotY
            bone.animRotZ = baked.animRotZ
            bone.animScaleX = baked.animScaleX
            bone.animScaleY = baked.animScaleY
            bone.animScaleZ = baked.animScaleZ
            bone.gimbal = baked.gimbal
            bone.worldGimbal = baked.worldGimbal
        }
    }

    fun reset(bones: Bones) {
        for (baked in bakedBones) {
            val bone = bones[baked.boneId]
            bone.animOffsetX = 0f
            bone.animOffsetY = 0f
            bone.animOffsetZ = 0f
            bone.animRotX = 0f
            bone.animRotY = 0f
            bone.animRotZ = 0f
            bone.animScaleX = 1f
            bone.animScaleY = 1f
            bone.animScaleZ = 1f
            bone.gimbal = false
            bone.worldGimbal = false
        }
    }

    companion object {
        val EMPTY = BakedAnimations(emptyList(), Quaternion.Identity)
    }
}
