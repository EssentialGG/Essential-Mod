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
//#if MC>=11200

import com.llamalad7.mixinextras.sugar.Local;
import dev.folomeev.kotgl.matrix.vectors.Vec3;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.mixins.impl.client.model.ElytraPoseSupplier;
import gg.essential.model.backend.PlayerPose;
import gg.essential.model.backend.minecraft.PlayerPoseKt;
import kotlin.Pair;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelElytra;
import net.minecraft.client.renderer.entity.layers.LayerElytra;
import net.minecraft.entity.EntityLivingBase;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12104
//$$ import net.minecraft.component.type.EquippableComponent;
//#endif

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import gg.essential.mixins.transformers.client.renderer.entity.equipment.EquipmentRendererAccessor;
//$$ import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
//$$ import net.minecraft.client.render.entity.state.BipedEntityRenderState;
//$$ import net.minecraft.item.equipment.EquipmentModel;
//$$ import net.minecraft.util.Identifier;
//#endif

@Mixin(LayerElytra.class)
public abstract class Mixin_ElytraPoseSupplier implements ElytraPoseSupplier {

    //#if MC>=12102
    //$$ private static final String RENDER_LAYER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V";
    //#if MC>=12104
    //$$ private static final String MODEL_RENDER = "Lnet/minecraft/client/render/entity/equipment/EquipmentRenderer;render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V";
    //#else
    //$$ private static final String MODEL_RENDER = "Lnet/minecraft/client/render/entity/equipment/EquipmentRenderer;render(Lnet/minecraft/item/equipment/EquipmentModel$LayerType;Lnet/minecraft/util/Identifier;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V";
    //#endif
    //#elseif MC>=11600
    //$$ private static final String RENDER_LAYER = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V";
    //#if MC>=12100
    //$$ private static final String MODEL_RENDER = "Lnet/minecraft/client/render/entity/model/ElytraEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V";
    //#else
    //$$ private static final String MODEL_RENDER = "Lnet/minecraft/client/renderer/entity/model/ElytraModel;render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;IIFFFF)V";
    //#endif
    //#else
    private static final String RENDER_LAYER = "doRenderLayer(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V";
    private static final String MODEL_RENDER = "Lnet/minecraft/client/model/ModelElytra;render(Lnet/minecraft/entity/Entity;FFFFFF)V";
    //#endif

    //#if MC>=12102
    //$$ @Shadow @Final private EquipmentRenderer equipmentRenderer;
    //#endif
    @Shadow @Final private ModelElytra modelElytra;

    @Unique
    private Pair<PlayerPose.Part, PlayerPose.Part> wingsPose;

    @Inject(method = RENDER_LAYER, at = @At("HEAD"))
    private void unsetRenderedPose(CallbackInfo ci) {
        wingsPose = null;
    }

    @Inject(method = RENDER_LAYER, at = @At(value = "INVOKE", target = MODEL_RENDER))
    //#if MC>=12102
    //$$ private void setRenderedPose(
    //$$     CallbackInfo ci,
    //$$     @Local(argsOnly = true) BipedEntityRenderState state,
        //#if MC>=12104
        //$$ @Local(ordinal = 0) EquippableComponent component
        //#else
        //$$ @Local(ordinal = 1) Identifier modelId
        //#endif
    //$$ ) {
    //$$     if (!(state instanceof PlayerEntityRenderStateExt)) return;
    //$$     CosmeticsRenderState cState = ((PlayerEntityRenderStateExt) state).essential$getCosmetics();
        //#if MC>=12104
        //$$ var modelId = component.assetId().get();
        //#endif
    //$$
    //$$     boolean modelHasNoWings = ((EquipmentRendererAccessor) this.equipmentRenderer)
    //$$         .getEquipmentModelLoader()
    //$$         .get(modelId)
    //$$         .getLayers(EquipmentModel.LayerType.WINGS)
    //$$         .isEmpty();
    //$$     if (modelHasNoWings) return;
    //#else
    private void setRenderedPose(CallbackInfo ci, @Local(argsOnly = true) EntityLivingBase entity) {
        if (!(entity instanceof AbstractClientPlayer)) return;
        CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayer) entity);
    //#endif

        wingsPose = ((ElytraPoseSupplier) this.modelElytra).getWingsPose();

        Vec3 offset = PlayerPoseKt.getElytraPoseOffset(cState);
        if (wingsPose != null) wingsPose = new Pair<>(
            wingsPose.component1().offset(offset),
            wingsPose.component2().offset(offset)
        );
    }

    @Override
    public @Nullable Pair<PlayerPose.Part, PlayerPose.Part> getWingsPose() {
        return wingsPose;
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class Mixin_ElytraPoseSupplier {
//$$ }
//#endif
