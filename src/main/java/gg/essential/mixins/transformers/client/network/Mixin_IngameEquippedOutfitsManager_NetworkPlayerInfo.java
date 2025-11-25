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
package gg.essential.mixins.transformers.client.network;

import gg.essential.mixins.impl.client.network.NetworkPlayerInfoExt;
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(NetworkPlayerInfo.class)
public class Mixin_IngameEquippedOutfitsManager_NetworkPlayerInfo implements NetworkPlayerInfoExt {

    @Unique
    private EquippedOutfitsManager equippedOutfitsManager;

    @Override
    public @Nullable EquippedOutfitsManager getEssential$equippedOutfitsManager() {
        return this.equippedOutfitsManager;
    }

    @Override
    public void setEssential$equippedOutfitsManager(@Nullable EquippedOutfitsManager equippedOutfitsManager) {
        this.equippedOutfitsManager = equippedOutfitsManager;
    }
}
