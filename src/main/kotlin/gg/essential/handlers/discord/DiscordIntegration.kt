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
package gg.essential.handlers.discord

import dev.cbyrne.kdiscordipc.KDiscordIPC
import dev.cbyrne.kdiscordipc.core.error.ConnectionError
import dev.cbyrne.kdiscordipc.core.event.DiscordEvent
import dev.cbyrne.kdiscordipc.core.event.impl.ActivityJoinEvent
import dev.cbyrne.kdiscordipc.core.event.impl.ErrorEvent
import dev.cbyrne.kdiscordipc.core.event.impl.ReadyEvent
import dev.cbyrne.kdiscordipc.data.activity.activity
import dev.cbyrne.kdiscordipc.data.activity.largeImage
import dev.cbyrne.kdiscordipc.data.activity.party
import dev.cbyrne.kdiscordipc.data.activity.secrets
import dev.cbyrne.kdiscordipc.data.activity.smallImage
import gg.essential.Essential
import gg.essential.config.EssentialConfig
import gg.essential.data.VersionData
import gg.essential.event.client.PostInitializationEvent
import gg.essential.event.client.ReAuthEvent
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.handlers.discord.activity.ActivityState
import gg.essential.handlers.discord.activity.provider.ActivityStateProvider
import gg.essential.handlers.discord.activity.provider.impl.GameActivityStateProvider
import gg.essential.handlers.discord.activity.provider.impl.GuiActivityStateProvider
import gg.essential.handlers.discord.extensions.fullUsername
import gg.essential.handlers.discord.party.PartyInformation
import gg.essential.handlers.discord.party.PartyManager
import gg.essential.network.connectionmanager.skins.PlayerSkinLookup
import gg.essential.universal.UMinecraft
import gg.essential.util.ServerType
import gg.essential.util.USession
import gg.essential.util.kdiscordipc.KDiscordIPCLoader
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.kbrewster.eventbus.Subscribe
import java.util.*
import kotlin.concurrent.fixedRateTimer

/**
 * Integration with discord
 */
object DiscordIntegration {
    private const val CLIENT_ID = "894984875755597825"

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is ConnectionError.Disconnected) {
            return@CoroutineExceptionHandler
        }

        Essential.logger.error("Exception caught in Discord IPC: ", throwable)
    }

    /**
     * Used to validate people joining SPS sessions through Discord
     */
    var spsJoinKey = UUID.randomUUID().toString()
        private set

    /**
     * The join secret that we receive when joining a party
     */
    var hostJoinSecret: String? = null

    private val scope = CoroutineScope(Job() + Dispatchers.IO + exceptionHandler)
    private val ipcPort = System.getProperty("essential.discord.ipc.port")?.toIntOrNull() ?: 0
    private val stateProviders = listOf(
        GuiActivityStateProvider(),
        GameActivityStateProvider()
    )

    private val kdiscordipcLoader = KDiscordIPCLoader()
    private val ipc = KDiscordIPC(CLIENT_ID, kdiscordipcLoader::getPlatformSocket)

    val partyManager = PartyManager(scope)

    /**
     * The current activity state
     * When this property is set, the discord client is notified of an activity change
     */
    var state: ActivityState = ActivityState.GUI.MainMenu
        set(value) {
            if (field == value) {
                return
            }

            field = value
            scope.launch { publishActivityUpdate() }
        }

    /**
     * The current party information
     * When this property is set, the discord client is notified of an activity change
     */
    private var partyInformation: PartyInformation? = null
        set(value) {
            if (field == value) return

            field = value
            scope.launch { publishActivityUpdate() }
        }

    private val referenceHolder = ReferenceHolderImpl()

    @Subscribe
    private fun onPostInit(event: PostInitializationEvent) {
        scope.launch { initialize() }
        stateProviders.forEach(ActivityStateProvider::init)

        fixedRateTimer(
            name = "Essential Discord IPC Polling",
            daemon = true,
            period = 500
        ) {
            // Set activity
            stateProviders
                .firstNotNullOfOrNull { it.provide() }
                ?.let {
                    state = it
                }

            // Set party information
            // We also want to be safe at all times, and if for some reason there's an exception thrown, we don't
            // want to throw off everything else by not catching it.
            partyInformation = providePartyInformation()
        }

        with(EssentialConfig) {
            listOf(
                discordRichPresenceState,
                discordAllowAskToJoinState,
                discordShowUsernameAndAvatarState,
                discordShowCurrentServerState,
                memo { PlayerSkinLookup.getSkin(USession.active().uuid)()?.hash }
            )
        }.forEach { state ->
            state.onSetValue(referenceHolder) { _ ->
                scope.launch { publishActivityUpdate() }
            }
        }

        Essential.getInstance().shutdownHookUtil().register(this::disconnect)
    }

    /**
     * Fired when the user switches accounts, we should update our activity when this happens
     */
    @Subscribe
    private fun onReAuthentication(event: ReAuthEvent) {
        scope.launch { publishActivityUpdate() }
    }

    /**
     * Initializes the connection with the Discord client
     */
    private suspend fun initialize() {
        ipc.on<ReadyEvent> {
            Essential.logger.info("Connected to Discord as ${data.user.fullUsername}")

            ipc.subscribe(DiscordEvent.ActivityJoinRequest)
            ipc.subscribe(DiscordEvent.ActivityJoin)
            ipc.subscribe(DiscordEvent.ActivityInvite)
            ipc.subscribe(DiscordEvent.ActivitySpectate)

            ipc.on<ActivityJoinEvent> {
                try {
                    partyManager.joinParty(data.secret)
                    hostJoinSecret = data.secret
                } catch (e: Exception) {
                    Essential.logger.error("Failed to join party $data: $e", e)
                }
            }

            publishActivityUpdate()
        }

        // If an error occurs, we should disconnect from IPC and re-connect, as sometimes these errors are fatal
        // and can't be recovered from.
        ipc.on<ErrorEvent> {
            Essential.logger.error("An error occurred in the Discord Integration: ${data.message}")

            ipc.disconnect()
            connect()
        }

        connect()
    }

    private suspend fun connect() {
        try {
            ipc.connect(ipcPort)
        } catch (e: ConnectionError) {
            Essential.logger.debug("Failed to connect to Discord: ", e)
        }
    }

    private fun disconnect() {
        // We forcefully disconnect on shutdown as sometimes Discord likes to hang on to our activity, which we
        // don't want.
        try {
            ipc.disconnect()
        } catch (ignored: Exception) {
            // Let's ignore any exceptions caught here as it's usually because the socket is already disconnected.
        }
    }

    private suspend fun publishActivityUpdate() {
        if (!ipc.connected) {
            return
        }

        if (!EssentialConfig.discordRichPresence) {
            // If the user disabled the rich presence, we want to clear it so that Discord doesn't hold on to it
            ipc.activityManager.clearActivity()
            return
        }

        val activityState = state
        val version = VersionData.getMinecraftVersion()

        val activity = activity("Playing Minecraft $version", activityState.text) {
            val session = USession.activeNow()

            // Icon is a magic constant referencing an asset uploaded
            // via the Discord development portal
            largeImage("icon")

            if (EssentialConfig.discordShowUsernameAndAvatar) {
                val skin = PlayerSkinLookup.getSkin(session.uuid).getUntracked()
                if (skin != null) {
                    smallImage("https://crafthead.net/helm/${skin.hash.padStart(64, '0')}", session.username)
                }
            }

            if (EssentialConfig.discordAllowAskToJoin) {
                partyInformation?.let { info ->
                    info.joinSecret?.let { secrets(join = it) }
                    party(info.data.id, info.data.members, info.data.maximumMembers)
                }
            }
        }

        ipc.activityManager.setActivity(activity)
    }

    private fun providePartyInformation(): PartyInformation? =
        when (val state = ServerType.current()) {
            is ServerType.Multiplayer -> {
                if (EssentialConfig.discordAllowAskToJoin) {
                    PartyInformation(
                        "multiplayer|${state.address}",
                        // We use a combination of the server address and user UUID here because we don't want people
                        // to appear like they are playing 'together' on a server. It would cause quite a few issues...
                        PartyInformation.Data("${state.address}${USession.activeNow().uuid}", 1, 8)
                    )
                } else {
                    null
                }
            }

            is ServerType.SPS.Guest -> {
                val uuid = "${state.hostUuid}"

                val partyInfo = PartyInformation.Data(
                    uuid,
                    // Sometimes the playerInfoMap's size can be 0 when the server is first connecting, and discord
                    // doesn't like parties with 0 members.
                    Integer.max(UMinecraft.getMinecraft().connection?.playerInfoMap?.size ?: 1, 1),
                    8
                )

                PartyInformation(hostJoinSecret, partyInfo)
            }

            is ServerType.SPS.Host -> {
                val integratedServer = UMinecraft.getMinecraft().integratedServer
                val uuid = "${state.hostUuid}"

                val partyInfo = PartyInformation.Data(
                    uuid,
                    // Sometimes the currentPlayerCount or maxPlayers can be 0 when the server is first connecting, and
                    // discord doesn't like parties with 0 members.
                    Integer.max(integratedServer?.currentPlayerCount ?: 1, 1),
                    Integer.max(integratedServer?.maxPlayers ?: 8, 1)
                )

                PartyInformation("sps|$uuid|$spsJoinKey", partyInfo)
            }

            else -> null
        }

    fun getAddress(joinSecret: String): String? {
        val hostInformation = joinSecret.split("|")
        if (hostInformation.size != 3) {
            Essential.logger.error("Invalid SPS joinSecret: $joinSecret")
            return null
        }

        val key = hostInformation[2]
        if (key != this.spsJoinKey) {
            Essential.logger.error("Invalid SPS key: $key ($joinSecret)")

            return null
        }

        val spsManager = Essential.getInstance().connectionManager.spsManager
        return spsManager.localSession?.ip
    }

    fun regenerateSpsJoinKey() {
        spsJoinKey = UUID.randomUUID().toString()
        scope.launch { publishActivityUpdate() }
    }
}