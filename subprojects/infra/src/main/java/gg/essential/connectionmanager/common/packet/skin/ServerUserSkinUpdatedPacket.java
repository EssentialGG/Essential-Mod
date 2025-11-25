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
package gg.essential.connectionmanager.common.packet.skin;

import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.lib.gson.annotations.SerializedName;

import java.util.UUID;

public class ServerUserSkinUpdatedPacket extends Packet {

    private final UUID uuid;

    @SerializedName("skin_texture")
    private final String skinTexture;

    public ServerUserSkinUpdatedPacket(final UUID uuid, final String skinTexture) {
        this.uuid = uuid;
        this.skinTexture = skinTexture;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getSkinTexture() {
        return this.skinTexture;
    }
}
