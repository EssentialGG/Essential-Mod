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
package gg.essential.network.connectionmanager;

import com.google.common.collect.Maps;
import com.sparkuniverse.toolbox.relationships.enums.FriendRequestPrivacySetting;
import com.sparkuniverse.toolbox.relationships.enums.RelationshipState;
import com.sparkuniverse.toolbox.relationships.enums.RelationshipType;
import com.sparkuniverse.toolbox.relationships.serialisation.FriendRequestPrivacySettingTypeAdapter;
import com.sparkuniverse.toolbox.relationships.serialisation.RelationshipStateAdapter;
import com.sparkuniverse.toolbox.relationships.serialisation.RelationshipTypeAdapter;
import com.sparkuniverse.toolbox.serialization.DateTimeTypeAdapter;
import com.sparkuniverse.toolbox.serialization.UUIDTypeAdapter;
import com.sparkuniverse.toolbox.util.DateTime;
import gg.essential.Essential;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.connectionmanager.common.packet.connection.ConnectionRegisterPacketTypeIdPacket;
import gg.essential.lib.gson.Gson;
import gg.essential.lib.gson.GsonBuilder;
import gg.essential.lib.gson.JsonParseException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ConnectionCodec {
    private static final String PACKET_PACKAGE = "gg.essential.connectionmanager.common.packet.";
    private static final boolean LOG_PACKETS = System.getProperty("essential.logPackets", "false").equals("true");

    private final Consumer<Connection.IOConsumer<PrintStream>> log;

    @NotNull
    private final AtomicInteger packetTypeId = new AtomicInteger();
    @NotNull
    private final Map<Integer, String> incomingPacketTypeIds = Maps.newConcurrentMap();
    @NotNull
    private final Map<String, Integer> outgoingPacketTypeIds = Maps.newConcurrentMap();

    @NotNull
    public static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
        .registerTypeAdapter(RelationshipType.class, new RelationshipTypeAdapter())
        .registerTypeAdapter(RelationshipState.class, new RelationshipStateAdapter())
        .registerTypeAdapter(FriendRequestPrivacySetting.class, new FriendRequestPrivacySettingTypeAdapter())
        .registerTypeAdapter(DateTime.class, new DateTimeTypeAdapter())
        .create();

    {
        final String packetName = this.splitPacketPackage(ConnectionRegisterPacketTypeIdPacket.class);
        this.incomingPacketTypeIds.put(0, packetName);
        this.outgoingPacketTypeIds.put(packetName, 0);
    }

    ConnectionCodec(Consumer<Connection.IOConsumer<PrintStream>> log) {
        this.log = log;
    }

    @Nullable
    public Packet decode(byte[] array) {
        final Packet packet;
        try (
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(array);
            final DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)
        ) {
            final int packetTypeId = dataInputStream.readInt();
            final String packetName = this.incomingPacketTypeIds.get(packetTypeId);

            if (packetName == null) {
                Essential.logger.warn("Unknown packet type id {} from connection manager.", packetTypeId);
                log.accept(out -> out.printf("-- protocol error: unknown packet type id %d --\n", packetTypeId));
                return null;
            }

            Class<? extends Packet> packetClass;
            try {
                packetClass = (Class<? extends Packet>) Class.forName(PACKET_PACKAGE + packetName);
            } catch (final ClassNotFoundException e) {
                packetClass = UnknownPacket.class;
            }

            final String packetIdString = this.readString(dataInputStream);
            UUID packetId = null;
            if (!StringUtils.isEmpty(packetIdString)) {
                packetId = UUID.fromString(packetIdString);
            }

            final String jsonString = this.readString(dataInputStream);

            if (LOG_PACKETS) {
                Essential.debug.info("IN " + packetId + " - " + packetName + " " + jsonString);
            }
            log.accept(out -> out.printf("{\"type\": \"RECV\", \"name\": \"%s\", \"payload\": %s, \"id\": \"%s\"}\n", packetName, jsonString, packetIdString));
            try {
                packet = gson.fromJson(jsonString, packetClass);
            } catch (final JsonParseException e) {
                Essential.logger.error("Error when deserialising json '{}' for '{}'.", jsonString, packetClass, e);
                log.accept(out -> out.print("-- protocol error: failed to parse above json --\n"));
                return null;
            }

            if (packetId != null) {
                packet.setUniqueId(packetId);
            }
        } catch (final IOException e) {
            Essential.logger.error("Error when reading byte buffer data '{}'.", array, e);
            log.accept(out -> out.printf("-- protocol error: failed to read %s --\n", Arrays.toString(array)));
            return null;
        }

        if (packet instanceof ConnectionRegisterPacketTypeIdPacket) {
            ConnectionRegisterPacketTypeIdPacket p = (ConnectionRegisterPacketTypeIdPacket) packet;
            this.incomingPacketTypeIds.put(p.getPacketId(), p.getClassName());
            return null;
        }

        return packet;
    }

    public void encode(Packet packet, Consumer<byte[]> send) {
        String packetName = splitPacketPackage(packet.getClass());
        final int packetTypeId = this.outgoingPacketTypeIds.computeIfAbsent(
            packetName,
            packetName_ -> {
                final int newId = this.packetTypeId.incrementAndGet();

                encode(new ConnectionRegisterPacketTypeIdPacket(packetName, newId), send);
                return newId;
            }
        );

        final UUID packetId = packet.getPacketUniqueId();

        String packetJson = gson.toJson(packet);
        byte[] packetBytes = packetJson.getBytes(StandardCharsets.UTF_8);
        String packetIdString = packetId != null ? packetId.toString() : "";
        byte[] packetIdBytes = packetIdString.getBytes(StandardCharsets.UTF_8);

        if (LOG_PACKETS) {
            Essential.debug.info("OUT " + packetId + " - " + packetName + " " + packetJson);
        }
        log.accept(out -> out.printf("{\"type\": \"SEND\", \"name\": \"%s\", \"payload\": %s, \"id\": \"%s\"}\n", packetName, packetJson, packetIdString));
        try (
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)
        ) {
            dataOutputStream.writeInt(packetTypeId);
            dataOutputStream.writeInt(packetIdBytes.length);
            dataOutputStream.write(packetIdBytes);
            dataOutputStream.writeInt(packetBytes.length);
            dataOutputStream.write(packetBytes);

            send.accept(byteArrayOutputStream.toByteArray());
        } catch (final IOException e) {
            Essential.logger.error("Error occurred when sending out packet '{}'.", packet, e);
        }
    }

    @NotNull
    private String readString(@NotNull final DataInputStream dataInputStream) throws IOException {
        final byte[] bytes = new byte[dataInputStream.readInt()];
        dataInputStream.read(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @NotNull
    private String splitPacketPackage(@NotNull final Class<? extends Packet> packetClass) {
        return packetClass.getName().replace(PACKET_PACKAGE, "");
    }
}
