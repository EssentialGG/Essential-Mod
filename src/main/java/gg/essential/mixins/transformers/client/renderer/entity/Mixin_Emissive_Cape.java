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
import gg.essential.model.backend.minecraft.MinecraftRenderBackend;
import gg.essential.universal.UGraphics;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//#if MC>=12109
//$$ import net.minecraft.client.model.Model;
//$$ import net.minecraft.client.render.command.ModelCommandRenderer;
//$$ import net.minecraft.client.render.command.OrderedRenderCommandQueue;
//#endif

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.render.entity.model.BipedEntityModel;
//$$ import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
//#endif

//#if MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import com.mojang.blaze3d.vertex.IVertexBuilder;
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer;
//$$ import net.minecraft.client.renderer.RenderType;
//#endif

@Mixin(LayerCape.class)
public abstract class Mixin_Emissive_Cape {

    //#if MC>=12109
    //$$ private static final String RENDER_LAYER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V";
    //$$ private static final String RENDER_CAPE = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V";
    //#elseif MC>=12102
    //$$ private static final String RENDER_LAYER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V";
    //$$ private static final String RENDER_CAPE = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V";
    //#elseif MC>=11600
    //$$ private static final String RENDER_LAYER = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;FFFFFF)V";
    //$$ private static final String RENDER_CAPE = "Lnet/minecraft/client/renderer/entity/model/PlayerModel;renderCape(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;II)V";
    //#else
    private static final String RENDER_LAYER = "doRenderLayer(Lnet/minecraft/client/entity/AbstractClientPlayer;FFFFFFF)V";
    private static final String RENDER_CAPE = "Lnet/minecraft/client/model/ModelPlayer;renderCape(F)V";
    //#endif

    @WrapOperation(method = RENDER_LAYER, at = @At(value = "INVOKE", target = RENDER_CAPE))
    private <S> void renderWithEmissiveLayer(
        //#if MC>=12109
        //$$ OrderedRenderCommandQueue queue,
        //$$ Model<? super S> model,
        //$$ S state,
        //#elseif MC>=12102
        //$$ BipedEntityModel model,
        //#else
        ModelPlayer model,
        //#endif
        //#if MC>=11400
        //$$ MatrixStack matrixStack,
        //#if MC>=12109
        //$$ RenderLayer renderLayer,
        //#else
        //$$ IVertexBuilder vertexConsumer,
        //#endif
        //$$ int light,
        //$$ int overlay,
        //#if MC>=12109
        //$$ int outlineColor,
        //$$ ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay,
        //#endif
        //#else
        float scale,
        //#endif
        Operation<Void> original
        //#if MC<12109
        //#if MC>=11400
        //$$ , @Local(argsOnly = true) IRenderTypeBuffer buffer
        //#endif
        //#if MC>=12102
        //$$ , @Local(argsOnly = true) PlayerEntityRenderState state
        //#else
        , @Local(argsOnly = true) AbstractClientPlayer player
        //#endif
        //#endif
    ) {
        // Regular cape
        original.call(
            //#if MC>=12109
            //$$ queue,
            //$$ model,
            //$$ state,
            //#else
            model,
            //#endif
            //#if MC>=11400
            //$$ matrixStack,
            //#if MC>=12109
            //$$ renderLayer,
            //#else
            //$$ vertexConsumer,
            //#endif
            //$$ light,
            //$$ overlay
            //#else
            scale
            //#endif
            //#if MC>=12109
            //$$ , outlineColor
            //$$ , crumblingOverlay
            //#endif
        );

        // Emissive layer
        //#if MC>=12102
        //$$ CosmeticsRenderState cState = ((PlayerEntityRenderStateExt) state).essential$getCosmetics();
        //#else
        CosmeticsRenderState cState = new CosmeticsRenderState.Live(player);
        //#endif
        ResourceLocation emissiveTexture = cState.emissiveCapeTexture();
        if (emissiveTexture == null) {
            return;
        }

        //#if MC>=12109
        //$$ // Note: Cannot just use `original.call` because that expects an **Ordered**RenderCommandQueue while
        //$$ //       `getBatchingQueue` only returns a RenderCommandQueue.
        //$$ queue.getBatchingQueue(1).submitModel(
        //$$     model,
        //$$     state,
        //$$     matrixStack,
        //$$     MinecraftRenderBackend.INSTANCE.getEmissiveLayer(emissiveTexture),
        //$$     light,
        //$$     overlay,
        //$$     outlineColor,
        //$$     crumblingOverlay
        //$$ );
        //#elseif MC>=11400
        //$$ original.call(
        //$$     model,
        //$$     matrixStack,
        //$$     buffer.getBuffer(MinecraftRenderBackend.INSTANCE.getEmissiveLayer(emissiveTexture)),
        //$$     light,
        //$$     overlay
        //$$ );
        //#else
        Function0<Unit> cleanup = MinecraftRenderBackend.INSTANCE.setupEmissiveRendering();
        UGraphics.bindTexture(0, emissiveTexture);
        original.call(model, scale);
        cleanup.invoke();
        //#endif
    }
}
