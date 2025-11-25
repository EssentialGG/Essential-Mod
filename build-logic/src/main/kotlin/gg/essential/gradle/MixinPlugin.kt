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

import gg.essential.gradle.multiversion.Platform
import essential.mixin
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

open class MixinPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val platform = project.extensions.getByType<Platform>()

        project.configureMixin(platform)
    }
}

private fun Project.configureMixin(platform: Platform) {
    // For versions which use mojmap at runtime, there is no need for refmap files and therefore the mixin AP
    val usesMojmapAtRuntime = platform.isNeoForge || (platform.isForge && platform.mcVersion >= 12006)
    if (usesMojmapAtRuntime) {
        tasks.named<ProcessResources>("processResources") {
            filesMatching("mixins.*.json") {
                filter { line -> if ("\"refmap\":" in line) "" else line }
            }
        }
        return
    }

    configureLoomMixin()

    if (!platform.isFabric) {
        addMixinDependency(platform)
    }
}

private fun Project.configureLoomMixin() {
    extensions.configure<LoomGradleExtensionAPI> {
        mixin {
            defaultRefmapName.set("mixins.essential.refmap.json")
        }
    }
}

private fun Project.addMixinDependency(platform: Platform) {
    repositories {
        mixin()
    }

    dependencies {
        if (platform.mcVersion < 11400) {
            // Our special mixin which has its Guava 21 dependency relocated, so it can run alongside Guava 17
            "implementation"(project(":mixin-compat"))
            // and outside dev, with extra patches for improved backwards compat (we cannot easily use those in dev
            // cause IntelliJ does not run any tasks during import)
            configurations.matching { it.name == "bundle" }.configureEach {
                "bundle"(project(":mixin-compat", "patched"))
            }
        }

        // Use more recent mixin AP so we get reproducible refmaps (and hopefully less bugs in general)
        if (!System.getProperty("idea.sync.active", "false").toBoolean()) {
            "annotationProcessor"("net.fabricmc:sponge-mixin:0.12.5+mixin.0.8.5")
        }
    }
}
