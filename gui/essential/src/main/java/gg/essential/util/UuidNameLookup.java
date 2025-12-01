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
package gg.essential.util;

import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.connectionmanager.common.packet.relationships.ClientLookupUuidByNamePacket;
import gg.essential.connectionmanager.common.packet.relationships.ServerLookupUuidByNameResponsePacket;
import gg.essential.elementa.state.BasicState;
import gg.essential.gui.common.ReadOnlyState;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.network.CMConnection;
import gg.essential.network.mojang.MojangNameToUuidApi;
import gg.essential.network.mojang.MojangProfileLookupApi;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.Dispatchers;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import static gg.essential.util.EssentialGuiExtensionsKt.toState;
import static kotlinx.coroutines.ExecutorsKt.asExecutor;

public class UuidNameLookup {

    // Stores any successful or in progress loading futures
    private static final ConcurrentHashMap<UUID, CompletableFuture<String>> uuidLoadingFutures = new ConcurrentHashMap<>();

    // Stores any successful or in progress loading futures
    private static final ConcurrentHashMap<String, CompletableFuture<UUID>> nameLoadingFutures = new ConcurrentHashMap<>();

    public static CompletableFuture<String> getName(UUID uuid) {
        return uuidLoadingFutures.computeIfAbsent(uuid, ignored1 -> CompletableFuture.supplyAsync(() -> {
            try {
                MojangProfileLookupApi.Profile profile = MojangProfileLookupApi.INSTANCE.fetchBlocking(uuid);
                if (profile == null) {
                    throw new PlayerNotFoundException("Player not found");
                }
                nameLoadingFutures.put(profile.getName().toLowerCase(Locale.ROOT), CompletableFuture.completedFuture(uuid));
                return profile.getName();
            } catch (Exception e) {
                // Delete cache so we can try again next call
                uuidLoadingFutures.remove(uuid);

                // Throw exception so future is completed with exception
                throw new CompletionException("Failed to load name", e);
            }
        }, asExecutor(Dispatchers.getIO())));
    }

    public static CompletableFuture<UUID> getUUID(String userName) {
        return nameLoadingFutures.computeIfAbsent(userName.toLowerCase(Locale.ROOT), nameLower -> {
            CompletableFuture<UUID> future = new CompletableFuture<>();
            CMConnection cmConnection = GuiEssentialPlatform.Companion.getPlatform().getCmConnection();
            cmConnection.send(new ClientLookupUuidByNamePacket(nameLower), maybeResponse -> {
                Packet response = maybeResponse.orElse(null);
                if (response instanceof ServerLookupUuidByNameResponsePacket) {
                    ServerLookupUuidByNameResponsePacket p = (ServerLookupUuidByNameResponsePacket) response;
                    uuidLoadingFutures.put(p.getUuid(), CompletableFuture.completedFuture(p.getUsername()));
                    future.complete(p.getUuid());
                } else {
                    Dispatchers.getIO().dispatch(EmptyCoroutineContext.INSTANCE, () -> {
                        try {
                            MojangNameToUuidApi.Profile profile = MojangNameToUuidApi.INSTANCE.fetchBlocking(nameLower);
                            if (profile == null) {
                                throw new PlayerNotFoundException("Player not found");
                            }
                            UUID loadedUuid = profile.getId();
                            uuidLoadingFutures.put(loadedUuid, CompletableFuture.completedFuture(profile.getName()));
                            future.complete(loadedUuid);
                        } catch (Exception e) {
                            // Delete cache so we can try again next call
                            nameLoadingFutures.remove(nameLower);

                            // Throw exception so future is completed with exception
                            future.completeExceptionally(new CompletionException("Failed to load UUID", e));
                        }
                    });
                }
            });
            return future;
        });
    }

    public static void populate(String username, UUID uuid) {
        uuidLoadingFutures.computeIfAbsent(uuid, k -> new CompletableFuture<>()).complete(username);
        nameLoadingFutures.computeIfAbsent(username.toLowerCase(Locale.ROOT), k -> new CompletableFuture<>()).complete(uuid);
    }

    @Deprecated // This uses StateV1, use `nameState` instead.
    public static ReadOnlyState<String> getNameAsState(UUID uuid) {
        return getNameAsState(uuid, "");
    }

    @Deprecated // This uses StateV1, use `nameState` instead.
    public static ReadOnlyState<String> getNameAsState(UUID uuid, String initialValue) {
        final BasicState<String> state = new BasicState<>(initialValue);
        getName(uuid).thenAcceptAsync(state::set, asExecutor(DispatchersKt.getClient(Dispatchers.INSTANCE)));
        return new ReadOnlyState<>(state);
    }

    public static State<String> nameState(UUID uuid) {
        return nameState(uuid, "");
    }

    public static State<String> nameState(UUID uuid, String initialValue) {
        State<String> nullableState = toState(getName(uuid));
        return observer -> {
            String value = nullableState.get(observer);
            if (value == null) {
                value = initialValue;
            }
            return value;
        };
    }
}
