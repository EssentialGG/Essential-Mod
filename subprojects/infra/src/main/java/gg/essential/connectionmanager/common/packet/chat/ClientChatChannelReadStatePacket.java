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
import org.jetbrains.annotations.Nullable;

public class ClientChatChannelReadStatePacket extends Packet {
    @SerializedName("channel_id")
    private final long channelId;

    @SerializedName("last_read_message_id")
    private final @Nullable Long lastReadMessageId;

    public ClientChatChannelReadStatePacket(long channelId, @Nullable Long lastReadMessageId) {
        this.channelId = channelId;
        this.lastReadMessageId = lastReadMessageId;
    }

    public long getChannelId() {
        return channelId;
    }

    public @Nullable Long getLastReadMessageId() {
        return lastReadMessageId;
    }
}