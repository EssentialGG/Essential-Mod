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
package gg.essential.gui.common

import gg.essential.universal.UMinecraft
import com.mojang.authlib.GameProfile
import dev.folomeev.kotgl.matrix.vectors.vec3
import dev.folomeev.kotgl.matrix.vectors.vecUnitY
import gg.essential.Essential
import gg.essential.api.profile.WrappedGameProfile
import gg.essential.api.profile.wrapped
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.elementa.state.v2.State as StateV2
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.network.EnumPacketDirection
import net.minecraft.network.NetworkManager
//#if MC>=11202
//#if MC>=11400
//$$ import net.minecraft.client.util.ClientRecipeBook
//$$ import net.minecraft.network.play.server.SPlayerListItemPacket
//$$ import net.minecraft.util.datafix.codec.DatapackCodec
//$$ import net.minecraft.world.GameRules
//$$ import net.minecraft.util.registry.DynamicRegistries
//$$ import java.util.function.Supplier
//#else
import net.minecraft.stats.RecipeBook
//#endif
import net.minecraft.stats.StatisticsManager
//#else
//$$ import net.minecraft.stats.StatFileWriter
//#endif
import net.minecraft.util.MovementInputFromOptions
import net.minecraft.util.ResourceLocation
import gg.essential.event.client.ClientTickEvent
import gg.essential.event.entity.PlayerTickEvent
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.handlers.RenderPlayerBypass
import gg.essential.mixins.ext.client.ParticleSystemHolder
import gg.essential.mixins.transformers.entity.player.EntityPlayerAccessor
import gg.essential.model.ParticleSystem
import gg.essential.model.collision.PlaneCollisionProvider
import gg.essential.model.light.LightProvider
import gg.essential.universal.UMatrixStack
import gg.essential.util.ModLoaderUtil
import gg.essential.util.executor
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraft.world.EnumDifficulty
import net.minecraft.world.GameType
//#if MC>=11400
//#if MC>=11900
//$$ import net.minecraft.world.dimension.DimensionTypes
//#else
//$$ import net.minecraft.world.DimensionType
//#endif
//$$ import net.minecraft.world.World
//#else
import net.minecraft.world.WorldSettings
import net.minecraft.world.WorldType
//#endif
import java.util.*
import kotlin.random.Random

//#if MC>=12002
//$$ import net.minecraft.client.util.SkinTextures
//#endif

//#if MC>=11400
//$$ import gg.essential.util.textLiteral
//#endif

//#if MC>=11700
//$$ import net.minecraft.util.registry.Registry
//#endif

class EmulatedUI3DPlayer(
    val showCape: State<Boolean> = BasicState(true),
    draggable: State<Boolean> = BasicState(true),
    val profile: State<WrappedGameProfile?> = BasicState(null),
    renderNameTag: State<Boolean> = BasicState(false),
    sounds: StateV2<Boolean> = stateOf(false),
    soundsVolume: StateV2<Float> = stateOf(1f),
) : UI3DPlayer(
    draggable = draggable,
    hideNameTags = renderNameTag.map { !it },
    player = null, // to be created by [initPlayer]
    profile = profile,
    sounds = sounds,
    soundsVolume = soundsVolume,
) {
    private val mcClient = UMinecraft.getMinecraft()

    private var tickCounter = ClientTickEvent.counter

    // Creating a player entity will fire forge events, so we must delay that creation until after forge and other mods
    // have been fully initialized (in particular until after CapabilityManager.injectCapabilities).
    private var initializedPlayer = false

    //#if MC>=11903
    //$$ init {
    //$$     errored = true // TODO: can this be done without the fallback renderer?
    //$$     // Note: The Wardrobe hover outline effect is no longer supported via the 1.21.9 vanilla renderer, so even
    //$$     //       if we do re-enable the vanilla renderer on 1.19.3+, we still have to use the fallback for that.
    //$$ }
    //#endif

    private var failedPlayerInit =
        //#if MC>=11903
        //$$ true // TODO: can this be done without the fallback renderer?
        //#else
        false
        //#endif

    init {
        if (ModLoaderUtil.isModLoaded("sereneseasons")) { // EM-1692: thread safety bug in Serene Seasons breaks the emulated player
            failedPlayerInit = true
            errored = true
        }
    }

    private var configurePlayer: EntityOtherPlayerMP.() -> Unit = {}

    fun configurePlayer(block: (player: EntityOtherPlayerMP) -> Unit) = apply {
        configurePlayer = block
    }

    private fun initPlayer() {
        // Creating a player entity will fire forge events and interact with the game state in nearly unpredictable
        // ways, so we need to make doubly sure we're running on the main thread to do that.
        UMinecraft.getMinecraft().executor.execute {
            if (failedPlayerInit) {
                // we've failed to initialize the player previously, let's not spam the log
                return@execute
            }
            try {
                val profile = profile.get() ?: getLocalGameProfile()
                val world = FakeWorld.fakeWorld
                val player = object : EntityOtherPlayerMP(
                    world,
                    profile.profile,
                    //#if MC>=11900 && MC<=11902
                    //$$ null,
                    //#endif
                ), EmulatedPlayer {

                    override val particleSystem = ParticleSystem(
                        Random(0),
                        PlaneCollisionProvider(vec3(0f, posY.toFloat(), 0f), vecUnitY()),
                        LightProvider.FullBright,
                        ::playSound,
                    )

                    //#if MC>=12002
                    //$$ override fun getSkinTextures(): SkinTextures {
                    //$$     return with(super.getSkinTextures()) {
                    //$$         if (showCape.get()) this
                            //#if MC>=12109
                            //$$ else SkinTextures(body(), null, elytra(), model(), secure())
                            //#else
                            //$$ else SkinTextures(texture, textureUrl, null, elytraTexture, model, secure)
                            //#endif
                    //$$     }
                    //$$ }
                    //#else
                    override fun getLocationCape(): ResourceLocation? {
                        if (showCape.get()) return super.getLocationCape();
                        return null
                    }
                    //#endif

                    override fun getAlwaysRenderNameTag(): Boolean {
                        return false
                    }

                    //#if MC>11200
                    override fun isBeingRidden(): Boolean {
                        return true
                    }
                    //#endif

                    override val originalProfile = profile.copy() // in case the original gets modified (e.g. by GameProfileManager)

                    override val networkInfo: NetworkPlayerInfo = NetworkPlayerInfo(
                        //#if MC>=11900
                        //#if MC>=11903
                        //$$ profile.profile,
                        //#else
                        //$$ PlayerListS2CPacket.Entry(profile.profile, 0, GameMode.DEFAULT, textLiteral(""), null),
                        //$$ mcClient.servicesSignatureVerifier,
                        //#endif
                        //#if MC>=11901
                        //$$ false,
                        //#endif
                        //#elseif MC>=11700
                        //$$ PlayerListS2CPacket.Entry(profile.profile, 0, GameMode.DEFAULT, textLiteral(""))
                        //#elseif MC>=11400
                        //$$ SPlayerListItemPacket().AddPlayerData(profile.profile, 0, GameType.NOT_SET, textLiteral(""))
                        //#else
                        profile.profile
                        //#endif
                    )

                    override val emulatedUI3DPlayer: EmulatedUI3DPlayer
                        get() = this@EmulatedUI3DPlayer
                }

                player.updateVisibleModelParts()

                //#if MC < 11400
                player.dimension = 0
                //#endif

                //#if FORGE && MC < 11400
                player.eyeHeight = 1.82f
                //#endif

                //#if MC<12109
                player.chasingPosX = player.posX
                player.chasingPosY = player.posY
                player.chasingPosZ = player.posZ
                player.prevChasingPosX = player.posX
                player.prevChasingPosY = player.posY
                player.prevChasingPosZ = player.posZ
                //#endif

                withFakeClientFields(player) {
                    configurePlayer(player)
                }

                this.player = player
            } catch (e: Throwable) {
                e.printStackTrace()
                failedPlayerInit = true
                errored = true
            }
        }
    }

    override fun close() {
        super.close()

        //#if MC>=11700
        //$$ player?.health = 0f
        //#else
        player?.isDead = true
        //#endif
    }

    override fun draw(matrixStack: UMatrixStack) {
        if (errored) {
            return super.draw(matrixStack)
        }
        RenderPlayerBypass.bypass = true;
        withFakeClientFields {
            it.updateVisibleModelParts()
            //#if MC>=12109
            //$$ mcClient.entityRenderDispatcher.configure(mcClient.gameRenderer.camera, null)
            //#elseif MC>=11400
            //$$ @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // `entity` parameter inappropriately marked as non-null by Forge
            //$$ mcClient.renderManager.cacheActiveRenderInfo(FakeWorld.fakeWorld, mcClient.gameRenderer.activeRenderInfo, null)
            //#endif
            super.draw(matrixStack)
        }
        RenderPlayerBypass.bypass = false;
    }

    init {
        addUpdateFunc { _, _ -> tick() }
    }

    private fun tick() {
        val ticks = ClientTickEvent.counter - tickCounter
        repeat(ticks.coerceAtMost(20)) {
            onGameTick()
        }
        tickCounter += ticks
    }

    private fun onGameTick() {
        withFakeClientFields { player ->
            // In theory this would just call:
            //   fakeWorld?.updateEntity(player)
            // but that does not actually do anything because we do not add any chunks
            // and it only updates entities which are in a chunk.
            // So instead we update manually what we need for animations and stuff:
            player.ticksExisted += 1
            Essential.EVENT_BUS.post(PlayerTickEvent(true, player))
            Essential.EVENT_BUS.post(PlayerTickEvent(false, player))
        }
    }

    private fun withFakeClientFields(block: (player: EntityPlayer) -> Unit) {
        if (!initializedPlayer) {
            initPlayer()
            initializedPlayer = true
        } else if ((player as? EmulatedPlayer?)?.originalProfile != (profile.get() ?: getLocalGameProfile())) {
            initPlayer() // profile has changed, re-create player entity
        }

        val player = player ?: return
        withFakeClientFields(player, block)
    }

    private fun withFakeClientFields(player: EntityPlayer, block: (player: EntityPlayer) -> Unit) {
        //#if MC>=12002
        //$$ TODO("only fallback player supported")
        //#else
        val oldPlayer = mcClient.player
        val oldWorld = mcClient.world
        val oldPlayerInfo = FakeWorld.fakeNetHandler.playerInfo

        try {
            mcClient.player = FakeWorld.fakePlayer ?: return
            mcClient.world = FakeWorld.fakeWorld
            FakeWorld.fakeNetHandler.playerInfo = (player as? EmulatedPlayer)?.networkInfo

            block(player)
        } finally {
            mcClient.player = oldPlayer
            mcClient.world = oldWorld
            FakeWorld.fakeNetHandler.playerInfo = oldPlayerInfo
        }
        //#endif
    }

    private fun EntityPlayer.updateVisibleModelParts() {
        val visibleParts = EnumPlayerModelParts.values()
            .filter { profile.get() != null || mcClient.gameSettings.isPlayerModelPartEnabled(it) }

        //#if MC>=11202
        val key = EntityPlayerAccessor.getPlayerModelFlag()
        //#else
        //$$ val key = 10
        //#endif
        dataManager.set(key, visibleParts.sumOf { it.partMask }.toByte())
    }

    //#if MC<11700
    private fun net.minecraft.client.settings.GameSettings.isPlayerModelPartEnabled(part: EnumPlayerModelParts): Boolean =
        part in modelParts
    //#endif

    interface EmulatedPlayer : ParticleSystemHolder {
        val originalProfile: WrappedGameProfile
        val networkInfo: NetworkPlayerInfo
        val emulatedUI3DPlayer: EmulatedUI3DPlayer
    }

    //#if MC>=12002
    //$$ @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
    //$$ private object FakeWorld {
    //$$     private val unsupported = lazy { TODO("only fallback player supported") }
    //$$     val fakeNetHandler: ClientPlayNetworkHandler by unsupported
    //$$     val fakeWorld: ClientWorld by unsupported
    //$$     val fakePlayer: ClientPlayerEntity? by unsupported
    //$$ }
    //#else
    private class FakeNetHandlerPlayClient(
        mcIn: Minecraft,
        netManager: NetworkManager,
    ) : NetHandlerPlayClient(
        mcIn,
        mcIn.currentScreen ?: object : GuiScreen(
            //#if MC>=11400
            //$$ textLiteral("")
            //#endif
        ) {},
        netManager,
        //#if MC>=11903
        //$$ null,
        //#endif
        GameProfile(UUID.randomUUID(), "EmulatedClient"),
        //#if MC>=11903
        //$$ mcIn.telemetryManager.createWorldSession(false, null,
            //#if MC>=12000
            //$$ null
            //#endif
        //$$ ),
        //#elseif MC>=11800
        //$$ mcIn.createTelemetrySender(),
        //#endif

    ) {
        var playerInfo: NetworkPlayerInfo? = null

        override fun getPlayerInfo(uniqueId: UUID) = playerInfo
        override fun getPlayerInfo(name: String) = playerInfo
    }

    private object FakeWorld {
        //#if MC < 11400
        private val basicWorldSettings = WorldSettings(
            0,
            //#if MC>=11202
            GameType.NOT_SET,
            //#else
            //$$ WorldSettings.GameType.NOT_SET,
            //#endif
            true,
            false,
            WorldType.DEFAULT
        )
        //#endif

        private val fakeNetworkManager = NetworkManager(EnumPacketDirection.CLIENTBOUND).also {
            // Forge blows up if a mod tries to send a packet into our fake connection if it does not have a channel.
            // Why would you send a packet from the model renderer? Not sure, Galacticraft seems to do this, however.
            EmbeddedChannel(it).pipeline().fireChannelActive()
        }

        val fakeNetHandler by lazy { // delayed to ensure MC is available
            FakeNetHandlerPlayClient(Minecraft.getMinecraft(), fakeNetworkManager)
        }

        val fakeWorld by lazy { // for best compatibility this needs to be constructed on the main thread and after boot
            val mc = UMinecraft.getMinecraft()
            //#if MC>=11400
            //$$ ClientWorld(
            //$$     fakeNetHandler,
            //$$     ClientWorld.ClientWorldInfo(Difficulty.EASY, false, false),
            //$$     World.OVERWORLD,
                //#if MC>=11903
                //$$ null, // TODO: figure out if this is possible
                //#elseif MC>=11900
                //$$ DynamicRegistryManager.createAndLoad().get(Registry.DIMENSION_TYPE_KEY).entryOf(DimensionTypes.OVERWORLD),
                //#elseif MC>=11802
                //$$ DynamicRegistryManager.createAndLoad().get(Registry.DIMENSION_TYPE_KEY).entryOf(DimensionType.OVERWORLD_REGISTRY_KEY),
                //#elseif MC>=11700
                //$$ DynamicRegistryManager.create().get(Registry.DIMENSION_TYPE_KEY)[DimensionType.OVERWORLD_REGISTRY_KEY]!!,
                //#else
                //$$ DynamicRegistries.func_239770_b_().func_230520_a_().getValueForKey(DimensionType.OVERWORLD),
                //#endif
            //$$     0,
                //#if MC>=11800
                //$$ 0,
                //#endif
            //$$     Supplier { mc.profiler },
            //$$     mc.worldRenderer,
            //$$     false,
            //$$     0
            //$$ )
            //#else
            WorldClient(
                fakeNetHandler,
                basicWorldSettings,
                0,
                EnumDifficulty.PEACEFUL,
                mc.mcProfiler
            ).apply { provider.setWorld(this) }
            //#endif
        }

        val fakePlayer: EntityPlayerSP? by lazy {
            try {
                val mcClient = Minecraft.getMinecraft()
                val player = EntityPlayerSP(
                    //#if MC>=11400
                    //$$ mcClient, fakeWorld, fakeNetHandler, StatisticsManager(), ClientRecipeBook(), false, false
                    //#elseif MC>=11202
                    mcClient, fakeWorld, fakeNetHandler, StatisticsManager(), RecipeBook()
                    //#else
                    //$$ mcClient, fakeWorld, fakeNetHandler, StatFileWriter()
                    //#endif
                )
                player.serverBrand = "Emulated3DPlayer"

                player.movementInput = MovementInputFromOptions(mcClient.gameSettings)

                try {
                    //#if MC>=11400
                    //$$ mcClient.gameRenderer.activeRenderInfo.update(fakeWorld, player, false, false, 0.0f)
                    //#else
                    mcClient.renderManager.cacheActiveRenderInfo(
                        fakeWorld,
                        mcClient.fontRenderer,
                        player,
                        player,
                        mcClient.gameSettings,
                        0.0f
                    )
                    //#endif
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                player
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }
    }
    //#endif

    companion object {
        fun getLocalGameProfile(): WrappedGameProfile {
            val mc = UMinecraft.getMinecraft()
            //#if MC>=12002
            //$$ return mc.gameProfile.wrapped()
            //#else
            return mc.session.profile.apply {
                properties.putAll(mc.profileProperties)
            }.wrapped()
            //#endif
        }
    }
}
