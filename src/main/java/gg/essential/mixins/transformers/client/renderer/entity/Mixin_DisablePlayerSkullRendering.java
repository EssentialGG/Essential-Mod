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
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.render.entity.state.LivingEntityRenderState;
//#endif

//#if MC>10809
import net.minecraft.inventory.EntityEquipmentSlot;
//#endif

/**
 * Disable armor rendering if cosmetics are conflicting.
 * <p>
 * We already have this for normal armor in {@link gg.essential.mixins.transformers.client.renderer.entity.Mixin_DisableArmorRendering},
 * but that doesn't take custom skulls or blocks in the player's HEAD equipment slot in to consideration, since they are
 * rendered separately.
 */
@Mixin(LayerCustomHead.class)
public abstract class Mixin_DisablePlayerSkullRendering {
    //#if MC>=12109
    //$$ private static final String RENDER_TARGET = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V";
    //#elseif MC>=12102
    //$$ private static final String RENDER_TARGET = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V";
    //#elseif MC>=11400
    //$$ private static final String RENDER_TARGET = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V";
    //#else
    private static final String RENDER_TARGET = "doRenderLayer";
    //#endif

    @Inject(method = RENDER_TARGET, at = @At(value = "HEAD"), cancellable = true)
    private void essential$disableArmorRendering(
        CallbackInfo ci,
        //#if MC>=12102
        //$$ @Local(argsOnly = true) LivingEntityRenderState state
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
        //#if MC<=10809
        //$$ int headSlotIndex = 2; // The slot for HEAD is 3, but we need to remove 1 to get the index.
        //#else
        int headSlotIndex = EntityEquipmentSlot.HEAD.getIndex();
        //#endif

        if (cState.blockedArmorSlots().contains(headSlotIndex)) {
            ci.cancel();
        }
    }
}
