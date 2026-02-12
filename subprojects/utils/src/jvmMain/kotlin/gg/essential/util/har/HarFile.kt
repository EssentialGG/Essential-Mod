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
@file:UseSerializers(
    DurationAsFractionalMillisSerializer::class,
    InstantAsIso8601Serializer::class,
)
package gg.essential.util.har

import gg.essential.util.DurationAsFractionalMillisSerializer
import gg.essential.util.InstantAsIso8601Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import kotlin.time.Duration

// http://www.softwareishard.com/blog/har-12-spec/
@Serializable
data class HarFile(
    val log: Log,
) {
    @Serializable
    data class Log(
        val version: String,
        val creator: Creator,
        val browser: Creator? = null,
        val pages: List<Page>,
        val entries: List<Entry>,
    )

    @Serializable
    data class Creator(
        val name: String,
        val version: String,
        val comment: String? = null,
    )

    @Serializable
    data class Page(
        val startedDateTime: Instant,
        val id: String,
        val title: String,
        val pageTimings: Timings,
        val comment: String? = null,
    ) {
        @Serializable
        data class Timings(
            val onContentLoad: Duration, // -1 if not available
            val onLoad: Duration, // -1 if not available
            val comment: String? = null,
        )
    }

    @Serializable
    data class Entry(
        val pageref: String? = null,
        val startedDateTime: Instant,
        val time: Duration,
        val request: Request,
        val response: Response,
        val cache: Cache,
        val timings: Timings,
        val serverIPAddress: String? = null,
        val connection: String? = null,
        val comment: String? = null,
    ) {
        @Serializable
        data class Request(
            val method: String,
            val url: String,
            val httpVersion: String,
            val cookies: List<Cookie>,
            val headers: List<NameValuePair>,
            val queryString: List<NameValuePair>,
            val postData: PostData? = null,
            val headersSize: Long, // -1 if not available
            val bodySize: Long, // -1 if not available
            val comment: String? = null,
        ) {
            @Serializable
            data class PostData(
                val mimeType: String,
                val params: List<Param>? = emptyList(),
                val text: String? = "",
                val comment: String? = null,
            ) {
                @Serializable
                data class Param(
                    val name: String,
                    val value: String? = null,
                    val fileName: String? = null,
                    val contentType: String? = null,
                    val comment: String? = null,
                )
            }
        }
        @Serializable
        data class Response(
            val status: Int,
            val statusText: String,
            val httpVersion: String,
            val cookies: List<Cookie>,
            val headers: List<NameValuePair>,
            val content: Content,
            val redirectURL: String,
            val headersSize: Long, // -1 if not available
            val bodySize: Long, // -1 if not available
            val comment: String? = null,
        ) {
            @Serializable
            data class Content(
                val size: Long,
                val compression: Long? = null,
                val mimeType: String,
                val text: String,
                val encoding: String? = null,
                val comment: String? = null,
            ) {
                companion object {
                    val EMPTY = Content(
                        size = 0,
                        mimeType = "",
                        text = "",
                    )
                }
            }

            companion object {
                val FAILURE = Response(
                    status = 0,
                    statusText = "",
                    httpVersion = "",
                    cookies = emptyList(),
                    headers = emptyList(),
                    content = Content.EMPTY,
                    redirectURL = "",
                    headersSize = -1,
                    bodySize = -1,
                )
            }
        }
        @Serializable
        data class Cookie(
            val name: String,
            val value: String,
            val path: String? = null,
            val domain: String? = null,
            val expires: Instant? = null,
            val httpOnly: Boolean? = null,
            val secure: Boolean? = null,
            val comment: String? = null,
        )
        @Serializable
        data class Cache(
            // We don't (yet) support local caching
            val unused: Int? = null,
        )
        @Serializable
        data class Timings(
            /**
             * Custom property used by Chrome for the additional time spent in a priority queue prior
             * to the request being formally started.
             */
            @SerialName("_blocked_queueing")
            val blockedQueueing: Duration, // -1 if not available
            val blocked: Duration, // -1 if not available
            val dns: Duration, // -1 if not available
            val connect: Duration, // -1 if not available
            val send: Duration,
            val wait: Duration,
            val receive: Duration,
            val ssl: Duration?, // -1 if not available
            val comment: String? = null,
        ) {
            private fun Duration.orZero() = coerceAtLeast(Duration.ZERO)
            val total: Duration
                // Note: Does not include `blockedQueueing` and `ssl` because those are already accounted for as part of
                //       `blocked` and `connect` respectively
                get() = blocked.orZero() + dns.orZero() + connect.orZero() + send.orZero() + wait.orZero() + receive.orZero()
        }
    }

    companion object {
        const val VERSION = "1.2"
    }
}

typealias NameValuePair = @Serializable(NameValuePairSerializer::class) Pair<String, String>

@Serializable
private data class NameValuePairDataClass(val name: String, val value: String, val comment: String? = null)
private object NameValuePairSerializer : KSerializer<Pair<String, String>> {
    private val inner = NameValuePairDataClass.serializer()
    override val descriptor: SerialDescriptor = inner.descriptor

    override fun deserialize(decoder: Decoder): Pair<String, String> {
        return decoder.decodeSerializableValue(inner).let { Pair(it.name, it.value) }
    }

    override fun serialize(encoder: Encoder, value: Pair<String, String>) {
        encoder.encodeSerializableValue(inner, NameValuePairDataClass(value.first, value.second))
    }
}
