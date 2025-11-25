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

import gg.essential.config.AccessedViaReflection
import org.lwjgl.opengl.ARBDebugOutput.*
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.KHRDebug.*
import org.slf4j.LoggerFactory

//#if MC>=11600
//$$ import org.lwjgl.opengl.GLDebugMessageARBCallback
//$$ import org.lwjgl.opengl.GLDebugMessageCallback
//#else
import org.lwjgl.opengl.ARBDebugOutputCallback
import org.lwjgl.opengl.KHRDebugCallback
//#endif

object GlDebug {
    private val LOGGER = LoggerFactory.getLogger(GlDebug::class.java)

    @JvmField
    val ENABLED = System.getProperty("essential.gl_debug")?.toBoolean() ?: false
    @JvmField
    val WITH_STACKTRACE = System.getProperty("essential.gl_debug.stacktrace")?.toBoolean() ?: false

    private var expectError = false

    @JvmStatic
    fun setupDebugOutput() {
        //#if MC>=11600
        //$$ val caps = org.lwjgl.opengl.GL.getCapabilities()
        //#else
        val caps = org.lwjgl.opengl.GLContext.getCapabilities()
        //#endif
        if (caps.GL_KHR_debug) {
            GL11.glEnable(GL_DEBUG_OUTPUT)
            GL11.glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS)
            //#if MC>=11600
            //$$ glDebugMessageCallback(GLDebugMessageCallback.create(::onDebugOutput), 0)
            //#else
            glDebugMessageCallback(KHRDebugCallback(::onDebugOutput))
            //#endif
        } else if (caps.GL_ARB_debug_output) {
            GL11.glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS_ARB)
            //#if MC>=11600
            //$$ glDebugMessageCallbackARB(GLDebugMessageARBCallback.create(::onDebugOutput), 0)
            //#else
            glDebugMessageCallbackARB(ARBDebugOutputCallback(::onDebugOutput))
            //#endif
        } else {
            LOGGER.warn("No DEBUG_OUTPUT extension appears to be supported.")
            return
        }

        if (checkErrorLoggingFunctional()) {
            LOGGER.info("OpenGL DEBUG_OUTPUT enabled and functional.")
        } else {
            LOGGER.error("OpenGL DEBUG_OUTPUT might not be working as expected! Expected error but didn't see one.")
        }
    }

    private fun checkErrorLoggingFunctional(): Boolean {
        expectError = true
        GL11.glEnable(-1)
        return !expectError.also {
            expectError = false
            // prevent MC / Forge's loading screen from running into the error and crashing
            GL11.glGetError()
        }
    }

    //#if MC>=11600
    //$$ private fun onDebugOutput(source: Int, type: Int, id: Int, severity: Int, length: Int, message: Long, userParam: Long) =
    //$$     onDebugOutput(source, type, id, severity, GLDebugMessageCallback.getMessage(length, message))
    //#endif

    private fun onDebugOutput(source: Int, type: Int, id: Int, severity: Int, message: String) {
        if (expectError) {
            expectError = false
            return
        }
        @Suppress("UNUSED_VARIABLE") val unused = id // not really useful for debugging
        val str = "OpenGL reported ${typeToString(type)} in ${sourceToString(source)}: $message"
        val throwable = if (WITH_STACKTRACE) Throwable() else null
        @Suppress("DUPLICATE_LABEL_IN_WHEN") // ids seem to match between KHR and ARB
        when (severity) {
            GL_DEBUG_SEVERITY_HIGH, GL_DEBUG_SEVERITY_HIGH_ARB -> LOGGER.error(str, throwable)
            GL_DEBUG_SEVERITY_MEDIUM, GL_DEBUG_SEVERITY_MEDIUM_ARB -> LOGGER.warn(str, throwable)
            GL_DEBUG_SEVERITY_LOW, GL_DEBUG_SEVERITY_LOW_ARB -> LOGGER.info(str, throwable)
            GL_DEBUG_SEVERITY_NOTIFICATION -> LOGGER.debug(str, throwable)
            else -> LOGGER.error("(unknown severity $severity) $str", throwable)
        }
    }

    // See https://www.khronos.org/opengl/wiki/Debug_Output#Message_Components
    @Suppress("DUPLICATE_LABEL_IN_WHEN") // ids seem to match between KHR and ARB
    private fun sourceToString(id: Int) = when (id) {
        GL_DEBUG_SOURCE_API, GL_DEBUG_SOURCE_API_ARB -> "API"
        GL_DEBUG_SOURCE_WINDOW_SYSTEM, GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB -> "Window System"
        GL_DEBUG_SOURCE_SHADER_COMPILER, GL_DEBUG_SOURCE_SHADER_COMPILER_ARB -> "Shader Compiler"
        GL_DEBUG_SOURCE_THIRD_PARTY, GL_DEBUG_SOURCE_THIRD_PARTY_ARB -> "Third-party"
        GL_DEBUG_SOURCE_APPLICATION, GL_DEBUG_SOURCE_APPLICATION_ARB -> "Application"
        GL_DEBUG_SOURCE_OTHER, GL_DEBUG_SOURCE_OTHER_ARB -> "other"
        else -> "unknown source $id"
    }

    @Suppress("DUPLICATE_LABEL_IN_WHEN") // ids seem to match between KHR and ARB
    private fun typeToString(id: Int) = when (id) {
        GL_DEBUG_TYPE_ERROR, GL_DEBUG_TYPE_ERROR_ARB -> "error"
        GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR, GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB -> "deprecated behavior"
        GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR, GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB -> "undefined behavior"
        GL_DEBUG_TYPE_PERFORMANCE, GL_DEBUG_TYPE_PERFORMANCE_ARB -> "performance issue"
        GL_DEBUG_TYPE_PORTABILITY, GL_DEBUG_TYPE_PORTABILITY_ARB -> "portability issue"
        GL_DEBUG_TYPE_OTHER, GL_DEBUG_TYPE_OTHER_ARB -> "other issue"
        GL_DEBUG_TYPE_MARKER -> "marker"
        GL_DEBUG_TYPE_PUSH_GROUP -> "push group"
        GL_DEBUG_TYPE_POP_GROUP -> "pop group"
        else -> "unknown type $id"
    }

    @JvmField
    var inBeginEndPair = false

    @JvmStatic
    @AccessedViaReflection("GlErrorCheckingTransformer")
    fun checkGlError(methodName: String) {
        // only a very specific set of methods may be called between begin and end, and glGetError isn't one of them
        if (inBeginEndPair) return

        // Need to manually check if there is an OpenGL context active in the current thread,
        // otherwise LWJGL3 will error in native code and abort the JVM
        //#if MC>=11400
        //$$ try {
        //$$     @Suppress("SENSELESS_COMPARISON") // may return null instead of throwing when CHECKS are disabled
        //$$     if (org.lwjgl.opengl.GL.getCapabilities() == null) {
        //$$         return
        //$$     }
        //$$ } catch (e: IllegalStateException) {
        //$$     return
        //$$ }
        //#endif

        try {
            while (true) {
                val error = GL11.glGetError()
                if (error == GL11.GL_NO_ERROR) return

                val message = "${glErrorToString(error)} in $methodName"
                onDebugOutput(GL_DEBUG_SOURCE_API, GL_DEBUG_TYPE_ERROR, 0, GL_DEBUG_SEVERITY_HIGH, message)
            }
        } catch (e: RuntimeException) {
            // LWJGL2 will throw if there's no OpenGL context yet, see GLContext.getCapabilities
            if (e.message == "No OpenGL context found in the current thread.") return
            throw e
        }
    }

    // See https://www.khronos.org/opengl/wiki/GLAPI/glGetError
    private fun glErrorToString(id: Int) = when (id) {
        GL11.GL_INVALID_ENUM -> "GL_INVALID_ENUM"
        GL11.GL_INVALID_VALUE -> "GL_INVALID_VALUE"
        GL11.GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
        GL30.GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
        GL11.GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
        GL11.GL_STACK_UNDERFLOW -> "GL_STACK_UNDERFLOW"
        GL11.GL_STACK_OVERFLOW -> "GL_STACK_OVERFLOW"
        else -> "unknown error $id"
    }
}
