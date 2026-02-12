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
package gg.essential.mixins.transformers.client.model.geom;

import dev.folomeev.kotgl.matrix.matrices.Mat4;
import gg.essential.mixins.ext.client.model.geom.ExtraTransformHolder;
import net.minecraft.client.model.ModelRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import dev.folomeev.kotgl.matrix.matrices.mutables.MutableMat4;
//$$ import gg.essential.util.ExtensionsKt;
//$$ import net.minecraft.util.math.vector.Matrix4f;
//$$ import static dev.folomeev.kotgl.matrix.matrices.mutables.MutableMatrices.times;
//$$ import static dev.folomeev.kotgl.matrix.matrices.mutables.MutableMatrices.toMutable;
//#else
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.essential.util.GLUtil;
import org.objectweb.asm.Opcodes;
//#endif

@Mixin(ModelRenderer.class)
public abstract class Mixin_ExtraTransform_ModelPart implements ExtraTransformHolder {
    @Unique
    @Nullable
    private Mat4 extra;

    @Nullable
    @Override
    public Mat4 getExtra() {
        return this.extra;
    }

    @Override
    public void setExtra(@Nullable Mat4 extra) {
        this.extra = extra;
    }

    //#if MC>=11600
    //$$ @SuppressWarnings("ConstantConditions") // intellij doesn't understand Ext interfaces
    //$$ @Inject(method = "translateRotate", at = @At("RETURN"))
    //$$ private void applyExtraTransformToRender(MatrixStack matrixStack, CallbackInfo ci) {
    //$$     Mat4 extra = this.extra;
    //$$     if (extra != null) {
    //$$         Matrix4f matrix = matrixStack.getLast().getMatrix();
    //$$         MutableMat4 extraScaled = toMutable(extra);
    //$$         extraScaled.setM03(extraScaled.getM03() / 16);
    //$$         extraScaled.setM13(extraScaled.getM13() / 16);
    //$$         extraScaled.setM23(extraScaled.getM23() / 16);
    //$$         ExtensionsKt.setKotgl(matrix, times(ExtensionsKt.getKotgl(matrix), extraScaled));
    //$$     }
    //$$ }
    //#else
    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/model/ModelRenderer;rotateAngleZ:F", ordinal = 0))
    private float forceSlowPathIfExtraIsPresent(float value) {
        if (extra != null) {
            return 1f; // just has to be non-0 to skip the fast path
        }
        return value;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;callList(I)V"))
    private void applyExtraTransformToRender(float scale, CallbackInfo ci) {
        applyExtraTransform(scale);
    }

    @Inject(method = "postRender", at = @At("RETURN"))
    private void applyExtraTransformToPostRender(float scale, CallbackInfo ci) {
        applyExtraTransform(scale);
    }

    private void applyExtraTransform(float scale) {
        Mat4 extra = this.extra;
        if (extra != null) {
            GLUtil.INSTANCE.glMultMatrix(extra, scale);
        }
    }
    //#endif
}
