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

import kotlinx.coroutines.Deferred
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

interface GatewayService {
    /**
     * Performs a http request to the given (relative) [endpoint][path] of this service.
     *
     * The request details may be configured via [configure]. The url and authentication are configured automatically.
     *
     * The response must be handled during [handleResponse] and is closed automatically when that method returns.
     * The [handleResponse] method is executed on the [IO dispatcher][kotlinx.coroutines.Dispatchers.IO], where it may
     * invoke the blocking methods on OkHttp's [ResponseBody] type to receive the response.
     */
    suspend fun <T> request(path: String, configure: Request.Builder.() -> Unit = {}, handleResponse: suspend (Response) -> T): T

    suspend fun <T> request(httpClient: OkHttpClient, path: String, configure: Request.Builder.() -> Unit = {}, handleResponse: suspend (Response) -> T): T

    fun withDefaultHttpClient(httpClient: Deferred<OkHttpClient>): GatewayService = object : GatewayService {
        private val base: GatewayService
            get() = this@GatewayService

        override suspend fun <T> request(path: String, configure: Request.Builder.() -> Unit, handleResponse: suspend (Response) -> T): T =
            request(httpClient.await(), path, configure, handleResponse)
        override suspend fun <T> request(httpClient: OkHttpClient, path: String, configure: Request.Builder.() -> Unit, handleResponse: suspend (Response) -> T): T =
            base.request(httpClient, path, configure, handleResponse)
    }
}
