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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// this is a fix to fire our GuiDrawScreenEvents for the title screen when fancymenu is installed
// specifically this affects fancy menu's post 1.20.6 implementations from v3.2.1
// which they fixed on their end in v3.4, but only for 1.21.1+, necessitating our own fix for 1.20.6 && 1.21

// the cause is an always false WrapWithCondition on TitleScreen.render()'s super.render() call done by fancymenu
// https://github.com/Keksuccino/FancyMenu/blob/da45916ccfd138ace0d87dd719fcf56591a3e677/common/src/main/java/de/keksuccino/fancymenu/mixin/mixins/common/client/MixinTitleScreen.java#L69
// this mixin still renders all the drawables, so we should still try and retain pre/post draw event consistency

// file is overwritten and disabled again in 1.21.2+

@Mixin(TitleScreen.class)
public class Mixin_FancyMainMenu_3_2_1_GuiDrawScreenEvent {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    private void emitEssentialPreEvent(final DrawContext context, final int mouseX, final int mouseY, final float delta, final CallbackInfo ci) {
        // fire pre draw event here as super.render is always cancelled by fancy menu in these versions
        Essential.EVENT_BUS.post(new GuiDrawScreenEvent(
                (Screen) (Object) this,
                new UDrawContext(context, new UMatrixStack(context.getMatrices())),
                mouseX, mouseY, delta, false));
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            shift = At.Shift.AFTER))
    private void emitEssentialPostEvent(final DrawContext context, final int mouseX, final int mouseY, final float delta, final CallbackInfo ci) {
        // fire post draw event here as super.render is always cancelled by fancy menu in these versions
        Essential.EVENT_BUS.post(new GuiDrawScreenEvent(
                (Screen) (Object) this,
                new UDrawContext(context, new UMatrixStack(context.getMatrices())),
                mouseX, mouseY, delta, true));

    }
}
