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

import gg.essential.Essential;
import gg.essential.event.gui.GuiKeyTypedEvent;
import net.minecraft.client.gui.GuiMainMenu;
import gg.essential.mixins.impl.client.gui.GuiMainMenuHook;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {
    private final GuiMainMenuHook guiMainMenuHook = new GuiMainMenuHook((GuiMainMenu) (Object) this);

    //#if MC>=11400
    //$$ // We now catch keyboard events before they get to the Screen instance
    //#else
    // GuiMainMenu does not call super method, so our hook needs to be called explicitly
    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void keyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (guiMainMenuHook.keyTyped(typedChar, keyCode).isCancelled()) {
            ci.cancel();
        }
    }

    // Same reason as above
    @Inject(method = "keyTyped", at = @At("TAIL"))
    private void keyTyped2(char typedChar, int keyCode, CallbackInfo ci) {
        Essential.EVENT_BUS.post(new GuiKeyTypedEvent.Post((GuiScreen) (Object) this, typedChar, keyCode));
    }
    //#endif
}
