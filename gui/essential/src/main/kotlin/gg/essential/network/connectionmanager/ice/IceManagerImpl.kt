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

import gg.essential.connectionmanager.common.packet.ice.IceCandidatePacket
import gg.essential.connectionmanager.common.packet.ice.IceSessionPacket
import gg.essential.gui.modal.sps.FirewallBlockingModal
import gg.essential.ice.CandidateManager
import gg.essential.ice.CandidateType
import gg.essential.ice.IceAgent
import gg.essential.ice.LocalCandidate
import gg.essential.ice.RemoteCandidate
import gg.essential.ice.RemoteCandidateImpl
import gg.essential.ice.stun.StunManager
import gg.essential.network.CMConnection
import gg.essential.network.connectionmanager.ice.IceManager.Companion.ICE_TIMEOUT
import gg.essential.network.connectionmanager.ice.IceManager.Companion.STUN_HOSTS
import gg.essential.network.connectionmanager.ice.IceManager.Companion.SUPPORTS_QUIC
import gg.essential.network.connectionmanager.ice.IceManager.Companion.TCP_TIMEOUT
import gg.essential.network.connectionmanager.ice.IceManager.Companion.TURN_HOSTS
import gg.essential.network.connectionmanager.ice.IceManager.Companion.VOICE_HEADER_BYTE
import gg.essential.network.registerPacketHandler
import gg.essential.slf4j.ChannelLogger
import gg.essential.slf4j.DelegatingLogger
import gg.essential.slf4j.CombinedLogger
import gg.essential.slf4j.withKeyValue
import gg.essential.util.Client
import gg.essential.util.FirewallUtil
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.USession
import gg.essential.util.UuidNameLookup
import gg.essential.util.forwardChannelsInto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.apache.commons.codec.binary.Hex
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import org.slf4j.event.LoggingEvent
import org.slf4j.helpers.MessageFormatter
import org.slf4j.spi.DefaultLoggingEventBuilder
import org.slf4j.spi.LoggingEventBuilder
import java.io.IOException
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.SocketException
import java.nio.file.Path
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.toJavaDuration

abstract class IceManagerImpl(
    private val cmConnection: CMConnection,
    private val logFolder: Path,
    private val isInvited: (UUID) -> Boolean,
) {
    protected val connectionsScope = CoroutineScope(SupervisorJob() + mainThread)

    private val stunManager = StunManager(connectionsScope)
    private var candidateManager: SharedCandidateManager? = null

    private val connections = mutableMapOf<UUID, IceConnection>()

    protected abstract var integratedServerVoicePort: Int

    protected abstract val resourcePackHttpServerPort: Int

    /**
     * The port on which the proxy accepts http connections.
     * `null` if http proxying is not supported by the current connection.
     */
    var proxyHttpPort: Int? = null

    init {
        cmConnection.registerPacketHandler<IceSessionPacket> { connectionsScope.launch { handlePacket(it) } }
        cmConnection.registerPacketHandler<IceCandidatePacket> { connectionsScope.launch { handlePacket(it) } }

        connectionsScope.launch(Dispatchers.IO) {
            if (!logFolder.exists()) {
                return@launch
            }
            for (entry in logFolder.listDirectoryEntries()) {
                try {
                    entry.deleteExisting()
                } catch (e: Throwable) {
                    LOGGER.warn("Failed to delete $entry:", e)
                }
            }
        }
    }

    private fun handlePacket(packet: IceSessionPacket) {
        val user = packet.user
        val outgoingConnection = connections[user]?.takeIf { it.job.isActive && !it.remoteCreds.isCompleted }
        if (outgoingConnection != null) {
            outgoingConnection.remoteCreds.complete(Pair(packet.ufrag, packet.password.encodeToByteArray()))
        } else if (isInvited(user)) {
            createServerAgent(user, packet.ufrag, packet.password)
        } else {
            LOGGER.debug("Ignoring IceSessionPacket from {} because they weren't invited.", user)
        }
    }

    private fun handlePacket(packet: IceCandidatePacket) {
        val user = packet.user
        val connection = connections[user]
        if (connection == null) {
            LOGGER.debug("Ignoring candidate from {} because they have no active session.", user)
            return
        }

        val candidateStr = packet.candidate
        if (candidateStr != null) {
            val candidate = candidateFromString(candidateStr) ?: return
            connection.agent.remoteCandidateChannel.trySend(candidate)
        } else {
            connection.agent.remoteCandidateChannel.close()
        }
    }

    protected suspend fun connect(user: UUID): McConnectionArgs = withContext(mainThread) {
        LOGGER.info("Creating client-side ICE agent for {}", user)

        // If firewall is enabled, block the thread until the user disables it or cancels the modal
        while (FirewallUtil.isFirewallBlocking()) {
            suspendCancellableCoroutine { continuation ->
                val manager = platform.createModalManager()
                manager.queueModal(FirewallBlockingModal(
                    manager,
                    user,
                    { continuation.resumeWithException(PrettyIOException("ICE setup failed - Firewall enabled", null)) },
                    { continuation.resume(Unit) },
                ))
            }
        }

        connections.forEach { it.value.job.cancel() }
        connections.clear()
        val connection = createIceConnection(connectionsScope, user, true, Telemetry.None)
        connections[user] = connection

        connection.connectJob.await()
    }

    protected abstract fun createServerAgent(user: UUID, ufrag: String, password: String)

    protected fun accept(
        scope: CoroutineScope,
        user: UUID,
        ufrag: String,
        password: String,
        telemetry: Telemetry,
    ): Deferred<McConnectionArgs> {
        LOGGER.info("Creating server-side ICE agent at request from {} (ufrag: {}, pwd: {})", user, ufrag, password)

        connections.remove(user)?.job?.cancel()
        val connection = createIceConnection(scope, user, false, telemetry)
        connections[user] = connection
        
        connection.remoteCreds.complete(Pair(ufrag, password.encodeToByteArray()))

        return connection.connectJob
    }

    protected data class McConnectionArgs(
        val coroutineScope: CoroutineScope,
        val inboundChannel: ReceiveChannel<ByteArray>,
        val outboundChannel: SendChannel<ByteArray>,
        val onClose: () -> Unit,
    )

    private class LoggerSetup(
        val id: Int,
        val logger: Logger,
        val file: Path,
        val fileJob: Job,
        val fileChannel: SendChannel<*>,
        val fileLogger: Logger,
    )

    private fun setupLogging(scope: CoroutineScope): LoggerSetup {
        val iceConnectionId = nextIceConnectionId.incrementAndGet()
        fun Instant.toLocalTime(): LocalTime =
            LocalTime.from(atZone(ZoneId.systemDefault()))

        val startInstant = Instant.now()
        val startTimeMark = TimeSource.Monotonic.markNow()

        val fileChannel = Channel<Pair<ComparableTimeMark, LoggingEvent>>(Channel.UNLIMITED)
        val fileLogger = ChannelLogger(Level.TRACE, fileChannel, TimeSource.Monotonic)
        fileLogger.info("Log starts at {} ({} local time)", startInstant, startInstant.toLocalTime())

        val logger: Logger = CombinedLogger(
            // We'll log to both, log4j and our own dedicated config file.
            // We use a dedicated file for each connection, so we don't need to include the connection id in there.
            PrettyPrintingLogger(LOGGER).withKeyValue("iceConnectionId", iceConnectionId),
            PrettyPrintingLogger(fileLogger),
        )

        val file = logFolder.resolve("connection-$iceConnectionId.log")
        val fileJob = scope.launch(Dispatchers.IO) {
            file.createParentDirectories()
            file.bufferedWriter().use { out ->
                val localTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                for ((timeMark, event) in fileChannel) {
                    val relativeTime = timeMark - startTimeMark
                    val localTime = LocalTime.from((startInstant + relativeTime.toJavaDuration()).atZone(ZoneId.systemDefault()))

                    val relativeTimeStr = "%03d.%03d".format(relativeTime.inWholeSeconds, relativeTime.inWholeMilliseconds % 1000)
                    val localTimeStr = localTime.format(localTimeFormat)
                    val message = MessageFormatter.basicArrayFormat(event.message, event.argumentArray)
                    val line = "[$relativeTimeStr][$localTimeStr][${event.level}] $message\n"
                    out.write(line)

                    event.throwable?.printStackTrace(PrintWriter(out))

                    @OptIn(ExperimentalCoroutinesApi::class) // https://github.com/Kotlin/kotlinx.coroutines/issues/1053#issuecomment-785138865
                    if (fileChannel.isEmpty) {
                        out.flush()
                    }
                }
            }
        }

        return LoggerSetup(iceConnectionId, logger, file, fileJob, fileChannel, fileLogger)
    }

    private fun getCandidateManager(): SharedCandidateManager {
        var active = candidateManager
        if (active == null || active.candidateManager.anyShutDown) {
            active?.children?.close()
            active = SharedCandidateManager()
            candidateManager = active
        }
        return active
    }

    /** A [CandidateManager] which is kept alive as long as required for concurrent use by multiple connections. */
    private inner class SharedCandidateManager {
        val job = Job(connectionsScope.coroutineContext.job)
        val scope = connectionsScope + job

        val logging = setupLogging(connectionsScope)
        init {
            connectionsScope.launch {
                job.join()
                logging.logger.debug("CandidateManager closing")
                logging.fileChannel.close()
            }
        }

        val candidateManager = CandidateManager(
            logging.logger,
            scope,
            stunManager,
            STUN_HOSTS.map { InetSocketAddress(InetAddress.getByName(it), 3478) },
            TURN_HOSTS.map { InetSocketAddress(InetAddress.getByName(it), 3478) },
        )

        /**
         * Channel containing all the jobs which use this [SharedCandidateManager].
         *
         * Once the shared candidate manager is replaced with a new one, this channel must be closed as no new
         * connections will use it.
         * The [SharedCandidateManager] will cancel itself once this channel is closed and all sent jobs have finished.
         */
        val children = Channel<Job>(Channel.UNLIMITED)

        init {
            // Always keep alive all candidates for at least 60 seconds so if another user connects shortly after the
            // first one, they can still use them, even if the ICE process of the first one already finished.
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                children.send(coroutineContext.job)
                val candidates = mutableListOf<LocalCandidate>()
                for (candidate in candidateManager.getCandidates(coroutineContext.job)) {
                    candidates.add(candidate)
                }
                delay(60.seconds)
                for (candidate in candidates) {
                    candidate.close()
                }
            }

            scope.launch {
                for (child in children) {
                    child.join()
                }
                job.cancel()
            }
        }
    }

    private fun createIceConnection(scope: CoroutineScope, user: UUID, client: Boolean, telemetry: Telemetry): IceConnection {
        val logging = setupLogging(scope)
        val logger = logging.logger
        logger.info(
            "Starting connection between {} ({}) and {} ({}), we are the {}",
            USession.activeNow().username, USession.activeNow().uuid,
            UuidNameLookup.nameState(user, "[name not yet known]").getUntracked(), user,
            if (client) "client" else "server",
        )

        val sharedCandidateManager = getCandidateManager()
        sharedCandidateManager.logging.logger.debug("Manager now in used by connection {}", logging.id)
        logging.fileLogger.debug("CandidateManager logs are stored in {}", sharedCandidateManager.logging.file)

        val connection = IceConnection(
            logger,
            scope,
            stunManager,
            sharedCandidateManager.candidateManager,
            user,
            client,
            telemetry,
        )
        sharedCandidateManager.children.trySend(connection.job)

        scope.launch(Dispatchers.IO) {
            try {
                connection.job.join()
                try {
                    connection.connectJob.await()
                } catch (ignored: CancellationException) {
                } catch (e: Throwable) {
                    logging.fileLogger.info("End of log. Connection job failed due to:", e)
                    logging.fileChannel.close()
                    logging.fileJob.join()
                    val transcript = logging.file.readText()
                    LOGGER.error("Transcript of failed ICE connection ${logging.id}:\n{}", transcript)
                }
            } finally {
                logging.fileLogger.info("End of log.")
                logging.fileChannel.close()
            }
        }

        return connection
    }

    protected inner class IceConnection(
        logger: Logger,
        parentScope: CoroutineScope,
        stunManager: StunManager,
        candidateManager: CandidateManager,
        val user: UUID,
        val client: Boolean,
        val telemetry: Telemetry,
    ) {
        /** Primary job of this connection. Completes once the connection is fully closed on the MC side. */
        val job = Job(parentScope.coroutineContext.job)
        val coroutineScope = parentScope + job

        val localCreds = run {
            // the "-q" in the ufrag is used to communicate to the other side that QUIC is supported (and preferred)
            // see [Flags.isQuic]
            var flags = if (SUPPORTS_QUIC) "q" else ""
            // the "v12345" in the ufrag is used to communicate to the other side the port used by third-party voice mods
            // see [IceConnection.getVoicePort]
            if (!client) flags += "v$integratedServerVoicePort"

            val randomness = ByteArray(20)
            secureRandom.nextBytes(randomness)

            val username = "essential-$flags-${Hex.encodeHexString(randomness.sliceArray(0 until 4))}"
            val password = Hex.encodeHexString(randomness.sliceArray(4 until 20)).encodeToByteArray()
            Pair(username, password)
        }
        val remoteCreds = CompletableDeferred<Pair<String, ByteArray>>()
        private val flags = coroutineScope.async { Flags(logger, localCreds, remoteCreds.await()) }

        val agent: IceAgent = IceAgent(
            logger,
            CoroutineScope(job + mainThread),
            stunManager,
            candidateManager,
            client,
            localCreds,
            remoteCreds,
        )

        val forwardLocalCandidatesJob = coroutineScope.launch(Dispatchers.Client) {
            cmConnection.call(IceSessionPacket(user, localCreds.first, localCreds.second.decodeToString()))
                .fireAndForget()
            for (candidate in agent.localCandidateChannel) {
                cmConnection.call(IceCandidatePacket(user, candidateToString(candidate)))
                    .fireAndForget()
            }
        }

        private val inboundDataChannel = Channel<ByteArray>(1000, BufferOverflow.DROP_OLDEST) { packet ->
            logger.warn("IceConnection.inboundDataChannel overflow, dropping packet of {} bytes", packet.size)
        }
        private val outboundDataChannel = Channel<ByteArray>(1000, BufferOverflow.DROP_OLDEST) { packet ->
            logger.warn("IceConnection.outboundDataChannel overflow, dropping packet of {} bytes", packet.size)
        }

        private val inboundVoiceChannel = Channel<ByteArray>(1000, BufferOverflow.DROP_OLDEST) { packet ->
            logger.warn("IceConnection.inboundVoiceChannel overflow, dropping packet of {} bytes", packet.size)
        }
        private val outboundVoiceChannel = Channel<ByteArray>(1000, BufferOverflow.DROP_OLDEST) { packet ->
            logger.warn("IceConnection.outboundVoiceChannel overflow, dropping packet of {} bytes", packet.size)
        }

        val inboundPacketSortingJob = coroutineScope.launch(Dispatchers.Unconfined) {
            for ((candidate, data) in agent.inboundDataChannel) {
                telemetry.packetReceived(data.size, candidate.isIPv6, candidate.isRelay)
                if (data.isNotEmpty() && data[0] == VOICE_HEADER_BYTE) {
                    inboundVoiceChannel.send(data)
                } else {
                    inboundDataChannel.send(data)
                }
            }
            inboundVoiceChannel.close()
            inboundDataChannel.close()
        }
        val outboundPacketMergingJob = coroutineScope.launch(Dispatchers.Unconfined) {
            val mergedChannel = Channel<ByteArray>()
            launch { forwardChannelsInto(mergedChannel, outboundVoiceChannel, outboundDataChannel) }
            launch {
                for (packet in mergedChannel) {
                    telemetry.packetSent(packet.size)
                    agent.outboundDataChannel.send(packet)
                }
                agent.outboundDataChannel.close()
            }
        }

        val connectJob: Deferred<McConnectionArgs> = coroutineScope.async {
            // Wait until we're ready to send data
            withTimeoutOrNull(ICE_TIMEOUT.seconds) {
                agent.readyForData.await()
            } ?: throw PrettyIOException("${if (client) "Server" else "Client"} is unreachable (ICE failed)", null)

            // Then try to connect to the server
            // We use a dedicated job for QUIC/PseudoTCP, so we can detect once they are done cleanly shutting down
            val connectionJob = Job(job)
            val connectionScope = coroutineScope + connectionJob
            val (recvChannel, sendChannel) = if (flags.await().isQuic) {
                val quicChannel = QuicChannel(connectionScope, logger, inboundDataChannel, outboundDataChannel)
                if (client) {
                    val (streams, httpPort) = quicChannel.connect()
                    proxyHttpPort = httpPort
                    streams
                } else {
                    quicChannel.accept(resourcePackHttpServerPort)
                }
            } else {
                proxyHttpPort = null
                val pseudoTcpChannel = PseudoTcpChannel(connectionScope, inboundDataChannel, outboundDataChannel)
                withTimeoutOrNull(TCP_TIMEOUT.toLong()) {
                    if (client) {
                        pseudoTcpChannel.connect()
                    } else {
                        pseudoTcpChannel.accept()
                    }
                } ?: throw PrettyIOException(if (client) "Connect timed out" else "Accept timed out", null)
            }
            McConnectionArgs(coroutineScope, recvChannel, sendChannel) {
                coroutineScope.launch {
                    // MC connection was closed, mark job as completed so it can transition into Completing state
                    // and we can await all its children by `join`ing it.
                    connectionJob.complete()
                    // Then give the TCP/QUIC connection some more time to cleanly shut down
                    val timedOut = withTimeoutOrNull(10.seconds) { connectionJob.join() } == null
                    if (timedOut) {
                        logger.warn("Failed to cleanly shut down connection, force closing now.")
                    } else {
                        logger.debug("Connection shut down cleanly, closing ICE now.")
                    }
                    // and finally, cancel everything
                    job.cancel()
                    telemetry.closed()
                }
            }
        }

        val voiceProxyJob = coroutineScope.launch(Dispatchers.IO) {
            val voicePort = flags.await().voicePort ?: return@launch

            DatagramSocket(null).use { proxySocket ->
                // If we are on the client, our proxy acts as the voice server and we won't know the
                // downstream (voice client) address until they start talking to us.
                // If we are on the server, our proxy acts as a voice client, and we know the voice server address.
                val proxyPort = if (client) voicePort else 0
                val downstreamAddress = AtomicReference<SocketAddress?>(
                    if (client) null else InetSocketAddress(InetAddress.getLoopbackAddress(), voicePort)
                )

                try {
                    proxySocket.bind(InetSocketAddress(null as InetAddress?, proxyPort))
                } catch (e: SocketException) {
                    logger.error("Failed to allocate port for voice chat forwarding:", e)
                    return@launch
                }

                supervisorScope {
                    val outboundJob = launch(CoroutineName("Voice Proxy Outbound")) {
                        val buf = ByteArray(0xffff)
                        buf[0] = VOICE_HEADER_BYTE
                        val packet = DatagramPacket(buf, 1, buf.size - 1)
                        try {
                            while (coroutineContext.isActive) {
                                proxySocket.receive(packet)
                                if (client) {
                                    downstreamAddress.set(packet.socketAddress)
                                }
                                outboundVoiceChannel.send(buf.sliceArray(0 until 1 + packet.length))
                            }
                        } catch (e: IOException) {
                            if (coroutineContext.isActive) {
                                throw e
                            }
                        } finally {
                            outboundVoiceChannel.close()
                        }
                    }
                    launch(CoroutineName("Voice Proxy Inbound")) {
                        try {
                            for (buf in inboundVoiceChannel) {
                                // Offset by 1 to remove our VOICE_HEADER_BYTE
                                proxySocket.send(DatagramPacket(buf, 1, buf.size - 1, downstreamAddress.get()))
                            }
                        } finally {
                            outboundJob.cancel() // cancel first so we don't print the Socket Closed exception
                            proxySocket.close() // otherwise we can be stuck in `receive` indefinitely
                        }
                    }
                }
            }
        }
    }

    private class Flags(logger: Logger, localCreds: Pair<String, ByteArray>, remoteCreds: Pair<String, ByteArray>) {
        /**
         * We use the ufrag to communicate alphanumeric flags between both sides.
         * This allows us to switch to an improved implementation if both sides support it without requiring infra
         * changes to communicate that.
         * The ufrag will generally follow the format "essential-${flags}-${random}". The flags part may be missing from
         * old clients.
         */
        private val flags: Pair<String, String> = run {
            val localUfrag: String = localCreds.first
            val remoteUfrag: String = remoteCreds.first
            val localParts = localUfrag.split("-")
            val remoteParts = remoteUfrag.split("-")
            val localFlags = if (localParts.size > 2) localParts[1] else ""
            val remoteFlags = if (remoteParts.size > 2) remoteParts[1] else ""
            Pair(localFlags, remoteFlags)
        }

        /** Whether this connection should use QUIC rather than Ice4j's PseudoTcpSocket.  */
        val isQuic: Boolean = run {
            // We communicate support for QUIC via the ufrag. If both sides support it, use it.
            val localSupport = "q" in flags.first
            val remoteSupport = "q" in flags.second
            if (localSupport && remoteSupport) {
                logger.info("Using QUIC because both parties support it.")
                true
            } else {
                val who = if (localSupport) {
                    "the remote client does"
                } else if (remoteSupport) {
                    "the local client does"
                } else {
                    "both sides do"
                }
                logger.warn("Not using QUIC (falling back to PseudoTCP) because {} not support it.", who)
                false
            }
        }

        /** The port used for voice on the server. On the client this requires the remote username. */
        val voicePort: Int? = run {
            val flags = if ("v" in flags.first) flags.first else flags.second
            val flag = flags.substringAfter("v", "").takeWhile { it.isDigit() }
            if (flag.isEmpty()) {
                logger.warn("Connection does not support voice tunneling.")
                return@run null
            }
            try {
                flag.toInt()
            } catch (e: NumberFormatException) {
                logger.error("Failed to parse voice port from \"$flags\":", e)
                null
            }
        }
    }

    // MC uses toString to display the error on the "Failed to connect to server" screen
    protected open class PrettyIOException(message: String, cause: Throwable?) : IOException(message, cause) {
        override fun toString(): String {
            return message!!
        }
    }

    private class PrettyPrintingLogger(delegate: Logger) : DelegatingLogger(delegate) {
        override fun log(event: LoggingEvent) {
            val result = StringBuilder()

            val keyValuePairs = event.keyValuePairs
            if (keyValuePairs != null && keyValuePairs.isNotEmpty()) {
                val keyValueMap = keyValuePairs.associateTo(mutableMapOf()) { it.key to it.value }

                keyValueMap.remove("iceConnectionId")?.let { result.append("[$it] ") }
                val parties = setOfNotNull(
                    keyValueMap.remove("hostAddress"),
                    keyValueMap.remove("turnServer"),
                    keyValueMap.remove("relayAddress"),
                    keyValueMap.remove("remoteAddress"),
                )
                if (parties.isNotEmpty()) {
                    parties.joinTo(result, " <> ", "[", "] ") { party ->
                        if (party is InetSocketAddress) {
                            "${party.address.hostAddress}:${party.port}"
                        } else {
                            party.toString()
                        }
                    }
                }
                keyValueMap.remove("tId")?.let { result.append("[$it] ") }

                if (keyValueMap.isNotEmpty()) {
                    keyValueMap.entries.joinTo(result, "") { "${it.key}=${it.value} "}
                }
            }

            result.append(event.message)

            delegate.atLevel(event.level).apply {
                setMessage(result.toString())
                event.arguments?.forEach { addArgument(it) }
                setCause(event.throwable)
            }.log()
        }

        override fun makeLoggingEventBuilder(level: Level): LoggingEventBuilder {
            // We explicitly do not want to forward building of this event to other loggers, we want to our [log] mehod
            // to be called, so we can do the formatting.
            return DefaultLoggingEventBuilder(this, level)
        }
    }

    interface Telemetry {
        fun packetReceived(bytes: Int, ipv6: Boolean, relay: Boolean)
        fun packetSent(bytes: Int)
        fun closed()

        object None : Telemetry {
            override fun packetReceived(bytes: Int, ipv6: Boolean, relay: Boolean) {}
            override fun packetSent(bytes: Int) {}
            override fun closed() {}
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger("essential/ice")
        @OptIn(ExperimentalCoroutinesApi::class) // will become stable in 1.9 (currently still in RC phase)
        private val mainThread = Dispatchers.Default.limitedParallelism(1)
        private var nextIceConnectionId = AtomicInteger(0)
        private val secureRandom = SecureRandom()

        private fun candidateToString(candidate: LocalCandidate): String = buildList {
            add(candidate.priority) // foundation; don't care, but must be unique (so does priority, so let's use that)
            add(1) // component
            add("udp")
            add(candidate.priority)
            add(candidate.address.address.hostAddress.substringBefore('%')) // remove scope for IPv6 addresses
            add(candidate.address.port)
            add("typ")
            add(candidate.type.shortName)
        }.joinToString(" ")

        private fun candidateFromString(string: String): RemoteCandidate? {
            try {
                val parts = string.split(" ")
                val priority = parts[3].toInt()
                val address = parts[4]
                val port = parts[5].toInt()
                val type = CandidateType.byShortName.getValue(parts[7])
                val socketAddress = InetSocketAddress(InetAddress.getByName(address), port)
                return RemoteCandidateImpl(type, socketAddress, priority)
            } catch (e: Exception) {
                LOGGER.warn("Failed to parse candidate \"$string\":", e)
                return null
            }
        }
    }
}