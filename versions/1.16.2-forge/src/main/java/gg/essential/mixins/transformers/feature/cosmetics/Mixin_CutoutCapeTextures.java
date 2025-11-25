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
package gg.essential.mixins.transformers.feature.cosmetics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Adds support for cutout (opaque but with holes, no partial translucency, each pixel is only 100% or 0% transparent)
 * textures to the vanilla cape renderer.
 */
@Mixin(CapeLayer.class)
public abstract class Mixin_CutoutCapeTextures {
    //#if MC>=12109
    //$$ private static final String RENDER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V";
    //#elseif MC>=12102
    //$$ private static final String RENDER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V";
    //#else
    private static final String RENDER = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;FFFFFF)V";
    //#endif

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;getEntitySolid(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private RenderType essential$useCutoutRenderType(ResourceLocation resourceLocation, Operation<RenderType> operation) {
        RenderType vanillaRenderType = RenderType.getEntitySolid(resourceLocation);
        RenderType actualRenderType = operation.call(resourceLocation);
        if (actualRenderType != vanillaRenderType) {
            // Another mod already changed the render type. We can work decently well with anything that's non-solid, so
            // we'll just take whatever we can get instead of forcing our specific cutout layer on it.
            return actualRenderType;
        }
        return RenderType.getEntityCutoutNoCull(resourceLocation);
    }
}
