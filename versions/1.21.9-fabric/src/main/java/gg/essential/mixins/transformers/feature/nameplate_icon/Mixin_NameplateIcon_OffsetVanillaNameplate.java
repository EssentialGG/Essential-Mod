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
import net.minecraft.client.render.command.LabelCommandRenderer;
import net.minecraft.client.util.math.MatrixStack;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.cosmetics.IconCosmeticRenderer;
import gg.essential.handlers.OnlineIndicator;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LabelCommandRenderer.Commands.class)
public class Mixin_NameplateIcon_OffsetVanillaNameplate<T extends Entity> {
    @Inject(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V", shift = At.Shift.AFTER, ordinal = 0))
    private void essential$translateNameplate(
        CallbackInfo ci,
        @Local(argsOnly = true) MatrixStack matrixStack
    ) {
        if (!OnlineIndicator.currentlyDrawingPlayerEntityName()) return;
        CosmeticsRenderState cState = OnlineIndicator.currentCosmeticsRenderState;
        if (cState == null) return;
        matrixStack.translate(IconCosmeticRenderer.INSTANCE.getNameplateXOffset(cState), 0f, 0f);
    }
}
