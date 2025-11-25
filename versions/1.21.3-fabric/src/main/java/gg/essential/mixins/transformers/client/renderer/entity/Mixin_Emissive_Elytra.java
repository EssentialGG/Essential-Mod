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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
import gg.essential.model.backend.minecraft.MinecraftRenderBackend;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentModel;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//#if MC>=12104
//$$ import net.minecraft.item.equipment.EquipmentAsset;
//$$ import net.minecraft.registry.RegistryKey;
//#endif

@Mixin(ElytraFeatureRenderer.class)
public abstract class Mixin_Emissive_Elytra {

    private static final String RENDER_LAYER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V";
    //#if MC>=12104
    //$$ private static final String RENDER_ELYTRA = "Lnet/minecraft/client/render/entity/equipment/EquipmentRenderer;render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V";
    //#else
    private static final String RENDER_ELYTRA = "Lnet/minecraft/client/render/entity/equipment/EquipmentRenderer;render(Lnet/minecraft/item/equipment/EquipmentModel$LayerType;Lnet/minecraft/util/Identifier;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V";
    //#endif

    @WrapOperation(method = RENDER_LAYER, at = @At(value = "INVOKE", target = RENDER_ELYTRA))
    private void renderWithEmissiveLayer(
        EquipmentRenderer equipmentRenderer,
        EquipmentModel.LayerType layerType,
        //#if MC>=12104
        //$$ RegistryKey<EquipmentAsset> modelId,
        //#else
        Identifier modelId,
        //#endif
        Model model,
        ItemStack stack,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        @Nullable Identifier texture,
        Operation<Void> original,
        @Local(argsOnly = true) BipedEntityRenderState state
    ) {
        // Regular elytra
        original.call(
            equipmentRenderer,
            layerType,
            modelId,
            model,
            stack,
            matrices,
            vertexConsumers,
            light,
            texture
        );

        // Emissive layer
        if (!(state instanceof PlayerEntityRenderStateExt)) {
            return;
        }
        CosmeticsRenderState cState = ((PlayerEntityRenderStateExt) state).essential$getCosmetics();
        Identifier emissiveTexture = cState.emissiveCapeTexture();
        if (emissiveTexture == null) {
            return;
        }

        RenderLayer vanillaLayer = RenderLayer.getArmorCutoutNoCull(emissiveTexture);

        original.call(
            equipmentRenderer,
            layerType,
            modelId,
            model,
            stack,
            matrices,
            (VertexConsumerProvider) layer -> {
                if (layer == vanillaLayer) {
                    return vertexConsumers.getBuffer(MinecraftRenderBackend.INSTANCE.getEmissiveArmorLayer(emissiveTexture));
                } else {
                    return MinecraftRenderBackend.NullMcVertexConsumer.INSTANCE;
                }
            },
            light,
            emissiveTexture
        );
    }
}
