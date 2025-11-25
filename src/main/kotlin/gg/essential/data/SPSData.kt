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
import gg.essential.util.USession
import gg.essential.util.getLevelNbtValue
import net.minecraft.world.EnumDifficulty
import net.minecraft.world.GameType
import net.minecraft.world.storage.WorldInfo
import net.minecraft.world.storage.WorldSummary
import java.nio.file.Path
import java.util.*
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

//#if MC>=12105
//$$ import kotlin.jvm.optionals.getOrNull
//#endif

//#if MC>11202
//$$ import net.minecraft.world.storage.IServerWorldInfo
//#endif

object SPSData {

    //#if MC>=11602
    //$$ fun getSPSSettings(worldFile: Path, worldSummary: WorldSummary? = null, worldInfo: IServerWorldInfo? = null): SPSSettings {
    //#else
    fun getSPSSettings(worldFile: Path, worldSummary: WorldSummary? = null, worldInfo: WorldInfo? = null): SPSSettings {
    //#endif
        worldSummary ?: worldInfo ?: throw IllegalArgumentException(
            "getSPSSettings must be called with either worldSummary or worldInfo; they cannot both be null."
        )

        val difficultyLocked = worldSummary?.getLevelNbtValue {
            //#if MC>=12105
            //$$ it.getCompound("Data").getOrNull()?.getBoolean("DifficultyLocked")?.getOrNull()
            //#else
            it.getCompoundTag("Data").getBoolean("DifficultyLocked")
            //#endif
        } ?: worldInfo?.isDifficultyLocked ?: false

        val file = (worldFile / "spsSettings.json")
        if (file.exists()) {
            try {
                val deserialized = Essential.GSON.fromJson(file.readText(), SPSSettings::class.java)
                val localUuid = USession.activeNow().uuid
                return deserialized.copy(
                    // Remove invites sent to the current player which were sent while signed into another account
                    invited = deserialized.invited.filter { it != localUuid }.toSet(),
                    difficultyLocked = difficultyLocked
                )
            } catch (exception: Exception) {
                Essential.logger.error("Failed to read SPS settings file.", exception)
            }
        }

        val spsManager = Essential.getInstance().connectionManager.spsManager
        return SPSSettings(
            worldSummary?.enumGameType ?: worldInfo!!.gameType,
            EnumDifficulty.getDifficultyEnum(worldSummary?.getLevelNbtValue {
                //#if MC>=12105
                //$$ it.getCompound("Data").getOrNull()?.getInt("Difficulty")?.getOrNull()
                //#else
                it.getCompoundTag("Data").getInteger("Difficulty")
                //#endif
            } ?: worldInfo?.difficulty?.difficultyId ?: EnumDifficulty.NORMAL.difficultyId),
            difficultyLocked,
            worldSummary?.cheatsEnabled ?: worldInfo!!.areCommandsAllowed(),
            spsManager.invitedUsers,
            spsManager.isShareResourcePack,
            spsManager.oppedPlayers,
        )
    }

    fun saveSPSSettings(settings: SPSSettings, worldFile: Path) {
        val file = (worldFile / "spsSettings.json")
        file.writeText(Essential.GSON.toJson(settings))
    }

    data class SPSSettings(
        val gameType: GameType = GameType.ADVENTURE,
        val difficulty: EnumDifficulty = EnumDifficulty.NORMAL,
        val difficultyLocked: Boolean = false,
        val cheats: Boolean = false,
        val invited: Set<UUID> = emptySet(),
        val shareResourcePack: Boolean = false,
        val oppedPlayers: Set<UUID> = mutableSetOf(),
    )

}