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
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class Mixin_NameplateIcon_Render {
    @Inject(method = "drawNameplate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableLighting()V"))
    private static void addEssentialIndicator(FontRenderer fontRendererIn, String str, float x, float y, float z, int verticalShift, float viewerYaw, float viewerPitch, boolean isThirdPersonFrontal, boolean isSneaking, CallbackInfo ci) {
        if (OnlineIndicator.currentlyDrawingPlayerEntityName()) {
            Entity entity = OnlineIndicator.nametagEntity;
            if (entity instanceof AbstractClientPlayer) {
                CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayer) entity);
                OnlineIndicator.drawNametagIndicator(new UMatrixStack(), cState, str, entity.getBrightnessForRender());
                return;
            }
        }

        // Light data for the background is passed implicitly via the global GL state on 1.12.2 and below.
        // That's why the vanilla `drawNameplate` method we inject into doesn't have an explicit `light` parameter.
        // Our padding-drawing method has one for cross-version consistency though, but can just pass 0 to it because
        // it never actually uses it (note how [TextRenderTypeVertexConsumer.create] uses `false` for `passTexLight`).
        int light = 0;

        // runs for non players and non-primary nameplates e.g. scoreboard
        IconCosmeticRenderer.INSTANCE.drawStandaloneVersionConsistentPadding(
            new UMatrixStack(), isSneaking, str, light);
    }
}
