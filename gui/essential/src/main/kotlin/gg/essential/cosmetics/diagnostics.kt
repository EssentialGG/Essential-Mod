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
package gg.essential.cosmetics

import gg.essential.cosmetics.events.AnimationEvent
import gg.essential.cosmetics.events.AnimationTarget
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.mod.Model
import gg.essential.mod.cosmetics.settings.CosmeticProperty
import gg.essential.model.BedrockModel
import gg.essential.model.file.AnimationFile
import gg.essential.model.file.ParticlesFile
import gg.essential.network.connectionmanager.cosmetics.AssetLoader
import gg.essential.network.connectionmanager.cosmetics.ModelLoader
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.network.cosmetics.Cosmetic.Diagnostic
import gg.essential.util.toState
import kotlinx.serialization.SerializationException
import java.util.concurrent.CompletionException

fun diagnose(modelLoader: ModelLoader, cosmetic: Cosmetic): State<List<Diagnostic>?> {
    val existingDiagnostics = cosmetic.diagnostics ?: listOf()

    if (cosmetic.type.id == "ERROR") {
        // metadata failed to load, can't do any further checks until that's fixed
        return stateOf(existingDiagnostics)
    }

    val generalDiagnostics = diagnoseCosmeticGenerally(cosmetic)

    val variants = cosmetic.variants?.map { it.name } ?: listOf("")
    val variantsAndSkins = variants.flatMap { variant ->
        val assets = cosmetic.assets(variant)
        listOfNotNull(
            VariantAndSkin(variant, Model.STEVE),
            if (assets.geometry.alex != null) VariantAndSkin(variant, Model.ALEX) else null,
        )
    }
    val variantsDiagnosticsState = variantsAndSkins.associateWith { variantAndSkin ->
        val (variant, skin) = variantAndSkin
        val modelFuture = modelLoader
            .getModel(cosmetic, variant, skin, AssetLoader.Priority.Background)
            .handle { v, t -> if (t == null) Result.success(v) else Result.failure((t as? CompletionException)?.cause ?: t) }
            .toState()

        val assetsFutures = modelLoader
            .getAssets(cosmetic, variant, skin, AssetLoader.Priority.Background)
            .associateWith { it.diagnostics.toState() }

        memo {
            modelFuture()?.fold({ model ->
                diagnoseModel(model)
            }, { throwable ->
                listOf(if (throwable is AssetLoader.ParseException) {
                    val checksum = throwable.asset.checksum
                    val file = cosmetic.assets(variant).allFiles.entries.find { it.value.checksum == checksum }?.key
                    val type = throwable.type.toString()
                    val cause = throwable.cause
                    if (cause is SerializationException) {
                        diagnoseParsingException(Diagnostic.Type.Fatal, cause, throwable.bytes.decodeToString())
                    } else {
                        val msg = "Failed to parse as $type"
                        Diagnostic.fatal(msg, stacktrace = throwable.cause?.stackTraceToString())
                    }.copy(file = file)
                } else {
                    val msg = "Unexpected error during loading of model"
                    Diagnostic.fatal(msg, stacktrace = throwable.stackTraceToString())
                })
            })?.plus(assetsFutures.flatMap { (asset, future) ->
                val checksum = asset.info.checksum
                val file = cosmetic.assets(variant).allFiles.entries.find { it.value.checksum == checksum }?.key
                val diagnostics = future() ?: return@memo null
                diagnostics.map { it.copy(file = file) }
            })
        }
    }

    return memo {
        val variantsDiagnostics = variantsDiagnosticsState.mapValues { it.value()?.toMutableList() ?: return@memo null }

        val diagnosticsMap = mutableMapOf<Diagnostic, MutableList<VariantAndSkin>>()
        for ((variantAndSkin, diagnostics) in variantsDiagnostics) {
            for (diagnostic in diagnostics) {
                diagnosticsMap.getOrPut(diagnostic.copy(variant = null, skin = null), ::mutableListOf).add(variantAndSkin)
            }
        }
        val diagnostics = diagnosticsMap.flatMap { (diagnostic, list) ->
            when {
                // Affects all variants and skins
                list.size == variantsAndSkins.size ->
                    listOf(diagnostic)
                // Affects a specific skin only (regardless of variant, i.e. all variants)
                list.all { it.skin == list.first().skin } && list.size == variants.size ->
                    listOf(diagnostic.copy(skin = list.first().skin))
                // Affects a specific variant only (regardless of skin, i.e. all skins)
                list.all { it.variant == list.first().variant } && list.size == Model.entries.size ->
                    listOf(diagnostic.copy(variant = list.first().variant))
                else -> list.map { diagnostic.copy(variant = it.variant, skin = it.skin) }
            }
        }

        existingDiagnostics + generalDiagnostics + diagnostics
    }
}

data class VariantAndSkin(val variant: String, val skin: Model)

private fun diagnoseCosmeticGenerally(cosmetic: Cosmetic): List<Diagnostic> {
    val diagnostics = mutableListOf<Diagnostic>()

    for (property in cosmetic.properties) {
        if (property is CosmeticProperty.UsesId) {
            @Suppress("DEPRECATION")
            val propertyId = property.id
            when (property) {
                is CosmeticProperty.IdShouldBeSelf -> if (propertyId != cosmetic.id) {
                    val msg = "Expected `id` of `${property.type}` to be `${cosmetic.id}` but was `${propertyId}`."
                    diagnostics.add(Diagnostic.error(msg, file = "settings.json"))
                }
                is CosmeticProperty.IdIsTarget -> {
                    // TODO could check if target exists, but we don't currently have an good way to get all cosmetics here
                }
                else -> throw AssertionError("Type ${property.javaClass} should implement one of the above interfaces.")
            }
        }
    }

    return diagnostics
}

private fun diagnoseModel(model: BedrockModel): List<Diagnostic> {
    val diagnostics = model.diagnostics.toMutableList()

    if (model.animationData != null && model.animationEvents.isEmpty()) {
        val msg = "No triggers found."
        diagnostics.add(Diagnostic.error(msg, file = "animations.json"))
    }

    for (trigger in model.animationEvents) {
        if (trigger.target != AnimationTarget.ALL) {
            val msg = "Trigger uses `${trigger.target}` target. Should probably be `ALL`."
            diagnostics.add(Diagnostic.error(msg, file = "animations.json"))
        }
    }

    ReferenceChecker(model, diagnostics).check()

    return diagnostics
}

private class ReferenceChecker(
    private val model: BedrockModel,
    private val diagnostics: MutableList<Diagnostic>,
) {
    private val referencedSounds = mutableSetOf<String>()
    private val referencedParticles = mutableSetOf<String>()
    private val referencedAnimations = mutableSetOf<String>()

    fun check() {
        for (trigger in model.animationData?.triggers ?: emptyList()) {
            visitTrigger(trigger)
        }

        checkForUnusedFiles()
        checkForUnusedData()
    }

    private fun checkForUnusedFiles() {
        val unusedFiles = model.cosmetic.assets(model.variant).otherFiles.keys.toMutableSet()
        for (file in model.particleData.keys) {
            unusedFiles.remove(file)
        }
        for (soundEffect in model.soundData?.definitions?.values ?: emptyList()) {
            for (sound in soundEffect.sounds) {
                unusedFiles.remove(sound.name + ".ogg")
            }
        }
        for (file in unusedFiles) {
            val msg = "File is unused."
            diagnostics.add(Diagnostic.warning(msg, file = file))
        }
    }

    private fun checkForUnusedData() {
        for (name in model.animationData?.animations?.keys ?: emptyList()) {
            if (name !in referencedAnimations) {
                val msg = "Animation `$name` is unused."
                diagnostics.add(Diagnostic.warning(msg, file = "animations.json"))
            }
        }

        for ((file, data) in model.particleData) {
            val name = data.particleEffect.description.identifier
            if (name !in referencedParticles) {
                val msg = "Particle effect `$name` is unused."
                diagnostics.add(Diagnostic.warning(msg, file = file))
            }
        }

        for (name in model.soundData?.definitions?.keys ?: emptyList()) {
            if (name !in referencedSounds) {
                val msg = "Sound effect `$name` is unused."
                diagnostics.add(Diagnostic.warning(msg, file = "sounds/sound_definitions.json"))
            }
        }
    }

    private fun visitBone(name: String, referringFile: String) {
        if (name !in model.bones) {
            val msg = "Referenced bone `$name` not found."
            diagnostics.add(Diagnostic.error(msg, file = referringFile))
        }
    }

    private fun visitSound(name: String, referringFile: String) {
        if (model.soundData?.definitions?.get(name) == null) {
            val msg = "Referenced sound effect `$name` not found."
            diagnostics.add(Diagnostic.error(msg, file = referringFile))
            return
        }

        referencedSounds.add(name)
    }

    private fun visitParticle(name: String, referringFile: String) {
        val entry = model.particleData.entries
            .find { it.value.particleEffect.description.identifier == name }
            ?.let { it.key to it.value.particleEffect }

        if (entry == null) {
            val msg = "Referenced particle effect `$name` not found."
            diagnostics.add(Diagnostic.error(msg, file = referringFile))
            return
        }

        val (sourceFile, particleEffect) = entry

        referencedParticles.add(name)

        fun visitEvent(event: ParticlesFile.Event) {
            event.sequence?.forEach { visitEvent(it) }
            event.randomize?.forEach { visitEvent(it.value) }
            event.particle?.let { options ->
                visitParticle(options.effect, sourceFile)
            }
            event.sound?.let { options ->
                visitSound(options.event, sourceFile)
            }
        }
        particleEffect.events.values.forEach(::visitEvent)
    }

    private fun visitAnimation(name: String, animation: AnimationFile.Animation) {
        referencedAnimations.add(name)

        val file = "animations.json"

        for (effects in animation.particleEffects.values) {
            for (effect in effects) {
                visitParticle(effect.effect, file)
                effect.locator?.let { visitBone(it, file) }
            }
        }
        for (effects in animation.soundEffects.values) {
            for (effect in effects) {
                visitSound(effect.effect, file)
                effect.locator?.let { visitBone(it, file) }
            }
        }

        for (bone in animation.bones.keys) {
            visitBone(bone, file)
        }
    }

    private fun visitTrigger(trigger: AnimationEvent) {
        val animation = model.animationData?.animations?.get(trigger.name)
        if (animation == null) {
            val msg = "Referenced animation `${trigger.name}` not found."
            diagnostics.add(Diagnostic.error(msg, file = "animations.json"))
        } else {
            visitAnimation(trigger.name, animation)
        }

        trigger.onComplete?.let { visitTrigger(it) }
    }
}

fun diagnoseParsingException(type: Diagnostic.Type, e: SerializationException, fileContent: String): Diagnostic {
    var msg = e.message ?: "<null>"

    // Parse line+column and strip that prefix from the message
    val lineColumn: Pair<Int, Int>?
    val offsetPrefix = "Unexpected JSON token at offset "
    if (msg.startsWith(offsetPrefix) && ':' in msg) {
        val (head, tail) = msg.split(':', limit = 2)
        msg = tail
        val offset = head.removePrefix(offsetPrefix).toInt()
        val precedingText = fileContent.substring(0 until offset)
        val line = precedingText.count { it == '\n' } + 1
        val column = precedingText.substringAfterLast('\n').length + 1
        lineColumn = Pair(line, column)
    } else {
        lineColumn = null
    }

    // Trim extra "hint" lines because those are meant for developers.
    // And the "JSON input:" lines because multiple lines in msg don't look good. One can always look at the full
    // stacktrace if one needs the details.
    if ('\n' in msg) {
        msg = msg.substringBefore('\n')
    }

    // Simplify common messages
    val unknownKeyMatch = Regex("Encountered an unknown key '([^`]+)' at path: ([^\n]+)").find(msg)
    if (unknownKeyMatch != null) {
        val (key, path) = unknownKeyMatch.destructured
        msg = "Unknown key `$key` at $path"
    }

    return Diagnostic(type, msg, stacktrace = e.stackTraceToString(), lineColumn = lineColumn)
}
