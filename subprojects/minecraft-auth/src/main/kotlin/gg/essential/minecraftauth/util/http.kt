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
package gg.essential.minecraftauth.util

import gg.essential.minecraftauth.exception.AuthenticationException
import gg.essential.util.httpCall
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.Request

val JSON_MEDIA_TYPE = MediaType.parse("application/json")

fun Request.execute(): Pair<Int, String> {
    return runBlocking {
        val response = httpCall(this@execute)
        val code = response.code()
        if (code == 429) {
            // If we are rate-limited, there is no other useful information that we can get, and we should just bail out.
            throw AuthenticationException.Ratelimited()
        }

        val content = response.body().use { it?.charStream()?.readText() } ?: ""
        code to content
    }
}
