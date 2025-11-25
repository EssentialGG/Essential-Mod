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
package gg.essential.gui.sps.categories

import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.gui.common.EssentialDropDown
import gg.essential.gui.common.Spacer
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.collections.*
import gg.essential.gui.sps.options.SettingInformation
import gg.essential.gui.sps.options.SpsOption
import net.minecraft.client.resources.I18n
import net.minecraft.world.EnumDifficulty
import net.minecraft.world.GameType

//#if MC>=11600
//$$ import gg.essential.universal.UMinecraft
//#endif

class SPSSettingsCategory : WorldSettingsCategory(
    "General",
    "No settings found",
) {
    private val allGameModes = GameType.values().filter { it.id >= 0 }


    private val gameMode by SpsOption.createDropdownOption(
            SettingInformation.SettingWithOptionalTooltip("Game Mode"),
            getCurrentGameMode(),
            mutableListStateOf(*allGameModes.map { EssentialDropDown.Option(I18n.format("selectWorld.gameMode.${it.name.lowercase()}"), it) }.toTypedArray())
        ) { spsManager.updateWorldSettings(spsManager.isAllowCheats, it, spsManager.difficulty, spsManager.isDifficultyLocked) } childOf scroller

    // @formatter:off
    private fun getCurrentGameMode() =
        //#if MC<=11202
        spsManager.currentGameMode ?: world.worldInfo.gameType
        //#else
        //$$ spsManager.currentGameMode ?: UMinecraft.getMinecraft().playerController?.currentGameType ?: GameType.SURVIVAL
        //#endif

    // @formatter:on

    private val difficulty by SpsOption.createDropdownOption(
            SettingInformation.SettingWithOptionalTooltip("Difficulty"),
            spsManager.difficulty ?: world.difficulty,
            mutableListStateOf(*getDifficulties().map { EssentialDropDown.Option(it.value, it.key) }.toTypedArray())
        ) { spsManager.updateWorldSettings(spsManager.isAllowCheats, spsManager.currentGameMode, it, spsManager.isDifficultyLocked) }.apply {
        if (!world.worldInfo.isDifficultyLocked) {
            this childOf scroller
        }
    }

    // @formatter:off
    private fun getDifficulties() =
        //#if MC<=11202
        EnumDifficulty.values().associateWith { I18n.format(it.difficultyResourceKey) }
        //#else
        //$$ Difficulty.values().associateWith { I18n.format("options.difficulty.${it.name.lowercase()}") }
        //#endif

    // @formatter:on

    val cheatsEnabled = BasicState(spsManager.isAllowCheats).apply {
        onSetValue {
            spsManager.updateWorldSettings(it, spsManager.currentGameMode, spsManager.difficulty, spsManager.isDifficultyLocked)
        }
    }

    private val cheatsToggle by SpsOption.createToggleOption(
        SettingInformation.SettingWithOptionalTooltip("Cheats", "Allow the use of cheating commands"),
        cheatsEnabled,
    ) childOf scroller

    private val shareResourcePack by SpsOption.createToggleOption(
        SettingInformation.SettingWithOptionalTooltip(
            "Share Resource Pack",
            "Share your equipped Resource Pack"
        ),
        BasicState(spsManager.isShareResourcePack).apply {
            onSetValue {
                spsManager.isShareResourcePack = it
            }
        },
    ) childOf scroller

    private val spacer by Spacer(height = 10f) childOf scroller

    override fun sort() {
        // No sorting
    }
}
