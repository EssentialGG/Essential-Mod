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
import gg.essential.mixins.impl.client.model.ElytraPoseSupplier;
import gg.essential.mixins.transformers.client.renderer.entity.equipment.EquipmentRendererAccessor;
import gg.essential.model.backend.PlayerPose;
import kotlin.Pair;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.EquippableComponent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.folomeev.kotgl.matrix.vectors.Vectors.vec3;

@Mixin(ElytraFeatureRenderer.class)
public abstract class Mixin_ElytraPoseSupplier implements ElytraPoseSupplier {

    private static final String RENDER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V";
    private static final String MODEL_RENDER = "Lnet/minecraft/client/render/entity/equipment/EquipmentRenderer;render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V";

    @Shadow @Final private EquipmentRenderer equipmentRenderer;

    @Unique
    private BipedEntityRenderState renderedState;
    @Unique
    private ElytraEntityModel renderedModel;
    @Unique
    private Vec3 renderedOffset;

    @Inject(method = RENDER, at = @At("HEAD"))
    private void unsetRendered(CallbackInfo ci) {
        renderedState = null;
        renderedModel = null;
        renderedOffset = null;
    }

    @WrapWithCondition(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private boolean captureTranslation(MatrixStack stack, float x, float y, float z) {
        renderedOffset = vec3(x * 16f, y * 16f, z * 16f);
        return true;
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = MODEL_RENDER))
    private void setRenderedPose(
        CallbackInfo ci,
        @Local(argsOnly = true) BipedEntityRenderState state,
        @Local EquippableComponent component,
        @Local ElytraEntityModel model
    ) {
        boolean modelHasNoWings = ((EquipmentRendererAccessor) this.equipmentRenderer)
            .getEquipmentModelLoader()
            .get(component.assetId().get())
            .getLayers(EquipmentModel.LayerType.WINGS)
            .isEmpty();
        if (modelHasNoWings) return;

        renderedState = state;
        renderedModel = model;
    }

    @Override
    public @Nullable Pair<PlayerPose.Part, PlayerPose.Part> getWingsPose() {
        BipedEntityRenderState state = renderedState;
        ElytraEntityModel model = renderedModel;
        if (state == null || model == null) {
            return null;
        }

        model.setAngles(state);

        var wingsPose = ((ElytraPoseSupplier) model).getWingsPose();

        Vec3 offset = renderedOffset;
        if (wingsPose != null && offset != null) wingsPose = new Pair<>(
            wingsPose.component1().offset(offset),
            wingsPose.component2().offset(offset)
        );

        return wingsPose;
    }
}
