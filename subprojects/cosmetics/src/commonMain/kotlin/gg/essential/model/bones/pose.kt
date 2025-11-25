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

import dev.folomeev.kotgl.matrix.matrices.mutables.inverse
import dev.folomeev.kotgl.matrix.matrices.mutables.times
import dev.folomeev.kotgl.matrix.vectors.vec4
import gg.essential.model.BedrockModel.Companion.OFFSETS
import gg.essential.model.BedrockModel.Offset
import gg.essential.model.Bone
import gg.essential.model.Bones
import gg.essential.model.EnumPart
import gg.essential.model.backend.PlayerPose
import gg.essential.model.util.UMatrixStack
import gg.essential.model.util.getRotationEulerZYX
import gg.essential.model.util.times
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator


fun applyPose(bones: Bones, pose: PlayerPose) {
    for ((part, bone) in bones.byPart) {
        if (part == EnumPart.ROOT) continue

        copy(pose[part], bone, OFFSETS.getValue(part))
        bone.child = pose.child
    }
}

private fun copy(pose: PlayerPose.Part, bone: Bone, offset: Offset) {
    bone.poseRotX = pose.rotateAngleX
    bone.poseRotY = pose.rotateAngleY
    bone.poseRotZ = pose.rotateAngleZ
    bone.poseOffsetX = pose.pivotX + offset.offsetX
    bone.poseOffsetY = -pose.pivotY + offset.offsetY
    bone.poseOffsetZ = pose.pivotZ + offset.offsetZ
    bone.poseExtra = pose.extra
}

fun retrievePose(bones: Bones, basePose: PlayerPose): PlayerPose {
    val parts = basePose.toMap(mutableMapOf())

    fun Bone.visit(matrixStack: UMatrixStack, parentHasScaling: Boolean) {
        if (!affectsPose) {
            return
        }

        val hasScaling = parentHasScaling || animScaleX != 1f || animScaleY != 1f || animScaleZ != 1f

        matrixStack.push()
        applyTransform(matrixStack)

        val part = part
        if (part != null && part != EnumPart.ROOT) { // ROOT is not needed for pose
            val offset = OFFSETS.getValue(part)
            val matrix = matrixStack.peek().model

            // We can easily get the local pivot point by simply undoing the last `matrixStack.translate` call
            // (ignoring user offset for now because that is unused for emotes)
            val localPivot = vec4(pivotX, pivotY, pivotZ, 1f)
            // We can transform that into global space by simply passing it through the matrix
            val globalPivot = localPivot.times(matrix)

            // Local rotation is even simpler because there is no residual local rotation "pivot", so our global
            // rotation is simply the rotation of the matrix stack.
            val globalRotation = matrix.getRotationEulerZYX()

            // We only need to compute the scale/shear matrix if there was some scaling, otherwise we'll just end
            // up with an identity (within rounding errors) matrix and do a bunch of extra work (here and when
            // applying it to other cosmetics) which we don't really need to do.
            val extra = if (!hasScaling) {
                null
            } else {
                // To compute the scale/shear matrix, we need to convert the global pivot and rotation back into a
                // matrix so we can then compute the difference between that and what we actually want to have
                val resultStack = UMatrixStack()
                resultStack.translate(globalPivot.x, globalPivot.y, globalPivot.z)
                resultStack.rotate(globalRotation.z, 0.0f, 0.0f, 1.0f, false)
                resultStack.rotate(globalRotation.y, 0.0f, 1.0f, 0.0f, false)
                resultStack.rotate(globalRotation.x, 1.0f, 0.0f, 0.0f, false)
                // To compute the difference, we also need to undo the final translate on the current stack because
                // the extra matrix is applied before that, right after rotation (because the MC renderer doesn't do
                // that final translate).
                val expectedStack = matrixStack.fork()
                expectedStack.translate(pivotX, pivotY, pivotZ)
                // The final transform for a given bone in other cosmetics will end up being
                //   M = R * X
                // where
                //   M is the final transform, this should end up matching the `expectedStack` computed above
                //   R is the combination of translation and rotation, this will match `resultStack` computed above
                //   X is a remainder of scale/shear transformations, this is what we need to compute and store
                // To do so, we simply multiply by the inverse of R (denoted by R') from the left on both sides.
                // The Rs on the right side will cancel out and we're left with:
                //   R' * M = X
                // or
                //   X = R' * M
                // which we can easily compute as follows:
                resultStack.peek().model.inverse().times(expectedStack.peek().model)
            }

            parts[part] = PlayerPose.Part(
                // As per the matrix stack transformations above, if we assume that this point doesn't have any
                // parent (as would be the case for regular cosmetics), the global pivot point can be computed as:
                //   (a) globalPivot = bone.pivot + bone.animOffset
                // As per above [copy] method, the right side variables are set as:
                //   (b) bone.pivot = offset.pivot
                //   (c) bone.animOffset = pose.pivot + offset.offset
                // We're trying to compute the `pose.pivot` value we need to store to replicate in other cosmetics
                // the `globalPivot` value observed above.
                // We can first rearrange the above equations as:
                //   (a) bone.animOffset = globalPivot - offset.pivot
                //   (b) bone.offset = bone.pivot
                //   (c) pose.pivot = bone.animOffset - bone.offset
                // Substituting (a) and (b) into (c) gives us the result we're looking for:
                //   pose.pivot = globalPivot - offset.pivot - offset.offset
                // A few of the signs get flipped for Y, the details of that are left as an exercise to the reader.
                pivotX = (globalPivot.x - offset.pivotX - offset.offsetX),
                pivotY = (globalPivot.y - offset.pivotY + offset.offsetY),
                pivotZ = (globalPivot.z - offset.pivotZ - offset.offsetZ),
                // Global rotation, again, is easier because we don't have to deal with any offsets
                rotateAngleX = globalRotation.x,
                rotateAngleY = globalRotation.y,
                rotateAngleZ = globalRotation.z,
                extra = extra,
            )
        }

        for (childModel in childModels) {
            childModel.visit(matrixStack, hasScaling)
        }

        matrixStack.pop()
    }

    bones.root.visit(UMatrixStack(), false)

    return PlayerPose.fromMap(parts, basePose.child)
}
