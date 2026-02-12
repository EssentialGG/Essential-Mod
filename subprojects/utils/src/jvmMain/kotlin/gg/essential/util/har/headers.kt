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

import okhttp3.Request

internal const val HTTP_LOGGER_HEADER_PREFIX = "XX-HttpLoggingEventListener-"
internal const val NO_CONSOLE_LOG_HEADER = "$HTTP_LOGGER_HEADER_PREFIX-NoConsoleLog"
internal const val REQUEST_BODY_CONTAINS_SECRETS_HEADER = "$HTTP_LOGGER_HEADER_PREFIX-RequestBodyContainsSecrets"
internal const val RESPONSE_BODY_CONTAINS_SECRETS_HEADER = "$HTTP_LOGGER_HEADER_PREFIX-ResponseBodyContainsSecrets"

/**
 * Excludes the request from the standard console logs because it is of little general interest.
 * Such excluded requests may still be show by passing `-Dessential.http.log.all=true`.
 *
 * This has no effect on the HAR file log, where the full details are always recorded.
 */
fun Request.Builder.noConsoleLog() = header(NO_CONSOLE_LOG_HEADER, "true")

/**
 * Marks the request body as potentially containing secrets, so that it will be redacted from
 * all logs to avoid potentially leaking these secrets to malicious third-parties.
 *
 * If the response body may contain secrets as well, consider also calling [responseBodyContainsSecrets].
 */
fun Request.Builder.requestBodyContainsSecrets() = header(REQUEST_BODY_CONTAINS_SECRETS_HEADER, "true")

/**
 * Marks the response body as potentially containing secrets, so that it will be redacted from
 * all logs to avoid potentially leaking these secrets to malicious third-parties.
 *
 * If the request body may contain secrets as well, consider also calling [requestBodyContainsSecrets].
 */
fun Request.Builder.responseBodyContainsSecrets() = header(RESPONSE_BODY_CONTAINS_SECRETS_HEADER, "true")
