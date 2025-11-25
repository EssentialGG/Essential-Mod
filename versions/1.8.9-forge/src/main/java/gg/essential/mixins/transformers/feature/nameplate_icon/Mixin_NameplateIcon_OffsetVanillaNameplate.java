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
import gg.essential.cosmetics.IconCosmeticRenderer;
import gg.essential.handlers.OnlineIndicator;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Render.class)
public class Mixin_NameplateIcon_OffsetVanillaNameplate<T extends Entity> {
    @Inject(method = "renderLivingLabel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V", shift = At.Shift.AFTER, ordinal = 0))
    private void essential$translateNameplate(CallbackInfo ci, @Local(argsOnly = true) T entity) {
        if (OnlineIndicator.currentlyDrawingPlayerEntityName()) {
            GlStateManager.translate(IconCosmeticRenderer.INSTANCE.getNameplateXOffset(entity), 0f, 0f);
        }
    }
}
