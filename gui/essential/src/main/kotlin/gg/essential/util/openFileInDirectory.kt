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

import gg.essential.universal.UDesktop
import java.awt.Desktop
import java.awt.HeadlessException
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.nio.file.Path

fun openFileInDirectory(path: Path) {
    try {
        val declaredMethod = Desktop::class.java.getDeclaredMethod("browseFileDirectory", File::class.java)
        declaredMethod.invoke(Desktop.getDesktop(), path.toFile())
        Unit
    } catch (throwable: Throwable) {
        when (throwable) {
            is NoSuchMethodException, is InvocationTargetException, is HeadlessException -> {}
            else -> throw throwable
        }
        if (throwable is InvocationTargetException && throwable.cause !is UnsupportedOperationException) {
            throwable.printStackTrace()
        }
        fun command(vararg command: String): Boolean {
            return try {
                Runtime.getRuntime().exec(command).let {
                    it != null && it.isAlive
                }
            } catch (e: IOException) {
                false
            }
        }
        //On Windows and Mac we can implement browseFileDirectory on older java versions using commands
        //Adding quotes to the Windows command causes it to fail to work. The , after select is required
        if (!((UDesktop.isWindows && command("explorer.exe", "/select,", path.toAbsolutePath().toString()))
                    || (UDesktop.isMac && command("open", "-R", "${path.toAbsolutePath()}")))
        ) {
            UDesktop.open(path.toFile().parentFile)
        }
    }
}
