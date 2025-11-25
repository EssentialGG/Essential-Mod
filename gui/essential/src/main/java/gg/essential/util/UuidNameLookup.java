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
import gg.essential.lib.gson.Gson;
import gg.essential.lib.gson.JsonElement;
import gg.essential.lib.gson.JsonObject;
import gg.essential.lib.gson.JsonParser;
import gg.essential.network.CMConnection;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.Dispatchers;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import static gg.essential.util.EssentialGuiExtensionsKt.toState;
import static kotlinx.coroutines.ExecutorsKt.asExecutor;

public class UuidNameLookup {

    private static final String UUID_TO_NAME_API = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String NAME_TO_UUID_API = "https://api.mojang.com/users/profiles/minecraft/";

    // Stores any successful or in progress loading futures
    private static final ConcurrentHashMap<UUID, CompletableFuture<String>> uuidLoadingFutures = new ConcurrentHashMap<>();

    // Stores any successful or in progress loading futures
    private static final ConcurrentHashMap<String, CompletableFuture<UUID>> nameLoadingFutures = new ConcurrentHashMap<>();

    private static Profile fetchProfile(String apiAddress) throws PlayerNotFoundException, RateLimitException, IOException {
        Request request = new Request.Builder().url(apiAddress).header("Content-Type", "application/json").build();

        try(Response response = HttpUtils.getHttpClient().join().newCall(request).execute()) {
            String json = response.body() != null ? response.body().string() : null;

            switch (response.code()) {
                case 204:
                case 404:
                    throw new PlayerNotFoundException("Player not found");
                case 429:
                    throw new RateLimitException("Rate limit exceeded");
                default:
                    if (json == null) {
                        throw new APIException("Failed to load profile: No response body");
                    }
            }

            JsonElement jsonElement = JsonParser.parseString(json);
            if (!jsonElement.isJsonObject()) {
                throw new APIException("Failed to load profile: Invalid response");
            }

            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject.has("errorMessage")) {
                throw new APIException("Failed to load profile: " + jsonObject.get("errorMessage").getAsString());
            }

            return (new Gson()).fromJson(jsonObject, Profile.class);
        }
    }

    public static Profile fetchProfileFromUsername(String username) throws PlayerNotFoundException, RateLimitException, IOException {
        return fetchProfile(NAME_TO_UUID_API + username);
    }

    public static Profile fetchProfileFromUUID(UUID uuid) throws PlayerNotFoundException, RateLimitException, IOException {
        return fetchProfile(UUID_TO_NAME_API + uuid.toString().replaceAll("-", ""));
    }

    public static CompletableFuture<String> getName(UUID uuid) {
        return uuidLoadingFutures.computeIfAbsent(uuid, ignored1 -> CompletableFuture.supplyAsync(() -> {
            try {
                Profile profile = fetchProfileFromUUID(uuid);
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
                            Profile profile = fetchProfileFromUsername(nameLower);
                            UUID loadedUuid = UUID.fromString(
                                new StringBuilder(profile.getId())
                                    .insert(20, '-')
                                    .insert(16, '-')
                                    .insert(12, '-')
                                    .insert(8, '-')
                                    .toString()
                            );
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

    public class Property {
        private String name;
        private String value;

        public String getName() {
            return this.name;
        }

        public String getValue() {
            return this.value;
        }
    }

    public static class Profile {
        private String id;
        private String name;
        private List<Property> properties;

        public String getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public List<Property> getProperties() {
            return this.properties;
        }
    }
}
