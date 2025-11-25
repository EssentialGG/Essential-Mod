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
package gg.essential.mixins.transformers.cosmetics.skinmask;

import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class Mixin_ApplyToPlayerRenderer {
    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("RETURN"))
    private void applyCosmeticsSkinMask(AbstractClientPlayerEntity entity, PlayerEntityRenderState state, float partialTicks, CallbackInfo ci) {
        SkinTextures textures = state.skinTextures;
        Identifier orgTexture = textures.texture();
        Identifier newTexture = ((AbstractClientPlayerExt) entity).applyEssentialCosmeticsMask(orgTexture);
        if (orgTexture != newTexture) {
            state.skinTextures = new SkinTextures(newTexture, textures.textureUrl(), textures.capeTexture(), textures.elytraTexture(), textures.model(), textures.secure());
        }
    }
}
