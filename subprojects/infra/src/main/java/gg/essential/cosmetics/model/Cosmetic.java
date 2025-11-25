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
package gg.essential.cosmetics.model;

import gg.essential.lib.gson.annotations.SerializedName;
import com.sparkuniverse.toolbox.util.DateTime;
import gg.essential.cosmetics.SkinLayer;
import gg.essential.cosmetics.holder.PriceHolder;
import gg.essential.cosmetics.holder.SkinLayersHolder;
import gg.essential.holder.DisplayNameHolder;
import gg.essential.model.EssentialAsset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class Cosmetic implements DisplayNameHolder, PriceHolder, SkinLayersHolder {

    @SerializedName("a")
    private final @NotNull String id;

    @SerializedName("b")
    private final @NotNull String type;

    @SerializedName("c")
    private final @NotNull Map<@NotNull String, @NotNull String> displayNames;

    @SerializedName("f")
    private final int storePackageId;

    @SerializedName("g")
    private @Nullable Map<@NotNull String, @NotNull Double> prices;

    @SerializedName("h")
    private @Nullable Set<@NotNull String> tags;

    @SerializedName("i")
    private final @NotNull DateTime createdAt;

    @SerializedName("j")
    private final @Nullable DateTime availableAfter;

    @SerializedName("k")
    private final @Nullable DateTime availableUntil;

    @SerializedName("show_timer_after")
    private final @Nullable DateTime showTimerAfter;

    @SerializedName("l")
    private @Nullable Map<@NotNull SkinLayer, @NotNull Boolean> skinLayers;

    @SerializedName("m")
    private @Nullable Map<@NotNull String, @NotNull Integer> categories;

    @SerializedName("n")
    private final @Nullable Integer defaultSortWeight;

    @SerializedName("o")
    private @Nullable Integer priceCoins;

    @SerializedName("p")
    private @Nullable CosmeticTier tier;

    @SerializedName("q")
    private @NotNull Map<@NotNull String, @NotNull EssentialAsset> assetsMap;

    public Cosmetic(
            final @NotNull String id,
            final @NotNull String type,
            final @NotNull Map<@NotNull String, @NotNull String> displayNames,
            final int storePackageId,
            final @Nullable Map<@NotNull String, @NotNull Double> prices,
            final @Nullable Set<@NotNull String> tags,
            final @NotNull DateTime createdAt,
            final @Nullable DateTime availableAfter,
            final @Nullable DateTime availableUntil,
            final @Nullable DateTime showTimerAfter,
            final @Nullable Map<@NotNull SkinLayer, @NotNull Boolean> skinLayers,
            final @Nullable Map<@NotNull String, @NotNull Integer> categories,
            final @Nullable Integer defaultSortWeight,
            final @Nullable Integer priceCoins,
            final @Nullable CosmeticTier tier,
            final @NotNull Map<@NotNull String, @NotNull EssentialAsset> assetsMap
    ) {
        this.id = id;
        this.type = type;
        this.displayNames = displayNames;
        this.storePackageId = storePackageId;
        this.prices = prices;
        this.tags = tags;
        this.createdAt = createdAt;
        this.availableAfter = availableAfter;
        this.availableUntil = availableUntil;
        this.showTimerAfter = showTimerAfter;
        this.skinLayers = skinLayers;
        this.categories = categories;
        this.defaultSortWeight = defaultSortWeight;
        this.priceCoins = priceCoins;
        this.tier = tier;
        this.assetsMap = assetsMap;
    }

    public @NotNull String getId() {
        return this.id;
    }

    public @NotNull String getType() {
        return this.type;
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull String> getDisplayNames() {
        return this.displayNames;
    }

    public int getStorePackageId() {
        return this.storePackageId;
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull Double> getPrices() {
        if (this.prices == null) {
            this.prices = Collections.emptyMap();
        }

        return this.prices;
    }

    public @NotNull Set<@NotNull String> getTags() {
        if (this.tags == null) {
            this.tags = Collections.emptySet();
        }

        return this.tags;
    }

    public @NotNull DateTime getCreatedAt() {
        return this.createdAt;
    }

    public @Nullable DateTime getAvailableAfter() {
        return this.availableAfter;
    }

    public @Nullable DateTime getAvailableUntil() {
        return this.availableUntil;
    }

    public @Nullable DateTime getShowTimerAfter() {
        return this.showTimerAfter;
    }

    public @NotNull Map<@NotNull SkinLayer, @NotNull Boolean> getSkinLayers() {
        if (this.skinLayers == null) {
            this.skinLayers = Collections.emptyMap();
        }

        return this.skinLayers;
    }

    public @NotNull Map<@NotNull String, @NotNull Integer> getCategories() {
        if (this.categories == null) {
            this.categories = Collections.emptyMap();
        }

        return categories;
    }

    public @Nullable Integer getDefaultSortWeight() {
        return this.defaultSortWeight;
    }

    public @Nullable Integer getPriceCoins() {
        return priceCoins;
    }

    public @Nullable CosmeticTier getTier() {
        return tier;
    }

    public @NotNull Map<@NotNull String, @NotNull EssentialAsset> getAssetsMap() {
        return assetsMap;
    }
}
