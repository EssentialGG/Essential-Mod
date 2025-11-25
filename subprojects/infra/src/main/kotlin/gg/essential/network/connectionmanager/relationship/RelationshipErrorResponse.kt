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
package gg.essential.network.connectionmanager.relationship

enum class RelationshipErrorResponse {

    EXISTING_REQUEST_IS_PENDING,
    SENDER_TYPE_VERIFIED_SLOT_LIMIT,
    TARGET_TYPE_VERIFIED_SLOT_LIMIT,
    NO_PENDING_REQUEST_TO_VERIFY,
    VERIFIED_RELATIONSHIP_ALREADY_EXISTS,
    NO_BLOCKED_RELATIONSHIP_TO_CONVERT,
    SENDER_BLOCKED_TARGET,
    TARGET_BLOCKED_SENDER,
    SENDER_TYPE_OUTGOING_SLOT_LIMIT,
    TARGET_TYPE_INCOMING_SLOT_LIMIT,
    TARGET_PRIVACY_SETTING_FRIEND_OF_FRIENDS,
    TARGET_PRIVACY_SETTING_NO_ONE,
    TARGET_NOT_EXIST,
    USER_IS_TEMPORARILY_SUSPENDED,
    USER_IS_PERMANENTLY_SUSPENDED,
    ;

    companion object {
        @JvmStatic
        fun getResponse(response: String): RelationshipErrorResponse? {
            return try {
                valueOf(response)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
