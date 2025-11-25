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
import gg.essential.event.gui.GuiClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHelper.class)
public abstract class Mixin_GuiClickEvent {

    //#if FORGE
    //#if MC>=12006
    //$$ @Inject(method = "method_1611", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/event/ForgeEventFactoryClient;onScreenMouseClicked([ZLnet/minecraft/client/gui/screens/Screen;DDI)V"), cancellable = true)
    //#elseif MC>=11700
    //$$ @Inject(method = { "lambda$onPress$0", "m_168084_" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(DDI)Z"), cancellable = true)
    //#else
    @Inject(method = { "lambda$mouseButtonCallback$0", "func_198033_b" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;mouseClicked(DDI)Z"), cancellable = true)
    //#endif
    //#else
    //$$ @Inject(method = "method_1611", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;mouseClicked(DDI)Z"), cancellable = true)
    //#endif
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
        Screen screen = Minecraft.getInstance().currentScreen;
        GuiClickEvent event = new GuiClickEvent(mouseX, mouseY, mouseButton, screen);
        Essential.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            result[0] = true;
            ci.cancel();
        } else if (Minecraft.getInstance().currentScreen != screen) {
            result[0] = true;
            ci.cancel(); // screen was closed, prevent the event from reaching the new screen
        }
    }
}
