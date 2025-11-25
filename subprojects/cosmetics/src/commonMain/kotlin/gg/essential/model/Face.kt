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

import gg.essential.model.util.UMatrixStack
import gg.essential.model.util.UVertexConsumer
import kotlin.jvm.JvmField
import kotlin.math.floor

// TODO clean up
class Face(@JvmField var vertexPositions: Array<PositionTexVertex>) {
    private var normal: Vector3

    init {
        val (a, b, c) = vertexPositions.map { it.vector3 }
        normal = (c - b).cross(a - b).normalize()
    }

    constructor(
        vertices: Array<PositionTexVertex>,
        texcoordU1: Float,
        texcoordV1: Float,
        texcoordU2: Float,
        texcoordV2: Float,
        frameWidth: Float,
        frameHeight: Float
    ) : this(vertices) {
        val u1 = floor(texcoordU1)
        val v1 = floor(texcoordV1)
        val u2 = floor(texcoordU2)
        val v2 = floor(texcoordV2)
        val du = 0.0f / frameWidth
        val dv = 0.0f / frameHeight
        vertices[0] = vertices[0].setTexturePosition(u2 / frameWidth - du, v1 / frameHeight + dv)
        vertices[1] = vertices[1].setTexturePosition(u1 / frameWidth + du, v1 / frameHeight + dv)
        vertices[2] = vertices[2].setTexturePosition(u1 / frameWidth + du, v2 / frameHeight - dv)
        vertices[3] = vertices[3].setTexturePosition(u2 / frameWidth - du, v2 / frameHeight - dv)
    }

    fun flipFace() {
        vertexPositions = vertexPositions.mapIndexed { i, _ ->
            vertexPositions[vertexPositions.size - i - 1]
        }.toTypedArray()
        normal = Vector3().sub(normal)
    }

    /**
     * Draw this primitve. This is typically called only once as the generated drawing instructions are saved by the
     * renderer and reused later.
     */
    fun draw(
        matrixStack: UMatrixStack,
        buffer: UVertexConsumer,
        light: Int,
        verticalUVOffset: Float
    ) {
        val nx = normal.x
        val ny = normal.y
        val nz = normal.z
        for (i in 0..3) {
            val PositionTexVertex = vertexPositions[i]
            buffer.pos(
                matrixStack,
                PositionTexVertex.vector3.x.toDouble(),
                PositionTexVertex.vector3.y.toDouble(),
                PositionTexVertex.vector3.z.toDouble(),
            )
            buffer.tex(
                PositionTexVertex.texturePositionX.toDouble(),
                (PositionTexVertex.texturePositionY + verticalUVOffset).toDouble(),
            )
            buffer.norm(matrixStack, nx, ny, nz)
            buffer.endVertex()
        }
    }
}