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
// 1.16 - 1.21
package gg.essential.mixins.transformers.events;

import gg.essential.Essential;
import gg.essential.event.gui.GuiClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MouseHelper.class, priority = 500)
public abstract class Mixin_GuiClickEvent_Priority  {

    @Inject(method = { "lambda$mouseButtonCallback$0", "func_198033_b", "lambda$onPress$0", "m_168084_", "method_1611" }, at = @At("HEAD"), cancellable = true, remap = false)
    //#if MC>=11700 && FORGE==0 || MC>=12006
    //$$ static
    //#endif
    private void onMouseClicked(
        boolean[] result,
        //#if MC>=11700 && FORGE==0 || MC>=11800
        //$$ Screen screenArg,
        //#endif
        double mouseX, double mouseY, int mouseButton, CallbackInfo ci
    ) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.currentScreen;
        GuiClickEvent event = new GuiClickEvent.Priority(mouseX, mouseY, mouseButton, screen);
        Essential.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            result[0] = true;
            ci.cancel();
        } else if (mc.currentScreen != screen) {
            result[0] = true;
            ci.cancel(); // screen was closed, prevent the event from reaching the new screen
        }
    }
}
