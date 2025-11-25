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
package gg.essential.mod.cosmetics

import gg.essential.model.BedrockModel
import gg.essential.model.backend.RenderBackend
import gg.essential.model.file.ModelFile
import gg.essential.model.util.now
import gg.essential.network.cosmetics.Cosmetic
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object CapeModel {
    val GEOMETRY_ID = "__internal_cape_model__"

    private val capeModelJson =
        """
        {
            "format_version": "1.12.0",
            "minecraft:geometry": [
                {
                    "description": {
                        "identifier": "$GEOMETRY_ID",
                        "texture_width": 64,
                        "texture_height": 32,
                        "visible_bounds_width": 3,
                        "visible_bounds_height": 4,
                        "visible_bounds_offset": [0, 1, 0]
                    },
                    "bones": [
                        {
                            "name": "root",
                            "pivot": [0, 0, 0]
                        },
                        {
                            "name": "cape",
                            "parent": "root",
                            "pivot": [0, 24, 2],
                            "rotation": [-6, 180, 0],
                            "cubes": [
                                {"origin": [-5, 8, 1], "size": [10, 16, 1], "uv": [0, 0]}
                            ]
                        },
                        {
                            "name": "left_wing",
                            "parent": "root",
                            "pivot": [0, 24, 2],
                            "rotation": [-15, 0, -15],
                            "cubes": [
                                {"origin": [-5, 4, 2], "size": [10, 20, 2], "uv": [22, 0], "inflate": 1}
                            ]
                        },
                        {
                            "name": "right_wing",
                            "parent": "root",
                            "pivot": [0, 24, 2],
                            "rotation": [-15, 0, 15],
                            "cubes": [
                                {"origin": [-5, 4, 2], "size": [10, 20, 2], "uv": [22, 0], "mirror": true, "inflate": 1}
                            ]
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

    val capeModelFile: ModelFile = Json.decodeFromString(capeModelJson)

    private val type = CosmeticType("CAPE", CosmeticSlot.CAPE, emptyMap(), emptyMap())
    private val cosmetic =
        Cosmetic(
            "CAPE",
            type,
            CosmeticTier.COMMON,
            emptyMap(),
            emptyMap(),
            emptyList(),
            -1,
            emptyMap(),
            emptySet(),
            now(),
            null,
            null,
            null,
            emptyMap(),
            emptyMap(),
            0,
        )

    private fun dummyTexture(height: Int) = object : RenderBackend.Texture {
        override val width: Int
            get() = 64
        override val height: Int
            get() = height
    }

    private val models = mutableMapOf<Int, BedrockModel>()

    fun get(textureHeight: Int) = models.getOrPut(textureHeight) {
        val capeModel = BedrockModel(cosmetic, "", capeModelFile, null, emptyMap(), null, dummyTexture(textureHeight), null, emptyMap())
        capeModel.texture = null
        capeModel
    }
}
