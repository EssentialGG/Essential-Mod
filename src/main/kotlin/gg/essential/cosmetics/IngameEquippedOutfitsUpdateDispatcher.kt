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

import gg.essential.cosmetics.IngameEquippedOutfitsManager.Update
import gg.essential.event.client.ClientTickEvent
import gg.essential.gui.elementa.state.v2.SetState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mapEach
import gg.essential.gui.elementa.state.v2.toList
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.mixins.ext.client.network.NetHandlerPlayClientExt
import gg.essential.mixins.transformers.network.NetworkManagerAccessor
import gg.essential.network.connectionmanager.cosmetics.InfraEquippedOutfitsManager
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import me.kbrewster.eventbus.Subscribe
import net.minecraft.client.Minecraft
import net.minecraft.network.EnumConnectionState
import net.minecraft.network.EnumPacketDirection
import net.minecraft.network.NetworkManager
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.server.SPacketCustomPayload
import java.util.*
import kotlin.collections.ArrayDeque

//#if MC>=12006
//$$ import gg.essential.mixins.transformers.network.DecoderHandlerAccessor
//$$ import net.minecraft.network.NetworkState
//$$ import net.minecraft.network.handler.DecoderHandler
//$$ import net.minecraft.network.listener.ClientPlayPacketListener
//#endif

/**
 * In charge of syncing the state of the [InfraEquippedOutfitsManager] via packets (which can also be recorded by the
 * ReplayMod) to the [IngameEquippedOutfitsManager].
 */
class IngameEquippedOutfitsUpdateDispatcher(
    managedPlayers: SetState<UUID>,
    outfitsManager: InfraEquippedOutfitsManager,
) {
    private val outfitsState = managedPlayers
        .toList()
        .mapEach { it to outfitsManager.getEquippedCosmeticsState(it) }
        // FIXME figure out some way to flatten this without re-computing the full list each time
        .let { state -> State { state().map { (uuid, outfit) -> uuid to outfit() } }.toListState() }

    @Subscribe
    private fun tick(event: ClientTickEvent) {
        val connection = Minecraft.getMinecraft().connection ?: return
        val encoder = (connection as NetHandlerPlayClientExt).`essential$ingameEquippedOutfitsUpdateEncoder`

        val updates = encoder.update(outfitsState.getUntracked())
        if (updates.isEmpty()) return
        sendUpdates((connection.networkManager as NetworkManagerAccessor).channel, updates)
    }

    companion object {
        fun sendUpdates(updates: List<Pair<UUID, List<Update>>>) {
            val connection = Minecraft.getMinecraft().connection ?: return
            sendUpdates((connection.networkManager as NetworkManagerAccessor).channel, updates)
        }

        private fun sendUpdates(channel: Channel, updates: List<Pair<UUID, List<Update>>>) {
            channel.eventLoop().execute {
                val pipeline = channel.pipeline()
                var ourHandler = pipeline.get(OurChannelHandler::class.java)
                if (ourHandler == null) {
                    ourHandler = if (pipeline.get("ReplayModReplay_packetHandler") != null) {
                        // If this is a replay, we'll want to ignore live updates and only show what's played back
                        OurChannelHandlerNoop()
                    } else {
                        OurChannelHandlerImpl(channel)
                    }
                    when {
                        // We want to inject into the channel pipeline before the ReplayMod's main channel handler, so it
                        // can record any packets we sent.
                        pipeline.get("replay_recorder_raw") != null -> pipeline.addBefore("replay_recorder_raw", OurChannelHandler.NAME, ourHandler)
                        // If ReplayMod isn't installed or isn't active, we want to inject where it would have been
                        // That is, generally before MC's "decoder" which will turn our bytes into a MC packet.
                        // See https://github.com/ReplayMod/ReplayMod/blob/55e6044bdd1194cd0521fceadf1c63480d9de64a/src/main/java/com/replaymod/recording/handler/ConnectionEventHandler.java#L160-L174
                        // "inbound_config" may temporarily take the place of "decoder" on 1.20.5+
                        pipeline.get("inbound_config") != null -> pipeline.addBefore("inbound_config", OurChannelHandler.NAME, ourHandler)
                        pipeline.get("decoder") != null -> pipeline.addBefore("decoder", OurChannelHandler.NAME, ourHandler)
                        // in Singleplayer prior to 1.20.5 there is no en/decoding at all
                        // (nor splitting, nor compression, so we can simply inject at the head of the entire pipeline)
                        else -> pipeline.addFirst(OurChannelHandler.NAME, ourHandler)
                    }
                }
                ourHandler.inject(OutfitUpdatesPayload(updates))
            }
        }
    }

    private interface OurChannelHandler : ChannelHandler {
        fun inject(payload: OutfitUpdatesPayload)

        companion object {
            const val NAME = "essential_outfits_injector"
        }
    }

    private class OurChannelHandlerNoop : ChannelInboundHandlerAdapter(), OurChannelHandler {
        override fun inject(payload: OutfitUpdatesPayload) {
        }
    }

    private class OurChannelHandlerImpl(private val channel: Channel) : ChannelInboundHandlerAdapter(), OurChannelHandler {
        private val ctx: ChannelHandlerContext by lazy { channel.pipeline().context(this) }
        private val queue = ArrayDeque<OutfitUpdatesPayload>()

        override fun inject(payload: OutfitUpdatesPayload) {
            queue.addLast(payload)
            tryFlush()
        }

        private fun tryFlush() {
            if (queue.isEmpty()) return

            //#if MC>=12006
            //$$ val decoderHandler = channel.pipeline().get(DecoderHandler::class.java) ?: return
            //$$ val connectionState = (decoderHandler as DecoderHandlerAccessor<*>).state
            //$$ if (connectionState.id() != NetworkPhase.PLAY) return
            //$$ @Suppress("UNCHECKED_CAST")
            //$$ connectionState as NetworkState<ClientPlayPacketListener>
            //#elseif MC>=12002
            //$$ val connectionState = channel.attr(ClientConnection.CLIENTBOUND_PROTOCOL_KEY).get()
            //$$ if (connectionState.state != NetworkState.PLAY) return
            //#else
            val connectionState = channel.attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY).get()
            if (connectionState != EnumConnectionState.PLAY) return
            //#endif

            // Prior to 1.20.5, the pipeline won't have a decoder in Singleplayer, in which case we need to send
            // the packet object directly (ReplayMod will automatically handle encoding it for recording).
            val hasDecoder = channel.pipeline().get("decoder") != null

            while (queue.isNotEmpty()) {
                val payload = queue.removeFirst()

                //#if MC>=12002
                //$$ val packet = CustomPayloadS2CPacket(payload)
                //#else
                val payloadByteBuf = Unpooled.buffer()
                OutfitUpdatesPayload.encode(payloadByteBuf, payload)
                val packet = SPacketCustomPayload(OutfitUpdatesPayload.CHANNEL_ID_MC, PacketBuffer(payloadByteBuf))
                //#endif

                if (!hasDecoder) {
                    ctx.fireChannelRead(packet)
                    continue
                }

                val packetByteBuf = ctx.alloc().buffer()
                val packetBuf = PacketBuffer(packetByteBuf)
                //#if MC>=12006
                //$$ connectionState.codec().encode(packetBuf, packet)
                //#else
                //#if MC>=12002
                //$$ val packetId = connectionState.getId(packet)
                //#else
                //#if MC<11600
                @Suppress("UNNECESSARY_NOT_NULL_ASSERTION") // Forge applies an inappropriate NonNullByDefault
                //#endif
                val packetId = connectionState.getPacketId(EnumPacketDirection.CLIENTBOUND, packet)!!
                //#endif
                packetBuf.writeVarInt(packetId)
                packet.writePacketData(packetBuf)
                //#if MC<12002
                payloadByteBuf.release()
                //#endif
                //#endif

                ctx.fireChannelRead(packetByteBuf)
            }
        }

        override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
            super.channelRead(ctx, msg)
            tryFlush()
        }
    }
}
