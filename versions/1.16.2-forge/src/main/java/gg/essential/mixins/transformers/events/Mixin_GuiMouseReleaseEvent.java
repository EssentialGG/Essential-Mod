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

import gg.essential.Essential;
import gg.essential.event.gui.GuiMouseReleaseEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHelper.class)
public class Mixin_GuiMouseReleaseEvent {

    @Inject(method = { "lambda$mouseButtonCallback$1", "func_198027_a", "lambda$onPress$1", "method_1605", "m_168078_" }, at = @At("HEAD"), cancellable = true, remap = false)
    //#if MC>=11700 && FORGE==0 || MC>=12006
    //$$ static
    //#endif
    private void onMouseReleased(
        boolean[] result,
        //#if MC>=11700 && FORGE==0 || MC>=11800
        //$$ Screen screenArg,
        //#endif
        double mouseX, double mouseY, int mouseButton, CallbackInfo ci
    ) {
        Screen screen = Minecraft.getInstance().currentScreen;
        Essential.EVENT_BUS.post(new GuiMouseReleaseEvent(screen, mouseButton));
        if (Minecraft.getInstance().currentScreen != screen) {
            result[0] = true;
            ci.cancel(); // screen was closed, prevent the event from reaching the new screen
        }
    }
}
