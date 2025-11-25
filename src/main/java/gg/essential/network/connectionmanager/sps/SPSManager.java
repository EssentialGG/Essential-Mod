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
package gg.essential.network.connectionmanager.sps;

import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.sparkuniverse.toolbox.util.DateTime;
import gg.essential.Essential;
import gg.essential.commands.EssentialCommandRegistry;
import gg.essential.compat.PlasmoVoiceCompat;
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket;
import gg.essential.connectionmanager.common.packet.upnp.*;
import gg.essential.data.SPSData;
import gg.essential.event.network.server.ServerLeaveEvent;
import gg.essential.event.network.server.ServerTickEvent;
import gg.essential.event.render.RenderTickEvent;
import gg.essential.event.sps.PlayerJoinSessionEvent;
import gg.essential.event.sps.PlayerLeaveSessionEvent;
import gg.essential.event.sps.SPSStartEvent;
import gg.essential.gui.elementa.state.v2.MutableState;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.gui.elementa.state.v2.StateKt;
import gg.essential.gui.friends.state.IStatusManager;
import gg.essential.gui.multiplayer.EssentialMultiplayerGui;
import gg.essential.mixins.transformers.server.integrated.LanConnectionsAccessor;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.NetworkedManager;
import gg.essential.network.connectionmanager.StateCallbackManager;
import gg.essential.network.connectionmanager.handler.upnp.ServerUPnPSessionInviteAddPacketHandler;
import gg.essential.network.connectionmanager.handler.upnp.ServerUPnPSessionPopulatePacketHandler;
import gg.essential.network.connectionmanager.handler.upnp.ServerUPnPSessionRemovePacketHandler;
import gg.essential.network.connectionmanager.queue.PacketQueue;
import gg.essential.network.connectionmanager.queue.SequentialPacketQueue;
import gg.essential.sps.ResourcePackSharingHttpServer;
import gg.essential.sps.TPSSessionMonitor;
import gg.essential.sps.WindowTitleManager;
import gg.essential.sps.SpsAddress;
import gg.essential.universal.UMinecraft;
import gg.essential.upnp.UPnPPrivacy;
import gg.essential.upnp.model.UPnPSession;
import gg.essential.util.*;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import me.kbrewster.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.UserListOps;
import net.minecraft.server.management.UserListWhitelist;
import net.minecraft.server.management.UserListWhitelistEntry;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//#if MC>=12109
//$$ import net.minecraft.server.PlayerConfigEntry;
//#endif

//#if MC>11202
//$$ import net.minecraft.world.World;
//$$ import net.minecraft.world.storage.FolderName;
//$$ import net.minecraft.world.storage.IServerWorldInfo;
//#endif

//#if MC>=11200
import static gg.essential.util.HelpersKt.textTranslatable;
//#else
//$$ import net.minecraft.client.resources.I18n;
//#endif

import static gg.essential.util.ExtensionsKt.getExecutor;

/**
 * SinglePlayer Sharing Manager
 */
public class SPSManager extends StateCallbackManager<IStatusManager> implements NetworkedManager {

    @NotNull
    private final ConnectionManager connectionManager;
    @NotNull
    private final PacketQueue updateQueue;
    @NotNull
    private final Object whitelistSemaphore = new Object();

    @NotNull
    private final Map<UUID, UPnPSession> remoteSessions = Maps.newConcurrentMap();

    @Nullable
    private UPnPSession localSession;
    @Nullable
    private SPSSessionSource localSessionSource;
    private GameType currentGameMode;
    private boolean allowCheats;
    private EnumDifficulty difficulty;
    private boolean difficultyLocked;
    @Nullable
    private String serverStatusResponse;

    private final Set<UUID> oppedPlayers = new HashSet<>();
    private final Map<UUID, MutableState<Boolean>> onlinePlayerStates = new HashMap<>();

    private boolean shareResourcePack = false;

    @Nullable
    private ResourcePackSharingHttpServer.PackInfo packInfo;
    private String resourcePackUrl = null; // Used by 1.19+ in Mixin_IntegratedServerResourcePack
    private String resourcePackChecksum = null;

    private Instant sessionStartTime = Instant.now();

    private TPSSessionMonitor tpsSessionMonitor = null;

    /**
     * A random ID for each session, used for telemetry purposes.
     */
    private UUID sessionId = null;

    /**
     * The maximum number of concurrent guests that connected during the session
     */
    private int maxConcurrentGuests = 0;

    public SPSManager(@NotNull final ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;

        this.updateQueue = new SequentialPacketQueue.Builder(connectionManager)
            .onTimeoutRetransmit()
            .create();

        connectionManager.registerPacketHandler(ServerUPnPSessionInviteAddPacket.class, new ServerUPnPSessionInviteAddPacketHandler());
        connectionManager.registerPacketHandler(ServerUPnPSessionPopulatePacket.class, new ServerUPnPSessionPopulatePacketHandler());
        connectionManager.registerPacketHandler(ServerUPnPSessionRemovePacket.class, new ServerUPnPSessionRemovePacketHandler());

        Runtime.getRuntime().addShutdownHook(new Thread(this::closeLocalSession)); // cleaning up UPnP if we can
    }

    public GameType getCurrentGameMode() {
        return currentGameMode;
    }

    public boolean isAllowCheats() {
        return allowCheats;
    }

    @Nullable
    public UPnPSession getRemoteSession(UUID hostUUID) {
        return this.remoteSessions.get(hostUUID);
    }

    @NotNull
    public Collection<UPnPSession> getRemoteSessions() {
        return Collections.unmodifiableCollection(this.remoteSessions.values());
    }

    public void addRemoteSession(@NotNull UPnPSession session) {
        final UPnPSession previousSession = this.remoteSessions.put(session.getHostUUID(), session);

        EssentialMultiplayerGui gui = EssentialMultiplayerGui.getInstance();
        if (gui != null) {
            gui.updateSpsSessions();
        }
        for (IStatusManager manager : getCallbacks()) {
            manager.refreshActivity(session.getHostUUID());
        }
    }

    public void removeRemoteSession(@NotNull UUID hostUUID) {
        this.remoteSessions.remove(hostUUID);
        for (IStatusManager manager : getCallbacks()) {
            manager.refreshActivity(hostUUID);
        }
        EssentialMultiplayerGui gui = EssentialMultiplayerGui.getInstance();
        if (gui != null) {
            gui.updateSpsSessions();
        }
    }

    @NotNull
    public Set<UUID> getInvitedUsers() {
        UPnPSession session = this.localSession;
        return session != null ? session.getInvites() : Collections.emptySet();
    }

    private void sendInvites(Set<UUID> invited) {
        if (invited.isEmpty()) {
            return;
        }

        this.updateQueue.enqueue(
            new ClientUPnPSessionInvitesAddPacket(invited),
            null
        );
    }

    private void revokeInvites(Set<UUID> removed) {
        if (removed.isEmpty()) {
            return;
        }

        this.updateQueue.enqueue(
            new ClientUPnPSessionInvitesRemovePacket(removed),
            null
        );
    }

    public synchronized void updateInvitedUsers(Set<UUID> invited) {
        if (this.localSession == null) {
            throw new IllegalStateException("Cannot update invites while no session is active.");
        }

        // Copy the set
        invited = new HashSet<>(invited);

        // Remove the client UUID, so we don't end up inviting ourselves
        invited.remove(UUIDUtil.getClientUUID());

        invited = Collections.unmodifiableSet(invited);

        revokeInvites(SetsKt.minus(this.localSession.getInvites(), invited));
        sendInvites(SetsKt.minus(invited, this.localSession.getInvites()));

        this.localSession = new UPnPSession(
            this.localSession.getHostUUID(),
            this.localSession.getIp(),
            this.localSession.getPort(),
            this.localSession.getPrivacy(),
            invited,
            this.localSession.getCreatedAt(),
            MinecraftUtils.getCurrentProtocolVersion(),
            MinecraftUtils.INSTANCE.getWorldName()
        );
        Multithreading.runAsync(this::refreshWhitelist);

        persistSettings();
    }

    public synchronized void reinviteUsers(Set<UUID> users) {
        if (this.localSession == null) {
            throw new IllegalStateException("Cannot update invites while no session is active.");
        }

        // Only revoke invites for users that aren't already in the session, otherwise they'd get kicked
        Set<UUID> offlineUsers = users.stream()
            .filter(uuid -> getInvitedUsers().contains(uuid) && !getOnlineState(uuid).getUntracked())
            .collect(Collectors.toSet());
        // No need to refresh the whitelist and persist settings when revoking since we'll immediately reinvite the users
        revokeInvites(offlineUsers);

        updateInvitedUsers(SetsKt.plus(this.localSession.getInvites(), users));
    }

    @Nullable
    public UPnPSession getLocalSession() {
        return this.localSession;
    }

    public EnumDifficulty getDifficulty() {
        return difficulty;
    }

    public boolean isDifficultyLocked() {
        return difficultyLocked;
    }

    public Instant getSessionStartTime() {
        return sessionStartTime;
    }

    public void startLocalSession(SPSSessionSource sessionSource) {
        sessionStartTime = Instant.now();
        sessionId = UUID.randomUUID();
        currentGameMode = GameType.ADVENTURE; // This is just a dummy value that will be updated later.
        this.localSessionSource = sessionSource;
        this.maxConcurrentGuests = 0;

        Multithreading.runAsync(ResourcePackSharingHttpServer.INSTANCE::startServer); // Load the class to start it
        updateResourcePack(packInfo); // Applies the current pack to the integrated server

        IntegratedServer server = UMinecraft.getMinecraft().getIntegratedServer();
        if (server == null) {
            return;
        }

        //#if MC>=11602
        //$$ World world = server.getWorld(World.OVERWORLD);
        //#else
        World world = server.getWorld(0);
        //#endif

        //#if MC>=11602
        //$$ IServerWorldInfo worldInfo = (IServerWorldInfo) world.getWorldInfo();
        //#else
        WorldInfo worldInfo = world.getWorldInfo();
        //#endif

        this.allowCheats = worldInfo.areCommandsAllowed();
        this.difficulty = worldInfo.getDifficulty();
        this.difficultyLocked = worldInfo.isDifficultyLocked();

        //#if MC>=12109
        //$$ server.setUseAllowlist(true);
        //#else
        server.getPlayerList().setWhiteListEnabled(true);
        //#endif

        updateOppedPlayers(new HashSet<>(), false);

        // We pass `false` for `allowCheats` to ensure that not everybody can enable commands.
        // This option by default will allow anyone to use operator commands, without being explicitly
        // added as operator.
        //#if MC>=11400
        //$$ int port = net.minecraft.util.HTTPUtil.getSuitableLanPort();
        //$$ if (!server.shareToLAN(currentGameMode, false, port)) {
        //$$     return;
        //$$ }
        //#else
        String portStr = server.shareToLAN(currentGameMode, false);
        // Method inappropriately marked as non-null by Forge
        //noinspection ConstantConditions
        if (portStr == null) {
            return;
        }
        int port = Integer.parseInt(portStr);
        //#endif

        {
            // Simple Voice Chat documentation claims that by default it uses port 24454, but it seems they actually
            // use the integrated server port by default. That's probably a good default as well
            int voicePort = port;

            // Plasmo Voice has 2 major versions, 1.x (using the modid plasmo_voice) and 2.x (using the modid plasmovoice)
            if (ModLoaderUtil.INSTANCE.isModLoaded("plasmo_voice")) {
                // Plasmo 1.x documentation claims that it uses the server port by default, but it seems
                // that they actually use 60606 for the integrated server.
                voicePort = 60606;
            } else if (ModLoaderUtil.INSTANCE.isModLoaded("plasmovoice")) {
                // Plasmo 2.x uses a random port, so we use their API to get the port.
                Optional<Integer> plasmoPort = PlasmoVoiceCompat.getPort();
                if (plasmoPort.isPresent()) {
                    voicePort = plasmoPort.get();
                }
            }
            connectionManager.getIceManager().setVoicePort(voicePort);
        }

        String address = new SpsAddress(UUIDUtil.getClientUUID()).toString();

        this.updateLocalSession(address, 0);

        Essential.EVENT_BUS.post(new SPSStartEvent(address));
        EssentialCommandRegistry.INSTANCE.registerSPSHostCommands();

        WindowTitleManager.INSTANCE.updateTitle();

        tpsSessionMonitor = new TPSSessionMonitor();
    }

    public synchronized void updateLocalSession(@NotNull String ip, int port) {
        // Currently all sessions are locked into invite only
        UPnPPrivacy privacy = UPnPPrivacy.INVITE_ONLY;

        int protocolVersion = MinecraftUtils.getCurrentProtocolVersion();
        String worldName = MinecraftUtils.INSTANCE.getWorldName();

        UPnPSession oldSession = this.localSession;
        UPnPSession session = new UPnPSession(
            UUIDUtil.getClientUUID(),
            ip,
            port,
            privacy,
            oldSession != null ? oldSession.getInvites() : Collections.emptySet(),
            oldSession != null ? oldSession.getCreatedAt() : new DateTime(),
            protocolVersion,
            worldName
        );

        if (this.localSession == null) {
            this.updateQueue.enqueue(new ClientUPnPSessionCreatePacket(ip, port, privacy, protocolVersion, worldName));
        } else {
            this.updateQueue.enqueue(new ClientUPnPSessionUpdatePacket(ip, port, privacy));
        }

        this.localSession = session;
        Multithreading.runAsync(this::refreshWhitelist);
    }

    public synchronized void closeLocalSession() {

        IntegratedServer server = Minecraft.getMinecraft().getIntegratedServer();
        UPnPSession oldSession = this.localSession;
        if (oldSession != null) {
            if (server != null) {
                sendSessionTelemetry(server, oldSession);
            }

            // Remove all invites without persisting the settings, so they will be re-added when a new session is created
            revokeInvites(oldSession.getInvites());
        }
        currentGameMode = null;
        allowCheats = false;
        oppedPlayers.clear();
        onlinePlayerStates.clear();

        this.localSession = null;
        this.localSessionSource = null;

        ResourcePackSharingHttpServer.INSTANCE.stopServer();

        resourcePackUrl = null;
        resourcePackChecksum = null;
        shareResourcePack = false;
        tpsSessionMonitor = null;

        ExtensionsKt.getExecutor(Minecraft.getMinecraft()).execute(EssentialCommandRegistry.INSTANCE::unregisterSPSHostCommands);

        this.updateQueue.enqueue(new ClientUPnPSessionClosePacket());

        ExtensionsKt.getExecutor(Minecraft.getMinecraft()).execute(WindowTitleManager.INSTANCE::updateTitle);
    }

    @Subscribe
    private void checkIfClosedByThirdParty(RenderTickEvent event) {
        if (localSession == null) return;

        IntegratedServer server = Minecraft.getMinecraft().getIntegratedServer();
        if (server == null || !server.getPublic()) {
            closeLocalSession();
        }
    }

    private static String cpuInfo() {
        //#if MC>=12105
        //$$ try {
        //$$     oshi.hardware.CentralProcessor processor = new oshi.SystemInfo().getHardware().getProcessor();
        //$$     (processor.getLogicalProcessorCount() + "x " + processor.getProcessorIdentifier().getName()).replaceAll("\\s+", " ");
        //$$     return "${}x ${processor.processorIdentifier.name}";
        //$$ } catch (Throwable e) {
        //$$     return "<unknown>";
        //$$ }
        //#elseif MC>=11600
        //$$ return com.mojang.blaze3d.platform.PlatformDescriptors.getCpuInfo();
        //#else
        return net.minecraft.client.renderer.OpenGlHelper.getCpu();
        //#endif
    }

    private void sendSessionTelemetry(IntegratedServer server, UPnPSession oldSession) {
        File worldDirectory = ExtensionsKt.getWorldDirectory(server).toFile();

        float averageTPS;
        float minTPS;
        float maxTPS;
        if (tpsSessionMonitor != null) {
            averageTPS = tpsSessionMonitor.getAverageTPS();
            minTPS = tpsSessionMonitor.getMinTPS();
            maxTPS = tpsSessionMonitor.getMaxTPS();
        } else {
            averageTPS = 0f;
            minTPS = 0f;
            maxTPS = 0f;
        }
        HashMap<String, Object> metadata = new HashMap<String, Object>() {{
            put("userCPU", cpuInfo());
            put("worldNameHash", DigestUtils.sha256Hex(UUIDUtil.getClientUUID() + worldDirectory.getName()));
            put("inviteCount", oldSession.getInvites().size());
            put("shareRP", shareResourcePack);
            put("maxConcurrentGuests", maxConcurrentGuests);
            put("allocatedMemoryMb", Runtime.getRuntime().maxMemory() / 1_000_000);
            put("sessionDurationSeconds", TimeUnit.MILLISECONDS.toSeconds(Duration.between(sessionStartTime, Instant.now()).toMillis()));
            put("sessionId", sessionId);
            put("initiatedFrom", localSessionSource);
            put("averageTPS", averageTPS);
            put("minTPS", minTPS);
            put("maxTPS", maxTPS);
        }};

        // Fork so calculating the world size doesn't block the main thread
        Multithreading.runAsync(() -> {

            long worldSizeBytes = FileUtils.sizeOfDirectory(worldDirectory);
            metadata.put("worldSizeMb", worldSizeBytes / 1_000_000);

            // Return to main thread because enqueue is not thread safe
            ExtensionsKt.getExecutor(Minecraft.getMinecraft()).execute(() -> connectionManager.getTelemetryManager().enqueue(new ClientTelemetryPacket("SPS_SESSION_4", metadata)));
        });
    }

    // Called from server main thread
    public void updateServerStatusResponse(@NotNull String updatedResponse) {
        if (this.localSession == null) {
            return;
        }

        if (updatedResponse.equals(this.serverStatusResponse)) {
            return;
        }
        this.serverStatusResponse = updatedResponse;

        ExtensionsKt.getExecutor(Minecraft.getMinecraft()).execute(() ->
            this.updateQueue.enqueue(new ClientUPnPSessionPingProxyUpdatePacket(updatedResponse))
        );
    }

    public void refreshWhitelist() {
        // There must only be one call to doRefreshWhitelist active at any time so we do not get any races between the
        // point where they retrieve the invited users and where they apply them (or rather, where they enter the server
        // task queue).
        synchronized (this.whitelistSemaphore) {
            this.doRefreshWhitelist();
        }
    }

    private void doRefreshWhitelist() {
        UPnPSession session = this.localSession;
        if (session == null) {
            return;
        }

        Set<UUID> invited;
        if (session.getPrivacy() == UPnPPrivacy.INVITE_ONLY) {
            invited = session.getInvites();
        } else /* session.getPrivacy() == UPnPPrivacy.FRIENDS */ {
            invited = new HashSet<>(this.connectionManager.getRelationshipManager().getFriends().keySet());
        }

        // Cache all user names so we do not unnecessarily block the server thread
        CollectionsKt.map(invited, UUIDUtil::getName).forEach(CompletableFuture::join);

        IntegratedServer server = UMinecraft.getMinecraft().getIntegratedServer();
        if (server == null) {
            return;
        }

        // Sync server whitelist with our list
        getExecutor(server).execute(() -> {
            UserListWhitelist whitelist = server.getPlayerList().getWhitelistedPlayers();
            for (String userName : whitelist.getKeys()) {
                //#if MC>=12109
                //$$ GameProfile profile = server.getApiServices().nameToIdCache().findByName(userName).map(it -> new GameProfile(it.id(), it.name())).orElse(null);
                //#elseif MC>=11701
                //$$ GameProfile profile = server.getUserCache().findByName(userName).orElse(null);
                //#else
                GameProfile profile = server.getPlayerProfileCache().getGameProfileForUsername(userName);
                //#endif
                if (profile != null && !invited.contains(profile.getId())) {
                    //#if MC>=12109
                    //$$ whitelist.remove(new PlayerConfigEntry(profile));
                    //#else
                    whitelist.removeEntry(profile);
                    //#endif
                }
            }
            for (UUID uuid : invited) {
                String userName = UUIDUtil.getName(uuid).join();
                //#if MC>=12109
                //$$ PlayerConfigEntry profile = new PlayerConfigEntry(uuid, userName);
                //#else
                GameProfile profile = new GameProfile(uuid, userName);
                //#endif
                //noinspection ConstantConditions forge is stupid
                if (whitelist.getEntry(profile) == null) {
                    whitelist.addEntry(new UserListWhitelistEntry(profile));
                }
            }

            // Kick anyone who is not on the whitelist
            for (EntityPlayerMP entity : ((LanConnectionsAccessor) server.getPlayerList()).getPlayerEntityList()) {
                if (!invited.contains(entity.getUniqueID()) && !UUIDUtil.getClientUUID().equals(entity.getUniqueID())) {
                    //#if MC>11200
                    entity.connection.disconnect(textTranslatable("multiplayer.disconnect.server_shutdown"));
                    //#else
                    //$$ entity.playerNetServerHandler.kickPlayerFromServer(
                    //$$     I18n.format("multiplayer.disconnect.server_shutdown")
                    //$$ );
                    //#endif
                }
            }
        });
    }

    @Subscribe
    private void onDisconnect(ServerLeaveEvent event) {
        closeLocalSession();
    }

    @Override
    public synchronized void onConnected() {
        this.updateQueue.reset();

        UPnPSession session = this.localSession;
        if (session != null) {
            this.updateQueue.enqueue(new ClientUPnPSessionCreatePacket(
                session.getIp(),
                session.getPort(),
                session.getPrivacy(),
                session.getProtocolVersion(),
                session.getWorldName()
            ));
            this.updateQueue.enqueue(new ClientUPnPSessionInvitesAddPacket(session.getInvites()));
            String serverStatusResponse = this.serverStatusResponse;
            if (serverStatusResponse != null) {
                this.updateQueue.enqueue(new ClientUPnPSessionPingProxyUpdatePacket(serverStatusResponse));
            }
        }

        resetState();
    }

    @Override
    public void resetState() {
        this.remoteSessions.clear();
    }

    public void updateWorldSettings(boolean cheats, @NotNull GameType gameType, @NotNull EnumDifficulty difficulty, boolean difficultyLocked) {
        final IntegratedServer integratedServer = UMinecraft.getMinecraft().getIntegratedServer();
        if (integratedServer != null) {
            getExecutor(integratedServer).execute(() -> {
                //#if MC<=11202
                integratedServer.getPlayerList().setGameType(gameType);
                //#elseif MC<=11602
                //$$ integratedServer.getPlayerList().setGameType(gameType);
                //#else
                //$$  integratedServer.setDefaultGameMode(gameType);
                //#endif
                updateCheatSettings(integratedServer, cheats);
                //#if MC>=11602
                //$$ integratedServer.setDifficultyForAllWorlds(difficulty, true);
                //#else
                integratedServer.setDifficultyForAllWorlds(difficulty);
                //#endif
            });
        }
        if (UMinecraft.getWorld() != null && !UMinecraft.getWorld().getWorldInfo().isDifficultyLocked()) {
            UMinecraft.getWorld().getWorldInfo().setDifficulty(difficulty);
        }
        this.allowCheats = cheats;
        this.currentGameMode = gameType;
        this.difficulty = difficulty;
        this.difficultyLocked = difficultyLocked;

        persistSettings();
    }

    private void persistSettings() {
        IntegratedServer integratedServer = UMinecraft.getMinecraft().getIntegratedServer();
        if (integratedServer != null) {
            SPSData.SPSSettings spsSettings = new SPSData.SPSSettings(
                    this.currentGameMode,
                    this.difficulty,
                    this.difficultyLocked,
                    this.allowCheats,
                    this.getInvitedUsers(),
                    this.shareResourcePack,
                    this.oppedPlayers
            );
            SPSData.INSTANCE.saveSPSSettings(spsSettings, ExtensionsKt.getWorldDirectory(integratedServer));
        }
    }

    public void updateWorldGameRules(GameRules gameRules, Map<String, String> gameRuleSettings) {
        HashMap<String, String> immutableGameRules = new HashMap<>(gameRuleSettings);
        final IntegratedServer integratedServer = UMinecraft.getMinecraft().getIntegratedServer();
        if (integratedServer != null) {
            getExecutor(integratedServer).execute(() -> {
                //#if MC<=11202
                immutableGameRules.forEach(gameRules::setOrCreateGameRule);
                //#else
                //#if MC>=12102
                //$$ integratedServer.getGameRules().accept(new GameRules.Visitor() {
                //#else
                //$$ GameRules.visitAll(new GameRules.IRuleEntryVisitor() {
                //#endif
                //$$     @Override
                //$$     public <T extends GameRules.RuleValue<T>> void visit(GameRules.RuleKey<T> key, GameRules.RuleType<T> type) {
                //$$         GameRules.IRuleEntryVisitor.super.visit(key, type);
                //$$
                //$$         if (immutableGameRules.containsKey(key.getName())) {
                //$$             String setting = immutableGameRules.get(key.getName());
                //$$             GameRules.RuleValue<T> value = gameRules.get(key);
                //$$
                //$$             if (value instanceof GameRules.BooleanValue) {
                //$$                 GameRules.BooleanValue newValue = new GameRules.BooleanValue((GameRules.RuleType<GameRules.BooleanValue>) type, Boolean.parseBoolean(setting));
                //$$                 ((GameRules.BooleanValue) value).changeValue(newValue, integratedServer);
                //$$             } else if (value instanceof GameRules.IntegerValue) {
                //$$                 GameRules.IntegerValue newValue = new GameRules.IntegerValue((GameRules.RuleType<GameRules.IntegerValue>) type, Integer.parseInt(setting));
                //$$                 ((GameRules.IntegerValue) value).changeValue(newValue, integratedServer);
                //$$             }
                //$$         }
                //$$     }
                //$$ });
                //#endif
            });
        }
    }

    private void updateCheatSettings(final IntegratedServer integratedServer, final boolean cheats) {
        //#if MC>=11600
        //$$ // See Mixin_ControlAreCommandsAllowed
        //#else
        if (integratedServer.worlds.length > 0) {
            integratedServer.worlds[0].getWorldInfo().setAllowCommands(cheats);
        }
        //#endif

        this.allowCheats = cheats;
        updateOppedPlayers(new HashSet<>(getOppedPlayers()), false);
    }

    public void updateOppedPlayers(Set<UUID> oppedPlayers) {
        updateOppedPlayers(oppedPlayers, true);
    }

    private void updateOppedPlayers(Set<UUID> oppedPlayers, boolean persistSettings) {
        final IntegratedServer integratedServer = Minecraft.getMinecraft().getIntegratedServer();
        if (integratedServer == null) {
            throw new IllegalStateException("No local session is currently active.");
        }

        this.oppedPlayers.clear();
        this.oppedPlayers.addAll(oppedPlayers);

        HashSet<UUID> immutableOppedPlayers = new HashSet<>();
        if (this.allowCheats) {
            immutableOppedPlayers.addAll(this.oppedPlayers);
            immutableOppedPlayers.add(UUIDUtil.getClientUUID());
        }

        if (persistSettings) {
            persistSettings();
        }

        getExecutor(integratedServer).execute(() -> {
            final PlayerList playerList = integratedServer.getPlayerList();
            final UserListOps opList = playerList.getOppedPlayers();

            List<GameProfile> allProfiles = Arrays.stream(opList.getKeys())
                //#if MC>=12109
                //$$ .map(username -> integratedServer.getApiServices().nameToIdCache().findByName(username).map(it -> new GameProfile(it.id(), it.name())))
                //#else
                .map(username -> integratedServer.getPlayerProfileCache().getGameProfileForUsername(username))
                //#endif
                    //#if MC>=11700
                    //$$ .filter(Optional::isPresent).map(Optional::get)
                    //#endif
                    .collect(Collectors.toList());

            // Remove all players that are no longer op
            for (GameProfile profile : allProfiles) {
                if (!immutableOppedPlayers.contains(profile.getId())) {
                    //#if MC>=12109
                    //$$ playerList.removeFromOperators(new PlayerConfigEntry(profile));
                    //#else
                    playerList.removeOp(profile);
                    //#endif
                }
            }

            // Op all new players
            for (UUID oppedPlayer : immutableOppedPlayers) {

                //#if MC>=12109
                //$$ PlayerConfigEntry gameProfile = new PlayerConfigEntry(oppedPlayer, UUIDUtil.getName(oppedPlayer).join());
                //#else
                GameProfile gameProfile = new GameProfile(oppedPlayer, UUIDUtil.getName(oppedPlayer).join());
                //#endif

                if (opList.getEntry(gameProfile) == null) {
                    playerList.addOp(gameProfile);
                }
            }
        });
    }

    /**
     * @return a set of players that were opped by the host.
     * Please check `allowCheats` to see if returned players are opped on the server currently.
     */
    public Set<UUID> getOppedPlayers() {
        return oppedPlayers;
    }

    /**
     * @param uuid UUID player to get the online state of
     * @return a state with value of whether the specified player is online or not
     */
    public State<Boolean> getOnlineState(UUID uuid) {
        if (uuid.equals(UUIDUtil.getClientUUID())) {
            return StateKt.stateOf(true);
        }
        return onlinePlayerStates.computeIfAbsent(uuid, k -> StateKt.mutableStateOf(false));
    }

    @Subscribe
    public void onPlayerJoinSession(PlayerJoinSessionEvent event) {
        onlinePlayerStates.computeIfAbsent(event.getProfile().getId(), k -> StateKt.mutableStateOf(true)).set(true);
        maxConcurrentGuests = (int) Math.max(maxConcurrentGuests, onlinePlayerStates.values().stream().filter(State::getUntracked).count());
    }

    @Subscribe
    public void onPlayerLeaveSession(PlayerLeaveSessionEvent event) {
        final MutableState<Boolean> onlineState = onlinePlayerStates.remove(event.getProfile().getId());
        if (onlineState != null) {
            onlineState.set(false);
        }
    }

    @Subscribe
    public void onServerTick(ServerTickEvent event) {
        if (tpsSessionMonitor != null) {
            tpsSessionMonitor.tick();
        }
    }

    public boolean isShareResourcePack() {
        return shareResourcePack;
    }

    public void setShareResourcePack(boolean shareResourcePack) {
        this.shareResourcePack = shareResourcePack;
        if (shareResourcePack) {
            ResourcePackSharingHttpServer.INSTANCE.onShareResourcePackEnable();
        }
        updateResourcePack(packInfo); // Update to either populate or empty the resource pack on the integrated server
        persistSettings();
    }

    public void updateResourcePack(@Nullable ResourcePackSharingHttpServer.PackInfo info) {
        this.packInfo = info;
        final IntegratedServer integratedServer = Minecraft.getMinecraft().getIntegratedServer();
        if (integratedServer != null) {
            if (packInfo == null || !shareResourcePack) {
                setServerResourcePack(null, null);
            } else {
                // We build url of the form `http://UUID.essential-sps/HASH` where `UUID` is the host's UUID and `HASH`
                // is the checksum of the resource pack to be loaded.
                // There's no paricular reason we chose that host, any magic value would do, it's replaced on the client
                // side, but this one feels most appropriate given it also matches the meaning of the url.
                // The resource pack hash is included because MC caches resource packs by url and we don't want it to
                // have re-download the whole thing every time when merely switching between two packs.
                String url = "http://" + new SpsAddress(UUIDUtil.getClientUUID()) + "/" + packInfo.getChecksum();
                setServerResourcePack(url, packInfo.getChecksum());
            }
        }
    }

    private void setServerResourcePack(String url, String checksum) {
        this.resourcePackUrl = url;
        this.resourcePackChecksum = checksum;
        // Resource pack is handled by Mixin_IntegratedServerResourcePack on 1.19+
        //#if MC<11900
        final IntegratedServer integratedServer = Minecraft.getMinecraft().getIntegratedServer();
        if (integratedServer == null) {
            return;
        }
        if (url == null || checksum == null) {
            integratedServer.setResourcePack("", "");
        } else {
            integratedServer.setResourcePack(url, checksum);
        }
        //#endif
    }

    public String getResourcePackUrl() {
        return resourcePackUrl;
    }

    public String getResourcePackChecksum() {
        return resourcePackChecksum;
    }

    public UUID getSessionId() {
        return sessionId;
    }
}
