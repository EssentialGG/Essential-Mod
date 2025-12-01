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
package gg.essential.util

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.div

val minecraftDirectory: File
    get() {
        return when (os) {
            OperatingSystem.WINDOWS -> File(System.getenv("APPDATA"), ".minecraft")
            OperatingSystem.MACOS -> File(
                System.getProperty("user.home"),
                "Library/Application Support/minecraft"
            )
            else -> File(System.getProperty("user.home"), ".minecraft")
        }
    }

val globalEssentialDirectory: Path
    get() {
        return when (os) {
            OperatingSystem.WINDOWS -> Paths.get(System.getenv("APPDATA"), "gg.essential.mod")
            OperatingSystem.MACOS -> Paths.get(
                System.getProperty("user.home"),
                "Library", "Application Support", "gg.essential.mod"
            )
            else -> {
                val xdgDataHome = System.getenv("XDG_DATA_HOME")?.let { Paths.get(it) }
                    ?: Paths.get(System.getProperty("user.home"), ".local", "share")

                xdgDataHome / "gg.essential.mod"
            }
        }
    }
