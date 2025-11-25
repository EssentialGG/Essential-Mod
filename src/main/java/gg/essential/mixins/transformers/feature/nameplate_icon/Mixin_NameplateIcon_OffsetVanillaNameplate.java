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

import gg.essential.cosmetics.IconCosmeticRenderer;
import gg.essential.handlers.OnlineIndicator;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class Mixin_NameplateIcon_OffsetVanillaNameplate {
    @Inject(method = "drawNameplate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I", ordinal = 0))
    private static void essential$translateNameplate(CallbackInfo ci) {
        if (OnlineIndicator.currentlyDrawingPlayerEntityName()) {
            GlStateManager.translate(IconCosmeticRenderer.INSTANCE.getNameplateXOffset(OnlineIndicator.nametagEntity), 0f, 0f);
        }
    }
}
