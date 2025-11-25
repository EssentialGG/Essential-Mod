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
package gg.essential.mixins.transformers.feature.nameplate_icon;

import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.mixins.impl.LabelCommandExt;
import gg.essential.model.backend.minecraft.MinecraftRenderBackend;
import net.minecraft.client.render.command.LabelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.cosmetics.IconCosmeticRenderer;
import gg.essential.universal.UMatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static gg.essential.universal.utils.TextUtilsKt.toFormattedString;

@Mixin(LabelCommandRenderer.class)
public class Mixin_NameplateIcon_Render<T extends Entity> {
    private static final String DRAW_TEXT = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V";

    @Inject(method = "render", at = @At(value = "INVOKE", target = DRAW_TEXT, ordinal = 0))
    private void renderEssentialIndicatorSeeThrough(
        CallbackInfo ci,
        @Local(argsOnly = true) VertexConsumerProvider.Immediate immediate,
        @Local OrderedRenderCommandQueueImpl.LabelCommand command
    ) {
        // FIXME This is a hack to split our single nametag drawing method into the two separate passes which MC wants.
        //       Ideally we refactor our nametag drawing method, but the vanilla code doesn't seem correct right now
        //       either, so I'm not yet sure how exactly to "correctly" integrate our code into it.
        VertexConsumerProvider vertexConsumerProvider = (renderLayer) -> {
            if ("text_see_through".equals(renderLayer.getName())) {
                return immediate.getBuffer(renderLayer);
            } else {
                return MinecraftRenderBackend.NullMcVertexConsumer.INSTANCE;
            }
        };

        // Nameplates while sneaking aren't see-through, so we can be sure that any time we try to draw a see-through
        // nameplate, it must have been from a non-sneaking source.
        boolean isSneaking = false;

        renderEssentialIndicator(vertexConsumerProvider, command, isSneaking);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = DRAW_TEXT, ordinal = 1))
    private void renderEssentialIndicatorNormal(
        CallbackInfo ci,
        @Local(argsOnly = true) VertexConsumerProvider.Immediate immediate,
        @Local OrderedRenderCommandQueueImpl.LabelCommand command
    ) {
        // FIXME see similar FIXME above
        VertexConsumerProvider vertexConsumerProvider = (renderLayer) -> {
            if ("text".equals(renderLayer.getName())) {
                return immediate.getBuffer(renderLayer);
            } else {
                return MinecraftRenderBackend.NullMcVertexConsumer.INSTANCE;
            }
        };

        // Normal labels are used both when sneaking and when not.
        // One way to differentiate is by their color.
        // Note that background color shouldn't be used because that can be configured to be the same (zero) for both.
        boolean isSneaking = command.color() != -1;

        renderEssentialIndicator(vertexConsumerProvider, command, isSneaking);
    }

    @Unique
    private void renderEssentialIndicator(
        VertexConsumerProvider vertexConsumerProvider,
        OrderedRenderCommandQueueImpl.LabelCommand command,
        boolean isSneaking
    ) {
        UMatrixStack matrixStack = new UMatrixStack();
        matrixStack.peek().getModel().set(command.matricesEntry());

        String text = toFormattedString(command.text());

        CosmeticsRenderState cState = LabelCommandExt.of(command).essential$getCosmeticsRenderState();
        if (cState != null) {
            IconCosmeticRenderer.INSTANCE.drawNameTagIconAndVersionConsistentPadding(
                matrixStack, vertexConsumerProvider, cState, text, command.lightCoords());
            return;
        }

        // runs for non players and non-primary nameplates e.g. scoreboard
        // FIXME Currently this uses a custom `white.png` texture, which results in a different RenderLayer and
        //  therefore much less batching than otherwise possible (basically pre-1.21.9 levels).
        //  We should try to use the same layer as vanilla such that texts can be drawn in a single call.
        IconCosmeticRenderer.INSTANCE.drawStandaloneVersionConsistentPadding(
            matrixStack, vertexConsumerProvider, isSneaking, text, command.lightCoords());
    }
}
