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
package gg.essential.mixins.transformers.client.model;

import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12109
//$$ import net.minecraft.entity.PlayerLikeEntity;
//#endif

@Mixin(PlayerEntityRenderer.class)
public abstract class Mixin_PlayerEntityRenderStateExt_UpdateRenderState {
    //#if MC>=12109
    //$$ @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("HEAD"))
    //$$ private void updateEssentialRenderState(PlayerLikeEntity playerLike, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
    //$$     if (!(playerLike instanceof AbstractClientPlayerEntity)) return;
    //$$     AbstractClientPlayerEntity entity = (AbstractClientPlayerEntity) playerLike;
    //#else
    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("HEAD"))
    private void updateEssentialRenderState(AbstractClientPlayerEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
    //#endif
        PlayerEntityRenderStateExt stateExt = (PlayerEntityRenderStateExt) state;
        stateExt.essential$getCosmetics().update(entity);
    }
}
