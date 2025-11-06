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
package gg.essential.gradle

import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import gg.essential.gradle.util.CONSTANT_TIME_FOR_ZIP_ENTRIES
import java.nio.file.Files
import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import org.gradle.api.Action
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

open class RelocatePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val relocateTask = project.createRelocateTask()
        project.createAbiValidationTasks(relocateTask)
    }
}

abstract class RelocateTask : ShadowJar() {
    @get:Internal
    internal val mappings = mutableMapOf<String, String>()

    override fun relocate(pattern: String, destination: String, configure: Action<SimpleRelocator>?): ShadowJar {
        mappings[pattern] = destination
        return super.relocate(pattern, destination, configure)
    }

    @TaskAction
    override fun copy() {
        super.copy()

        // The shadow plugin only remaps at most one class per string, so we need to manually remap extra ones in Mixin
        // target references because those may contain more than one Ice4J class reference.
        val remapper =
            object : Remapper() {
                override fun mapValue(value: Any?): Any {
                    if (value is String) {
                        return value.replace("Lorg/ice4j/", "Lgg/essential/lib/ice4j/")
                    }
                    return super.mapValue(value)
                }
            }

        val output = archiveFile.get().asFile.toPath()
        val tmp = Files.createTempFile(output.parent, "", ".jar")
        try {
            Files.move(output, tmp, StandardCopyOption.REPLACE_EXISTING)
            ZipOutputStream(Files.newOutputStream(output)).use { zipOut ->
                // Also need to sort all zip entries because diff-updates always produce sorted output and we need our
                // jars to exactly match the result for it to be accepted.
                ZipFile(tmp.toFile()).use { zipFile ->
                    for (inputEntry in zipFile.entries().toList().sortedBy { it.name }) {
                        val zipIn = zipFile.getInputStream(inputEntry)
                        val name = inputEntry.name

                        val outputEntry = ZipEntry(name)
                        outputEntry.time = CONSTANT_TIME_FOR_ZIP_ENTRIES
                        zipOut.putNextEntry(outputEntry)

                        if (name.startsWith("gg/essential/mixins/transformers/feature/ice/common") && name.endsWith(".class")) {
                            val reader = ClassReader(zipIn.readBytes())
                            val writer = ClassWriter(reader, 0)
                            reader.accept(ClassRemapper(writer, remapper), 0)
                            zipOut.write(writer.toByteArray())
                        } else {
                            zipIn.copyTo(zipOut)
                        }

                        zipOut.closeEntry()
                    }
                }
            }
        } finally {
            Files.deleteIfExists(tmp)
        }
    }
}

private fun Project.createRelocateTask(): TaskProvider<RelocateTask> {
    val relocatedJar by tasks.registering(RelocateTask::class) {
        val jarTask = tasks.getByName<Jar>("bundleJar")
        from(jarTask.archiveFile)
        manifest.inheritFrom(jarTask.manifest)
        mergeServiceFiles()
    }
    project.tasks.named("assemble") { dependsOn(relocatedJar) }
    return relocatedJar
}

private fun Project.createAbiValidationTasks(relocateTask: TaskProvider<RelocateTask>) {
    fun KotlinApiBuildTask.configureAbi() {
        // Should maybe move our impl into a dedicated package? (but not before `next` merge)
        ignoredClasses.addAll(listOf(
            "gg.essential.Essential",
            "gg.essential.DI",
        ))
        ignoredPackages.addAll(listOf(
            "gg.essential.asm",
            "gg.essential.clipboard",
            "gg.essential.commands",
            "gg.essential.compatibility",
            "gg.essential.config",
            "gg.essential.cosmetics",
            "gg.essential.data",
            "gg.essential.dev",
            "gg.essential.event",
            "gg.essential.gui",
            "gg.essential.handlers",
            "gg.essential.ice",
            "gg.essential.image",
            "gg.essential.key",
            "gg.essential.main",
            "gg.essential.minecraftauth",
            "gg.essential.mixins",
            "gg.essential.mod",
            "gg.essential.model",
            "gg.essential.network",
            "gg.essential.quic",
            "gg.essential.render",
            "gg.essential.serialization",
            "gg.essential.slf4j",
            "gg.essential.sps",
            "gg.essential.util",
            // Internally pre-relocated dependencies
            "gg.essential.lib.gson",
            // Internally pre-relocated dependencies (for mixin-compat)
            "gg.essential.lib.guava21",
            // Packages pulled in via connection-manager dependency
            "com.sparkuniverse.toolbox",
            "gg.essential.connectionmanager",
            "gg.essential.enums",
            "gg.essential.holder",
            "gg.essential.media",
            "gg.essential.notices",
            "gg.essential.serverdiscovery",
            "gg.essential.upnp",
            "gg.essential.forge",
        ))

        // These are our internal dependencies as declared in the relocatedJar task and as the name implies, we don't
        // care about their public API.
        for ((src, dst) in relocateTask.get().mappings) {
            ignoredPackages.addAll(src)
            ignoredPackages.addAll(dst)
        }
    }

    val devAbiBuild by tasks.registering(KotlinApiBuildTask::class) {
        configureAbi()
        val jarTask = tasks.getByName<Jar>("bundleJar")
        dependsOn(jarTask)
        inputClassesDirs = zipTree(jarTask.archiveFile)
        inputDependencies = zipTree(jarTask.archiveFile)
        outputApiFile.set(layout.buildDirectory.file("dev.api"))
    }

    val relocatedAbiBuild by tasks.registering(KotlinApiBuildTask::class) {
        configureAbi()
        val jarTask = relocateTask.get()
        dependsOn(jarTask)
        inputClassesDirs = zipTree(jarTask.archiveFile)
        inputDependencies = zipTree(jarTask.archiveFile)
        outputApiFile.set(layout.buildDirectory.file("relocated.api"))
    }

    val relocatedAbiCheck by tasks.registering(KotlinApiCompareTask::class) {
        dependsOn(devAbiBuild)
        dependsOn(relocatedAbiBuild)
        projectApiFile.set(devAbiBuild.get().outputApiFile)
        generatedApiFile.set(relocatedAbiBuild.get().outputApiFile)
    }
    tasks.named("check").configure {
        dependsOn(relocatedAbiCheck)
    }
}
