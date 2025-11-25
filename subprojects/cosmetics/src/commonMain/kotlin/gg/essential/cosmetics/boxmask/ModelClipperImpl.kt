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
package gg.essential.cosmetics.boxmask

import gg.essential.model.Box3
import gg.essential.model.Cube
import gg.essential.model.Face
import gg.essential.model.RenderGeometry
import kotlin.math.min

// TODO clean up
class ModelClipperImpl : ModelClipper {
    override fun compute(geometry: RenderGeometry, masks: List<Box3>): RenderGeometry {
        if (masks.isEmpty()) {
            return geometry
        }
        return geometry.map { apply(it, masks) }
    }

    private fun apply(cubeList: List<Cube>, renderExclusions: List<Box3>): List<Cube> {
        return cubeList.map { orgCube ->
            val cube = orgCube.deepCopy()
            val iterator = cube.getQuadList().iterator()
            val newFaces = mutableListOf<Face>()
            //We detect if this face enters a region that we don't want to cosmetics inside of.
            //If we detect that we are inside an excluded region, we will decrease the size of the face
            //So that it reaches the edge but does not enter this region. There are 4 regions (A,B,C,D)
            //That are created on intersection. These are shown in docs/cosmetic masking.jpeg
            //Currently only the A region is implemented on the Y axis as that is the only one
            //We need to support for cosmetics
            while (iterator.hasNext()) {
                val next = iterator.next()
                val selfBox = Box3()
                val vertices = next.vertexPositions
                val a = vertices[0].vector3.clone()
                val b = vertices[1].vector3.clone()
                val c = vertices[2].vector3.clone()
                val d = vertices[3].vector3.clone()
                selfBox.setFromPoints(listOf(a, b, c, d))
                var matched = false
                val intersectedFace = IntersectedFace(next, cube.mirror)
                for (exclusion in renderExclusions) {
                    val intersect = selfBox.clone().intersect(exclusion)

                    //Completely encased
                    if (intersect == selfBox) {
                        iterator.remove()
                        matched = false //A previous face might have only partially clipped but we want to remove it
                        break
                    }
                    if (!intersect.isEmpty()) {
                        matched = true
                        val xHeight = intersect.max.x - intersect.min.x
                        val zHeight = intersect.max.z - intersect.min.z
                        val minYIntersect = min(intersect.min.y, intersectedFace.aRegion.points[3].vector3.y)
                        if (xHeight == 0f) {
                            generateARegion(intersectedFace, minYIntersect)
                        } else if (zHeight == 0f) {
                            generateARegion(intersectedFace, minYIntersect)
                        }
                    }
                }
                if (matched) {
                    iterator.remove()
                    newFaces.addAll(intersectedFace.generateFaces())
                }
            }
            cube.getQuadList().addAll(newFaces)
            cube
        }
    }

    private fun generateARegion(intersectedFace: IntersectedFace, minYIntersect: Float) {
        intersectedFace.aRegion.points[2].vector3.y = minYIntersect
        intersectedFace.aRegion.points[3].vector3.y = minYIntersect
        val texY = intersectedFace.aRegion.points[1].texturePositionY + (minYIntersect - intersectedFace.aRegion.points[0].vector3.y) / intersectedFace.aRegion.spacialYDistance * intersectedFace.aRegion.textureYDistance
        intersectedFace.aRegion.points[2].texturePositionY = texY
        intersectedFace.aRegion.points[3].texturePositionY = texY
    }

    //Render regions for intersected faces
    private enum class EnumRegion {
        A, B, C, D
    }

    //Holds all render regions
    private class IntersectedFace(base: Face, mirror: Boolean) {
        val aRegion = FaceRegion(EnumRegion.A, base, mirror)

        fun generateFaces(): List<Face> {
            return listOf(aRegion.toFace())
        }
    }

    //Contains data about what the face modified due to clipping
    private class FaceRegion(region: EnumRegion, base: Face, mirror: Boolean) {
        var points = base.vertexPositions.map { it.copy() }.toTypedArray()
        var spacialYDistance = 0f
        var textureYDistance = 0f

        init {
            if (mirror) flipFace()
            if (region == EnumRegion.A) {
                spacialYDistance = points[2].vector3.y - points[1].vector3.y
                textureYDistance = points[2].texturePositionY - points[1].texturePositionY
            }
        }

        fun flipFace() {
            points = points.mapIndexed { i, _ ->
                points[points.size - i - 1]
            }.toTypedArray()
        }

        fun toFace(): Face {
            return Face(points.map { it.copy() }.toTypedArray())
        }
    }
}