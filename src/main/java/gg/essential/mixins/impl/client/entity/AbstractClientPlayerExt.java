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
package gg.essential.mixins.impl.client.entity;

import gg.essential.cosmetics.CosmeticsState;
import gg.essential.cosmetics.EquippedCosmetic;
import gg.essential.cosmetics.WearablesManager;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.model.backend.PlayerPose;
import gg.essential.model.util.PlayerPoseManager;
import gg.essential.util.UIdentifier;
import kotlin.Pair;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AbstractClientPlayerExt {
    UUID getCosmeticsSourceUuid();

    State<Map<CosmeticSlot, EquippedCosmetic>> getCosmeticsSource();

    void setCosmeticsSource(State<Map<CosmeticSlot, EquippedCosmetic>> source);

    @NotNull WearablesManager getWearablesManager();

    @NotNull
    CosmeticsState getCosmeticsState();

    void setEssentialCosmeticsCape(@Nullable String cape, @Nullable Pair<List<UIdentifier>, @Nullable List<UIdentifier>> textures);

    @Nullable UIdentifier getEmissiveCapeTexture();

    ResourceLocation applyEssentialCosmeticsMask(ResourceLocation skin);

    boolean[] wasArmorRenderingSuppressed();

    @NotNull
    PlayerPoseManager getPoseManager();

    boolean isPoseModified();

    void setPoseModified(boolean poseModified);

    @Nullable PlayerPose getRenderedPose();
    void setRenderedPose(PlayerPose pose);

    float essential$getCosmeticFrozenYaw();
    void essential$setCosmeticFrozenYaw(float frozenYaw);
}
