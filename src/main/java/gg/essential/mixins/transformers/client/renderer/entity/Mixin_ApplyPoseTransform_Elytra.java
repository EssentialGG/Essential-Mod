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
import gg.essential.mixins.impl.client.model.ElytraPoseSupplier;
import gg.essential.model.backend.PlayerPose;
import gg.essential.model.backend.minecraft.PlayerPoseKt;
import gg.essential.model.util.PlayerPoseManager;
import kotlin.Pair;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelElytra;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static gg.essential.model.backend.minecraft.PlayerPoseKt.toPose;

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.render.entity.state.BipedEntityRenderState;
//#endif

@Mixin(ModelElytra.class)
public abstract class Mixin_ApplyPoseTransform_Elytra implements ElytraPoseSupplier {

    @Shadow
    @Final
    private ModelRenderer leftWing;

    @Shadow
    @Final
    private ModelRenderer rightWing;

    @Unique
    private PlayerPose resetPose;

    /**
     * Reset the vanilla models back to their initial pose. This is necessary because the vanilla code is not guaranteed
     * to reset all values by itself (only the ones it also modifies itself).
     */
    @Inject(method = "setRotationAngles", at = @At("HEAD"))
    private void resetPose(
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

        if (resetPose == null) {
            resetPose = PlayerPoseKt.withElytraPose(PlayerPose.Companion.neutral(), this.leftWing, this.rightWing, cState);
        } else {
            PlayerPoseKt.applyElytraPose(resetPose, this.leftWing, this.rightWing, cState);
        }
    }

    @Inject(method = "setRotationAngles", at = @At("TAIL"))
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
        PlayerPoseManager poseManager = cState.poseManager();
        if (poseManager == null) {
            return;
        }

        PlayerPose basePose = PlayerPoseKt.withElytraPose(PlayerPose.Companion.neutral(), this.leftWing, this.rightWing, cState);
        PlayerPose transformedPose = poseManager.computePose(cState.wearablesManager(), basePose);

        if (basePose.equals(transformedPose)) {
            return;
        }

        PlayerPoseKt.applyElytraPose(transformedPose, this.leftWing, this.rightWing, cState);
    }

    @Override
    public @Nullable Pair<PlayerPose.Part, PlayerPose.Part> getWingsPose() {
        return new Pair<>(toPose(this.leftWing), toPose(this.rightWing));
    }
}
