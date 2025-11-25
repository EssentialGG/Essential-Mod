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
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.server.SPacketCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class Mixin_HandleOutfitsUpdatePacket {
    @Shadow private Minecraft gameController;

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void handleEssentialOutfitUpdatesPackets(SPacketCustomPayload packet, CallbackInfo ci) {
        if (!packet.getChannelName().equals(OutfitUpdatesPayload.CHANNEL_ID_MC)) return;

        PacketThreadUtil.checkThreadAndEnqueue(packet, (NetHandlerPlayClient) (Object) this, this.gameController);

        OutfitUpdatesPayload payload = OutfitUpdatesPayload.decode(packet.getBufferData());

        ((NetHandlerPlayClientExt) this).getEssential$ingameEquippedOutfitsManager()
            .applyUpdates(payload.getUpdates());

        ci.cancel();
    }
}
