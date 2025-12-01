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
package gg.essential.connectionmanager.common.packet.store;

import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class ServerRedeemStoreClaimResponsePacket extends Packet {

    @NotNull
    private final List<Product> products;

    // Shown to the user as the first line of the modal, e.g. {"en_us": "Congratulations!"}.
    @NotNull
    @SerializedName("redemption_message")
    private final Map<String, String> redemptionMessage;

    public ServerRedeemStoreClaimResponsePacket(@NotNull List<Product> products, @NotNull Map<String, String> redemptionMessage) {
        this.products = products;
        this.redemptionMessage = redemptionMessage;
    }

    public @NotNull List<Product> getProducts() {
        return products;
    }

    public @NotNull Map<String, String> getRedemptionMessage() {
        return redemptionMessage;
    }
}
