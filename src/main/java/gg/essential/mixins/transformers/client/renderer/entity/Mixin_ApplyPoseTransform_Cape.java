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

import dev.folomeev.kotgl.matrix.matrices.mutables.MutableMat4;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.mixins.impl.client.model.CapePoseSupplier;
import gg.essential.mixins.transformers.client.model.ModelPlayerAccessor;
import gg.essential.model.backend.PlayerPose;
import gg.essential.model.backend.minecraft.PlayerPoseKt;
import gg.essential.model.util.PlayerPoseManager;
import gg.essential.util.GLUtil;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer;
//$$ import net.minecraft.client.renderer.entity.layers.LayerRenderer;
//$$ import net.minecraft.client.renderer.entity.model.PlayerModel;
//$$ import net.minecraft.util.math.vector.Matrix3f;
//#else
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
//#endif

/**
 * Applying the pose transform for the cape is *slightly* complicated by the fact that MC doesn't actually use the model
 * transforms for it like it does for all other models and instead does translates and rotates (in non-standard order)
 * directly on the GL stack.
 * To work with this, we first capture MC's transformation in the form of a Mat4, then compute the real pose from that,
 * discard it and work with the properly configured model part only.
 */
@Mixin(LayerCape.class)
public abstract class Mixin_ApplyPoseTransform_Cape
    //#if MC>=11600
    //$$ extends LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>>
    //#endif
    implements CapePoseSupplier
{

    //#if MC>=11600
    //$$ private static final String RENDER_LAYER = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;FFFFFF)V";
    //$$ private static final String PUSH_MATRIX = "Lcom/mojang/blaze3d/matrix/MatrixStack;push()V";
    //$$ private static final String RENDER_CAPE = "Lnet/minecraft/client/renderer/entity/model/PlayerModel;renderCape(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;II)V";
    //#else
    private static final String RENDER_LAYER = "doRenderLayer(Lnet/minecraft/client/entity/AbstractClientPlayer;FFFFFFF)V";
    private static final String PUSH_MATRIX = "Lnet/minecraft/client/renderer/GlStateManager;pushMatrix()V";
    private static final String RENDER_CAPE = "Lnet/minecraft/client/model/ModelPlayer;renderCape(F)V";
    //#endif

    //#if MC<11600
    @Shadow
    @Final
    private RenderPlayer playerRenderer;
    //#endif

    @Unique
    private PlayerPose.Part renderedPose;

    @Unique
    private PlayerPose.Part resetPose;

    @Inject(method = RENDER_LAYER, at = @At("HEAD"))
    private void unsetRenderedPose(CallbackInfo ci) {
        renderedPose = null;
    }

    @Inject(method = RENDER_LAYER, at = @At(value = "INVOKE", target = PUSH_MATRIX, shift = At.Shift.AFTER))
    private void isolateCapeMatrix(
        //#if MC>=11400
        //$$ MatrixStack matrixStack,
        //$$ IRenderTypeBuffer buffer,
        //$$ int light,
        //#endif
        AbstractClientPlayer player,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        //#if MC<11400
        float scale,
        //#endif
        CallbackInfo ci
    ) {
        CosmeticsRenderState cState = new CosmeticsRenderState.Live(player);
        PlayerPoseManager poseManager = cState.poseManager();
        if (poseManager == null) {
            return;
        }
        // We want MC's cape transformations only, not any transformations applied outside of the renderer.
        // So to isolate those transformations only, we clear the top of the (just pushed) matrix stack and have MC
        // apply its transformations on there. Once it's done, we can then either read those and compute a proper pose
        // from them or multiply them on top of the existing stack if we don't need to animate the cape.
        //#if MC>=11600
        //$$ matrixStack.getLast().getMatrix().setIdentity();
        //$$ matrixStack.getLast().getNormal().setIdentity();
        //#else
        GlStateManager.loadIdentity();
        //#endif
    }

    @Inject(method = RENDER_LAYER, at = @At(value = "INVOKE", target = RENDER_CAPE))
    private void applyPoseTransform(
        //#if MC>=11400
        //$$ MatrixStack matrixStack,
        //$$ IRenderTypeBuffer buffer,
        //$$ int light,
        //#endif
        AbstractClientPlayer player,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        //#if MC<11400
        float scale,
        //#endif
        CallbackInfo ci
    ) {
        CosmeticsRenderState cState = new CosmeticsRenderState.Live(player);
        PlayerPoseManager poseManager = cState.poseManager();
        if (poseManager == null) {
            return;
        }
        //#if MC>=11600
        //$$ float scale = 1f / 16f;
        //#endif
        ModelRenderer capeModel = this.getCapeModel();

        // Read the matrix which MC has constructed on our freshly cleared matrix stack
        //#if MC>=11600
        //$$ MutableMat4 capeMatrix = GLUtil.INSTANCE.glGetMatrix(matrixStack, scale);
        //$$ Matrix3f capeMatrixNormals = new Matrix3f(matrixStack.getLast().getNormal());
        //#else
        MutableMat4 capeMatrix = GLUtil.INSTANCE.glGetMatrix(scale);
        //#endif

        // and then pop those changes so we get back the original stack, we'll reintegrate the above matrix later
        //#if MC>=11600
        //$$ matrixStack.pop();
        //$$ matrixStack.push();
        //#else
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        //#endif

        //#if MC<11400
        // When sneaking, the Minecraft renderer shifts the entire model down, the cape renderer has this assumption
        // baked in, however our Pose system does not and wants absolute coordinates.
        // To get those, we need to undo the offset in the stack we read from MC (but immediately re-do it in the
        // real stack because the vanilla renderer assumes it).
        if (player.isSneaking()) { // from LayerCustomHead
            capeMatrix.setM13(capeMatrix.getM13() - 0.2f / scale);
            GlStateManager.translate(0f, 0.2f, 0f);
        }
        //#endif

        // Compute the proper cape pose from the model and the matrix which MC constructed
        PlayerPose basePose = PlayerPoseKt.withCapePose(PlayerPose.Companion.neutral(), capeModel, capeMatrix);
        // and optionally apply animations
        PlayerPose transformedPose = poseManager.computePose(cState.wearablesManager(), basePose);

        renderedPose = transformedPose.getCape();

        // If there are no animations, we don't need to hijack rendering any more than we've already done up to this point
        if (basePose.equals(transformedPose)) {
            // We still gotta multiply MC's matrix back onto the real stack though, because we've separated those just
            // in case we need them separate (turns out we didn't this time round).
            //#if MC>=11600
            //$$ GLUtil.INSTANCE.glMultMatrix(matrixStack, capeMatrix, scale);
            //$$ matrixStack.getLast().getNormal().mul(capeMatrixNormals);
            //#else
            GLUtil.INSTANCE.glMultMatrix(capeMatrix, scale);
            //#endif
            return;
        }

        // Capture the state of the model, so we can restore it afterwards
        resetPose = PlayerPoseKt.toPose(capeModel);
        // and finally apply our computed pose with all the animations that affect it
        PlayerPoseKt.applyTo(transformedPose.getCape(), capeModel);
    }

    /**
     * Reset the vanilla models back to their initial pose. This is necessary because the vanilla code is not guaranteed
     * to reset all values by itself (only the ones it also modifies itself).
     */
    @Inject(method = RENDER_LAYER, at = @At(value = "INVOKE", target = RENDER_CAPE, shift = At.Shift.AFTER))
    private void resetPose(CallbackInfo ci) {
        if (resetPose != null) {
            PlayerPoseKt.applyTo(resetPose, getCapeModel());
            resetPose = null;
        }
    }

    @Unique
    private ModelRenderer getCapeModel() {
        //#if MC>=11600
        //$$ return ((ModelPlayerAccessor) getEntityModel()).getCape();
        //#else
        return ((ModelPlayerAccessor) this.playerRenderer.getMainModel()).getCape();
        //#endif
    }

    @Override
    public @Nullable PlayerPose.Part getCapePose() {
        return renderedPose;
    }

    //#if MC>=11600
    //$$ Mixin_ApplyPoseTransform_Cape() { super(null); }
    //#endif
}
