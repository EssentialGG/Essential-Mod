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
import gg.essential.model.file.AnimationFile
import gg.essential.model.file.ModelFile
import gg.essential.model.util.now
import gg.essential.network.cosmetics.Cosmetic
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object PlayerModel {
    private val cosmeticType =
        CosmeticType("PLAYER", CosmeticSlot.FULL_BODY, emptyMap(), emptyMap())

    private val cosmetic =
        Cosmetic(
            "PLAYER",
            cosmeticType,
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

    private val steveModelJson =
        """
        {
            "format_version": "1.12.0",
            "minecraft:geometry": [
                {
                    "description": {
                        "identifier": "geometry.steve",
                        "texture_width": 64,
                        "texture_height": 64,
                        "visible_bounds_width": 3,
                        "visible_bounds_height": 3.5,
                        "visible_bounds_offset": [0, 1.25, 0]
                    },
                    "bones": [
                        {
                            "name": "root",
                            "pivot": [0, 0, 0]
                        },
                        {
                            "name": "Head",
                            "parent": "root",
                            "pivot": [0, 24, 0],
                            "cubes": [
                                {"origin": [-4, 24, -4], "size": [8, 8, 8], "uv": [0, 0]}
                            ]
                        },
                        {
                            "name": "hat",
                            "parent": "Head",
                            "pivot": [0, 0, 0],
                            "cubes": [
                                {"origin": [-4, 24, -4], "size": [8, 8, 8], "inflate": 0.5, "uv": [32, 0]}
                            ]
                        },
                        {
                            "name": "Body",
                            "parent": "root",
                            "pivot": [0, 12, 0],
                            "cubes": [
                                {"origin": [-4, 12, -2], "size": [8, 12, 4], "uv": [16, 16]}
                            ]
                        },
                        {
                            "name": "jacket",
                            "parent": "Body",
                            "pivot": [0, 0, 0],
                            "cubes": [
                                {"origin": [-4, 12, -2], "size": [8, 12, 4], "inflate": 0.25, "uv": [16, 32]}
                            ]
                        },
                        {
                            "name": "LeftLeg",
                            "parent": "root",
                            "pivot": [1.9, 12, 0],
                            "cubes": [
                                {"origin": [-0.1, 0, -2], "size": [4, 12, 4], "uv": [16, 48]}
                            ]
                        },
                        {
                            "name": "left_pants_leg",
                            "parent": "LeftLeg",
                            "pivot": [0, 0, 0],
                            "cubes": [
                                {"origin": [-0.1, 0, -2], "size": [4, 12, 4], "inflate": 0.25, "uv": [0, 48]}
                            ]
                        },
                        {
                            "name": "RightLeg",
                            "parent": "root",
                            "pivot": [-1.9, 12, 0],
                            "cubes": [
                                {"origin": [-3.9, 0, -2], "size": [4, 12, 4], "uv": [0, 16]}
                            ]
                        },
                        {
                            "name": "right_pants_leg",
                            "parent": "RightLeg",
                            "pivot": [0, 0, 0],
                            "cubes": [
                                {"origin": [-3.9, 0, -2], "size": [4, 12, 4], "inflate": 0.25, "uv": [0, 32]}
                            ]
                        },
                        {
                            "name": "LeftArm",
                            "parent": "root",
                            "pivot": [5, 22, 0],
                            "cubes": [
                                {"origin": [4, 12, -2], "size": [4, 12, 4], "uv": [32, 48]}
                            ]
                        },
                        {
                            "name": "left_sleeve",
                            "parent": "LeftArm",
                            "pivot": [0, 0, 0],
                            "cubes": [
                                {"origin": [4, 12, -2], "size": [4, 12, 4], "inflate": 0.25, "uv": [48, 48]}
                            ]
                        },
                        {
                            "name": "RightArm",
                            "parent": "root",
                            "pivot": [-5, 22, 0],
                            "cubes": [
                                {"origin": [-8, 12, -2], "size": [4, 12, 4], "uv": [40, 16]}
                            ]
                        },
                        {
                            "name": "right_sleeve",
                            "parent": "RightArm",
                            "pivot": [0, 0, 0],
                            "cubes": [
                                {"origin": [-8, 12, -2], "size": [4, 12, 4], "inflate": 0.25, "uv": [40, 32]}
                            ]
                        }
                    ]
                }
            ]
        }
        """.trimIndent()
    private val alexModelJson =
        """
        {
            "format_version": "1.12.0",
            "minecraft:geometry": [
                {
                    "description": {
                        "identifier": "geometry.alex",
                        "texture_width": 64,
                        "texture_height": 64,
                        "visible_bounds_width": 3,
                        "visible_bounds_height": 3.5,
                        "visible_bounds_offset": [0, 1.25, 0]
                    },
                    "bones": [
                        {
                            "name": "root",
                            "pivot": [0, 0, 0]
                        },
                        {
                            "name": "Head",
                            "parent": "root",
                            "pivot": [0, 24, 0],
                            "cubes": [
                                {"origin": [-4, 24, -4], "size": [8, 8, 8], "uv": [0, 0]}
                            ]
                        },
                        {
                            "name": "hat",
                            "parent": "Head",
                            "pivot": [0, 0, 0],
                            "cubes": [
                                {"origin": [-4, 24, -4], "size": [8, 8, 8], "inflate": 0.5, "uv": [32, 0]}
                            ]
                        },
                        {
                            "name": "Body",
                            "parent": "root",
                            "pivot": [0, 12, 0],
                            "cubes": [
                                {"origin": [-4, 12, -2], "size": [8, 12, 4], "uv": [16, 16]}
                            ]
                        },
                        {
                            "name": "jacket",
                            "parent": "Body",
                            "pivot": [0, 0, 0],
                            "cubes": [
                                {"origin": [-4, 12, -2], "size": [8, 12, 4], "inflate": 0.25, "uv": [16, 32]}
                            ]
                        },
                        {
                            "name": "LeftLeg",
                            "parent": "root",
                            "pivot": [1.9, 12, 0],
                            "cubes": [
                                {"origin": [-0.1, 0, -2], "size": [4, 12, 4], "uv": [16, 48]}
                            ]
                        },
                        {
                            "name": "left_pants_leg",
                            "parent": "LeftLeg",
                            "pivot": [0, 0, 0],
                            "cubes": [
                                {"origin": [-0.1, 0, -2], "size": [4, 12, 4], "inflate": 0.25, "uv": [0, 48]}
                            ]
                        },
                        {
                            "name": "RightLeg",
                            "parent": "root",
                            "pivot": [-1.9, 12, 0],
                            "cubes": [
                                {"origin": [-3.9, 0, -2], "size": [4, 12, 4], "uv": [0, 16]}
                            ]
                        },
                        {
                            "name": "right_pants_leg",
                            "parent": "RightLeg",
                            "pivot": [0, 0, 0],
                            "cubes": [
                                {"origin": [-3.9, 0, -2], "size": [4, 12, 4], "inflate": 0.25, "uv": [0, 32]}
                            ]
                        },
                        {
                            "name": "LeftArm",
                            "parent": "root",
                            "pivot": [5, 22, 0],
                            "cubes": [
                                {"origin": [4, 12, -2], "size": [3, 12, 4], "uv": [32, 48]}
                            ]
                        },
                        {
                            "name": "left_sleeve",
                            "parent": "LeftArm",
                            "pivot": [0, 0, 0],
                            "cubes": [
                                {"origin": [4, 12, -2], "size": [3, 12, 4], "inflate": 0.25, "uv": [48, 48]}
                            ]
                        },
                        {
                            "name": "RightArm",
                            "parent": "root",
                            "pivot": [-5, 22, 0],
                            "cubes": [
                                {"origin": [-7, 12, -2], "size": [3, 12, 4], "uv": [40, 16]}
                            ]
                        },
                        {
                            "name": "right_sleeve",
                            "parent": "RightArm",
                            "pivot": [0, 0, 0],
                            "cubes": [
                                {"origin": [-7, 12, -2], "size": [3, 12, 4], "inflate": 0.25, "uv": [40, 32]}
                            ]
                        }
                    ]
                }
            ]
        }
        """.trimIndent()
    private val animationJson = """
        {
            "format_version": "1.8.0",
            "animations": {
                "animation.player.idle": {
                    "loop": true,
                    "animation_length": 1,
                    "bones": {
                        "RightArm": {
                            "rotation": {
                                "0.0": [
                                    "(math.sin(query.life_time * 20 * 0.067 / math.pi * 180) * 0.05) / math.pi * 180",
                                    0,
                                    "(math.cos(query.life_time * 20 * 0.09 / math.pi * 180) * 0.05 + 0.05) / math.pi * 180"
                                 ]
                            }
                        },
                        "LeftArm": {
                            "rotation": {
                                "0.0": [
                                    "-(math.sin(query.life_time * 20 * 0.067 / math.pi * 180) * 0.05) / math.pi * 180",
                                    0,
                                    "-(math.cos(query.life_time * 20 * 0.09 / math.pi * 180) * 0.05 + 0.05) / math.pi * 180"
                                 ]
                            }
                        }
                    }
                }
            } ,
            "triggers": [
                {
                    "type": "IDLE",
                    "target": "ALL",
                    "probability": 1,
                    "name": "animation.player.idle",
                    "skips": 0,
                    "priority": 1,
                    "loops": 0
                }
            ]
        }
        """.trimIndent()

    private val steveModelFile: ModelFile = Json.decodeFromString(steveModelJson)
    private val alexModelFile: ModelFile = Json.decodeFromString(alexModelJson)

    private val animationFile: AnimationFile = Json.decodeFromString(animationJson)

    val steveBedrockModel = BedrockModel(cosmetic, "", steveModelFile, animationFile, emptyMap(), null, null, null, emptyMap())
    val alexBedrockModel = BedrockModel(cosmetic, "", alexModelFile, animationFile, emptyMap(), null, null, null, emptyMap())
}
