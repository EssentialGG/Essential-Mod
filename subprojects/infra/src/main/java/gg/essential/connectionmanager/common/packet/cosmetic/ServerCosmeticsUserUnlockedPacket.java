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
package gg.essential.connectionmanager.common.packet.cosmetic;

import gg.essential.cosmetics.model.CosmeticUnlockData;
import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Sent by the server to the client to inform them which cosmetics they just unlocked. */
public class ServerCosmeticsUserUnlockedPacket extends Packet {
    public static final Set<String> suppressNotifications = new HashSet<>();

    // a: unlocked - list of unlocked items (deprecated for unlockedCosmetics)

    @SerializedName("b")
    private final boolean occurredFromPurchase;

    @SerializedName("c")
    private final @Nullable UUID targetUUID;

    private final @NotNull @SerializedName("d") Map<String, CosmeticUnlockData> unlockedCosmetics;

    public ServerCosmeticsUserUnlockedPacket(
        final @Nullable UUID targetUUID,
        final @NotNull Map<String, CosmeticUnlockData> unlockedCosmetics
    ) {
        this(false, targetUUID, unlockedCosmetics);
    }

    public ServerCosmeticsUserUnlockedPacket(
        final boolean occurredFromPurchase,
        final @Nullable UUID targetUUID,
        final @NotNull Map<String, CosmeticUnlockData> unlockedCosmetics
    ) {
        this.occurredFromPurchase = occurredFromPurchase;
        this.targetUUID = targetUUID;
        this.unlockedCosmetics = unlockedCosmetics;
    }

    public boolean occurredFromPurchase() {
        return this.occurredFromPurchase;
    }

    public @Nullable UUID getTargetUUID() {
        return this.targetUUID;
    }

    public @NotNull Map<String, CosmeticUnlockData> getUnlockedCosmetics() {
        return this.unlockedCosmetics;
    }

}
