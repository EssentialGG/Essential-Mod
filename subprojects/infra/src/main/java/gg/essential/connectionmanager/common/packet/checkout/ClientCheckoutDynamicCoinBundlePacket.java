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
package gg.essential.connectionmanager.common.packet.checkout;

import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Currency;

public class ClientCheckoutDynamicCoinBundlePacket extends Packet {

    private final int coins;

    private final @NotNull Currency currency;

    @SerializedName("partner_code")
    private final @Nullable String partnerCode;

    public ClientCheckoutDynamicCoinBundlePacket(int coins, @NotNull Currency currency, @Nullable String partnerCode) {
        this.coins = coins;
        this.currency = currency;
        this.partnerCode = partnerCode;
    }

    public int getCoins() {
        return coins;
    }

    public @NotNull Currency getCurrency() {
        return currency;
    }

    public @Nullable String getPartnerCode() {
        return partnerCode;
    }

}
