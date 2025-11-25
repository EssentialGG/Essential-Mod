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

import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static gg.essential.mixins.ext.client.multiplayer.ServerDataExtKt.getExt;

@Mixin(targets = "net/minecraft/client/network/ServerPinger$1")
public class Mixin_OverridePing {

    @Final
    @Shadow(aliases = {
        //#if NEOFORGE
        //#if MC>=12106
        //$$ "val$p_105460_"
        //#else
        //$$ "val$data",
        //#endif
        //#else
        "val$server",
        "field_3776",
        "val$p_147224_1_",
        "val$p_105460_",
        //#endif
    })
    private ServerData server;

    @Inject(
        // FIXME remap bug: should be able to remap these
        //#if FABRIC
        //#if MC>=12002
        //$$ method = "onPingResult",
        //#else
        //$$ method = "onPong",
        //#endif
        //#else
        //#if MC>=11700
        //$$ method = "handlePongResponse",
        //#else
        method = "handlePong",
        //#endif
        //#endif
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/ServerData;pingToServer:J", shift = At.Shift.AFTER)
    )
    private void overridePing(CallbackInfo ci) {
        Long pingOverride = getExt(this.server).getEssential$pingOverride();
        if (pingOverride != null) {
            this.server.pingToServer = pingOverride;
        }
    }

}
