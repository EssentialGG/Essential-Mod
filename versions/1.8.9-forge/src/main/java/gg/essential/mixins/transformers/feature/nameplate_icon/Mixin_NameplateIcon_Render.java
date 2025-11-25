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
package gg.essential.mixins.transformers.feature.nameplate_icon;

import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.cosmetics.IconCosmeticRenderer;
import gg.essential.handlers.OnlineIndicator;
import gg.essential.universal.UMatrixStack;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Render.class)
public class Mixin_NameplateIcon_Render<T extends Entity> {
    @Inject(method = "renderLivingLabel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableLighting()V"))
    private void renderEssentialIndicator(T entityIn, String str, double x, double y, double z, int maxDistance, CallbackInfo ci) {

        int light = (((int) OpenGlHelper.lastBrightnessY) << 16) + (int) OpenGlHelper.lastBrightnessX;
        if (OnlineIndicator.currentlyDrawingPlayerEntityName()) {
            if (entityIn instanceof AbstractClientPlayer) {
                CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayer) entityIn);
                OnlineIndicator.drawNametagIndicator(new UMatrixStack(), cState, str, light);
                return;
            }
        }

        // runs for non players and non-primary nameplates e.g. scoreboard
        IconCosmeticRenderer.INSTANCE.drawStandaloneVersionConsistentPadding(new UMatrixStack(), entityIn.isSneaking(), str, light);
    }
}
