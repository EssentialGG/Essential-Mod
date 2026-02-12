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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gg.essential.mixins.impl.client.gui.EssentialScreenMayAllowPlayerInput;
import gg.essential.universal.UScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class Mixin_FixKeybindUnpressedInScreensThatAllowPlayerMovement {

    //#if MC>=11202
    @WrapWithCondition(method = "displayGuiScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;unPressAllKeys()V"))
    //#else
    //$$ @WrapWithCondition(method = "setIngameNotInFocus", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;unPressAllKeys()V"))
    //#endif
    private boolean essential$isNotScreenThatAllowsPlayerInputPassthrough() {
        GuiScreen screen = UScreen.getCurrentScreen();
        return !(screen instanceof EssentialScreenMayAllowPlayerInput && ((EssentialScreenMayAllowPlayerInput) screen).allowsPlayerInput());
    }
}
