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
package gg.essential.connectionmanager.common.packet.wardrobe;

import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.model.EssentialAsset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerWardrobeSettingsPacket extends Packet {

    @SerializedName("outfits_limit")
    private final int outfitsLimit;

    @SerializedName("skins_limit")
    private final int skinsLimit;

    @SerializedName("gifting_coin_spend_requirement")
    private final int giftingCoinSpendRequirement;

    @SerializedName("fallback_featured_page_config")
    private final @NotNull EssentialAsset fallbackFeaturedPageConfig;

    @SerializedName("current_featured_page_config")
    private final @Nullable EssentialAsset currentFeaturedPageConfig;

    @SerializedName("you_need_minimum_amount")
    private final @Nullable Integer youNeedMinimumAmount;

    public ServerWardrobeSettingsPacket(int outfitsLimit, int skinsLimit, int giftingCoinSpendRequirement, @NotNull EssentialAsset fallbackFeaturedPageConfig, @Nullable EssentialAsset currentFeaturedPageConfig, @Nullable Integer youNeedMinimumAmount) {
        this.outfitsLimit = outfitsLimit;
        this.skinsLimit = skinsLimit;
        this.giftingCoinSpendRequirement = giftingCoinSpendRequirement;
        this.fallbackFeaturedPageConfig = fallbackFeaturedPageConfig;
        this.currentFeaturedPageConfig = currentFeaturedPageConfig;
        this.youNeedMinimumAmount = youNeedMinimumAmount;
    }

    public int getOutfitsLimit() {
        return outfitsLimit;
    }

    public int getSkinsLimit() {
        return skinsLimit;
    }

    public int getGiftingCoinSpendRequirement() {
        return giftingCoinSpendRequirement;
    }

    public @NotNull EssentialAsset getFallbackFeaturedPageConfig() {
        return fallbackFeaturedPageConfig;
    }

    public @Nullable EssentialAsset getCurrentFeaturedPageConfig() {
        return currentFeaturedPageConfig;
    }

    public int getYouNeedMinimumAmount() {
        return youNeedMinimumAmount != null ? youNeedMinimumAmount : 100;
    }
}
