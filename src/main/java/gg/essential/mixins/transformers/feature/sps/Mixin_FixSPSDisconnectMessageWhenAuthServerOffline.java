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
package gg.essential.mixins.transformers.feature.sps;


import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.essential.Essential;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.server.network.NetHandlerLoginServer$2")
public class Mixin_FixSPSDisconnectMessageWhenAuthServerOffline {

    @WrapOperation(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isSinglePlayer()Z"))
    private boolean modifyServerState(MinecraftServer instance, Operation<Boolean> original) {
        return original.call(instance) && Essential.getInstance().getConnectionManager().getSpsManager().getLocalSession() == null;
    }
}
