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
package gg.essential.minecraftauth.exception

/**
 * Thrown when a bad/invalid response is received during the Microsoft authorization process.
 */
sealed class AuthenticationException(message: String) : Exception(message) {
    class InvalidResponse(status: Int, body: String) :
        AuthenticationException("Received an invalid response during authorization ($status): $body")

    class InvalidCredentials : AuthenticationException("One or more of your tokens have expired.")

    class Ratelimited : AuthenticationException("Your request has been rate-limited, please try again later.")
}