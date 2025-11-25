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

import gg.essential.cosmetics.CosmeticSlot;
import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CosmeticStoreBundle {

    private final @NotNull String id;

    private final @NotNull String name;

    private final @Nullable CosmeticStoreBundleSkin skin;

    private final @NotNull CosmeticTier tier;

    private final float discount;

    @SerializedName("rotate_on_preview")
    private final boolean rotateOnPreview;

    private final @NotNull Map<@NotNull CosmeticSlot, @NotNull String> cosmetics;

    private final @NotNull Map<@NotNull String, @NotNull List<@NotNull CosmeticSetting>> settings;


    public CosmeticStoreBundle(
        @NotNull String id,
        @NotNull String name,
        @Nullable CosmeticStoreBundleSkin skin,
        @NotNull CosmeticTier tier,
        float discount,
        boolean rotateOnPreview,
        @NotNull Map<@NotNull CosmeticSlot, @NotNull String> cosmetics,
        @NotNull Map<@NotNull String, @NotNull List<@NotNull CosmeticSetting>> settings
    ) {
        this.id = id;
        this.name = name;
        this.skin = skin;
        this.tier = tier;
        this.discount = discount;
        this.rotateOnPreview = rotateOnPreview;
        this.cosmetics = cosmetics;
        this.settings = settings;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getName() {
        return name;
    }

    public @Nullable CosmeticStoreBundleSkin getSkin() {
        return skin;
    }

    public @NotNull CosmeticTier getTier() {
        return tier;
    }

    public float getDiscount() {
        return discount;
    }

    public boolean getRotateOnPreview() {
        return rotateOnPreview;
    }

    public @NotNull Map<@NotNull CosmeticSlot, @NotNull String> getCosmetics() {
        return cosmetics;
    }

    public @NotNull Map<@NotNull String, @NotNull List<@NotNull CosmeticSetting>> getSettings() {
        return settings;
    }
}
