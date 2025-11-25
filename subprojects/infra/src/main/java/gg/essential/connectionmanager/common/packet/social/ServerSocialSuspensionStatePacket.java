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
package gg.essential.connectionmanager.common.packet.social;

import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class ServerSocialSuspensionStatePacket extends Packet {

    private final boolean suspended;

    // Only present when the user has been suspended.
    private final @Nullable String reason;

    @SerializedName("expires_at")
    private final @Nullable Long expiresAt;

    // Only present when the user has been suspended.
    @SerializedName("recently_started")
    private final @Nullable Boolean recentlyStarted;

    public ServerSocialSuspensionStatePacket(
            final boolean suspended,
            final @Nullable String reason,
            final @Nullable Long expiresAt,
            final @Nullable Boolean recentlyStarted
    ) {
        this.suspended = suspended;
        this.reason = reason;
        this.expiresAt = expiresAt;
        this.recentlyStarted = recentlyStarted;
    }

    public boolean isSuspended() {
        return this.suspended;
    }

    public @NotNull String getReason() {
        return this.reason == null ? "" : this.reason;
    }

    public @Nullable Instant getExpiresAt() {
        return this.expiresAt == null ? null : Instant.ofEpochMilli(this.expiresAt);
    }

    public boolean isRecentlyStarted() {
        return this.recentlyStarted != null && this.recentlyStarted;
    }
}
