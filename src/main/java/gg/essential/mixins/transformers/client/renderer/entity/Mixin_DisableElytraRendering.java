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
package gg.essential.mixins.transformers.client.renderer.entity;

import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.cosmetics.CosmeticsRenderState;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerElytra;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.render.entity.state.BipedEntityRenderState;
//#endif

@Mixin(value = LayerElytra.class)
public class Mixin_DisableElytraRendering {

    //#if MC>=11600
    //$$ private static final String RENDER_LAYER = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V";
    //#else
    private static final String RENDER_LAYER = "doRenderLayer";
    //#endif

    @Inject(method = RENDER_LAYER, at = @At(value = "HEAD"), cancellable = true)
    private void essential$disableElytraRendering(
        CallbackInfo info,
        //#if MC>=12102
        //$$ @Local(argsOnly = true) BipedEntityRenderState state
        //#else
        @Local(argsOnly = true) EntityLivingBase entity
        //#endif
    ) {
        //#if MC>=12102
        //$$ if (!(state instanceof PlayerEntityRenderStateExt)) return;
        //$$ CosmeticsRenderState cState = ((PlayerEntityRenderStateExt) state).essential$getCosmetics();
        //#else
        if (!(entity instanceof AbstractClientPlayer)) return;
        CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayer) entity);
        //#endif
        if (cState.blockedArmorSlots().contains(EntityEquipmentSlot.CHEST.getIndex())) {
            info.cancel();
        }
    }
}
