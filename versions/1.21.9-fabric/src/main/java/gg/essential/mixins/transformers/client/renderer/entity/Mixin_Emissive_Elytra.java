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
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
import gg.essential.model.backend.minecraft.MinecraftRenderBackend;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EquipmentRenderer.class)
public abstract class Mixin_Emissive_Elytra {

    private static final String RENDER = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V";

    @Inject(method = RENDER, at = @At("TAIL"))
    private <S> void renderWithEmissiveLayer(
        EquipmentModel.LayerType layerType,
        RegistryKey<EquipmentAsset> modelId,
        Model<? super S> model,
        S state,
        ItemStack stack,
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        int light,
        @Nullable Identifier texture,
        int outlineColor,
        int order,
        CallbackInfo ci,
        @Local(ordinal = 4) LocalIntRef layerOrderRef
    ) {
        if (layerType != EquipmentModel.LayerType.WINGS) {
            return;
        }

        if (!(state instanceof PlayerEntityRenderStateExt)) {
            return;
        }
        CosmeticsRenderState cState = ((PlayerEntityRenderStateExt) state).essential$getCosmetics();

        Identifier emissiveTexture = cState.emissiveCapeTexture();
        if (emissiveTexture == null) {
            return;
        }

        int layerOrder = layerOrderRef.get();
        layerOrderRef.set(layerOrder + 1);
        queue.getBatchingQueue(layerOrder).submitModel(
            model,
            state,
            matrices,
            MinecraftRenderBackend.INSTANCE.getEmissiveArmorLayer(emissiveTexture),
            light,
            OverlayTexture.DEFAULT_UV,
            -1,
            null,
            outlineColor,
            null
        );
    }
}
