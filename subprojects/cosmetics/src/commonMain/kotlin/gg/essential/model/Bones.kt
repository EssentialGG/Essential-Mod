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

import gg.essential.model.ModelParser.Companion.ROOT_BONE_NAME

class Bones(bones: List<Bone>) : List<Bone> by bones {
    val byId: List<Bone> get() = this
    val byName: Map<String, Bone> = bones.associateBy { it.boxName }

    val root = byName.getValue(ROOT_BONE_NAME)
    val byPart = bones.mapNotNull { if (it.part != null) it.part to it else null }.toMap()

    operator fun get(name: String) = byName[name]
    operator fun contains(name: String) = name in byName

    constructor() : this(listOf(Bone(0, ROOT_BONE_NAME)))
}
