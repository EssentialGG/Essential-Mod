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
package gg.essential.mixins.transformers.client.gui;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import gg.essential.event.gui.GuiDrawScreenEvent;
import gg.essential.mixins.impl.client.gui.EssentialGuiScreenBeforeClose;
import gg.essential.mixins.impl.client.gui.EssentialPostScreenDrawHook;
import gg.essential.universal.UMatrixStack;
import gg.essential.util.UDrawContext;
import net.minecraft.client.gui.GuiScreen;
import gg.essential.mixins.impl.client.gui.GuiScreenHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12000
//$$ import net.minecraft.client.gui.DrawContext;
//#endif

//#if MC >= 11400
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//#endif

@Mixin(GuiScreen.class)
public class MixinGuiScreen implements EssentialPostScreenDrawHook, EssentialGuiScreenBeforeClose {
    private final GuiScreenHook guiScreenHook = new GuiScreenHook((GuiScreen) (Object) this);

    //#if MC>=11400
    //$$ @Inject(method = {
    //$$     "init(Lnet/minecraft/client/Minecraft;II)V",
        //#if MC>=11904
        //$$ "resize"
        //#endif
    //$$ }, at = @At("RETURN"))
    //#else
    @Inject(method = "setWorldAndResolution", at = @At("RETURN"))
    //#endif
    private void setWorldAndResolution(CallbackInfo ci) {
        guiScreenHook.setWorldAndResolution();
    }


    @Inject(method = "drawScreen", at = @At("HEAD"))
    //#if MC>=12000
    //$$ private void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks, CallbackInfo ci,
    //$$                         @Local(ordinal = 0, argsOnly = true) LocalIntRef mouseXRef, @Local(ordinal = 1, argsOnly = true) LocalIntRef mouseYRef) {
    //$$     UMatrixStack matrixStack = new UMatrixStack(context.getMatrices());
    //$$     UDrawContext drawContext = new UDrawContext(context, matrixStack);
    //#elseif MC>=11400
    //$$ private void drawScreen(MatrixStack vMatrixStack, int mouseX, int mouseY, float partialTicks, CallbackInfo ci,
    //$$                         @Local(ordinal = 0, argsOnly = true) LocalIntRef mouseXRef, @Local(ordinal = 1, argsOnly = true) LocalIntRef mouseYRef) {
    //$$     UMatrixStack matrixStack = new UMatrixStack(vMatrixStack);
    //$$     UDrawContext drawContext = new UDrawContext(matrixStack);
    //#else
    protected void drawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci,
                              @Local(ordinal = 0, argsOnly = true) LocalIntRef mouseXRef, @Local(ordinal = 1, argsOnly = true) LocalIntRef mouseYRef) {
        UMatrixStack matrixStack = new UMatrixStack();
        UDrawContext drawContext = new UDrawContext(matrixStack);
    //#endif
        GuiDrawScreenEvent event = guiScreenHook.drawScreen(drawContext, mouseX, mouseY, partialTicks, false);

        // Overwrite the mouse X/Y if it was changed during the event (but don't mess with it if we don't need to)
        if (event.getMouseX() != event.getOriginalMouseX()) {
            mouseXRef.set(event.getMouseX());
        }
        if (event.getMouseY() != event.getOriginalMouseY()) {
            mouseYRef.set(event.getMouseY());
        }
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    //#if MC>=12000
    //$$ private void drawScreenPost(DrawContext context, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
    //$$     UMatrixStack matrixStack = new UMatrixStack(context.getMatrices());
    //$$     UDrawContext drawContext = new UDrawContext(context, matrixStack);
    //#elseif MC>=11400
    //$$ private void drawScreenPost(MatrixStack vMatrixStack, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
    //$$     UMatrixStack matrixStack = new UMatrixStack(vMatrixStack);
    //$$     UDrawContext drawContext = new UDrawContext(matrixStack);
    //#else
    protected void drawScreenPost(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        UMatrixStack matrixStack = new UMatrixStack();
        UDrawContext drawContext = new UDrawContext(matrixStack);
    //#endif
        guiScreenHook.drawScreen(drawContext, mouseX, mouseY, partialTicks, true);
        essential$afterDraw(drawContext, mouseX, mouseY, partialTicks);
    }

    @Override
    public void essential$afterDraw(UDrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    protected void onGuiClosed(final CallbackInfo ci) {
        essential$beforeClose();
    }

    @Override
    public void essential$beforeClose() {
    }
}
