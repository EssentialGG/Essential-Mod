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

import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.cosmetics.IconCosmeticRenderer;
import gg.essential.handlers.OnlineIndicator;
import gg.essential.universal.UMatrixStack;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// On 1.8.9, sneaking entity's nametags are rendered using a copy-paste of the nametag rendering code in another class :)
// This class contains corresponding copies of Mixin_NameplateIcon_Render and Mixin_NameplateIcon_OffsetVanillaNameplate
@Mixin(RendererLivingEntity.class)
public class Mixin_NameplateIcon_Handle10809SneakingCopy<T extends EntityLivingBase> {
    @Inject(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V", shift = At.Shift.AFTER))
    private void essential$translateNameplate(CallbackInfo ci, @Local(argsOnly = true) T entity) {
        GlStateManager.translate(IconCosmeticRenderer.INSTANCE.getNameplateXOffset(entity), 0f, 0f);
    }

    @Inject(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableLighting()V"))
    private void renderEssentialIndicator(T entityIn, double x, double y, double z, CallbackInfo ci, @Local String str) {
        if (entityIn instanceof AbstractClientPlayer) {
            CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayer) entityIn);
            int light = (((int) OpenGlHelper.lastBrightnessY) << 16) + (int) OpenGlHelper.lastBrightnessX;
            OnlineIndicator.drawNametagIndicator(new UMatrixStack(), cState, str, light);
        }
    }
}
