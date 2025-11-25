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
package com.sparkuniverse.toolbox.chat.model;

import gg.essential.lib.gson.annotations.SerializedName;
import com.sparkuniverse.toolbox.chat.enums.ChannelType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public final class Channel {

    @SerializedName("a")
    private final long id;

    @SerializedName("b")
    @NotNull
    private final ChannelType type;

    @SerializedName("c")
    @NotNull
    private String name;

    @SerializedName("d")
    @Nullable
    private String topic;

    @SerializedName("e")
    @Nullable
    private final ChannelSettings settings;

    @SerializedName("f")
    @NotNull
    private final Set<UUID> members;

    @SerializedName("g")
    @NotNull
    private final CreatedInfo createdInfo;

    @SerializedName("h")
    @Nullable
    private final ClosedInfo closedInfo;

    @SerializedName("i")
    private boolean muted;

    @SerializedName("joined_at")
    public final long joinedAt;

    @SerializedName("last_read_message_id")
    private @Nullable Long lastReadMessageId;

    @SerializedName("unread_messages")
    private int unreadMessages;

    public Channel(
            final long id, @NotNull final ChannelType type, @NotNull final String name, @Nullable final String topic,
            @Nullable final ChannelSettings settings, @NotNull final Set<UUID> members,
            @NotNull final CreatedInfo createdInfo, @Nullable final ClosedInfo closedInfo,
            final boolean muted, final long joinedAt, @Nullable Long lastReadMessageId, int unreadMessages
    ) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.topic = topic;
        this.settings = settings;
        this.members = members;
        this.createdInfo = createdInfo;
        this.closedInfo = closedInfo;
        this.muted = muted;
        this.joinedAt = joinedAt;
        this.lastReadMessageId = lastReadMessageId;
        this.unreadMessages = unreadMessages;
    }

    public long getId() {
        return this.id;
    }

    @NotNull
    public ChannelType getType() {
        return this.type;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @Nullable
    public String getTopic() {
        return this.topic;
    }

    @Nullable
    public ChannelSettings getSettings() {
        return this.settings;
    }

    @NotNull
    public Set<UUID> getMembers() {
        return this.members;
    }

    @NotNull
    public CreatedInfo getCreatedInfo() {
        return this.createdInfo;
    }

    public boolean isMuted() {
        return this.muted;
    }

    public long getJoinedAt() {
        return this.joinedAt;
    }

    public @Nullable Long getLastReadMessageId() {
        return lastReadMessageId;
    }

    public int getUnreadMessages() {
        return unreadMessages;
    }

    public void setName(@NotNull final String name) {
        this.name = name;
    }

    public void setTopic(@Nullable final String topic) {
        this.topic = topic;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public void setLastReadMessageId(@Nullable Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }

    public void setUnreadMessages(int unreadMessages) {
        this.unreadMessages = unreadMessages;
    }
}
