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
package gg.essential.mixins.transformers.events;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import gg.essential.Essential;
import gg.essential.event.gui.GuiDrawScreenEvent;
import gg.essential.universal.UMatrixStack;
import gg.essential.util.UDrawContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12100
//$$ import net.minecraft.client.render.RenderTickCounter;
//#endif

//#if MC>=12000
//$$ import net.minecraft.client.gui.DrawContext;
//#endif

//#if MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import org.spongepowered.asm.mixin.injection.Slice;
//#endif

@Mixin(EntityRenderer.class)
public abstract class Mixin_GuiDrawScreenEvent_Priority_Pre {

    @Shadow @Final private Minecraft mc;

    @Inject(
        method = "updateCameraAndRender",
        //#if MC>=11600
        //$$ at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", ordinal = 0),
        //$$ slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=Rendering overlay"))
        //#else
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;clear(I)V", shift = At.Shift.AFTER)
        //#endif
    )
    private void drawScreenPriorityPreEvent(
        CallbackInfo ci,
        //#if MC>=12000
        //$$ @Local DrawContext context,
        //#elseif MC>=11700
        //$$ @Local(ordinal = 1) MatrixStack vMatrixStack,
        //#elseif MC>=11600
        //$$ @Local MatrixStack vMatrixStack,
        //#endif
        //#if MC>=11600
        //$$ @Local(ordinal = 0) LocalIntRef mouseX,
        //$$ @Local(ordinal = 1) LocalIntRef mouseY,
        //#else
        @Local(ordinal = 2) LocalIntRef mouseX,
        @Local(ordinal = 3) LocalIntRef mouseY,
        //#endif
        //#if MC>=12100
        //$$ @Local(argsOnly = true) RenderTickCounter tickCounter
        //#else
        @Local(argsOnly = true) float partialTicks
        //#endif
    ) {
        GuiScreen screen = this.mc.currentScreen;
        if (screen == null) return;

        GuiDrawScreenEvent.Priority event = new GuiDrawScreenEvent.Priority(
            screen,
            //#if MC>=12000
            //$$ new UDrawContext(context, new UMatrixStack(context.getMatrices())),
            //#elseif MC>=11600
            //$$ new UDrawContext(new UMatrixStack(vMatrixStack)),
            //#else
            new UDrawContext(new UMatrixStack()),
            //#endif
            mouseX.get(),
            mouseY.get(),
            //#if MC>=12100
            //$$ tickCounter.getTickDelta(false),
            //#else
            partialTicks,
            //#endif
            false
        );
        Essential.EVENT_BUS.post(event);

        if (event.getMouseX() != event.getOriginalMouseX()) {
            mouseX.set(event.getMouseX());
        }
        if (event.getMouseY() != event.getOriginalMouseY()) {
            mouseY.set(event.getMouseY());
        }
    }
}
