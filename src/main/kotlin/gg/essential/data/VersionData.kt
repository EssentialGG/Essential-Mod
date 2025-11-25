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
package gg.essential.data

import gg.essential.Essential
import gg.essential.connectionmanager.common.packet.mod.ClientModsAnnouncePacket
import gg.essential.lib.gson.annotations.SerializedName
import java.io.File

//#if MC>=11400
//$$ import gg.essential.universal.UMinecraft
//$$ import net.minecraft.util.SharedConstants
//#endif

//#if FORGE
import net.minecraftforge.common.ForgeVersion

//#endif

object VersionData : VersionInfo() {
    private val versionFile = File(Essential.getInstance().baseDir, "version.json")
    private var state: State = State()

    private val platform by lazy {
        "${getMinecraftPlatform().name.lowercase()}_${getMinecraftVersion().replace(".", "-")}"
    }

    init {
        if (!versionFile.exists()) {
            saveData()
        } else {
            try {
                state = Essential.GSON.fromJson(versionFile.readText(), State::class.java)
            } catch (e: Exception) {
                Essential.logger.error("Failed to read from Version JSON, rewriting file.")
                saveData() // rewrite the config
            }
        }
    }

    fun getMajorComponents(version: String) = version.split(".", "+", "-", "_").take(3)

    fun getLastSeenModal() = state.lastSeenModal

    fun updateLastSeenModal() {
        if (getLastSeenModal() != essentialVersion) {
            state = state.copy(lastSeenModal = essentialVersion)
            saveData()
        }
    }

    // The previous version in which the user viewed the new changelog divider
    fun getLastSeenChangelog() = state.lastSeenChangelog

    fun updateLastSeenChangelog() {
        if (getLastSeenChangelog() != essentialVersion) {
            state = state.copy(lastSeenChangelog = essentialVersion)
            saveData()
        }
    }

    private fun saveData() = versionFile.writeText(Essential.GSON.toJson(state))

    fun getEssentialPlatform() = platform

    fun getMinecraftVersion(): String {
        //#if MC>=11400
        //$$ return SharedConstants.getVersion().id
        //#else
        return ForgeVersion::mcVersion.get()
        //#endif
    }

    fun getMinecraftPlatform(): ClientModsAnnouncePacket.Platform {
        //#if NEOFORGE
        //$$ return ClientModsAnnouncePacket.Platform.NEOFORGE
        //#elseif FORGE
        return ClientModsAnnouncePacket.Platform.FORGE
        //#elseif FABRIC
        //$$ return ClientModsAnnouncePacket.Platform.FABRIC
        //#endif
    }

    fun formatPlatform(unformatted: String): String {
        val split = unformatted.split("_")
        val platform =
            if (split.size > 1) {
                " [" + split[0].replaceFirstChar { it.uppercase() } + "]"
            } else {
                ""
            }
        val version = split.last().replace("-", ".")
        return "MC $version$platform"
    }

    private data class State(
        @SerializedName("version")
        val lastSeenModal: String = noSavedVersion,
        val lastSeenChangelog: String = essentialVersion,
    )
}
