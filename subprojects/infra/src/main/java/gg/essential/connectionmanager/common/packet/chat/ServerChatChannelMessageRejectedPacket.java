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
package gg.essential.connectionmanager.common.packet.chat;

import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.lib.gson.annotations.SerializedName;

public class ServerChatChannelMessageRejectedPacket extends Packet {

    // Identifier for the reason for this message being rejected (e.g. "CONTAINS_NON_WHITELISTED_DOMAIN"), to then be processed by the mod.
    private final String reason;

    // The index in the sent message that the offending content starts at.
    @SerializedName("start_index")
    private final int startIndex;

    // The index in the sent message that the offending content ends at (exclusive).
    @SerializedName("end_index")
    private final int endIndex;

    public ServerChatChannelMessageRejectedPacket(String reason, int startIndex, int endIndex) {
        this.reason = reason;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public String getReason() {
        return this.reason;
    }

    public int getStartIndex() {
        return this.startIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }
}
