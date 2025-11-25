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
package gg.essential.mixins.transformers.feature.emote;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.essential.gui.emotes.EmoteWheel;
import net.minecraft.client.Keyboard;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(Keyboard.class)
public abstract class Mixin_AllowMovementDuringEmoteWheel_HandleKeys {
    @ModifyExpressionValue(
        method = "onKey",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", ordinal = 0),
        //#if MC>=12109
        //$$ slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=keyPressed event handler"))
        //#elseif MC>=12102
        //$$ slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;keyPressed(III)Z"))
        //#else
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
        //#endif
    )
    private Screen essential$emoteWheelAllowsMovement(Screen screen) {
        if (screen instanceof EmoteWheel) {
            screen = null;
        }
        return screen;
    }
}
