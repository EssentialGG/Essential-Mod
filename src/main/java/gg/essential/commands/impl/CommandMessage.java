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
package gg.essential.commands.impl;

import com.google.common.collect.ImmutableSet;
import com.sparkuniverse.toolbox.chat.model.Message;
import gg.essential.api.commands.*;
import gg.essential.commands.engine.EssentialFriend;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.connectionmanager.common.packet.chat.ServerChatChannelMessagePacket;
import gg.essential.util.MinecraftUtils;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class CommandMessage extends Command {

    public CommandMessage() {
        super("essentialmessage");
    }

    @Nullable
    @Override
    public Set<Alias> getCommandAliases() {
        return ImmutableSet.of(new Alias("emsg"));
    }

    @DefaultHandler
    public void handle(@DisplayName("ign")EssentialFriend friend, @DisplayName("message") @Greedy String message) throws ExecutionException, InterruptedException {
        CommandMessageKt.handleMessageCommand(friend, message, new EarlyResponseHandler(friend));
    }

    public void onConfirm(String message) {
        MinecraftUtils.INSTANCE.sendMessage(message);
    }

    private class EarlyResponseHandler implements gg.essential.network.connectionmanager.EarlyResponseHandler {

        private final EssentialFriend friend;

        public EarlyResponseHandler(EssentialFriend friend) {
            this.friend = friend;
        }

        @Override
        public void accept(Optional<Packet> packet) {
            if (packet.isPresent() && packet.get() instanceof ServerChatChannelMessagePacket) {
                Message messageObj = CollectionsKt.first(Arrays.asList(((ServerChatChannelMessagePacket) packet.get()).getMessages()));
                onConfirm("§dTo " + friend.getIgn() + "§r: " + messageObj.getContents());
            } else {
                onConfirm("Error!");
            }
        }

        @NotNull
        @Override
        public Consumer<Optional<Packet>> andThen(@NotNull Consumer<? super Optional<Packet>> after) {
            return packet -> {};
        }
    }
}
