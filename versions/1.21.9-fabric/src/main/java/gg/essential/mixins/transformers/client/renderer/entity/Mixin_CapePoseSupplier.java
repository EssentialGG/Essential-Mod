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
import dev.folomeev.kotgl.matrix.vectors.Vec3;
import gg.essential.mixins.impl.client.model.CapePoseSupplier;
import gg.essential.model.backend.PlayerPose;
import gg.essential.model.backend.minecraft.PlayerPoseKt;
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
public abstract class Mixin_CapePoseSupplier implements CapePoseSupplier {
    private static final String RENDER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V";
    private static final String SUBMIT_MODEL = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V";

    @Shadow @Final private BipedEntityModel<PlayerEntityRenderState> model;

    @Unique
    private PlayerEntityRenderState renderedState;
    @Unique
    private Vec3 renderedOffset;

    @Inject(method = RENDER, at = @At("HEAD"))
    private void unsetRendered(CallbackInfo ci) {
        renderedState = null;
        renderedOffset = null;
    }

    @WrapWithCondition(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private boolean captureTranslation(MatrixStack stack, float x, float y, float z) {
        renderedOffset = vec3(x * 16f, y * 16f, z * 16f);
        return true;
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = SUBMIT_MODEL))
    private void captureSubmit(CallbackInfo ci, @Local(argsOnly = true) PlayerEntityRenderState state) {
        renderedState = state;
    }

    @Override
    public @Nullable PlayerPose.Part getCapePose() {
        if (renderedState == null) {
            return null;
        }

        this.model.setAngles(renderedState);

        ModelPart bodyModel = this.model.body;
        ModelPart capeModel = bodyModel.getChild("cape");

        Vec3 extraOffset = renderedOffset;
        if (extraOffset == null) {
            extraOffset = vecZero();
        }

        PlayerPose pose = PlayerPose.Companion.neutral();
        pose = PlayerPoseKt.withCapePose(pose, extraOffset, bodyModel, capeModel);
        return pose.getCape();
    }
}
