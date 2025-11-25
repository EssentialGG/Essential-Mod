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
package gg.essential.mixins.transformers.compatibility.fancymenu;

import gg.essential.Essential;
import gg.essential.event.gui.GuiDrawScreenEvent;
import gg.essential.universal.UMatrixStack;
import gg.essential.util.UDrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FancyMenu cancels the regular rendering of the Main Menu when customizations are enabled.
 * We still want to draw our main menu stuff though, so this mixin fires our post event from Fancy Menu's handler.
 */
@Pseudo
@Mixin(targets = "de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase", remap = false)
public class Mixin_FancyMainMenu_GuiDrawScreenEvent_Post {
    @Unique
    protected boolean emitPostEvent;

    @Inject(method = "onRenderPost", at = @At("TAIL"))
    private void emitEssentialPostEvent(@Coerce KonkreteDrawScreenEventAcc event, CallbackInfo ci) {
        if (!emitPostEvent) {
            return;
        }
        emitPostEvent = false;

        emitEssentialEvent(event, true);
    }

    @Unique
    protected void emitEssentialEvent(KonkreteDrawScreenEventAcc event, boolean post) {
        Essential.EVENT_BUS.post(new GuiDrawScreenEvent(
            event.invokeGetGui(),
            //#if MC>=12000
            //$$ new UDrawContext(event.invokeGetDrawContext(), new UMatrixStack(event.invokeGetDrawContext().getMatrices())),
            //#elseif MC>=11600
            //$$ new UDrawContext(new UMatrixStack(event.invokeGetMatrixStack())),
            //#else
            new UDrawContext(new UMatrixStack()),
            //#endif
            event.invokeGetMouseX(),
            event.invokeGetMouseY(),
            event.invokeGetRenderPartialTicks(),
            post
        ));
    }
}
