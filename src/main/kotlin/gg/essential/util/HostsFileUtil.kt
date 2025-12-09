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

import gg.essential.Essential
import kotlin.io.path.Path
import kotlin.io.path.readText

object HostsFileUtil {
    /**
     * This is a very loose check for modifications made by other launchers to the host file.
     * If any line contains `mojang`, we will assume they are trying to redirect authentication.
     */
    val containsRulesForMojangServers by lazy { readHostsFile()?.contains("mojang") ?: false }

    /**
     * Attempts to read the system's "hosts" file.
     * If the file does not exist, or could not be read, null will be returned.
     */
    private fun readHostsFile(): String? {
        val path = Path(if (os == OperatingSystem.WINDOWS) "C:\\Windows\\System32\\etc\\hosts" else "/etc/hosts")

        try {
            return path.readText()
        } catch (e: Exception) {
            Essential.logger.warn("Failed to read hosts file to check for modifications.", e)
            return null
        }
    }
}