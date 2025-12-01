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

import org.jetbrains.annotations.NotNull;

public class Product {
    // COSMETIC, CURRENCY.
    @NotNull
    private final String type;

    // For cosmetics, it will be the cosmetic ID.
    // For coin redemptions, it will be `coins`.
    @NotNull
    private final String key;

    // The amount of the item granted to the user, equal to 1 for cosmetics.
    // Equal to the amount of the currency for currency products.
    private final int amount;

    // The state that the product is in, i.e. whether the user already owned it.
    @NotNull
    private final ProductRedemptionState state;

    public Product(@NotNull String type, @NotNull String key, int amount, @NotNull ProductRedemptionState state) {
        this.type = type;
        this.key = key;
        this.amount = amount;
        this.state = state;
    }

    public @NotNull String getType() {
        return type;
    }

    public @NotNull String getKey() {
        return key;
    }

    public int getAmount() {
        return amount;
    }

    public @NotNull ProductRedemptionState getState() {
        return state;
    }
}
