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
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class Mixin_ApplyToPlayerRenderer {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("RETURN"))
    private void applyCosmeticsSkinMask(PlayerLikeEntity entity, PlayerEntityRenderState state, float partialTicks, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayerExt)) return;
        SkinTextures textures = state.skinTextures;
        var body = textures.body();
        Identifier orgTexture = body.texturePath();
        Identifier newTexture = ((AbstractClientPlayerExt) entity).applyEssentialCosmeticsMask(orgTexture);
        if (orgTexture != newTexture) {
            String url = body instanceof AssetInfo.SkinAssetInfo withUrl ? withUrl.url() : null;
            state.skinTextures = new SkinTextures(new AssetInfo.SkinAssetInfo(newTexture, url), textures.cape(), textures.elytra(), textures.model(), textures.secure());
        }
    }
}
