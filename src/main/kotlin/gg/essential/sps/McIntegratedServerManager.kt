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

import com.mojang.authlib.GameProfile
import gg.essential.compat.PlasmoVoiceCompat
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.MutableListState
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.elementa.state.v2.filter
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableListStateOf
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.withSetter
import gg.essential.mixins.ext.server.coroutineScope
import gg.essential.mixins.ext.server.dispatcher
import gg.essential.mixins.transformers.server.integrated.LanConnectionsAccessor
import gg.essential.sps.IntegratedServerManager.Difficulty
import gg.essential.sps.IntegratedServerManager.GameMode
import gg.essential.sps.IntegratedServerManager.ServerResourcePack
import gg.essential.universal.wrappers.UPlayer
import gg.essential.util.Client
import gg.essential.util.ModLoaderUtil
import gg.essential.util.USession
import gg.essential.util.UuidNameLookup
import gg.essential.util.textTranslatable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.minecraft.client.resources.I18n
import net.minecraft.server.integrated.IntegratedServer
import net.minecraft.server.management.UserListWhitelistEntry
import java.nio.file.Path
import net.minecraft.world.EnumDifficulty as McDifficulty
import net.minecraft.world.GameType as McGameMode
import java.util.*

//#if MC>=12109
//$$ import net.minecraft.server.PlayerConfigEntry
//#endif

class McIntegratedServerManager(val server: IntegratedServer) : IntegratedServerManager {
    private val refHolder = ReferenceHolderImpl()

    override val worldFolder: Path =
        //#if MC>=11600
        //$$ server.func_240776_a_(net.minecraft.world.storage.FolderName.DOT)
        //#else
        server.dataDirectory.toPath().resolve("saves").resolve(server.folderName)
        //#endif

    override val serverPort: MutableState<Int?> = mutableStateOf(null)
    override val thirdPartyVoicePort: MutableState<Int?> = mutableStateOf(null)
    override val connectedPlayers: MutableListState<UUID> = mutableListStateOf()

    private val hostUuid = USession.activeNow().uuid
    override val connectedGuests: ListState<UUID>
        get() = connectedPlayers.filter { it != hostUuid }

    override val coroutineScope: CoroutineScope
        get() = server.coroutineScope + Dispatchers.Client

    override val serverDispatcher: CoroutineDispatcher
        get() = server.dispatcher

    private val mutableStatusResponseJson = mutableStateOf<String?>(null)
    override val statusResponseJson: State<String?> = mutableStatusResponseJson

    private val openToLanSourceState = mutableStateOf<State<Boolean>?>(null)
    private val whitelistSourceState = mutableStateOf<State<Set<UUID>>?>(null)
    private val opsSourceState = mutableStateOf<State<Set<UUID>>?>(null)
    private val resourcePackSourceState = mutableStateOf<State<ServerResourcePack?>?>(null)
    private val difficultySourceState = mutableStateOf<MutableState<Difficulty>?>(null)
    private val difficultyLockedSourceState = mutableStateOf<MutableState<Boolean>?>(null)
    private val defaultGameModeSourceState = mutableStateOf<MutableState<GameMode>?>(null)
    private val cheatsEnabledSourceState = mutableStateOf<State<Boolean>?>(null)
    private var openToLanUpdateJob: Job? = null
    private var whitelistUpdateJob: Job? = null
    private var opsUpdateJob: Job? = null

    //#if MC>=11900
    //$$ // For Mixin_IntegratedServerResourcePack only
    //$$ var appliedServerResourcePack: Optional<ServerResourcePack>? = null
    //#endif

    //#if MC>=11600
    //$$ // For Mixin_ControlAreCommandsAllowed only
    //$$ var appliedCheatsEnabled: Boolean? = null
    //#endif

    var appliedOpenToLan: Boolean = false

    init {
        effect(refHolder) {
            val openToLan = (openToLanSourceState() ?: return@effect)()

            // The vanilla IntegratedServer.openToLan method is quite thread unsafe, it does non-trivial
            // accesses to both client and server state, and vanilla can also call it from either the client
            // thread (via the Open To LAN button) or the server thread (via /publish command).
            // We'll schedule our task on the server thread but also make it block the client thread while it's
            // executing, so we won't have to worry about those thread unsafe calls. This does risk dead locking if at
            // the same time a client thread tries to run something on the server in a blocking way, but that seems
            // decently unlikely, and is better than some race-induced state corruption, so we'll take it.
            val prevJob = openToLanUpdateJob
            openToLanUpdateJob = server.coroutineScope.launch {
                // Keep updates serialized so we don't end up applying the wrong thing last.
                // We cannot just cancel the previous job because openToLan can only be called once, and if the previous
                // job has already called it, we need to allow it to finish.
                prevJob?.join()

                appliedOpenToLan = openToLan

                if (openToLan && !server.public) {
                    // IntegratedServer.openToLan assumes that the client is fully connected (mc.player is initialized).
                    // This may however not yet be the case here, e.g. it is possible to set this State immediately
                    // as soon as the server becomes available, while the client is still handshaking, so we need to
                    // wait until it's actually the case.
                    withContext(Dispatchers.Client) {
                        while (UPlayer.getPlayer() == null) {
                            delay(10)
                        }
                    }

                    // Intentionally blocking the server thread, see big comment block above
                    runBlocking(Dispatchers.Client) {
                        // We pass `false` for `allowCheats` to ensure that not everybody can enable commands.
                        // This option by default will allow anyone to use operator commands, without being explicitly
                        // added as operator.
                        // TODO we do not want to actually set the gamemode via this method because it has pretty bad
                        //  behavior, see the comment in the defaultGameMode effect
                        //#if MC>=11400
                        //$$ val port = net.minecraft.util.HTTPUtil.getSuitableLanPort()
                        //$$ if (!server.shareToLAN(GameMode.Adventure.toMc(), false, port)) {
                        //$$     return@runBlocking
                        //$$ }
                        //#else
                        @Suppress("USELESS_ELVIS") // Forge applies an inappropriate NonNullByDefault
                        val portStr: String = server.shareToLAN(GameMode.Adventure.toMc(), false) ?: return@runBlocking
                        val port = Integer.parseInt(portStr)
                        //#endif

                        // Simple Voice Chat documentation claims that by default it uses port 24454, but it seems they actually
                        // use the integrated server port by default. That's probably a good default as well
                        var voicePort = port

                        // Plasmo Voice has 2 major versions, 1.x (using the modid plasmo_voice) and 2.x (using the modid plasmovoice)
                        if (ModLoaderUtil.isModLoaded("plasmo_voice")) {
                            // Plasmo 1.x documentation claims that it uses the server port by default, but it seems
                            // that they actually use 60606 for the integrated server.
                            voicePort = 60606
                        } else if (ModLoaderUtil.isModLoaded("plasmovoice")) {
                            // Plasmo 2.x uses a random port, so we use their API to get the port.
                            val plasmoPort = PlasmoVoiceCompat.getPort()
                            if (plasmoPort.isPresent) {
                                voicePort = plasmoPort.get()
                            }
                        }

                        serverPort.set(port)
                        thirdPartyVoicePort.set(voicePort)
                    }
                }
            }
        }

        effect(refHolder) {
            val whitelist = (whitelistSourceState() ?: return@effect)()

            whitelistUpdateJob?.cancel()
            whitelistUpdateJob = server.coroutineScope.launch {
                applyWhitelist(whitelist)
                //#if MC>=12109
                //$$ server.useAllowlist = true
                //#else
                server.playerList.isWhiteListEnabled = true
                //#endif
            }
        }

        effect(refHolder) {
            val ops = (opsSourceState() ?: return@effect)()

            opsUpdateJob?.cancel()
            opsUpdateJob = server.coroutineScope.launch {
                applyOps(ops)
            }
        }

        effect(refHolder) {
            val resourcePack = (resourcePackSourceState() ?: return@effect)()

            server.coroutineScope.launch {
                //#if MC>=11900
                //$$ appliedServerResourcePack = Optional.ofNullable(resourcePack)
                //#else
                server.setResourcePack(resourcePack?.url ?: "", resourcePack?.checksum ?: "")
                //#endif
            }
        }

        effect(refHolder) {
            val difficulty = (difficultySourceState() ?: return@effect)()

            server.coroutineScope.launch {
                isDifficultyControlledByState = false
                //#if MC>=11600
                //$$ server.setDifficultyForAllWorlds(difficulty.toMc(), true)
                //#else
                server.setDifficultyForAllWorlds(difficulty.toMc())
                //#endif
                isDifficultyControlledByState = true
            }
        }

        effect(refHolder) {
            val difficultyLocked = (difficultyLockedSourceState() ?: return@effect)()

            server.coroutineScope.launch {
                isDifficultyLockedControlledByState = false
                //#if MC>=11600
                //$$ server.setDifficultyLocked(difficultyLocked )
                //#else
                server.worlds.filterNotNull().forEach { it.worldInfo.isDifficultyLocked = difficultyLocked }
                //#endif
                isDifficultyLockedControlledByState = true
            }
        }

        effect(refHolder) {
            val gameMode = (defaultGameModeSourceState() ?: return@effect)()

            server.coroutineScope.launch {
                isDefaultGameModeControlledByState = false
                // TODO this doesn't set the default game mode (at least on 1.12.2)
                //  it sets the gamemode which is applied to everyone who joins, regardless of whether they've joined
                //  or even changed their gamemode before
                //#if MC>=11700
                //$$ server.setDefaultGameMode(gameMode.toMc())
                //#elseif MC>=11600
                //$$ server.playerList.setGameType(gameMode.toMc())
                //#else
                server.playerList.setGameType(gameMode.toMc())
                //#endif
                isDefaultGameModeControlledByState = true
            }
        }

        effect(refHolder) {
            val cheatsEnabled = (cheatsEnabledSourceState() ?: return@effect)()

            server.coroutineScope.launch {
                //#if MC>=11600
                //$$ appliedCheatsEnabled = cheatsEnabled
                //#else
                server.worlds.firstOrNull()?.worldInfo?.setAllowCommands(cheatsEnabled);
                //#endif
            }
        }

        effect(refHolder) {
            return@effect
        }

        // TODO sync difficulty back when changed via vanilla menu or command
    }

    override fun setOpenToLanSource(source: State<Boolean>) = openToLanSourceState.set(source.memo())
    override fun setWhitelistSource(source: State<Set<UUID>>) = whitelistSourceState.set(source.memo())
    override fun setOpsSource(source: State<Set<UUID>>) = opsSourceState.set(source.memo())
    override fun setResourcePackSource(source: State<ServerResourcePack?>) = resourcePackSourceState.set(source.memo())
    override fun setDifficultySource(source: MutableState<Difficulty>) = difficultySourceState.set(source.memo().withSetter { source.set(it) })
    override fun setDifficultyLockedSource(source: MutableState<Boolean>) = difficultyLockedSourceState.set(source.memo().withSetter { source.set(it) })
    override fun setDefaultGameModeSource(source: MutableState<GameMode>) = defaultGameModeSourceState.set(source.memo().withSetter { source.set(it) })
    override fun setCheatsEnabledSource(source: State<Boolean>) = cheatsEnabledSourceState.set(source.memo())

    override val whitelist: State<Set<UUID>?> = State { whitelistSourceState()?.invoke() }

    private suspend fun applyWhitelist(desiredWhitelist: Set<UUID>) {
        val whitelist = server.playerList.whitelistedPlayers

        // Add new players to the whitelist
        for (uuid in desiredWhitelist) {
            val name = UuidNameLookup.getName(uuid).asDeferred().await()
            //#if MC>=12109
            //$$ val profile = PlayerConfigEntry(uuid, name)
            //#else
            val profile = GameProfile(uuid, name)
            //#endif
            @Suppress("SENSELESS_COMPARISON") // Forge applies an inappropriate NonNullByDefault
            if (whitelist.getEntry(profile) == null) {
                whitelist.addEntry(UserListWhitelistEntry(profile))
            }
        }

        // Remove undesired players from the whitelist
        for (userName in whitelist.keys) {
            val profile = server.findProfileForName(userName)
            if (profile != null && profile.id !in desiredWhitelist) {
                //#if MC>=12109
                //$$ whitelist.remove(PlayerConfigEntry(profile))
                //#else
                whitelist.removeEntry(profile)
                //#endif
            }
        }

        // Kick anyone who is not on the whitelist
        for (entity in (server.playerList as LanConnectionsAccessor).getPlayerEntityList()) {
            if (entity.uniqueID !in desiredWhitelist) {
                //#if MC>=11200
                entity.connection.disconnect(textTranslatable("multiplayer.disconnect.server_shutdown"))
                //#else
                //$$ entity.playerNetServerHandler.kickPlayerFromServer(
                //$$     I18n.format("multiplayer.disconnect.server_shutdown")
                //$$ )
                //#endif
            }
        }
    }

    private suspend fun applyOps(desiredOps: Set<UUID>) {
        val playerList = server.playerList
        val opList = playerList.oppedPlayers

        val allProfiles = opList.keys.mapNotNull { server.findProfileForName(it) }

        // Remove all players that are no longer op
        for (profile in allProfiles) {
            if (profile.id !in desiredOps) {
                //#if MC>=12109
                //$$ playerList.removeFromOperators(PlayerConfigEntry(profile))
                //#else
                playerList.removeOp(profile)
                //#endif
            }
        }

        // Op all new players
        for (uuid in desiredOps) {
            val name = UuidNameLookup.getName(uuid).asDeferred().await()
            //#if MC>=12109
            //$$ val profile = PlayerConfigEntry(uuid, name)
            //#else
            val profile = GameProfile(uuid, name)
            //#endif
            @Suppress("SENSELESS_COMPARISON") // Forge applies an inappropriate NonNullByDefault
            if (opList.getEntry(profile) == null) {
                playerList.addOp(profile)
            }
        }
    }

    private fun IntegratedServer.findProfileForName(name: String): GameProfile? {
        //#if MC>=12109
        //$$ return apiServices.nameToIdCache.findByName(name).map { GameProfile(it.id, it.name) }.orElse(null)
        //#else
        val userCache = playerProfileCache
            //#if MC>=12000
            //$$ ?: error("userCache should not be null") // it's only nullable for TestServer
            //#endif
        //#if MC>=11700
        //$$ return userCache.findByName(name).orElse(null)
        //#else
        return userCache.getGameProfileForUsername(name)
        //#endif
        //#endif
    }

    // NOTE: Called from server main thread!
    fun updateServerStatusResponse(statusJson: String) {
        coroutineScope.launch {
            mutableStatusResponseJson.set(statusJson)
        }
    }

    // These serve dual purpose:
    // 1. Once true, they prevent the server from changing its own state, instead calling the updateServerX methods
    //    below which delegate the change requests to the configured MutableState.
    // 2. When the server state is to be modified because our State changed, they are temporarily set to false so the
    //    redirection in point 1 is skipped and the actual server state is properly updated.
    var isDifficultyControlledByState: Boolean = false
    var isDifficultyLockedControlledByState: Boolean = false
    var isDefaultGameModeControlledByState: Boolean = false

}

fun Difficulty.toMc(): McDifficulty = McDifficulty.getDifficultyEnum(ordinal)
fun GameMode.toMc(): McGameMode = McGameMode.getByID(ordinal)
