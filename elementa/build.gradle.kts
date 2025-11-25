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
import essential.SyncToExternalRepoTask
import java.nio.file.Paths

tasks.create<SyncToExternalRepoTask>("syncCommitsToElementa") {
    targetRepoPath.set(Paths.get("${project.rootDir}/../Elementa"))
    targetDirectories.set(listOf(
        "unstable/statev2/src",
        "unstable/layoutdsl/src"
    ))
    sourceDirectories.set(listOf(
        "elementa/statev2/src",
        "elementa/layoutdsl/src"
    ))
    replacements.set(listOf(
        "elementa/statev2" to "unstable/statev2",
        "elementa/layoutdsl" to "unstable/layoutdsl",
        "gg/essential/gui/elementa" to "gg/essential/elementa/unstable",
        "gg.essential.gui.elementa" to "gg.essential.elementa.unstable",
        "gg/essential/gui" to "gg/essential/elementa/unstable",
        "gg.essential.gui" to "gg.essential.elementa.unstable",
        // remove accessed via reflection annotation
        "  @AccessedViaReflection(\"DelegatingStateBase\")" to "",
        "import gg.essential.config.AccessedViaReflection" to ""
    ))

}