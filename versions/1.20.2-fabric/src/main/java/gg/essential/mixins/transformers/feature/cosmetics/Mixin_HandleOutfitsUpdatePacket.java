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
package gg.essential.mixins.transformers.feature.cosmetics;

import gg.essential.cosmetics.OutfitUpdatesPayload;
import gg.essential.mixins.ext.client.network.NetHandlerPlayClientExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class Mixin_HandleOutfitsUpdatePacket {
    @Shadow @Final protected MinecraftClient client;

    @Inject(method = "onCustomPayload(Lnet/minecraft/network/packet/s2c/common/CustomPayloadS2CPacket;)V", at = @At("HEAD"), cancellable = true)
    private void handleEssentialOutfitUpdatesPackets(CustomPayloadS2CPacket packet, CallbackInfo ci) {
        var payload = packet.payload();
        if (!(payload instanceof OutfitUpdatesPayload)) return;

        //#if MC>=12109
        //$$ NetworkThreadUtils.forceMainThread(packet, (ClientCommonNetworkHandler) (Object) this, this.client.getPacketApplyBatcher());
        //#else
        NetworkThreadUtils.forceMainThread(packet, (ClientCommonNetworkHandler) (Object) this, this.client);
        //#endif

        ((NetHandlerPlayClientExt) this).getEssential$ingameEquippedOutfitsManager()
            .applyUpdates(((OutfitUpdatesPayload) payload).getUpdates());

        ci.cancel();
    }
}
