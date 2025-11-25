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

import com.mojang.blaze3d.matrix.MatrixStack;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.cosmetics.IconCosmeticRenderer;
import gg.essential.handlers.OnlineIndicator;
import gg.essential.universal.UMatrixStack;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static gg.essential.universal.utils.TextUtilsKt.toFormattedString;

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.render.entity.state.EntityRenderState;
//#endif

@Mixin(EntityRenderer.class)
public class Mixin_NameplateIcon_Render<T extends Entity> {
    @Inject(method = "renderName", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/matrix/MatrixStack;pop()V"))
    private void renderEssentialIndicator(
        //#if MC>=12102
        //$$ EntityRenderState state,
        //#else
        T entity,
        //#endif
        ITextComponent name,
        MatrixStack vMatrixStack,
        IRenderTypeBuffer bufferIn,
        int packedLightIn,
        //#if MC>=12005 && MC<12102
        //$$ float timeDelta,
        //#endif
        CallbackInfo ci
    ) {
        if (OnlineIndicator.currentlyDrawingPlayerEntityName()) {
            //#if MC>=12102
            //$$ if (state instanceof PlayerEntityRenderStateExt) {
            //$$     CosmeticsRenderState cState = ((PlayerEntityRenderStateExt) state).essential$getCosmetics();
            //#else
            if (entity instanceof AbstractClientPlayerEntity) {
                CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayerEntity) entity);
            //#endif
                OnlineIndicator.drawNametagIndicator(new UMatrixStack(vMatrixStack), bufferIn, cState, toFormattedString(name), packedLightIn);
                return;
            }
        }

        //#if MC>=12102
        //$$ boolean isSneaking = state.sneaking;
        //#else
        boolean isSneaking = entity.isSneaking();
        //#endif

        // runs for non players and non-primary nameplates e.g. scoreboard
        IconCosmeticRenderer.INSTANCE.drawStandaloneVersionConsistentPadding(
                new UMatrixStack(vMatrixStack), bufferIn, isSneaking, toFormattedString(name), packedLightIn);
    }
}
