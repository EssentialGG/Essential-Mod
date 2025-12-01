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
import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class ServerRecognizedTelemetryPacket extends Packet {

    @SerializedName("accepted_telemetry_categories")
    @Nullable
    private Set<String> acceptedTelemetryCategories = null;

    @SerializedName("recognized_telemetry")
    @NotNull
    private final List<String> recognizedTelemetry;

    public ServerRecognizedTelemetryPacket(@Nullable final Set<String> acceptedTelemetryCategories, @NotNull final List<String> recognizedTelemetry) {
        this.acceptedTelemetryCategories = acceptedTelemetryCategories;
        this.recognizedTelemetry = recognizedTelemetry;
    }

    public ServerRecognizedTelemetryPacket(@NotNull final List<String> recognizedTelemetry) {
        this.recognizedTelemetry = recognizedTelemetry;
    }

    @NotNull
    public List<String> getRecognizedTelemetry() {
        return this.recognizedTelemetry;
    }

    @Nullable
    public Set<String> getAcceptedTelemetryCategories() {
        return this.acceptedTelemetryCategories;
    }
}
