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
package gg.essential.connectionmanager.common.packet.telemetry;

import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ClientTelemetryPacket extends Packet {

    @NotNull
    private final String key;

    @NotNull
    private final Map<String, Object> metadata;

    public ClientTelemetryPacket(@NotNull final String key) {
        this(key, Collections.emptyMap());
    }

    public ClientTelemetryPacket(@NotNull final String key, @NotNull final Map<String, Object> metadata) {
        this.key = key;
        this.metadata = metadata;
    }

    @NotNull
    public String getKey() {
        return this.key;
    }

    @NotNull
    public Map<String, Object> getMetadata() {
        return this.metadata;
    }

    public static ClientTelemetryPacket forAction(String name) {
        return forAction(name, null);
    }

    public static ClientTelemetryPacket forAction(String name, String context) {
        return new ClientTelemetryPacket("CLIENT_ACTION", new HashMap<String, Object>() {{
            put("action", name);
            put("context", context == null ? "" : context); // Null context is sent as empty string due to schema restrictions on the CM
        }});
    }

}
