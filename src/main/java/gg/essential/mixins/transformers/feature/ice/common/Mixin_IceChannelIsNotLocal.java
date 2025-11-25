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
package gg.essential.mixins.transformers.feature.ice.common;

import io.netty.channel.Channel;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static gg.essential.network.connectionmanager.ice.IceEventLoopGroups.ICE_CLIENT_EVENT_LOOP_GROUP;
import static gg.essential.network.connectionmanager.ice.IceEventLoopGroups.ICE_SERVER_EVENT_LOOP_GROUP;

@Mixin(NetworkManager.class)
public class Mixin_IceChannelIsNotLocal {
    @Shadow
    private Channel channel;

    @Inject(method = "isLocalChannel", at = @At("RETURN"), cancellable = true)
    private void ifIceChannelThenNotLocal(CallbackInfoReturnable<Boolean> ci) {
        if (!ci.getReturnValue()) {
            return; // not a local channel anyway
        }

        // ICE channels should not be considered truly local (even though they technically are under the hood).
        if (ICE_SERVER_EVENT_LOOP_GROUP.isInitialized() && this.channel.eventLoop().parent() == ICE_SERVER_EVENT_LOOP_GROUP.getValue()) {
            ci.setReturnValue(false);
        }
        if (ICE_CLIENT_EVENT_LOOP_GROUP.isInitialized() && this.channel.eventLoop().parent() == ICE_CLIENT_EVENT_LOOP_GROUP.getValue()) {
            ci.setReturnValue(false);
        }
    }
}
