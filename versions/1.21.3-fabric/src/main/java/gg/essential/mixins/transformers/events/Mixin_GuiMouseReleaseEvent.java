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
import gg.essential.Essential;
import gg.essential.event.gui.GuiMouseReleaseEvent;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class Mixin_GuiMouseReleaseEvent {
    //#if FORGE
    //$$ private static final String MOUSE_RELEASED = "Lnet/minecraftforge/client/event/ForgeEventFactoryClient;onScreenMouseReleased(Lnet/minecraft/client/gui/screens/Screen;DDI)Z";
    //#else
    private static final String MOUSE_RELEASED = "Lnet/minecraft/client/gui/screen/Screen;mouseReleased(DDI)Z";
    //#endif

    @Inject(method = "onMouseButton", at = @At(value = "INVOKE", target = MOUSE_RELEASED))
    private void onMouseClicked(CallbackInfo ci, @Local(ordinal = 0) Screen screen) {
        Essential.EVENT_BUS.post(new GuiMouseReleaseEvent(screen));
    }
}
