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
package gg.essential.mod.cosmetics.preview

import dev.folomeev.kotgl.matrix.vectors.Vec3
import dev.folomeev.kotgl.matrix.vectors.mutables.minus
import dev.folomeev.kotgl.matrix.vectors.mutables.plus
import dev.folomeev.kotgl.matrix.vectors.mutables.times
import dev.folomeev.kotgl.matrix.vectors.vec3
import dev.folomeev.kotgl.matrix.vectors.vecUnitY
import dev.folomeev.kotgl.matrix.vectors.vecZero
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.model.util.Quaternion
import gg.essential.model.util.UMatrixStack

data class PerspectiveCamera(val camera: Vec3, val target: Vec3, val fov: Float) {
    val rotation: Quaternion
        get() = Quaternion.fromLookAt(target.minus(camera), vecUnitY())

    fun createModelViewMatrix(): UMatrixStack {
        val stack = UMatrixStack()
        stack.rotate(rotation.invert())
        stack.translate(vecZero().minus(camera))
        return stack
    }

    companion object {
        fun forCosmeticSlot(slot: CosmeticSlot): PerspectiveCamera {

            @Suppress("FunctionName")
            fun Vector3(x: Number, y: Number, z: Number): Vec3 =
                vec3(x.toFloat(), y.toFloat(), z.toFloat())

            infix fun Vec3.to(target: Vec3): PerspectiveCamera =
                PerspectiveCamera(this.times(1 / 16f), target.times(1 / 16f), 22f)

            // The player is standing on 0/0/0 looking towards negative Z.
            return when (slot) {
                CosmeticSlot.CAPE -> Vector3(-30.3, 38.5, 46.6) to Vector3(0.0, 14.9, 0.0)
                CosmeticSlot.BACK -> Vector3(-50.3, 48.5, 60.6) to Vector3(0.0, 16.9, 0.0)
                CosmeticSlot.HAT, CosmeticSlot.PET -> Vector3(34.6, 50.5, -40) to Vector3(0, 30, 0)
                CosmeticSlot.WINGS -> Vector3(0, 20, 138) to Vector3(0, 20, 0)
                CosmeticSlot.FACE, CosmeticSlot.HEAD -> Vector3(29.7, 44, -34.3) to Vector3(0, 26, 0)
                CosmeticSlot.SHOULDERS -> Vector3(32.3, 37, -36.8) to Vector3(2.6, 24, -2.5)
                CosmeticSlot.ARMS -> Vector3(32.3, 34.9, -36.8) to Vector3(2.6, 16.9, -2.5)
                CosmeticSlot.PANTS, CosmeticSlot.SKIRT -> Vector3(34, 29.1, -38.4) to Vector3(4.3, 11.1, -4.1)
                CosmeticSlot.SHOES -> Vector3(30.5, 12.4, -29.4) to Vector3(4.2, 4.8, -3.8)
                CosmeticSlot.SUITS -> Vector3(73.3, 20.7, -81.3) to Vector3(3.7, 19.2, -2.8)
                CosmeticSlot.EFFECT -> Vector3(73.3, 20.7, -81.3) to Vector3(3.7, 18.2, -2.8)
                CosmeticSlot.EARS -> Vector3(29.7, 44, -34.3) to Vector3(0, 26, 0) // FIXME
                CosmeticSlot.EMOTE -> Vector3(71.8, 17.2, -81.3) to Vector3(2.2, 15.7, -2.8)
                CosmeticSlot.ICON -> Vector3(34.6, 50.5, -40) to Vector3(0, 30, 0)
                CosmeticSlot.TOP -> Vector3(34.7, 36.2, -39.5) to Vector3(2.6, 17.0, -2.5)
                CosmeticSlot.ACCESSORY -> Vector3(34.7, 36.2, -39.5) to Vector3(2.6, 17.0, -2.5)
                CosmeticSlot.FULL_BODY -> Vector3(73.3, 20.7, -81.3) to Vector3(3.7, 18.2, -2.8)

                // These have a camera config but no cosmetics yet
                // CosmeticSlot.RIDEABLE -> Vector3(73.3, 20.7, -81.3) to Vector3(3.7, 19.2, -2.8),

                // Fall back for other/unknown slots (currently same as FULL_BODY, leaving separate if it changes)
                else -> Vector3(73.3, 20.7, -81.3) to Vector3(3.7, 18.2, -2.8)
            }
        }
    }
}
