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
package gg.essential.connectionmanager.common.packet.profile;

import gg.essential.connectionmanager.common.model.profile.ProfilePunishmentStatus;
import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.enums.ProfileStatus;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ServerProfileStatusPacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final UUID uuid;

    @SerializedName("b")
    @NotNull
    private final ProfileStatus status;

    private final @Nullable Long lastOnlineTimestamp;

    @SerializedName("punishment_status")
    private @Nullable ProfilePunishmentStatus punishmentStatus = null;

    public ServerProfileStatusPacket(final @NotNull UUID uuid, final @NotNull ProfileStatus profileStatus) {
        this(uuid, profileStatus, (Long) null);
    }

    public ServerProfileStatusPacket(@NotNull final UUID uuid, @NotNull final ProfileStatus status, final @Nullable Long lastOnlineTimestamp) {
        this.uuid = uuid;
        this.status = status;
        this.lastOnlineTimestamp = lastOnlineTimestamp;
    }

    public ServerProfileStatusPacket(final @NotNull UUID uuid, final @NotNull ProfileStatus profileStatus, final @Nullable ProfilePunishmentStatus punishmentStatus) {
        this(uuid, profileStatus, null, punishmentStatus);
    }

    public ServerProfileStatusPacket(@NotNull final UUID uuid, @NotNull final ProfileStatus status, final @Nullable Long lastOnlineTimestamp, final @Nullable ProfilePunishmentStatus punishmentStatus) {
        this.uuid = uuid;
        this.status = status;
        this.lastOnlineTimestamp = lastOnlineTimestamp;
        this.punishmentStatus = punishmentStatus;
    }

    @NotNull
    public UUID getUUID() {
        return this.uuid;
    }

    @NotNull
    public ProfileStatus getStatus() {
        return this.status;
    }

    public @Nullable Long getLastOnlineTimestamp() {
        return this.lastOnlineTimestamp;
    }

    @Nullable
    public ProfilePunishmentStatus getPunishmentStatus() {
        return this.punishmentStatus;
    }
}
