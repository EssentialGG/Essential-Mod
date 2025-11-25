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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.essential.Essential;
import gg.essential.cosmetics.IngameEquippedOutfitsManager;
import gg.essential.cosmetics.IngameEquippedOutfitsUpdateEncoder;
import gg.essential.cosmetics.events.CosmeticEventEmitter;
import gg.essential.mixins.ext.client.network.NetHandlerPlayClientExt;
import gg.essential.mixins.impl.client.network.NetworkPlayerInfoExt;
import kotlin.Unit;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NetHandlerPlayClient.class)
public abstract class Mixin_IngameEquippedOutfitsManager implements NetHandlerPlayClientExt {
    @Unique
    private final IngameEquippedOutfitsManager ingameEquippedOutfitsManager = new IngameEquippedOutfitsManager(
        Essential.getInstance().getConnectionManager().getCosmeticsManager().getCosmeticsData(),
        (uuid, slot, event) -> {
            CosmeticEventEmitter.doTriggerEvent(uuid, slot, event);
            return Unit.INSTANCE;
        }
    );

    @Unique
    private final IngameEquippedOutfitsUpdateEncoder ingameEquippedOutfitsUpdateEncoder = new IngameEquippedOutfitsUpdateEncoder();

    @Override
    public @NotNull IngameEquippedOutfitsManager getEssential$ingameEquippedOutfitsManager() {
        return ingameEquippedOutfitsManager;
    }

    @Override
    public @NotNull IngameEquippedOutfitsUpdateEncoder getEssential$ingameEquippedOutfitsUpdateEncoder() {
        return ingameEquippedOutfitsUpdateEncoder;
    }

    @ModifyExpressionValue(method = "handlePlayerListItem", at = @At(value = "NEW", target = "net/minecraft/client/network/NetworkPlayerInfo"))
    private NetworkPlayerInfo forwardIngameEquippedOutfitsManager(NetworkPlayerInfo networkPlayerInfo) {
        ((NetworkPlayerInfoExt) networkPlayerInfo).setEssential$equippedOutfitsManager(ingameEquippedOutfitsManager);
        return networkPlayerInfo;
    }
}
