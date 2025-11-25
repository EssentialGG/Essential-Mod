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

import gg.essential.model.Bones
import gg.essential.model.EnumPart
import gg.essential.model.Side
import gg.essential.model.Vector3
import gg.essential.model.backend.PlayerPose
import gg.essential.model.util.Quaternion
import kotlin.collections.any
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.let

class BedrockModelState(
    val playerPose: PlayerPose,
    val bakedAnimations: BakedAnimations,
    val userOffset: Vector3,
    val side: Side?,
    val hiddenBones: Set<String>,
    val visibleParts: Set<EnumPart>,
) {
    fun apply(bones: Bones) {
        applyPose(bones, playerPose)

        bakedAnimations.apply(bones)

        if (userOffset != Vector3.ZERO) {
            for (bone in bones.byPart.values) {
                if (bone.part == EnumPart.ROOT) continue
                bone.userOffsetX = userOffset.x
                bone.userOffsetY = userOffset.y
                bone.userOffsetZ = userOffset.z
            }
        }

        applyVisibility(bones)

        // Note: Gimbal propagation needs to read the final rotation of all bones. So it needs to happen last.
        if (bakedAnimations.bakedBones.any { it.gimbal }) {
            val entityRotation = bakedAnimations.entityRotation
                .let { it.copy(x = -it.x, y = -it.y) } // see RenderLivingBase.prepareScale
            // TODO maybe optimize traversal, don't need to compute subtrees that do not even have any gimbal parts
            bones.root.propagateGimbal(Quaternion.Identity, entityRotation)
        }
    }

    fun reset(bones: Bones) {
        // Player pose does not need to be reset because properties of all parts are overwritten each time

        bakedAnimations.reset(bones)

        if (userOffset != Vector3.ZERO) {
            for (bone in bones.byPart.values) {
                if (bone.part == EnumPart.ROOT) continue
                bone.userOffsetX = 0f
                bone.userOffsetY = 0f
                bone.userOffsetZ = 0f
            }
        }

        resetVisibility(bones)

        // Gimbal propagation does not need resetting, the `gimbal` flag is already reset by `bakedAnimations.reset`
    }

    private fun applyVisibility(bones: Bones) {
        for ((part, bone) in bones.byPart) {
            bone.visible = part in visibleParts
        }
        for (hiddenBone in hiddenBones) {
            bones[hiddenBone]?.visible = false
        }
        bones.root.propagateVisibility(true, side)
    }

    private fun resetVisibility(bones: Bones) {
        for ((_, bone) in bones.byPart) {
            bone.visible = null
        }
        for (hiddenBone in hiddenBones) {
            bones[hiddenBone]?.visible = null
        }
        // propagation does not need explicit resetting, the propagate on the next apply call will update everything
    }
}
