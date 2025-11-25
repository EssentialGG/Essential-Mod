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

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.Nullable;

public class ClientChatChannelMessagesRetrievePacket extends Packet {

    @SerializedName("a")
    private final long channelId;

    @SerializedName("b")
    private final Long before;

    @SerializedName("c")
    private final Long after;

    @SerializedName("d")
    private final int limit;

    public ClientChatChannelMessagesRetrievePacket(
            final long channelId, final Long before, final Long after, final int limit
    ) {
        this.channelId = channelId;
        this.before = before;
        this.after = after;
        this.limit = limit;
    }

    public long getChannelId() {
        return this.channelId;
    }

    @Nullable
    public Long getBefore() {
        return this.before;
    }

    @Nullable
    public Long getAfter() {
        return this.after;
    }

    public int getLimit() {
        return this.limit;
    }

}
