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

import gg.essential.universal.UMinecraft
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.SocketAddress
import java.util.*

interface IceManager {
    val proxyHttpPort: Int?

    fun setVoicePort(port: Int)

    @Throws(IOException::class)
    fun createClientAgent(user: UUID): SocketAddress

    @Suppress("MayBeConstant") // https://youtrack.jetbrains.com/issue/KT-64878/Lift-JvmField-restriction-usage-on-Companion-object-of-interfaces
    companion object {
        @JvmField
        val ICE_TIMEOUT: Long = 30 /* sec */
        @JvmField
        val TCP_TIMEOUT: Int = 10 /* sec */ * 1000

        /**
         * Chosen such that it does not conflict with STUN nor TURN nor QUIC nor PseudoTCP.
         *
         * As per the NEW TEXT in RFC7983, STUN and TURN can be identified by the value of the first byte being 0..3 and
         * 64..79 respectively.
         * As per section 17.3.1 of RFC9000, a QUIC Version 1 packet that has the first bit set to 0 must have the second
         * bit set to 1. As such, values in the range 0..63 can never be valid QUICv1 packets.
         * As per implementation, the first four bytes of a PseudoTCP packet are the conversation id which is 0 by default.
         *
         * As such, we should be safe to choose any value from 4..64. We chose 16 because according to RFC7983 that's
         * allocated to ZRTP, which we don't use, so it should most definitely be free.
         */
        @JvmField
        val VOICE_HEADER_BYTE: Byte = 16

        @JvmField
        val STUN_HOSTS: List<String> = System.getProperty("essential.sps.stun_hosts")?.split(",") ?: listOf(
            "us.stun.essential.gg",
            "eu.stun.essential.gg",
            "as.stun.essential.gg",
            "au.stun.essential.gg",
        )

        @JvmField
        val TURN_HOSTS: List<String> = System.getProperty("essential.sps.turn_hosts")?.split(",") ?: listOf(
            "us.turn.essential.gg",
            "eu.turn.essential.gg",
            "as.turn.essential.gg",
            "au.turn.essential.gg",
        )

        @JvmField
        val SUPPORTS_QUIC: Boolean = run {
            val logger = LoggerFactory.getLogger(IceManager::class.java)
            val property = System.getProperty("essential.sps.quic")
            if (property != null) {
                val enabled = property.toBoolean()
                logger.info("Explicitly {} QUIC for SPS.", if (enabled) "enabled" else "disabled")
                enabled
            } else {
                val arch = System.getProperty("os.arch")
                val supported = "amd64" == arch || UMinecraft.isRunningOnMac && ("aarch64" == arch || "x86_64" == arch)
                if (!supported) {
                    logger.warn(
                        "Disabling QUIC for SPS because OS architecture ($arch) is unsupported. " +
                                "This may result in slow connections under certain circumstances. " +
                                "Try reducing the server render distance in these cases.",
                    )
                }
                supported
            }
        }
    }
}