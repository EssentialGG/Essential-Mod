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
package gg.essential.network.connectionmanager.handler.chat;

import com.sparkuniverse.toolbox.chat.enums.ChannelType;
import com.sparkuniverse.toolbox.chat.model.Channel;
import com.sparkuniverse.toolbox.chat.model.Message;
import gg.essential.api.gui.Slot;
import gg.essential.config.EssentialConfig;
import gg.essential.connectionmanager.common.packet.chat.ServerChatChannelMessagePacket;
import gg.essential.gui.friends.SocialMenu;
import gg.essential.gui.notification.Notifications;
import gg.essential.mod.Model;
import gg.essential.mod.Skin;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.chat.ChatManager;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import gg.essential.universal.USound;
import gg.essential.util.CachedAvatarImage;
import gg.essential.util.ExtensionsKt;
import gg.essential.util.GuiUtil;
import gg.essential.util.UUIDUtil;
import kotlin.Unit;
import net.minecraft.client.Minecraft;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static gg.essential.gui.skin.SkinUtilsKt.showSkinReceivedToast;
import static gg.essential.util.ChannelExtensionsKt.getOtherUser;
import static gg.essential.util.ExtensionsKt.getExecutor;

public class ServerChatChannelMessagePacketHandler extends PacketHandler<ServerChatChannelMessagePacket> {

    @Override
    protected void onHandle(@NotNull final ConnectionManager connectionManager, @NotNull final ServerChatChannelMessagePacket packet) {
        final ChatManager chatManager = connectionManager.getChatManager();

        List<@NotNull Message> sortedMessages = Arrays.stream(packet.getMessages()).sorted(Comparator.comparing(message -> ((Message) message).getCreatedAt()).reversed()).collect(Collectors.toList());
        for (@NotNull final Message message : sortedMessages) {
            final Optional<Channel> channelOptional = chatManager.getChannel(message.getChannelId());
            if (!channelOptional.isPresent()) {
                return;
            }

            boolean isFromHistoryRequest = packet.getPacketUniqueId() != null;
            final Channel channel = channelOptional.get();

            // Upsert the message and cancel the toast if the message already existed
            // and therefore this is an edit
            if (chatManager.upsertMessageToChannel(channel.getId(), message, isFromHistoryRequest)) {
                continue;
            }

            // Avoid sending toasts:
            // - from messages the user sent
            // - messages that are read
            // - from muted channels
            // - if we are prefetching
            // - if essential is not enabled
            final boolean isRead;
            if (connectionManager.getUsingProtocol() >= 9) {
                Long lastReadMessageId = channel.getLastReadMessageId();
                if (lastReadMessageId == null) {
                    isRead = false;
                } else {
                    isRead = channel.getLastReadMessageId() >= message.getId();
                }
            } else {
                isRead = message.isRead();
            }
            if (isRead ||
                    message.getSender().equals(UUIDUtil.getClientUUID()) ||
                    channel.isMuted() ||
                    isFromHistoryRequest ||
                    !EssentialConfig.INSTANCE.getEssentialFull()
            ) continue;

            HttpUrl url = HttpUrl.parse(message.getContents());
            if (url != null && url.host().equals("essential.gg")) {
                List<String> pathSegments = url.pathSegments();
                if (pathSegments.size() > 2 && pathSegments.get(0).equals("skin")) {
                    Skin skin = new Skin(pathSegments.get(2), Model.byVariantOrDefault(pathSegments.get(1)));
                    UUID uuid = message.getSender();
                    UUIDUtil.getName(uuid).thenAcceptAsync(name -> showSkinReceivedToast(skin, uuid, name, channel), getExecutor(Minecraft.getMinecraft()));
                    continue;
                } else if (pathSegments.size() > 1 && pathSegments.get(0).equals("gift")) {
                    // Don't show toasts for gift embeds; they're handled in GiftedCosmeticNoticeListener
                    continue;
                }
            }

            boolean notification = !(GuiUtil.INSTANCE.openedScreen() instanceof SocialMenu);

            if (notification) {
                final UUID uuid = channel.getType() == ChannelType.DIRECT_MESSAGE ? getOtherUser(channel) : message.getSender();
                UUIDUtil.getName(uuid).thenAcceptAsync(new NotificationHandler(channel, message), getExecutor(Minecraft.getMinecraft()));
            }
        }
    }

    /**
     * Cannot use lambda because the compiler explodes
     */
    static class NotificationHandler implements Consumer<String> {

        private final Channel channel;
        private final Message message;

        NotificationHandler(Channel channel, Message message) {
            this.channel = channel;
            this.message = message;
        }

        @Override
        public void accept(String name) {
            boolean dm = channel.getType() == ChannelType.DIRECT_MESSAGE;

            if((dm && EssentialConfig.INSTANCE.getMessageReceivedNotifications()) || (!dm && EssentialConfig.INSTANCE.getGroupMessageReceivedNotifications())) {
                String notificationTitle = dm ? name : String.format(Locale.ROOT, "%s [%s]", name, channel.getName());

                if (EssentialConfig.INSTANCE.getMessageSound() && !EssentialConfig.INSTANCE.getStreamerMode()) {
                    USound.INSTANCE.playExpSound();
                }
               
                Notifications.INSTANCE.push(
                        notificationTitle,
                        message.getContents(),
                        4f,
                        () -> {
                            GuiUtil.openScreen(SocialMenu.class, () -> new SocialMenu(channel.getId()));
                            return Unit.INSTANCE;
                        },
                        () -> Unit.INSTANCE,
                        (notificationBuilder) -> {
                            notificationBuilder.setTrimTitle(true);
                            notificationBuilder.setTrimMessage(true);

                            notificationBuilder.withCustomComponent(Slot.ICON, CachedAvatarImage.create(message.getSender()));

                            return Unit.INSTANCE;
                        }
                );
            }
        }
    }
}
