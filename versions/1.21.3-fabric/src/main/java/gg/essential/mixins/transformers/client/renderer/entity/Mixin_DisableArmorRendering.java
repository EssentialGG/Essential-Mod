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
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//#if MC>=12109
//$$ import net.minecraft.client.render.command.OrderedRenderCommandQueue;
//#endif

@Mixin(value = ArmorFeatureRenderer.class)
public abstract class Mixin_DisableArmorRendering<S extends BipedEntityRenderState, M extends BipedEntityModel<S>, A extends BipedEntityModel<S>> {
    //#if MC>=12109
    //$$ private static final String RENDER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V";
    //#else
    private static final String RENDER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V";
    //#endif

    //#if MC>=12109
    //$$ private static final String RENDER_ARMOR = "Lnet/minecraft/client/render/entity/feature/ArmorFeatureRenderer;renderArmor(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EquipmentSlot;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V";
    //#else
    //#if FORGE
    //$$ private static final String RENDER_ARMOR = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V";
    //#else
    private static final String RENDER_ARMOR = "Lnet/minecraft/client/render/entity/feature/ArmorFeatureRenderer;renderArmor(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EquipmentSlot;ILnet/minecraft/client/render/entity/model/BipedEntityModel;)V";
    //#endif
    //#endif

    @WrapWithCondition(
        method = RENDER,
        at = @At(value = "INVOKE", target = RENDER_ARMOR)
    )
    private boolean essential$disableArmorRendering(
        ArmorFeatureRenderer<S, M, A> self,
        MatrixStack matrixStack,
        //#if MC>=12109
        //$$ OrderedRenderCommandQueue commandQueue,
        //#else
        VertexConsumerProvider vertexConsumers,
        //#endif
        ItemStack stack,
        EquipmentSlot slot,
        int light,
        //#if MC>=12109
        //$$ S state
        //#else
        A armorModel,
        //#if FORGE
        //$$ S state
        //#else
        @Local(argsOnly = true) S state
        //#endif
        //#endif
    ) {
        if (!(state instanceof PlayerEntityRenderStateExt)) return true;
        CosmeticsRenderState cState = ((PlayerEntityRenderStateExt) state).essential$getCosmetics();
        return !cState.blockedArmorSlots().contains(slot.getEntitySlotId());
    }
}
