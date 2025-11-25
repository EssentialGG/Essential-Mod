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
import essential.*
import gg.essential.gradle.compatmixin.CompatMixinTask
import gg.essential.gradle.util.prebundle
import gg.essential.gradle.util.RelocationTransform.Companion.registerRelocationAttribute

plugins {
    id("com.gradleup.shadow")
    id("java-library")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

repositories {
    mavenCentral()
    mixin()
    minecraft()
}

val relocated = registerRelocationAttribute("essential-guava21-relocated") {
    relocate("com.google.common", "gg.essential.lib.guava21")
    relocate("com.google.thirdparty.publicsuffix", "gg.essential.lib.guava21.publicsuffix")
}

val fatMixin by configurations.creating {
    attributes { attribute(relocated, true) }
}

dependencies {
    fatMixin("org.spongepowered:mixin:0.8.4")
    // this is usually provided by MC but 1.8.9's is too old, so we need to bundle (and relocate) our own
    fatMixin("com.google.guava:guava:21.0")

    // Our special mixin which has its Guava 21 dependency relocated, so it can run alongside Guava 17
    api(prebundle(fatMixin))
    // Mixin needs at least asm 5.2 but older versions provide only 5.0.3
    api("org.ow2.asm:asm-debug-all:5.2")

    compileOnly("net.minecraft:launchwrapper:1.12")
}

val patchedJar by tasks.registering(CompatMixinTask::class) {
    mixinClasses.from(sourceSets.main.map { it.output })
    input.set(tasks.shadowJar.flatMap { it.archiveFile })
    output.set(buildDir.resolve("patched.jar"))
}

val patched by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}
artifacts.add(patched.name, patchedJar)
