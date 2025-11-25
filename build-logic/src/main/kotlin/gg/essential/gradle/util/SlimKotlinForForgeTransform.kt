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
package gg.essential.gradle.util

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

abstract class SlimKotlinForForgeTransform : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    abstract val input: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = input.get().asFile
        val output = outputs.file(input.nameWithoutExtension + "-slim.jar")

        fun constantZipEntry(name: String) = ZipEntry(name).apply { time = CONSTANT_TIME_FOR_ZIP_ENTRIES }

        output.outputStream().use { fileOut ->
            ZipOutputStream(fileOut).use { zipOut ->
                ZipInputStream(input.inputStream()).use { zipIn ->
                    while (true) {
                        val entry = zipIn.nextEntry ?: break
                        if (!(entry.name.startsWith("kotlin/") || entry.name.startsWith("kotlinx/")
                                // Also need to remove Kotlin's service files, latest NeoForge versions will verify
                                // that the implementation classes exist and fail because we removed them
                                || entry.name.startsWith("META-INF/services/kotlin.")
                                // Also need to delete the coroutines version file, so that lib gets overwritten as well
                                || entry.name == "META-INF/kotlinx_coroutines_core.version")) {
                            zipOut.putNextEntry(entry)
                            zipIn.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    }
                }

                // Need to have at least one class in the `kotlin` package so loader correctly identifies this jar as KFF.
                zipOut.putNextEntry(constantZipEntry("kotlin/"))
                zipOut.closeEntry()
                zipOut.putNextEntry(constantZipEntry("kotlin/Unit.class"))
                zipOut.closeEntry()
            }
        }
    }
}
