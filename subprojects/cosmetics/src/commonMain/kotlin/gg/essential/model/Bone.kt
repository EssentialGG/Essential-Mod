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

import dev.folomeev.kotgl.matrix.matrices.Mat4
import dev.folomeev.kotgl.matrix.matrices.mutables.timesSelf
import dev.folomeev.kotgl.matrix.vectors.vecUnitX
import dev.folomeev.kotgl.matrix.vectors.vecUnitY
import dev.folomeev.kotgl.matrix.vectors.vecUnitZ
import gg.essential.model.util.Quaternion
import gg.essential.model.util.UMatrixStack
import gg.essential.model.util.UVertexConsumer

typealias BoneId = Int

// TODO clean up
class Bone(
    val id: BoneId,
    val boxName: String,
    val childModels: List<Bone> = emptyList(),
    val pivotX: Float = 0f,
    val pivotY: Float = 0f,
    val pivotZ: Float = 0f,
    var poseRotX: Float = 0f,
    var poseRotY: Float = 0f,
    var poseRotZ: Float = 0f,
    val side: Side? = null,
) {
    val part: EnumPart? = EnumPart.fromBoneName(boxName)

    var poseOffsetX = 0f
    var poseOffsetY = 0f
    var poseOffsetZ = 0f
    var poseExtra: Mat4? = null

    var animOffsetX = 0f
    var animOffsetY = 0f
    var animOffsetZ = 0f
    var animRotX = 0f
    var animRotY = 0f
    var animRotZ = 0f
    var animScaleX = 0f
    var animScaleY = 0f
    var animScaleZ = 0f

    var userOffsetX = 0f
    var userOffsetY = 0f
    var userOffsetZ = 0f

    var child = false

    var visible: Boolean? = null // determines visibility for all bones in this tree unless overwritten in a child
    private var fullyInvisible = false // propagateVisibility has determined that we can skip this entire tree
    var isVisible = true // actual visibility for this specific bone, set in propagateVisibility
        private set // private to ensure isVisible is still only set by propagateVisibility

    /** Whether an animation targeting this bone will have an effect on the player pose. */
    val affectsPose: Boolean
        get() = affectsPoseParts.isNotEmpty()
    /** Which parts of the player pose will be affected by an animation targeting this bone. */
    val affectsPoseParts: Set<EnumPart> = buildSet {
        part?.let { add(it) }
        childModels.forEach { addAll(it.affectsPoseParts) }
    }

    /** Whether this bone should undo all parent rotation, as if it was stabilized by three gimbals. */
    var gimbal = false
    /** Quaternion representing the rotation of all parents to be undone if [gimbal] is `true` */
    var parentRotation: Quaternion = Quaternion.Identity
    /** Whether this bone should also undo the rotation of the entity in addition to [gimbal]. */
    var worldGimbal = false

    init {
        resetAnimationOffsets(false)
    }

    private fun propagateSideVisibilityOff(){
        // Propagate visibility for disabled sides so Locator.isVisible is always correct for these Bones
        isVisible = false
        fullyInvisible = true
        for (child in childModels) {
            child.propagateSideVisibilityOff()
        }
    }

    fun propagateVisibility(parentVisible: Boolean, side: Side?) {
        if (this.side != null && side != null && this.side !== side) {
            propagateSideVisibilityOff()
            return
        }
        val isVisible = if (visible == null) parentVisible else visible!!
        var fullyInvisible = !isVisible
        for (child in childModels) {
            child.propagateVisibility(isVisible, side)
            fullyInvisible = fullyInvisible and child.fullyInvisible
        }
        this.isVisible = isVisible
        this.fullyInvisible = fullyInvisible
    }

    fun resetAnimationOffsets(recursive: Boolean) {
        animOffsetZ = 0f
        animOffsetY = animOffsetZ
        animOffsetX = animOffsetY
        animRotZ = 0f
        animRotY = animRotZ
        animRotX = animRotY
        animScaleZ = 1f
        animScaleY = animScaleZ
        animScaleX = animScaleY
        gimbal = false
        if (recursive) {
            for (childModel in childModels) {
                childModel.resetAnimationOffsets(true)
            }
        }
    }

    fun applyTransform(matrixStack: UMatrixStack) {
        if (child) {
            val isHead = part == EnumPart.HEAD
            val childScale = if (isHead) 0.75f else 0.5f
            matrixStack.scale(childScale, childScale, childScale)
            if (isHead) matrixStack.translate(0f, 8f, 0f)
        }
        matrixStack.translate(pivotX + poseOffsetX + animOffsetX, pivotY - poseOffsetY - animOffsetY, pivotZ + poseOffsetZ + animOffsetZ)
        if (gimbal) {
            matrixStack.rotate(parentRotation.conjugate())
        }
        matrixStack.rotate(poseRotZ + animRotZ, 0.0f, 0.0f, 1.0f, false)
        matrixStack.rotate(poseRotY + animRotY, 0.0f, 1.0f, 0.0f, false)
        matrixStack.rotate(poseRotX + animRotX, 1.0f, 0.0f, 0.0f, false)
        poseExtra?.let {
            matrixStack.peek().model.timesSelf(it)
        }
        matrixStack.scale(animScaleX, animScaleY, animScaleZ)
        matrixStack.translate(-pivotX - userOffsetX, -pivotY - userOffsetY, -pivotZ - userOffsetZ)
    }

    fun render(
        matrixStack: UMatrixStack,
        renderer: UVertexConsumer,
        geometry: RenderGeometry,
        light: Int,
        verticalUVOffset: Float
    ) {
        if (!fullyInvisible) {
            matrixStack.push()
            applyTransform(matrixStack)
            if (isVisible) {
                for (cube in geometry[id]) {
                    cube.render(matrixStack, renderer, light, verticalUVOffset)
                }
            }
            for (childModel in childModels) {
                if (childModel.part != null) {
                    // Special parts do not actually inherit the matrix stack, because it was already baked into their
                    // pose, so we'll render them separately
                    continue
                }
                childModel.render(matrixStack, renderer, geometry, light, verticalUVOffset)
            }
            matrixStack.pop()
        }
    }

    /**
     * Returns true if this bone or any of its children contain visible boxes
     */
    fun containsVisibleBoxes(geometry: RenderGeometry): Boolean {
        return !fullyInvisible && ((geometry[id].isNotEmpty() && isVisible) || this.childModels.any { it.containsVisibleBoxes(geometry) })
    }

    fun propagateGimbal(parentRotation: Quaternion, entityRotation: Quaternion) {
        if (gimbal) {
            // If this is a gimbal, ignore parent and pose rotation, only keep animation rotation
            this.poseRotX = 0f
            this.poseRotY = 0f
            this.poseRotZ = 0f
            this.parentRotation = if (worldGimbal) entityRotation * parentRotation else parentRotation
        }

        var ownRotation = if (gimbal) Quaternion.Identity else parentRotation
        ownRotation *= Quaternion.fromAxisAngle(vecUnitZ(), poseRotZ + animRotZ)
        ownRotation *= Quaternion.fromAxisAngle(vecUnitY(), poseRotY + animRotY)
        ownRotation *= Quaternion.fromAxisAngle(vecUnitX(), poseRotX + animRotX)
        for (child in childModels) {
            child.propagateGimbal(ownRotation, if (worldGimbal) Quaternion.Identity else entityRotation)
        }
    }
}
