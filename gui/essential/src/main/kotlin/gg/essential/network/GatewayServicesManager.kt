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
package gg.essential.network

import gg.essential.connectionmanager.common.packet.features.ClientExternalServiceRequestPacket
import gg.essential.connectionmanager.common.packet.features.ServerExternalServicePopulatePacket
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.onChange
import gg.essential.util.Client
import gg.essential.util.USession
import gg.essential.util.executeAwait
import gg.essential.util.httpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.io.IOException
import kotlin.collections.iterator

class GatewayServicesManager(
    private val cmConnection: CMConnection,
) {
    private val refHolder = ReferenceHolderImpl()

    private val services = mutableMapOf<String, ServiceImpl>()
    private var initialPacketReceived = Job()

    init {
        cmConnection.registerPacketHandler<ServerExternalServicePopulatePacket> { packet ->
            for ((id, info) in packet.services) {
                // We use HttpUrl.resolve to combine the base url with a given api endpoint.
                // For this to work correctly, the base url needs to end with a `/`, otherwise the part after the last
                // `/` will be discarded (because that's how relative URL resolving works).
                var baseUrlStr = info.url
                if (!baseUrlStr.endsWith("/")) baseUrlStr += "/"
                val baseUrl = HttpUrl.parse(baseUrlStr)
                if (baseUrl == null) {
                    LOGGER.warn("Failed to parse url for service `{}`: {}", id, info.url)
                    continue
                }

                val parsedInfo = ServiceInfo(baseUrl, info.token)

                getServiceImpl(id).info = parsedInfo
            }
            initialPacketReceived.complete()
        }

        // Unlike most regular [NetworkedManager]s, which immediately stop working when switching accounts by
        // virtue of the cmConnection being closed, the gateway services would continue to be functional (at least
        // until infra revokes the auth tokens).
        // To avoid taking actions as the wrong user, we'll therefore explicitly clear all service information
        // immediately on any account change.
        // There is then little reason to do this in `resetState`, where most other [NetworkedManager]s do it.
        // We could have also done this in [NetworkedManager.onDisconnect] but then we get unnecessary downtime on
        // account-switching-unrelated reconnects.
        memo { USession.active().uuid }.onChange(refHolder) { _ ->
            services.values.forEach { service ->
                service.info = null
            }
            if (initialPacketReceived.isCompleted)  {
                initialPacketReceived = Job()
            }
        }
    }

    fun getService(id: String): GatewayService = getServiceImpl(id)

    private fun getServiceImpl(id: String): ServiceImpl = services.getOrPut(id) { ServiceImpl(id) }

    private class ServiceInfo(
        val baseUrl: HttpUrl,
        val token: String?,
        // Thread-safety: May be set by any thread. Will never be un-set.
        var expired: Boolean = false,
    )

    private inner class ServiceImpl(val id: String) : GatewayService {
        // Thread-safety: This must be safe to access from any thread, because `request` can be used on any thread.
        //                It will however only be modified from the main thread!
        var info: ServiceInfo? = null

        // Thread-safety: This must be safe to call from any thread.
        //                And it must not unnecessarily switch to the main thread (e.g. this should continue to work
        //                even when the client thread has crashed).
        override suspend fun <T> request(httpClient: OkHttpClient, path: String, configure: Request.Builder.() -> Unit, handleResponse: suspend (Response) -> T): T {
            val builder = Request.Builder()
            builder.configure()

            while (true) {
                val serviceInfo = serviceInfo()
                builder.configure(serviceInfo, path)
                val response = httpClient.newCall(builder.build()).executeAwait()

                if (response.code() == 401) {
                    response.close()
                    serviceInfo.expired = true
                    continue
                }

                return withContext(Dispatchers.IO) {
                    response.use { handleResponse(it) }
                }
            }
        }

        private fun Request.Builder.configure(info: ServiceInfo, path: String) {
            if (path.startsWith("/")) {
                throw IllegalArgumentException("Path must be relative.")
            }

            val url = info.baseUrl.resolve(path)
                ?: throw IllegalArgumentException("Invalid path `$path` (try to resolve against `${info.baseUrl}` of service `$id`)")

            url(url)
            if (info.token != null) {
                header("X-Token", info.token)
            }
        }

        private val fetchMutex = Mutex()

        // Thread-safety: See above!
        private suspend fun serviceInfo(): ServiceInfo {
            // Fast path
            info.let { if (it != null && !it.expired) return it }

            // Slow path
            fetchMutex.lock()
            try {
                // Check if someone else has fetched it by now
                info.let { if (it != null && !it.expired) return it }

                return withContext(Dispatchers.Client) {
                    // Infra will send us a bunch of services on connect, wait for those before explicitly requesting
                    // any.
                    initialPacketReceived.join()

                    // Check if we've received our info with the above packet
                    info.let { if (it != null && !it.expired) return@withContext it }

                    // Need to go fetch it
                    val request = ClientExternalServiceRequestPacket(setOf(id))
                    val response = cmConnection.call(request)
                        .exponentialBackoff()
                        .await<ServerExternalServicePopulatePacket>()

                    if (id !in response.services) {
                        throw IOException("Infra failed to return info for service with id `$id`")
                    }

                    info!! // should have been filled in by the packet handler
                }
            } finally {
                fetchMutex.unlock()
            }
        }

        override suspend fun <T> request(path: String, configure: Request.Builder.() -> Unit, handleResponse: suspend (Response) -> T): T =
            request(httpClient(), path, configure, handleResponse)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(GatewayServicesManager::class.java)
    }
}
