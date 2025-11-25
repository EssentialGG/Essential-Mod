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
package gg.essential.mixins.transformers.server.integrated;

import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.Essential;
import gg.essential.network.connectionmanager.sps.SPSManager;
import gg.essential.universal.UMinecraft;
import gg.essential.util.ExtensionsKt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11600
//$$ @Mixin(net.minecraft.world.storage.ServerWorldInfo.class)
//#else
@Mixin(net.minecraft.world.storage.WorldInfo.class)
//#endif
public abstract class Mixin_SetDifficultyLocked {

    @Inject(method = "setDifficultyLocked", at = @At(value = "HEAD"), cancellable = true)
    public void onSetDifficultyLocked(CallbackInfo ci, @Local(argsOnly = true) boolean locked) {
        ExtensionsKt.getExecutor(UMinecraft.getMinecraft()).execute(() -> {
            SPSManager sps = Essential.getInstance().getConnectionManager().getSpsManager();
            if (sps.getLocalSession() != null && sps.isDifficultyLocked() != locked) {
                sps.updateWorldSettings(
                        sps.isAllowCheats(),
                        sps.getCurrentGameMode(),
                        sps.getDifficulty(),
                        locked
                );
            }
        });
    }
}
