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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.folomeev.kotgl.matrix.vectors.Vec3;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.mixins.impl.client.model.CapePoseSupplier;
import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
import gg.essential.model.backend.PlayerPose;
import gg.essential.model.backend.minecraft.PlayerPoseKt;
import gg.essential.model.util.PlayerPoseManager;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.folomeev.kotgl.matrix.vectors.Vectors.vec3;
import static dev.folomeev.kotgl.matrix.vectors.Vectors.vecZero;

@Mixin(CapeFeatureRenderer.class)
public abstract class Mixin_ApplyPoseTransform_Cape implements CapePoseSupplier {
    private static final String RENDER_LAYER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V";
    private static final String RENDER_CAPE = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V";

    @Shadow @Final private BipedEntityModel<PlayerEntityRenderState> model;

    @Unique
    private PlayerPose.Part renderedPose;

    @Inject(method = RENDER_LAYER, at = @At("HEAD"))
    private void unsetRenderedPose(CallbackInfo ci) {
        renderedPose = null;
    }

    @WrapWithCondition(method = RENDER_LAYER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private boolean captureTranslation(MatrixStack stack, float x, float y, float z, @Share("offset") LocalRef<Vec3> offset) {
        offset.set(vec3(x * 16f, y * 16f, z * 16f));
        return true;
    }

    @Inject(method = RENDER_LAYER, at = @At(value = "INVOKE", target = RENDER_CAPE))
    private void applyPoseTransform(
        CallbackInfo ci,
        @Local(argsOnly = true) PlayerEntityRenderState state,
        @Share("offset") LocalRef<Vec3> offset
    ) {
        CosmeticsRenderState cState = ((PlayerEntityRenderStateExt) state).essential$getCosmetics();
        PlayerPoseManager poseManager = cState.poseManager();
        if (poseManager == null) {
            return;
        }

        ModelPart bodyModel = this.model.body;
        ModelPart capeModel = bodyModel.getChild("cape");

        Vec3 extraOffset = offset.get();
        if (extraOffset == null) {
            extraOffset = vecZero();
        }

        PlayerPose basePose = PlayerPoseKt.withCapePose(PlayerPose.Companion.neutral(), extraOffset, bodyModel, capeModel);
        PlayerPose transformedPose = poseManager.computePose(cState.wearablesManager(), basePose);

        renderedPose = transformedPose.getCape();

        if (basePose.equals(transformedPose)) {
            return;
        }

        // Apply our computed pose with all the animations that affect it
        PlayerPoseKt.applyCapePose(transformedPose, extraOffset, bodyModel, capeModel);
    }

    @Override
    public @Nullable PlayerPose.Part getCapePose() {
        return renderedPose;
    }
}
