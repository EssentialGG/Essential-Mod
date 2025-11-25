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
package gg.essential.mixins.transformers.client.multiplayer;

import com.mojang.realmsclient.RealmsMainScreen;
import gg.essential.Essential;
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RealmsMainScreen.class)
public class Mixin_RealmsScreenOpenedTelemetry {

    @Inject(
            method = "init",
            at = @At("HEAD"),
            //#if MC>=11600
            //$$ remap = true
            //#else
            remap = false
            //#endif
    )
    private void onInit(CallbackInfo ci) {
        Essential.getInstance().getConnectionManager().getTelemetryManager().enqueue(ClientTelemetryPacket.forAction("REALMS_SCREEN_OPENED"));
    }

}
