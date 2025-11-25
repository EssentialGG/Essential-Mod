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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.matrix.MatrixStack;
import gg.essential.Essential;
import gg.essential.event.gui.GuiDrawScreenEvent;
import gg.essential.universal.UMatrixStack;
import gg.essential.util.UDrawContext;
import net.minecraft.client.gui.ResourceLoadProgressGui;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//#if MC>=12000
//$$ import net.minecraft.client.gui.DrawContext;
//#endif

@Mixin(ResourceLoadProgressGui.class)
public abstract class Mixin_GuiDrawScreenEvent_Priority_LoadingScreen {
    //#if MC>=12106
    //$$ private static final String SCREEN_RENDER = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V";
    //#elseif MC>=12000
    //$$ private static final String SCREEN_RENDER = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V";
    //#else
    private static final String SCREEN_RENDER = "Lnet/minecraft/client/gui/screen/Screen;render(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V";
    //#endif

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = SCREEN_RENDER))
    private void wrapDrawScreen(
        Screen screen,
        //#if MC>=12000
        //$$ DrawContext context,
        //#else
        MatrixStack vMatrixStack,
        //#endif
        int mouseX,
        int mouseY,
        float partialTicks,
        Operation<Void> original
    ) {
        //#if MC>=12000
        //$$ UDrawContext drawContext = new UDrawContext(context, new UMatrixStack(context.getMatrices()));
        //#else
        UDrawContext drawContext = new UDrawContext(new UMatrixStack(vMatrixStack));
        //#endif

        GuiDrawScreenEvent.Priority preEvent = new GuiDrawScreenEvent.Priority(screen, drawContext, mouseX, mouseY, partialTicks, false);
        Essential.EVENT_BUS.post(preEvent);

        if (preEvent.getMouseX() != preEvent.getOriginalMouseX()) {
            mouseX = preEvent.getMouseX();
        }
        if (preEvent.getMouseY() != preEvent.getOriginalMouseY()) {
            mouseY = preEvent.getMouseY();
        }

        original.call(
            screen,
            //#if MC>=12000
            //$$ context,
            //#else
            vMatrixStack,
            //#endif
            mouseX,
            mouseY,
            partialTicks
        );

        Essential.EVENT_BUS.post(new GuiDrawScreenEvent.Priority(screen, drawContext, mouseX, mouseY, partialTicks, true));
    }
}
