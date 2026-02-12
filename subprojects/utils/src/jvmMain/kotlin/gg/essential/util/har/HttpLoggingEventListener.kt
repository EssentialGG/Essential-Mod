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
package gg.essential.util.har

import gg.essential.util.Sha256
import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ByteString
import okio.Okio
import okio.Source
import okio.Timeout
import org.slf4j.Logger
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.time.Instant
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark
import kotlin.time.toJavaDuration

class HttpLoggingEventListener(
    private val call: Call,
    private val consoleLogger: Logger,
    private val harFileLogger: HarFileLogger,
) : EventListener() {

    private val id = NEXT_ID.getAndIncrement()

    private var observedConnection: Connection? = null
    private var observedRequest: Request? = null
    private var observedRequestBodySize: Long = -1
    private var observedResponse: Response? = null
    private var observedResponseBodySize: Long = -1

    private var initInstant: Instant = Instant.now()
    private var initTime: ValueTimeMark = TimeSource.Monotonic.markNow()

    private var queueTime: ValueTimeMark? = customQueueTime.get()?.also { customQueueTime.remove() }
    private var startTime: ValueTimeMark? = null
    private var dnsStart: ValueTimeMark? = null
    private var dnsEnd: ValueTimeMark? = null
    private var sslStart: ValueTimeMark? = null
    private var sslEnd: ValueTimeMark? = null
    private var connectStart: ValueTimeMark? = null
    private var connectEnd: ValueTimeMark? = null
    private var sendStart: ValueTimeMark? = null
    private var sendEnd: ValueTimeMark? = null
    private var waitEnd: ValueTimeMark? = null
    private var receiveEnd: ValueTimeMark? = null

    private fun reset() {
        observedConnection = null
        observedRequest = null
        observedRequestBodySize = -1
        observedResponse = null
        observedResponseBodySize = -1

        queueTime = null
        startTime = TimeSource.Monotonic.markNow()
        dnsStart = null
        dnsEnd = null
        sslStart = null
        sslEnd = null
        connectStart = null
        connectEnd = null
        sendStart = null
        sendEnd = null
        waitEnd = null
        receiveEnd = null
    }

    override fun callStart(call: Call) {
        startTime = TimeSource.Monotonic.markNow()

        if (shouldLogToConsole()) {
            consoleLogRequest()
        }
    }

    override fun dnsStart(call: Call, domainName: String) {
        flushPendingEntry()
        dnsStart = TimeSource.Monotonic.markNow()
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>?) {
        dnsEnd = TimeSource.Monotonic.markNow()
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        flushPendingEntry()
        connectStart = TimeSource.Monotonic.markNow()
    }

    override fun secureConnectStart(call: Call) {
        sslStart = TimeSource.Monotonic.markNow()
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        sslEnd = TimeSource.Monotonic.markNow()
    }

    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy?, protocol: Protocol?) {
        connectEnd = TimeSource.Monotonic.markNow()
    }

    override fun connectionAcquired(call: Call, connection: Connection) {
        flushPendingEntry()
        observedConnection = connection
    }

    override fun requestHeadersStart(call: Call) {
        sendStart = TimeSource.Monotonic.markNow()
    }

    override fun requestHeadersEnd(call: Call, request: Request) {
        observedRequest = request
    }

    override fun requestBodyStart(call: Call) {
    }

    override fun requestBodyEnd(call: Call, byteCount: Long) {
        sendEnd = TimeSource.Monotonic.markNow()
        observedRequestBodySize = byteCount
    }

    override fun responseHeadersStart(call: Call) {
        // This method is called when we start receiving the response headers (not once we receive the first one,
        // but when that's the next thing we expect to happen), i.e. right after we're done sending the request.
        // We need to set `sendEnd` here, and not just in `requestBodyEnd`, because not all requests have a body.
        // But it's also not sufficient to only do it in here, because requests can use the `Expect: 100-continue`
        // mechanism, which means we'll be start receiving headers before we're actually sending the request body.
        sendEnd = TimeSource.Monotonic.markNow()
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        // Ideally we'd record this once we receive the first byte of the first header, not the all the headers,
        // but this is the closest callback okhttp provides.
        waitEnd = TimeSource.Monotonic.markNow()
        observedResponse = response
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        receiveEnd = TimeSource.Monotonic.markNow()
        observedResponseBodySize = byteCount
    }

    // Okhttp will automatically re-try on certain errors and follow redirects (and custom interceptors can do so too).
    // There's no explicit callback when this happens, we recognize it by the fact that one of the earlier
    // handlers is suddenly called when we've already received a response from the server.
    // In such a case, we emit the pending log entry, and reset everything to record a new one for the new request.
    private fun flushPendingEntry() {
        val pendingResponse = observedResponse
        if (pendingResponse == null) return

        log(makeLogRequest(observedRequest!!), makeLogResponse(pendingResponse, null))
        reset()
    }

    override fun callEnd(call: Call) {
        // Getting the response body is a bit tricky.
        // Our Interceptor below passively collects it, that is, as the application code reads it, our interceptor
        // observes the data passing by and makes a copy for us.
        // Where it gets tricky is that `callEnded` may be called before the response body has been fully processed by
        // the application (e.g. with http1.1 it seems that reading the last byte from the socket is what invokes
        // `callEnded`, but those last bytes aren't returned until after `callEnded`). But this isn't always the case
        // either.
        // So we need to support two code paths here. One for when we already have a response body, and another one
        // where the Interceptor calls us once the full body has been processed.
        val responseBody: CopiedResponseBody?
        synchronized(observedResponseBodyMap) {
            responseBody = observedResponseBodyMap.remove(call)
            if (responseBody == null) {
                awaitingResponseBodyMap[call] = this
            }
        }
        if (responseBody != null) {
            callEndedWithBody(responseBody)
        }
    }

    private fun callEndedWithBody(responseBody: CopiedResponseBody) {
        val request = observedRequest!!
        val response = observedResponse!!

        if (shouldLogToConsole()) {
            consoleLogResponse(response, responseBody)
        }

        log(makeLogRequest(request), makeLogResponse(response, responseBody))
    }

    override fun callFailed(call: Call, ioe: IOException) {
        if (shouldLogToConsole()) {
            consoleLogger.error("[$id] -> ", ioe)
        }

        log(
            makeLogRequest(observedRequest ?: call.request()),
            HarFile.Entry.Response.FAILURE.copy(comment = ioe.toString())
        )
    }

    private fun log(request: HarFile.Entry.Request, response: HarFile.Entry.Response) {
        harFileLogger.log(makeLogEntry(request, response))
    }

    private fun makeLogEntry(request: HarFile.Entry.Request, response: HarFile.Entry.Response): HarFile.Entry {
        fun duration(start: ComparableTimeMark?, end: ComparableTimeMark?) =
            if (start != null && end != null) end - start else (-1).milliseconds

        val timings = HarFile.Entry.Timings(
            blockedQueueing = duration(queueTime, startTime),
            blocked = duration(queueTime ?: startTime, dnsStart ?: connectStart ?: sendStart),
            dns = duration(dnsStart, dnsEnd),
            connect = duration(dnsEnd ?: connectStart, connectEnd),
            send = duration(connectEnd ?: sendStart, sendEnd),
            wait = duration(sendEnd, waitEnd),
            receive = duration(waitEnd, receiveEnd),
            ssl = duration(sslStart, sslEnd),
        )

        return HarFile.Entry(
            pageref = null,
            startedDateTime = initInstant + ((queueTime ?: startTime ?: error("startTime should always be set by this point")) - initTime).toJavaDuration(),
            time = timings.total,
            request = request,
            response = response,
            cache = HarFile.Entry.Cache(),
            timings = timings,
            serverIPAddress = observedConnection?.socket()?.inetAddress?.hostAddress,
            connection = observedConnection?.socket()?.port?.toString(),
        )
    }

    private fun makeLogRequest(request: Request): HarFile.Entry.Request {
        return HarFile.Entry.Request(
            method = request.method(),
            url = request.url().toString(),
            httpVersion = observedConnection?.protocol()?.toString() ?: "unknown",
            cookies = emptyList(), // TODO would need to extract these from the headers, but we don't use them yet
            headers = request.headers().let { headers ->
                List(headers.size()) { i ->
                    val name = headers.name(i)
                    val value = headers.value(i)
                    name to when (name) {
                        "Authorization",
                        "X-Token" -> value.redact()
                        else -> value
                    }
                }
            },
            queryString = request.url().let { url ->
                List(url.querySize()) { i ->
                    url.queryParameterName(i) to url.queryParameterValue(i)
                }
            },
            postData = request.body()?.let { body ->
                if (call.request().header(REQUEST_BODY_CONTAINS_SECRETS_HEADER) != null) {
                    return@let HarFile.Entry.Request.PostData(
                        mimeType = body.contentType()?.toString() ?: "",
                        params = emptyList(),
                        text = "<body redacted>",
                    )
                }
                // FIXME seems like HAR doesn't support binary request bodies?
                //  This is an issue both with `postData` and with `text`.
                //  We may want to include the raw bytes in a custom field when they cannot be accurately
                //  represented with UTF8, if we ever come across a case where we need to know the exact content.
                if (body is MultipartBody) {
                    HarFile.Entry.Request.PostData(
                        mimeType = body.contentType()?.toString() ?: "",
                        params = body.parts().map { part ->
                            val namePrefix = "form-data; name="
                            val filenamePrefix = "; filename="
                            val disposition = part.headers()?.get("Content-Disposition") ?: namePrefix
                            assert(disposition.startsWith(namePrefix))
                            HarFile.Entry.Request.PostData.Param(
                                name = disposition.substringAfter(namePrefix).substringBefore(filenamePrefix),
                                value = Buffer().let { buffer ->
                                    body.writeTo(buffer)
                                    buffer.readUtf8()
                                },
                                fileName = if (filenamePrefix in disposition) disposition.substringAfter(filenamePrefix) else null,
                                contentType = part.body().contentType()?.toString() ?: "",
                            )
                        },
                        text = "",
                    )
                } else {
                    HarFile.Entry.Request.PostData(
                        mimeType = body.contentType()?.toString() ?: "",
                        params = emptyList(),
                        text = Buffer().let { buffer ->
                            body.writeTo(buffer)
                            buffer.readUtf8()
                        },
                    )
                }
            },
            headersSize = -1, // okhttp doesn't expose this detail
            bodySize = observedRequestBodySize,
        )
    }

    private fun makeLogResponse(response: Response, observedBody: CopiedResponseBody?): HarFile.Entry.Response {
        return HarFile.Entry.Response(
            status = response.code(),
            statusText = response.message() ?: "",
            httpVersion = observedConnection?.protocol()?.toString() ?: "unknown",
            cookies = emptyList(), // TODO would need to extract these from the headers, but we don't use them yet
            headers = response.headers().let { headers ->
                List(headers.size()) { i ->
                    headers.name(i) to headers.value(i)
                }
            },
            content = observedBody?.let { body ->
                if (call.request().header(RESPONSE_BODY_CONTAINS_SECRETS_HEADER) != null) {
                    return@let HarFile.Entry.Response.Content(
                        size = body.fullContentLength,
                        mimeType = body.contentType()?.toString() ?: "",
                        text = "<body redacted, len=${body.fullContentLength}>",
                    )
                }
                val contentType = body.contentType()
                val text: String
                val encoding: String?
                if (contentType != null && isTextLikeMimeType(contentType)) {
                    text = body.string()
                    encoding = null
                } else {
                    text = Base64.getEncoder().encodeToString(body.bytes())
                    encoding = "base64"
                }
                HarFile.Entry.Response.Content(
                    size = body.fullContentLength,
                    mimeType = contentType?.toString() ?: "",
                    text = text,
                    encoding = encoding,
                )
            } ?: HarFile.Entry.Response.Content.EMPTY.copy(comment = "Response body was not recorded"),
            redirectURL = response.header("Location") ?: "",
            headersSize = -1, // okhttp doesn't expose this detail
            bodySize = observedResponseBodySize,
        )
    }

    private fun shouldLogToConsole(): Boolean {
        if (!CONSOLE_LOG) return false

        val request = call.request()

        if (!CONSOLE_LOG_ALL) {
            if (request.header(NO_CONSOLE_LOG_HEADER) != null) return false
        }

        return true
    }

    private fun consoleLogRequest() {
        val request = call.request()

        val bodyStr = request.body()?.let { body ->
            val bytes = Buffer().also { body.writeTo(it) }.snapshot()
            if (request.header(REQUEST_BODY_CONTAINS_SECRETS_HEADER) != null) {
                "<request body redacted, len=${bytes.size()}>"
            } else {
                toStringOrHex(bytes, bytes.size().toLong())
            }
        } ?: ""
        consoleLogger.info("[$id] ${request.method()} ${request.url()} $bodyStr")
    }

    private fun consoleLogResponse(response: Response, body: CopiedResponseBody) {
        val request = call.request()
        if (request.header(NO_CONSOLE_LOG_HEADER) != null && !CONSOLE_LOG_ALL) return

        val bodyStr =
            if (request.header(RESPONSE_BODY_CONTAINS_SECRETS_HEADER) != null) {
                "<response body redacted, len=${body.fullContentLength}>"
            } else {
                toStringOrHex(body.buffer.snapshot(), body.fullContentLength)
            }
        consoleLogger.info("[$id] ${response.code()} $bodyStr")
    }

    private fun toStringOrHex(byteString: ByteString, fullSize: Long): String {
        return buildString {
            append(if (CONSOLE_LOG_FULL_BODY) "\n" else "")

            val str = byteString.utf8()
            if (str.none { it == UNICODE_REPLACEMENT_CHAR || (it.isISOControl() && it != '\n') }) {
                if (CONSOLE_LOG_FULL_BODY) {
                    append(str)
                } else {
                    val subStr = str.substring(0, str.length.coerceAtMost(4096))
                    append(subStr.replace("\n", "").replace("\r", ""))
                    if (subStr.length < str.length) {
                        append("…")
                    }
                }
            } else {
                val maxLen = 512
                append("<binary")
                if (byteString.size() > maxLen) {
                    append(" size=")
                    append(byteString.size())
                }
                append(" sha256=")
                append(byteString.sha256().hex())
                append(" hex=")
                append(byteString.substring(0, byteString.size().coerceAtMost(maxLen)).hex())
                if (byteString.size() > maxLen) {
                    append("…")
                }
                append(">")
            }

            if (byteString.size() < fullSize) {
                append(if (CONSOLE_LOG_FULL_BODY) "\n" else " ")
                append("(truncated, full size is ")
                append(fullSize)
                append(" bytes)")
            }
        }
    }

    private fun String.redact(): String {
        val sha256 = Sha256.compute(this.encodeToByteArray())
        return "<redacted token ${sha256.hexStr.substring(0, 8)}>"
    }

    class Interceptor : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
            val request = removeInternalHeaders(chain.request())
            val response = chain.proceed(request)
            return captureResponseBody(chain.call(), response)
        }

        private fun removeInternalHeaders(request: Request): Request {
            val headers = request.headers()
            if ((0 until headers.size()).none { headers.name(it).startsWith(HTTP_LOGGER_HEADER_PREFIX) }) {
                return request
            }

            val builder = request.newBuilder()
            for (i in 0 until headers.size()) {
                val name = headers.name(i)
                if (name.startsWith(HTTP_LOGGER_HEADER_PREFIX)) {
                    builder.removeHeader(name)
                }
            }
            return builder.build()
        }

        private fun captureResponseBody(call: Call, rawResponse: Response): Response {
            val rawBody = rawResponse.body()!!

            val rawSource = rawBody.source()
            val wrappedSource = Okio.buffer(object : Source {
                private var totalSize = 0L
                private val buffer = Buffer()

                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = rawSource.read(sink, byteCount)
                    if (bytesRead > 0) {
                        totalSize += bytesRead

                        val bytesToCopy = bytesRead.coerceAtMost(MAX_OBSERVED_BODY_SIZE - buffer.size())
                        sink.copyTo(buffer, sink.size() - bytesRead, bytesToCopy)
                    }
                    return bytesRead
                }

                override fun close() {
                    val copiedBody = CopiedResponseBody(rawBody, totalSize, buffer)

                    // See comments in [callEnded]
                    val eventListener: HttpLoggingEventListener?
                    synchronized(observedResponseBodyMap) {
                        eventListener = awaitingResponseBodyMap.remove(call)
                        if (eventListener == null) {
                            observedResponseBodyMap[call] = copiedBody
                        }
                    }
                    eventListener?.callEndedWithBody(copiedBody)

                    rawSource.close()
                }

                override fun timeout(): Timeout = rawSource.timeout()
            })

            return rawResponse.newBuilder()
                .body(ResponseBodyWithWrappedSource(rawBody, wrappedSource))
                .build()
        }
    }

    private class ResponseBodyWithWrappedSource(val inner: ResponseBody, val source: BufferedSource) : ResponseBody() {
        override fun source(): BufferedSource = source
        override fun contentType(): MediaType? = inner.contentType()
        override fun contentLength(): Long = inner.contentLength()
    }

    private class CopiedResponseBody(val inner: ResponseBody, val fullContentLength: Long, val buffer: Buffer) : ResponseBody() {
        override fun source(): BufferedSource = buffer
        override fun contentType(): MediaType? = inner.contentType()
        // Note: Must not delegate to `inner.contentLength` since our `buffer` may be truncated and therefore have a
        //       different length.
        //       We aren't returning the true length of the buffer either because `fullContentLength` should almost
        //       certainly be used instead if we want to know the length.
        override fun contentLength(): Long = -1
    }

    class Factory(private val consoleLogger: Logger, private val fileLogger: HarFileLogger) : EventListener.Factory {
        override fun create(call: Call): EventListener {
            return HttpLoggingEventListener(call, consoleLogger, fileLogger)
        }
    }

    companion object {
        private const val UNICODE_REPLACEMENT_CHAR = '\ufffd' // used when an invalid utf-8 sequence is found
        private const val MAX_OBSERVED_BODY_SIZE = 512 * 1024

        private val CONSOLE_LOG = System.getProperty("essential.http.log", "false") == "true"
        private val CONSOLE_LOG_FULL_BODY = System.getProperty("essential.http.log.full_body", "false") == "true"
        private val CONSOLE_LOG_ALL = System.getProperty("essential.http.log.all", "false") == "true"

        private val NEXT_ID = AtomicInteger()

        private val TEXT_LIKE_MIME_TYPES = mapOf(
            "application" to setOf(
                "json",
            ),
        )
        private fun isTextLikeMimeType(mediaType: MediaType): Boolean {
            if (mediaType.type() == "text") return true
            return TEXT_LIKE_MIME_TYPES[mediaType.type()]?.contains(mediaType.subtype()) == true
        }

        // Thread-safety: Access to either of these must be `synchronize`d on [observedResponseBodyMap].
        private val observedResponseBodyMap: MutableMap<Call, CopiedResponseBody> = mutableMapOf()
        private val awaitingResponseBodyMap: MutableMap<Call, HttpLoggingEventListener> = mutableMapOf()

        private val customQueueTime = ThreadLocal<ValueTimeMark?>()
        fun supplyCustomQueueTime(time: ValueTimeMark) {
            customQueueTime.set(time)
        }
    }
}
