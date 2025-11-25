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
import dev.folomeev.kotgl.matrix.matrices.mutables.MutableMat4;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.model.backend.PlayerPose;
import gg.essential.model.util.PlayerPoseManager;
import gg.essential.model.util.UMatrixStack;
import gg.essential.util.GLUtil;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerEntityOnShoulder;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.folomeev.kotgl.matrix.matrices.Matrices.identityMat3;
import static dev.folomeev.kotgl.matrix.matrices.Matrices.identityMat4;
import static dev.folomeev.kotgl.matrix.matrices.mutables.MutableMatrices.timesSelf;

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
//#endif

//#if MC>=11600
//$$ import gg.essential.mixins.impl.util.math.Matrix4fExt;
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import net.minecraft.util.math.vector.Matrix3f;
//$$ import static dev.folomeev.kotgl.matrix.matrices.mutables.MutableMatrices.times;
//$$ import static gg.essential.util.ExtensionsKt.setKotgl;
//#endif

@Mixin(LayerEntityOnShoulder.class)
public abstract class Mixin_ApplyPoseTransform_EntityOnShoulder {

    @Unique
    private void applyPoseTransform(
        CosmeticsRenderState cState,
        boolean leftSide,
        //#if MC>=11600
        //$$ com.mojang.blaze3d.matrix.MatrixStack matrixStack
        //#else
        float scale
        //#endif
    ) {
        PlayerPoseManager poseManager = cState.poseManager();
        if (poseManager == null) {
            return;
        }

        PlayerPose basePose = PlayerPose.Companion.neutral();
        PlayerPose transformedPose = poseManager.computePose(cState.wearablesManager(), basePose);

        if (basePose.equals(transformedPose)) {
            return;
        }

        PlayerPose.Part part = leftSide ? transformedPose.getLeftShoulderEntity() : transformedPose.getRightShoulderEntity();
        UMatrixStack uMatrixStack = new UMatrixStack(identityMat4(), identityMat3());
        uMatrixStack.translate(part.getPivotX(), part.getPivotY(), part.getPivotZ());
        uMatrixStack.rotate(part.getRotateAngleZ(), 0.0f, 0.0f, 1.0f, false);
        uMatrixStack.rotate(part.getRotateAngleY(), 0.0f, 1.0f, 0.0f, false);
        uMatrixStack.rotate(part.getRotateAngleX(), 1.0f, 0.0f, 0.0f, false);
        MutableMat4 modelMatrix = uMatrixStack.peek().getModel();
        if (part.getExtra() != null) {
            timesSelf(modelMatrix, part.getExtra());
        }
        //#if MC>=11600
        //$$ GLUtil.INSTANCE.glMultMatrix(matrixStack, modelMatrix, 1 / 16f);
        //$$ Matrix3f normalMatrix = new Matrix3f();
        //$$ setKotgl(normalMatrix, uMatrixStack.peek().getNormal());
        //$$ matrixStack.getLast().getNormal().mul(normalMatrix);
        //#else
        GLUtil.INSTANCE.glMultMatrix(modelMatrix, scale);
        //#endif
    }

    //#if MC>=12102
    //$$ @Inject(
        //#if MC>=12109
        //$$ method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/entity/passive/ParrotEntity$Variant;FFZ)V",
        //#else
        //$$ method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/entity/passive/ParrotEntity$Variant;FFZ)V",
        //#endif
    //$$     at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER)
    //$$ )
    //$$ private void applyPoseTransform(
    //$$     CallbackInfo ci,
    //$$     @Local(argsOnly = true) MatrixStack matrixStack,
    //$$     @Local(argsOnly = true) PlayerEntityRenderState state,
    //$$     @Local(argsOnly = true) boolean leftSide
    //$$ ) {
    //$$     if (!(state instanceof PlayerEntityRenderStateExt)) return;
    //$$     CosmeticsRenderState cState = ((PlayerEntityRenderStateExt) state).essential$getCosmetics();
    //$$     applyPoseTransform(cState, leftSide, matrixStack);
    //$$ }
    //#else
    //#if MC>=11600
    //$$ @Inject(method = {
        //#if FABRIC
        //$$ "method_17958",
        //#else
        //#if MC>=11700
        //#if MC>=11903
        //$$ "m_262347_", // prod
        //#else
        //$$ "m_117327_", // prod
        //#endif
        //$$ "lambda$render$1", // dev
        //#else
        //$$ "func_229137_a_",
        //#endif
        //$$ "lambda$renderParrot$1", // optifine (forge-only; optifabric detects the rename and undoes it)
        //#endif
    //$$ },
    //#if FABRIC
    //$$ remap = false, // we don't want the mixin AP to qualify the method reference, since optifine changes its arguments
    //#endif
    //$$ at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/matrix/MatrixStack;push()V", shift = At.Shift.AFTER, remap = true))
    //#else
    @Inject(method = "renderEntityOnShoulder", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;pushMatrix()V", shift = At.Shift.AFTER))
    //#endif
    private void applyPoseTransform(
        //#if MC>=11600
        //$$ com.mojang.blaze3d.matrix.MatrixStack matrixStack,
        //$$ boolean leftSide,
        //$$ PlayerEntity player,
        //#if MC>=11903
        //$$ NbtCompound nbt,
        //$$ net.minecraft.client.render.VertexConsumerProvider buffer,
        //#else
        //$$ net.minecraft.client.renderer.IRenderTypeBuffer buffer,
        //$$ CompoundNBT nbt,
        //#endif
        //$$ int packedLight,
        //$$ float limbSwing,
        //$$ float limbSwingAmount,
        //$$ float netHeadYaw,
        //$$ float headPitch,
        //$$ // FIXME remap bug
        //#if MC>=11700 && FORGELIKE
        //$$ net.minecraft.world.entity.EntityType<?> entityType,
        //#else
        //$$ net.minecraft.entity.EntityType<?> entityType,
        //#endif
        //$$ org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci
        //#else
        EntityPlayer player,
        java.util.UUID uuid,
        NBTTagCompound nbt,
        net.minecraft.client.renderer.entity.RenderLivingBase<? extends EntityLivingBase> entity,
        net.minecraft.client.model.ModelBase modelBase,
        net.minecraft.util.ResourceLocation resourceLocation,
        Class<?> class_,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scale,
        boolean leftSide,
        org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<?> ci
        //#endif
    ) {
        if (!(player instanceof AbstractClientPlayer)) return;
        CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayer) player);
        applyPoseTransform(cState, leftSide,
            //#if MC>=11600
            //$$ matrixStack
            //#else
            scale
            //#endif
        );
    }

    //#if MC>=11600
    //$$ @Dynamic("Optifine changes arguments")
    //$$ @Surrogate
    //$$ private void applyPoseTransform(
    //$$     PlayerEntity player,
    //$$     boolean leftSide,
    //$$     com.mojang.blaze3d.matrix.MatrixStack matrixStack,
    //$$     net.minecraft.client.renderer.IRenderTypeBuffer buffer,
    //$$     CompoundNBT  nbt,
    //$$     int packedLight,
    //$$     float limbSwing,
    //$$     float limbSwingAmount,
    //$$     float netHeadYaw,
    //$$     float headPitch,
    //$$     // FIXME remap bug
        //#if MC>=11700 && FORGELIKE
        //$$ net.minecraft.world.entity.EntityType<?> entityType,
        //#else
        //$$ net.minecraft.entity.EntityType<?> entityType,
        //#endif
    //$$     org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci
    //$$ ) {
    //$$     if (!(player instanceof AbstractClientPlayerEntity)) return;
    //$$     CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayerEntity) player);
    //$$     applyPoseTransform(cState, leftSide, matrixStack);
    //$$ }
    //$$
    //$$ @Dynamic("Optifine changes arguments")
    //$$ @Surrogate
    //$$ private void applyPoseTransform(
    //$$     PlayerEntity player,
    //$$     boolean leftSide,
    //$$     CompoundNBT  nbt,
    //$$     com.mojang.blaze3d.matrix.MatrixStack matrixStack,
    //$$     net.minecraft.client.renderer.IRenderTypeBuffer buffer,
    //$$     int packedLight,
    //$$     float limbSwing,
    //$$     float limbSwingAmount,
    //$$     float netHeadYaw,
    //$$     float headPitch,
    //$$     // FIXME remap bug
        //#if MC>=11700 && FORGELIKE
        //$$ net.minecraft.world.entity.EntityType<?> entityType,
        //#else
        //$$ net.minecraft.entity.EntityType<?> entityType,
        //#endif
    //$$     org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci
    //$$ ) {
    //$$     if (!(player instanceof AbstractClientPlayerEntity)) return;
    //$$     CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayerEntity) player);
    //$$     applyPoseTransform(cState, leftSide, matrixStack);
    //$$ }
    //#endif
    //#endif
}
