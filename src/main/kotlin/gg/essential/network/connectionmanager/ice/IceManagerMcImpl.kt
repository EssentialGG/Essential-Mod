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
package gg.essential.network.connectionmanager.ice

import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.mixins.ext.network.getIceEndpoint
import gg.essential.network.connectionmanager.ConnectionManager
import gg.essential.network.connectionmanager.ice.IceEventLoopGroups.ICE_CLIENT_EVENT_LOOP_GROUP
import gg.essential.network.connectionmanager.ice.IceEventLoopGroups.ICE_SERVER_EVENT_LOOP_GROUP
import gg.essential.network.connectionmanager.ice.netty.CloseAfterFirstMessage
import gg.essential.network.connectionmanager.ice.netty.CoroutinesChannelInitializer
import gg.essential.sps.McIntegratedServerManager
import gg.essential.sps.ResourcePackSharingHttpServer
import gg.essential.util.Client
import gg.essential.util.ProtocolUtils.IPV4_HEADER_SIZE
import gg.essential.util.ProtocolUtils.IPV6_HEADER_SIZE
import gg.essential.util.ProtocolUtils.UDP_HEADER_SIZE
import gg.essential.util.executor
import io.netty.channel.local.LocalAddress
import io.netty.channel.local.LocalServerChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.SocketAddress
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException

class IceManagerMcImpl(
    private val cmConnection: ConnectionManager,
    baseDir: Path,
    private val integratedServerManager: State<McIntegratedServerManager?>,
    isInvited: (UUID) -> Boolean,
) : IceManagerImpl(cmConnection, baseDir.resolve("ice-logs"), isInvited), IceManager {
    private val refHolder = ReferenceHolderImpl()

    override var integratedServerVoicePort: Int = 0

    init {
        effect(refHolder) {
            integratedServerVoicePort = integratedServerManager()?.thirdPartyVoicePort() ?: return@effect
        }
    }

    override fun setVoicePort(port: Int) {
        this.integratedServerVoicePort = port
    }

    override val resourcePackHttpServerPort: Int
        get() = ResourcePackSharingHttpServer.port ?: 9

    // Called from dedicated thread. Must be thread-safe and may block.
    @Throws(IOException::class)
    override fun createClientAgent(user: UUID): SocketAddress =
        try {
            runBlocking(Dispatchers.Client) {
                // Wait until we have a connection established
                val mcConnectionArgs = connect(user)

                // And once that's done, spin up a single-use internal proxy server which MC can connect to.
                io.netty.bootstrap.ServerBootstrap()
                    .channel(LocalServerChannel::class.java)
                    .handler(CloseAfterFirstMessage())
                    .childHandler(mcConnectionArgs.toChannelInitializer())
                    .group(ICE_CLIENT_EVENT_LOOP_GROUP.value)
                    .localAddress(LocalAddress.ANY)
                    .bind()
                    .syncUninterruptibly()
                    .channel()
                    .localAddress()
            }
        } catch (e: CancellationException) {
            throw PrettyIOException("ICE setup failed. Contact support.", e)
        }

    override fun createServerAgent(user: UUID, ufrag: String, password: String) {
        val server = Minecraft.getMinecraft().integratedServer
        if (server == null) {
            LOGGER.error("Tried to register ICE socket but server was not running!")
            return
        }

        val acceptJob = super.accept(connectionsScope, user, ufrag, password, TelemetryImpl(user))

        connectionsScope.launch(server.executor.asCoroutineDispatcher()) {
            // Wait until we have a connection established
            val mcConnectionArgs = acceptJob.await()

            // And once we've that's done, connect the proxy to the MC server (much like a local connection)
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION") // has an inappropriate @Nullable on 1.16-1.20.1
            val iceEndpoint = server.networkSystem!!.getIceEndpoint()
            io.netty.bootstrap.Bootstrap()
                .group(ICE_SERVER_EVENT_LOOP_GROUP.value)
                .handler(mcConnectionArgs.toChannelInitializer())
                .channel(io.netty.channel.local.LocalChannel::class.java)
                .connect(iceEndpoint)
        }
    }

    private fun McConnectionArgs.toChannelInitializer() =
        CoroutinesChannelInitializer(coroutineScope, inboundChannel, outboundChannel, onClose)

    private inner class TelemetryImpl(private val client: UUID) : Telemetry {
        private val sessionId = connectionsScope.async(Dispatchers.Client) {
            cmConnection.spsManager.sessionId
        }
        private val receivedPackets = AtomicInteger()
        private val receivedBytes = AtomicLong()
        private val sentPackets = AtomicInteger()
        private val sentBytes = AtomicLong()
        private val lastWasIPv6 = AtomicBoolean()
        private val lastWasRelayed = AtomicBoolean()

        override fun packetReceived(bytes: Int, ipv6: Boolean, relay: Boolean) {
            lastWasIPv6.set(ipv6)
            lastWasRelayed.set(relay)
            receivedPackets.incrementAndGet()
            receivedBytes.addAndGet((estimateHeaderSize() + bytes).toLong())
        }

        override fun packetSent(bytes: Int) {
            sentPackets.incrementAndGet()
            sentBytes.addAndGet((estimateHeaderSize() + bytes).toLong())
        }

        private fun estimateHeaderSize() =
            (if (lastWasIPv6.get()) IPV6_HEADER_SIZE else IPV4_HEADER_SIZE) +
                    UDP_HEADER_SIZE +
                    // https://www.rfc-editor.org/rfc/rfc8656.html#name-the-channeldata-message
                    (if (lastWasRelayed.get()) 4 else 0)

        override fun closed() {
            connectionsScope.launch(Dispatchers.Client) {
                val metadata = buildMap {
                    put("client", client)
                    put("sessionId", sessionId.await() ?: return@launch)
                    put("sentBytes", sentBytes.get())
                    put("sentPackets", sentPackets.get())
                    put("receivedBytes", receivedBytes.get())
                    put("receivedPackets", receivedPackets.get())
                    put("relayed", lastWasRelayed.get())
                }
                cmConnection.telemetryManager.enqueue(ClientTelemetryPacket("SPS_CONNECTION", metadata))
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(IceManagerMcImpl::class.java)
    }
}