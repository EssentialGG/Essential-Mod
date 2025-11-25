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
import gg.essential.cosmetics.WearablesManager;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12104
//$$ import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
//#endif

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.render.entity.state.LivingEntityRenderState;
//#endif

@Mixin(LayerHeldItem.class)
public class Mixin_HideHeldItemLayer {


    //#if MC>=11600
    //$$ private static final String RENDER_LAYER = "render";
    //#else
    private static final String RENDER_LAYER = "doRenderLayer";
    //#endif


    @Inject(method = RENDER_LAYER, at = @At(value = "HEAD"), cancellable = true)
    private void hideHeldItem(final CallbackInfo ci,
        //#if MC>=12104
        //$$ @Local(argsOnly = true) ArmedEntityRenderState entityRenderState
        //#elseif MC>=12102
        //$$ @Local(argsOnly = true) LivingEntityRenderState entityRenderState
        //#else
        @Local(argsOnly = true) EntityLivingBase entity
        //#endif
    ) {
        //#if MC>=12102
        //$$ if (!(entityRenderState instanceof PlayerEntityRenderStateExt)) return;
        //$$ CosmeticsRenderState cState = ((PlayerEntityRenderStateExt) entityRenderState).essential$getCosmetics();
        //#else
        if (!(entity instanceof AbstractClientPlayer)) return;
        CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayer) entity);
        //#endif

        WearablesManager wearables = cState.wearablesManager();
        if (wearables != null && wearables.getState().getHidesHeldItems()) {
            // if any cosmetic hides held items, cancel rendering
            ci.cancel();
        }

    }
}
