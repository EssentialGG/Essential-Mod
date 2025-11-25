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
package gg.essential.util

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.essential.connectionmanager.common.packet.pingproxy.ClientPingProxyPacket
import gg.essential.connectionmanager.common.packet.pingproxy.ServerPingProxyResponsePacket
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.util.*
import javax.imageio.ImageIO

class ServerPingInfo(
    val host: String,
    val port: Int,
    val onlinePlayers: Int,
    val maxPlayers: Int,
    val version: String,
    val description: JsonElement,
    val iconBytes: ByteArray?,
) {
    /** Name of the world. Assumes description to be formatted like vanilla. `null` if parsing fails. */
    val worldName: String?
        get() = try {
            if (description is JsonObject) {
                description.get("text").asString
            } else {
                description.asString
            }.split(" - ").drop(1).joinToString(" - ")
        } catch (exception: Exception) {
            LOGGER.warn("Failed to parse world name of `$host:$port`", exception)
            null
        }

    val iconImage: BufferedImage? by lazy {
        try {
            iconBytes?.let { ImageIO.read(it.inputStream()) }
        } catch (exception: Exception) {
            LOGGER.warn("Failed to decode icon of `$host:$port`", exception)
            null
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ServerPingInfo::class.java)

        suspend fun fetchViaPingProxy(address: String): ServerPingInfo? {
            val (host, port) = platform.splitHostAndPort(address)
            return fetchViaPingProxy(host, port)
        }

        suspend fun fetchViaPingProxy(host: String, port: Int): ServerPingInfo? {
            try {
                val request = ClientPingProxyPacket(host, port, platform.mcProtocolVersion)
                val response = platform.cmConnection.call(request).await<ServerPingProxyResponsePacket>()
                    ?: return null

                val json = JsonParser().parse(response.rawJson).asJsonObject

                val players = json.get("players").asJsonObject
                val onlinePlayers = players.get("online").asInt
                val maxPlayers = players.get("max").asInt
                val version = json.get("version").asJsonObject.get("name").asString
                val description = json.get("description")
                val icon = json.get("favicon")?.asString?.let { iconStr ->
                    val prefix = "data:image/png;base64,"
                    if (iconStr.startsWith(prefix)) {
                        val base64 = iconStr.substring(prefix.length)
                            // older versions have the base64 string split over multiple lines
                            .replace("\n", "")
                        Base64.getDecoder().decode(base64)
                    } else {
                        LOGGER.warn("Don't know how to decode icon from `$host:$port`: `$iconStr`")
                        null
                    }
                }
                return ServerPingInfo(host, port, onlinePlayers, maxPlayers, version, description, icon)
            } catch (exception: Exception) {
                LOGGER.warn("Failed to fetch server ping info from `$host:$port", exception)
                return null
            }
        }
    }
}
