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
package gg.essential.sps

import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import java.nio.file.Path
import java.util.*

// Note: This object is for use from the logical client side and will internally communicate with the server.
//       Unless explicitly noted otherwise, despite its name, it may not be accessed from the server thread.
interface IntegratedServerManager {
    val worldFolder: Path

    val serverPort: State<Int?>
    val thirdPartyVoicePort: State<Int?>

    /**
     * A coroutine scope which is cancelled when the server begins to shut down.
     * Coroutines are executed using [Client], not on the server thread!
     */
    val coroutineScope: CoroutineScope

    /**
     * A coroutine dispatcher which executes coroutines on the server main thread.
     * After server shutdown, it'll dispatch to [Dispatchers.IO] with parallelism limited to 1.
     */
    val serverDispatcher: CoroutineDispatcher

    /** Current server whitelist. May be `null` if no whitelist has been applied yet. */
    val whitelist: State<Set<UUID>?>

    /** UUID of every player connected (and logged in) to this server. Includes the host while they are connected. */
    val connectedPlayers: ListState<UUID>
    /** [connectedPlayers] excluding the host */
    val connectedGuests: ListState<UUID>

    /**
     * The JSON which the server will return as its server list status when pinged.
     * May be `null` if the server hasn't prepared a response yet.
     */
    val statusResponseJson: State<String?>

    fun setOpenToLanSource(source: State<Boolean>)
    fun setWhitelistSource(source: State<Set<UUID>>)
    fun setOpsSource(source: State<Set<UUID>>)
    fun setResourcePackSource(source: State<ServerResourcePack?>)
    fun setDifficultySource(source: MutableState<Difficulty>)
    fun setDifficultyLockedSource(source: MutableState<Boolean>)
    fun setDefaultGameModeSource(source: MutableState<GameMode>)
    fun setCheatsEnabledSource(source: State<Boolean>)

    data class ServerResourcePack(val url: String, val checksum: String)

    enum class Difficulty {
        Peaceful,
        Easy,
        Normal,
        Hard,
        ;
        companion object
    }
    enum class GameMode {
        Survival,
        Creative,
        Adventure,
        Spectator,
        ;
        companion object
    }
}
