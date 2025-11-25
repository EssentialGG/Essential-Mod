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
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class Mixin_SetDifficulty {

    //#if MC>=11600
    //$$ private static final String SET_DIFFICULTY = "Lnet/minecraft/world/storage/IServerConfiguration;setDifficulty(Lnet/minecraft/world/Difficulty;)V";
    //#else
    private static final String SET_DIFFICULTY = "Lnet/minecraft/world/storage/WorldInfo;setDifficulty(Lnet/minecraft/world/EnumDifficulty;)V";
    //#endif

    @Inject(method = "setDifficultyForAllWorlds", at = @At(value = "INVOKE", target = SET_DIFFICULTY), cancellable = true)
    public void onSetDifficulty(CallbackInfo ci, @Local(argsOnly = true) EnumDifficulty difficulty) {
        ExtensionsKt.getExecutor(UMinecraft.getMinecraft()).execute(() -> {
            SPSManager sps = Essential.getInstance().getConnectionManager().getSpsManager();
            if (sps.getLocalSession() != null && sps.getDifficulty() != difficulty) {
                sps.updateWorldSettings(
                        sps.isAllowCheats(),
                        sps.getCurrentGameMode(),
                        difficulty,
                        sps.isDifficultyLocked()
                );
            }
        });
    }
}
