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
package gg.essential.mixins.transformers.entity.player;

import gg.essential.mixins.impl.entity.player.EntityPlayerHook;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {

    private final EntityPlayerHook minecraftHook = new EntityPlayerHook((EntityPlayer) (Object) this);

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void renderTickPre(CallbackInfo callbackInfo) {
        minecraftHook.tickPre();
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void renderTickPost(CallbackInfo callbackInfo) {
        minecraftHook.tickPost();
    }
}