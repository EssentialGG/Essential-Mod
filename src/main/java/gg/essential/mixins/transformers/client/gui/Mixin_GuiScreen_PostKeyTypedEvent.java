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
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=12109
//$$ import net.minecraft.client.input.KeyInput;
//#endif

//#if MC>=11600
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

@Mixin(GuiScreen.class)
public class Mixin_GuiScreen_PostKeyTypedEvent {

    @Inject(method =
            //#if MC>=11600
            //$$ "keyPressed"
            //#else
            "keyTyped"
            //#endif
            , at = @At("TAIL"))
    private void onKeyTyped(
            //#if MC>=12109
            //$$ final KeyInput input, final CallbackInfoReturnable<Boolean> cir
            //#elseif MC>=11600
            //$$ int keyCode, int scanCode, int modifiers, final CallbackInfoReturnable<Boolean> cir
            //#else
            char typedChar, int keyCode, CallbackInfo ci
            //#endif
            ) {

        //#if MC>=11600
        //$$ // Do not emit event if key press was already handled, incidentally this also won't run if this method got early returned
        //$$ if (cir.getReturnValue()) return;
        //$$
        //$$ char typedChar = '\0';
        //#endif

        //#if MC>=12109
        //$$ int keyCode = input.getKeycode();
        //#endif
        Essential.EVENT_BUS.post(new GuiKeyTypedEvent.Post((GuiScreen) (Object) this, typedChar, keyCode));
    }
}
