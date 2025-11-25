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
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.render.entity.state.BipedEntityRenderState;
//#endif

//#if MC>=11202
import net.minecraft.inventory.EntityEquipmentSlot;
//#endif

@Mixin(value = LayerArmorBase.class, priority = 900)
public class Mixin_StoreArmorRenderedState {

    //#if MC>=12109
    //$$ private static final String RENDER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V";
    //#elseif MC>=12102
    //$$ private static final String RENDER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V";
    //#elseif MC>=11600
    //$$ private static final String RENDER = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V";
    //#else
    private static final String RENDER = "doRenderLayer";
    //#endif

    private static final ThreadLocal<boolean[]> suppressedArmor = new ThreadLocal<>();

    @Inject(method = RENDER, at = @At("HEAD"))
    private void essential$beginObservingArmorRendering(CallbackInfo info) {
        suppressedArmor.set(new boolean[]{ true, true, true, true });
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void essential$storeObservedArmorRendering(
        CallbackInfo info,
        //#if MC>=12102
        //$$ @Local(argsOnly = true) BipedEntityRenderState state
        //#else
        @Local(argsOnly = true) EntityLivingBase entitylivingbaseIn
        //#endif
    ) {
        //#if MC>=12102
        //$$ if (!(state instanceof PlayerEntityRenderStateExt)) return;
        //$$ CosmeticsRenderState cState = ((PlayerEntityRenderStateExt) state).essential$getCosmetics();
        //#else
        if (!(entitylivingbaseIn instanceof AbstractClientPlayer)) return;
        CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayer) entitylivingbaseIn);
        //#endif
        cState.setSuppressedArmor(suppressedArmor.get());
    }

    @Inject(method = "renderArmorLayer", at = @At(value = "HEAD", shift = At.Shift.AFTER))
    private void essential$markRenderingNotSuppressed(
        CallbackInfo info,
        //#if MC>=11200
        @Local(argsOnly = true) EntityEquipmentSlot slotIn
        //#else
        //$$ @Local(argsOnly = true) int slotIn
        //#endif
    ) {
        //#if MC>=11200
        int slotIndex = slotIn.getIndex();
        //#else
        //$$ int slotIndex = slotIn-1;
        //#endif
        boolean[] slots = suppressedArmor.get();
        if (slots == null) return;
        slots[slotIndex] = false;
    }
}
