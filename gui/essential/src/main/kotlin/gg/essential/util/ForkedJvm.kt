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

import gg.essential.util.GuiEssentialPlatform.Companion.platform
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.LogManager
import java.io.Closeable
import java.nio.file.Paths
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.full.IllegalCallableAccessException

/**
 * Forks a separate JVM process and executes the main method of the given class.
 * This is particularly useful on 1.14+ where AWT cannot be used because it is incompatible with LWJGL3/GLFW.
 *
 * Some care must be taken when implementing the main method because only the jar file containing this class as well as
 * a select few others (e.g. Kotlin) will be on the classpath of the forked JVM.
 * Also note that therefore classes may be available in a dev env which will not be available in a production env.
 */
class ForkedJvm(main: String, classpath: String? = null, jvmArgs: List<String> = listOf()) : Closeable {
    val process: Process

    constructor(main: Class<*>) : this(main.name, defaultClassPath(main))

    init {
        val cmd = mutableListOf<String>()
        cmd += Paths.get(System.getProperty("java.home"))
            .resolve("bin")
            .resolve("java")
            .toAbsolutePath()
            .toString()
        cmd.addAll(jvmArgs)
        cmd += "-cp"
        cmd += classpath ?: defaultClassPath()
        cmd += main

        LOGGER.debug("Starting forked JVM: " + cmd.joinToString(separator = " "))

        process = ProcessBuilder(cmd).apply {
            redirectError(ProcessBuilder.Redirect.PIPE)
            redirectOutput(ProcessBuilder.Redirect.PIPE)
            redirectInput(ProcessBuilder.Redirect.PIPE)
        }.start()

        Dispatchers.IO.dispatch(EmptyCoroutineContext) {
            val logger = LogManager.getLogger(main)
            val reader = process.errorStream.bufferedReader()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.startsWith("[DEBUG] ")) {
                    logger.debug(line.substring("[DEBUG] ".length))
                } else {
                    logger.error(line)
                }
            }
        }
    }

    override fun close() {
        process.destroy()
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ForkedJvm::class.java)

        fun defaultClassPath(vararg extraClasses: Class<*>): String = listOf(
            ForkedJvm::class.java,
            Unit::class.java, // kotlin-stdlib
            Class.forName("kotlin.io.path.PathsKt"), // kotlin-stdlib-jdk7
            Class.forName("kotlin.collections.jdk8.CollectionsJDK8Kt"), // kotlin-stdlib-jdk8
            IllegalCallableAccessException::class.java, // kotlin-reflect
            CoroutineContext::class.java, // kotlin-coroutines
            Serializable::class.java, // kotlin-serialization-core
            Json::class.java, // kotlin-serialization-json
            *extraClasses,
        ).map { cls ->
            platform.findCodeSource(cls) ?: throw UnsupportedOperationException("Failed to find $cls jar location")
        }.also {
            if (it.first() is CodeSource.Directory) {
                return System.getProperty("java.class.path") // dev env, just use the system classpath
            }
        }.mapNotNull {
            when (it) {
                is CodeSource.Jar -> it.path.toAbsolutePath().toString()
                is CodeSource.Directory -> null
            }
        }.toSet().joinToString(System.getProperty("path.separator"))
    }
}