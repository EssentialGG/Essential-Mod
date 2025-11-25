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
package gg.essential.connectionmanager.common.model.profile;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.network.connectionmanager.common.model.profile.PunishmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProfilePunishmentStatus {

    @SerializedName("punishment_type")
    private final PunishmentType punishmentType;

    @SerializedName("expires_at")
    private final @Nullable Long expiresAt;

    public ProfilePunishmentStatus(final @NotNull PunishmentType punishmentType, final @Nullable Long expiresAt) {
        this.punishmentType = punishmentType;
        this.expiresAt = expiresAt;
    }

    public PunishmentType getPunishmentType() {
        return this.punishmentType;
    }

    public @Nullable Long getExpiresAt() {
        return this.expiresAt;
    }
}
