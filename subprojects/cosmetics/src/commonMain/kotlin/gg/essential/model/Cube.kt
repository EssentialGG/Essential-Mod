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

// TODO clean up
class Cube {
    /**
     * X vertex coordinate of lower box corner
     */
    var posX1 = 0f

    /**
     * Y vertex coordinate of lower box corner
     */
    var posY1 = 0f

    /**
     * Z vertex coordinate of lower box corner
     */
    var posZ1 = 0f

    /**
     * X vertex coordinate of upper box corner
     */
    var posX2 = 0f

    /**
     * Y vertex coordinate of upper box corner
     */
    var posY2 = 0f

    /**
     * Z vertex coordinate of upper box corner
     */
    var posZ2 = 0f

    /**
     * An arraylist of TexturedQuads. Min of two per face for each orientation but can be more depending on intersections
     */
    private val quadList = mutableListOf<Face>()

    var boxName: String? = null
    val mirror: Boolean

    constructor(
        texU: Float,
        texV: Float,
        x: Float,
        y: Float,
        z: Float,
        dx: Float,
        dy: Float,
        dz: Float,
        delta: Float,
        mirror: Boolean,
        textureWidth: Int, textureHeight: Int,
    ) : this(x, y, z, dx, dy, dz, delta, mirror, textureWidth, textureHeight, texU, texV, null)

    constructor(
        x: Float,
        y: Float,
        z: Float,
        dx: Float,
        dy: Float,
        dz: Float,
        delta: Float,
        mirror: Boolean,
        textureWidth: Int, textureHeight: Int,
        uvData: CubeUvData?
    ) : this(x, y, z, dx, dy, dz, delta, mirror, textureWidth, textureHeight, 0f, 0f, uvData)

    constructor(precomputedFaces: List<Face>, mirror: Boolean) {
        quadList.addAll(precomputedFaces)
        this.mirror = mirror
    }

    private constructor(
        x: Float, y: Float, z: Float,
        dx: Float, dy: Float, dz: Float,
        delta: Float, mirror: Boolean,
        textureWidth: Int, textureHeight: Int,
        texU: Float, texV: Float, // these are only used if uvData is null
        uvData: CubeUvData?
    ) {
        var x = x
        var y = y
        var z = z
        posX1 = x
        posY1 = y
        posZ1 = z
        posX2 = x + dx
        posY2 = y + dy
        posZ2 = z + dz
        var x2 = x + dx
        var y2 = y + dy
        var z2 = z + dz
        x = x - delta
        y = y - delta
        z = z - delta
        x2 = x2 + delta
        y2 = y2 + delta
        z2 = z2 + delta
        if (mirror) {
            val f3 = x2
            x2 = x
            x = f3
        }
        val PositionTexVertex7 = PositionTexVertex(x, y, z, 0.0f, 0.0f)
        val PositionTexVertex = PositionTexVertex(x2, y, z, 0.0f, 8.0f)
        val PositionTexVertex1 = PositionTexVertex(x2, y2, z, 8.0f, 8.0f)
        val PositionTexVertex2 = PositionTexVertex(x, y2, z, 8.0f, 0.0f)
        val PositionTexVertex3 = PositionTexVertex(x, y, z2, 0.0f, 0.0f)
        val PositionTexVertex4 = PositionTexVertex(x2, y, z2, 0.0f, 8.0f)
        val PositionTexVertex5 = PositionTexVertex(x2, y2, z2, 8.0f, 8.0f)
        val PositionTexVertex6 = PositionTexVertex(x, y2, z2, 8.0f, 0.0f)
        val DEFAULT_UV_NORTH = floatArrayOf(texU + dz, texV + dz, texU + dz + dx, texV + dz + dy)
        val DEFAULT_UV_EAST = floatArrayOf(texU, texV + dz, texU + dz, texV + dz + dy)
        val DEFAULT_UV_SOUTH = floatArrayOf(texU + dz + dx + dz, texV + dz, texU + dz + dx + dz + dx, texV + dz + dy)
        val DEFAULT_UV_WEST = floatArrayOf(texU + dz + dx, texV + dz, texU + dz + dx + dz, texV + dz + dy)
        val DEFAULT_UV_UP = floatArrayOf(texU + dz, texV, texU + dz + dx, texV + dz)
        val DEFAULT_UV_DOWN = floatArrayOf(texU + dz + dx, texV + dz, texU + dz + dx + dx, texV)
        val north = uvData?.north ?: DEFAULT_UV_NORTH
        val east = uvData?.east ?: DEFAULT_UV_EAST
        val south = uvData?.south ?: DEFAULT_UV_SOUTH
        val west = uvData?.west ?: DEFAULT_UV_WEST
        val up = uvData?.up ?: DEFAULT_UV_UP
        val down = uvData?.down ?: DEFAULT_UV_DOWN
        quadList.add(Face(arrayOf(PositionTexVertex4, PositionTexVertex, PositionTexVertex1, PositionTexVertex5), west[0], west[1], west[2], west[3], textureWidth.toFloat(), textureHeight.toFloat())) //+x
        quadList.add(Face(arrayOf(PositionTexVertex7, PositionTexVertex3, PositionTexVertex6, PositionTexVertex2), east[0], east[1], east[2], east[3], textureWidth.toFloat(), textureHeight.toFloat())) //-x
        quadList.add(Face(arrayOf(PositionTexVertex4, PositionTexVertex3, PositionTexVertex7, PositionTexVertex), up[0], up[1], up[2], up[3], textureWidth.toFloat(), textureHeight.toFloat()))
        quadList.add(Face(arrayOf(PositionTexVertex1, PositionTexVertex2, PositionTexVertex6, PositionTexVertex5), down[0], down[1], down[2], down[3], textureWidth.toFloat(), textureHeight.toFloat()))
        quadList.add(Face(arrayOf(PositionTexVertex, PositionTexVertex7, PositionTexVertex2, PositionTexVertex1), north[0], north[1], north[2], north[3], textureWidth.toFloat(), textureHeight.toFloat()))
        quadList.add(Face(arrayOf(PositionTexVertex3, PositionTexVertex4, PositionTexVertex5, PositionTexVertex6), south[0], south[1], south[2], south[3], textureWidth.toFloat(), textureHeight.toFloat()))

        // TODO could skip generation of inverted faces when we know the corresponding texture to be fully opaque
        for (i in 0..5) {
            val (a, b, c, d) = quadList[i].vertexPositions
            quadList.add(Face(arrayOf(b, a, d, c)))
        }
        this.mirror = mirror
        if (mirror) {
            for (texturedquad in quadList) {
                texturedquad.flipFace()
            }
        }
    }

    fun render(
        matrixStack: UMatrixStack,
        renderer: UVertexConsumer,
        light: Int,
        verticalUVOffset: Float
    ) {
        for (texturedquad in quadList) {
            texturedquad.draw(matrixStack, renderer, light, verticalUVOffset)
        }
    }

    fun setBoxName(name: String?): Cube {
        boxName = name
        return this
    }

    fun getQuadList(): MutableList<Face> {
        return quadList
    }

    fun deepCopy(): Cube {
        return Cube(quadList.map { face ->
            Face(face.vertexPositions.map { it.copy() }.toTypedArray())
        }, mirror)
    }
}