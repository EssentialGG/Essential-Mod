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
import gg.essential.mixins.impl.client.model.ModelBipedExt;
import gg.essential.mixins.impl.client.model.ModelBipedUtil;
import gg.essential.model.backend.PlayerPose;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.render.entity.state.BipedEntityRenderState;
//#endif

@Mixin(ModelBiped.class)
public abstract class Mixin_ApplyPoseTransform implements ModelBipedExt {

    //#if MC>=11600
    //$$ private static final String COPY_MODEL_ANGLES = "Lnet/minecraft/client/renderer/model/ModelRenderer;copyModelAngles(Lnet/minecraft/client/renderer/model/ModelRenderer;)V";
    //#else
    private static final String COPY_MODEL_ANGLES = "Lnet/minecraft/client/model/ModelBiped;copyModelAngles(Lnet/minecraft/client/model/ModelRenderer;Lnet/minecraft/client/model/ModelRenderer;)V";
    //#endif

    @Unique
    private PlayerPose resetPose;

    @Override
    public PlayerPose getResetPose() {
        return resetPose;
    }

    @Override
    public void setResetPose(PlayerPose pose) {
        resetPose = pose;
    }

    /**
     * Reset the vanilla models back to their initial pose. This is necessary because the vanilla code is not guaranteed
     * to reset all values by itself (only the ones it also modifies itself).
     */
    @Inject(method = "setRotationAngles", at = @At("HEAD"))
    private void resetPose(CallbackInfo ci) {
        ModelBipedUtil.resetPose((ModelBiped) (Object) this);
    }

    @Inject(
        method = "setRotationAngles",
        //#if MC >= 1.21.2
        //$$ at = @At("RETURN")
        //#else
        at = @At(value = "INVOKE", target = COPY_MODEL_ANGLES)
        //#endif
    )
    private void applyPoseTransform(
        CallbackInfo ci,
        //#if MC>=12102
        //$$ @Local(argsOnly = true) BipedEntityRenderState state
        //#elseif MC>=11400
        //$$ @Local(argsOnly = true) net.minecraft.entity.LivingEntity entity
        //#else
        @Local(argsOnly = true) Entity entity
        //#endif
    ) {
        //#if MC>=12102
        //$$ if (!(state instanceof PlayerEntityRenderStateExt)) return;
        //$$ CosmeticsRenderState cState = ((PlayerEntityRenderStateExt) state).essential$getCosmetics();
        //#else
        if (!(entity instanceof AbstractClientPlayer)) return;
        CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayer) entity);
        //#endif
        ModelBipedUtil.applyPoseTransform((ModelBiped) (Object) this, cState);
    }
}
