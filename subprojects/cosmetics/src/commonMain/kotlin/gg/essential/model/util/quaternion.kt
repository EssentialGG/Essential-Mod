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
package gg.essential.model.util

import dev.folomeev.kotgl.matrix.matrices.Mat3
import dev.folomeev.kotgl.matrix.matrices.mat3
import dev.folomeev.kotgl.matrix.vectors.Vec3
import dev.folomeev.kotgl.matrix.vectors.dot
import dev.folomeev.kotgl.matrix.vectors.mutables.cross
import dev.folomeev.kotgl.matrix.vectors.mutables.minus
import dev.folomeev.kotgl.matrix.vectors.mutables.mutableVec3
import dev.folomeev.kotgl.matrix.vectors.mutables.normalizeSelf
import dev.folomeev.kotgl.matrix.vectors.mutables.times
import dev.folomeev.kotgl.matrix.vectors.vecZero
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Quaternion(val x: Float, val y: Float, val z: Float, val w: Float) {
    fun normalize(): Quaternion {
        val sqr = x * x + y * y + z * z + w * w
        if (sqr < 1e-8) return Identity // Practically no rotation, avoids divide by 0, and sqrt()
        val invSqrt = 1 / sqrt(sqr)
        return Quaternion(x * invSqrt, y * invSqrt, z * invSqrt, w * invSqrt)
    }

    fun conjugate(): Quaternion =
        Quaternion(-x, -y, -z, w)

    fun invert(): Quaternion {
        val invNorm = 1 / (x * x + y * y + z * z + w * w)
        return Quaternion(-x * invNorm, -y * invNorm, -z * invNorm, w * invNorm)
    }

    operator fun times(q: Quaternion): Quaternion {
        if (this === Identity) return q
        if (q === Identity) return this
        return Quaternion(
            w * q.x + x * q.w + y * q.z - z * q.y,
            w * q.y - x * q.z + y * q.w + z * q.x,
            w * q.z + x * q.y - y * q.x + z * q.w,
            w * q.w - x * q.x - y * q.y - z * q.z,
        )
    }

    operator fun times(v: Vec3): Vec3 = v.rotateBy(this)

    /**
     * Projects this quaternion into a quaternion rotating around the given [axis].
     * This given [axis] must be normalized.
     *
     * The result is the twist part of the
     * [swing-twist decomposition](https://www.euclideanspace.com/maths/geometry/rotations/for/decomposition/)
     * of this quaternion.
     */
    fun projectAroundAxis(axis: Vec3): Quaternion {
        val rotationAxis = mutableVec3(x, y, z)
        val projectedLength = axis.dot(rotationAxis)
        val projectedAxis = axis.times(projectedLength)
        return if (projectedLength > 0) {
            Quaternion(projectedAxis.x, projectedAxis.y, projectedAxis.z, w).normalize()
        } else {
            Quaternion(-projectedAxis.x, -projectedAxis.y, -projectedAxis.z, -w).normalize()
        }
    }

    /**
     * If this quaternion represents a camera orientation, this method returns the opposite orientation.
     * The roll of the camera is unaffected.
     */
    fun opposite() = this * Y180

    companion object {
        val Identity = Quaternion(0f, 0f, 0f, 1f)

        /** Equivalent to `fromAxisAngle(vecUnitX(), PI)` */
        val X180 = Quaternion(1f, 0f, 0f, 0f)
        /** Equivalent to `fromAxisAngle(vecUnitY(), PI)` */
        val Y180 = Quaternion(0f, 1f, 0f, 0f)
        /** Equivalent to `fromAxisAngle(vecUnitZ(), PI)` */
        val Z180 = Quaternion(0f, 0f, 1f, 0f)

        /** Returns a quaternion that rotates around the given axis by the given angle. */
        fun fromAxisAngle(axis: Vec3, angleRad: Float): Quaternion {
            if (angleRad == 0f) {
                return Identity
            }
            val s = sin(angleRad * 0.5f)
            val c = cos(angleRad * 0.5f)
            return Quaternion(axis.x * s, axis.y * s, axis.z * s, c)
        }

        /**
         * Returns a quaternion that rotates from the negative Z axis to the given [lookAt] vector.
         * This matches OpenGL convention, that is, a non-rotated camera is looking towards negative Z.
         * [up] must be normalized.
         *
         * E.g.
         * ```
         * fromLookAt(vec3(0, 0, -1), vecUnitY()) == Unit
         * fromLookAt(vec3(0, 0, 1), vecUnitY()) == fromAxisAngle(vecUnitY(), PI)
         * fromLookAt(vec3(-1, 0, 0), vecUnitY()) == fromAxisAngle(vecUnitY(), PI / 2)
         * fromLookAt(vec3(1, 0, 0), vecUnitY()) == fromAxisAngle(vecUnitY(), -PI / 2)
         * fromLookAt(vec3(0, 1, 0), vecUnitZ()) == fromAxisAngle(vecUnitX(), PI / 2)
         * ```
         */
        fun fromLookAt(lookAt: Vec3, up: Vec3): Quaternion {
            val z = vecZero().minus(lookAt).normalizeSelf()
            val x = up.cross(z).normalizeSelf()
            val y = z.cross(x)
            return fromRotationMatrix(mat3(
                x.x, y.x, z.x,
                x.y, y.y, z.y,
                x.z, y.z, z.z,
            ))
        }

        /** Returns a quaternion that rotates the same way as the given (pure) rotation matrix. */
        fun fromRotationMatrix(m: Mat3): Quaternion = with(m) {
            // Algorithm and variable names from https://en.wikipedia.org/wiki/Rotation_matrix#Quaternion
            // Rotational variants from https://github.com/JOML-CI/JOML/blob/1067a8e0e176866b266efeeaebb1674e35ab9eb1/src/main/java/org/joml/Quaternionf.java#L716
            val trace = m00 + m11 + m22
            if (trace >= 0) {
                val r = sqrt(trace + 1f)
                val s = 0.5f / r
                return Quaternion(
                    (m21 - m12) * s,
                    (m02 - m20) * s,
                    (m10 - m01) * s,
                    0.5f * r,
                )
            } else {
                if (m00 >= m11 && m00 >= m22) {
                    val r = sqrt(m00 - (m11 + m22) + 1f)
                    val s = 0.5f / r
                    return Quaternion(
                        0.5f * r,
                        (m10 + m01) * s,
                        (m02 + m20) * s,
                        (m21 - m12) * s,
                    )
                } else if (m11 > m22) {
                    val r = sqrt(m11 - (m22 + m00) + 1f)
                    val s = 0.5f / r
                    return Quaternion(
                        (m10 + m01) * s,
                        0.5f * r,
                        (m21 + m12) * s,
                        (m02 - m20) * s,
                    )
                } else {
                    val r = sqrt(m22 - (m00 + m11) + 1f)
                    val s = 0.5f / r
                    return Quaternion(
                        (m02 + m20) * s,
                        (m21 + m12) * s,
                        0.5f * r,
                        (m10 - m01) * s,
                    )
                }
            }
        }
    }
}
