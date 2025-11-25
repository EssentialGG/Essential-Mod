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

import essential.universalLibs
import gg.essential.gradle.util.KotlinVersion
import gg.essential.gradle.util.setJvmDefault

plugins {
    kotlin("jvm")
    id("gg.essential.defaults")
}

universalLibs()

dependencies {
    implementation(kotlin("stdlib-jdk8", KotlinVersion.minimal.stdlib))
    implementation(project(":feature-flags"))

    testImplementation(kotlin("test"))
    testImplementation(libs.elementa)
}

tasks.test {
    useJUnitPlatform()
}

// We need to use the compatibility mode on old versions because we used to use the old Kotlin defaults for those
// And while this isn't currently part of our ABI, once stuff migrates to Elementa, it will be, so we consider it now.
tasks.compileKotlin.setJvmDefault("all-compatibility")

kotlin.jvmToolchain(8)

tasks.compileKotlin {
    kotlinOptions {
        moduleName = "essential" + project.path.replace(':', '-').lowercase()
    }
}