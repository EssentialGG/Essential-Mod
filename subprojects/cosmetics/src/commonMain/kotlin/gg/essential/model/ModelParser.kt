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

import gg.essential.mod.cosmetics.CapeModel
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.model.file.ModelFile
import gg.essential.network.cosmetics.Cosmetic
import kotlin.math.PI

class ModelParser(
    private val cosmetic: Cosmetic,
    private val textureWidth: Int,
    private val textureHeight: Int,
) {
    val boundingBoxes = mutableListOf<Pair<Box3, Side?>>()
    var textureFrameCount = 1
    var translucent = false

    fun parse(file: ModelFile): Pair<Bones, RenderGeometry>? {
        val geometry = file.geometries.firstOrNull() ?: return null

        textureFrameCount = (textureHeight / geometry.description.textureHeight).coerceAtLeast(1)
        translucent = geometry.description.textureTranslucent

        val extraInflate = when {
            cosmetic.type.id == "PLAYER" -> 0f
            else -> ((EXTRA_INFLATE_GROUPS.find { it.value.contains(cosmetic.type.slot) }?.index ?: 0) * 0.01f) + 0.01f
        }

        val bones = mutableListOf<Bone>()
        val renderGeometry = mutableListOf<List<Cube>>()
        val children = mutableMapOf<String, MutableList<Bone>>()

        // Iterating in reverse order so we parse all children before making the parent
        for (bone in geometry.bones.asReversed()) {
            if (bone.name.startsWith("bbox_")) {
                for (cube in bone.cubes) {
                    val origin = cube.origin
                    val size = cube.size
                    val box = Box3()
                    box.expandByPoint(origin.copy().negateY())
                    box.expandByPoint((origin + size).negateY())
                    box.expandByScalar(cube.inflate + 0.025f)
                    boundingBoxes.add(box to bone.side)
                }
                continue // only for data purposes, do not render
            }
            val offset = EnumPart.fromBoneName(bone.name)?.let { BedrockModel.OFFSETS.getValue(it) }
            val boneModel = Bone(
                bones.size,
                bone.name,
                children.remove(bone.name) ?: emptyList(),
                pivotX = offset?.pivotX ?: bone.pivot.x,
                pivotY = offset?.pivotY ?: -bone.pivot.y,
                pivotZ = offset?.pivotZ ?: bone.pivot.z,
                poseRotX = bone.rotation.x.toRadians(),
                poseRotY = bone.rotation.y.toRadians(),
                poseRotZ = bone.rotation.z.toRadians(),
                side = bone.side,
            )
            bones.add(boneModel)

            renderGeometry.add(parseCubes(geometry, bone, extraInflate))

            // Add to front of list because we iterate all bones in reverse
            children.getOrPut(bone.parent ?: ROOT_BONE_NAME, ::mutableListOf).add(0, boneModel)
        }

        bones.add(Bone(bones.size, ROOT_BONE_NAME, children.remove(ROOT_BONE_NAME) ?: emptyList()))
        renderGeometry.add(emptyList())

        return Pair(Bones(bones), renderGeometry)
    }

    private fun parseCubes(geometry: ModelFile.Geometry, bone: ModelFile.Bone, extraInflate: Float): List<Cube> {
        val cubeList = mutableListOf<Cube>()

        for (cube in bone.cubes) {
            val (x, y, z) = cube.origin.copy().negateY()
            val (dx, dy, dz) = cube.size
            val mirror = cube.mirror ?: bone.mirror

            val inflate = cube.inflate + extraInflate
            val cubeModel = when (val uv = cube.uv) {
                is ModelFile.Uvs.PerFace -> {
                    val uvData = CubeUvData(
                        uv.north.toFloatArray(),
                        uv.east.toFloatArray(),
                        uv.south.toFloatArray(),
                        uv.west.toFloatArray(),
                        uv.up.toFloatArray(),
                        uv.down.toFloatArray()
                    )
                    Cube(x, y - dy, z, dx, dy, dz, inflate, mirror, textureWidth, textureHeight, uvData)
                }
                is ModelFile.Uvs.Box -> {
                    val (u, v) = uv.uv
                    Cube(u, v, x, y - dy, z, dx, dy, dz, inflate, mirror, textureWidth, textureHeight)
                }
            }
            cubeList.add(cubeModel)
        }

        // For capes, we render the actual cape separately (so conceptually, the model only includes *extra*
        // geometry). However, for backwards compatibility, we still include the cape cube in the cosmetic file, so
        // we need to remove it from the model.
        // (except for the internal CapeModel which we use in place of the vanilla cape renderer in certain cases)
        if (EnumPart.fromBoneName(bone.name) == EnumPart.CAPE && geometry.description.identifier != CapeModel.GEOMETRY_ID) {
            cubeList.removeFirstOrNull()
        }

        return cubeList
    }

    private fun Float.toRadians() = (this / 180.0 * PI).toFloat()

    private fun ModelFile.UvFace?.toFloatArray(): FloatArray {
        this ?: return floatArrayOf(0f, 0f, 0f, 0f)
        val (u, v) = uv
        val (du, dv) = size
        return floatArrayOf(u, v, u + du, v + dv)
    }

    companion object {
        // Ascending priority -> higher inflate
        private val EXTRA_INFLATE_GROUPS = listOf(
            setOf(
                CosmeticSlot.PANTS,
            ),
            setOf(
                CosmeticSlot.TOP,
                CosmeticSlot.HEAD,
                CosmeticSlot.FACE,
                CosmeticSlot.BACK,
            ),
            setOf(
                CosmeticSlot.SKIRT,
            ),
            setOf(
                CosmeticSlot.HAT,
                CosmeticSlot.FULL_BODY,
            ),
            setOf(
                CosmeticSlot.ACCESSORY,
                CosmeticSlot.ARMS,
                CosmeticSlot.SHOES,
            )
        ).withIndex()

        const val ROOT_BONE_NAME = "__internal_root__"
    }
}
