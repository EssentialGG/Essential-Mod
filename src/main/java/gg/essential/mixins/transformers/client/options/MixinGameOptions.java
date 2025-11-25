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
package gg.essential.mixins.transformers.client.options;

import gg.essential.key.EssentialKeybindingRegistry;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if FORGE==0
//$$ import org.spongepowered.asm.mixin.Mutable;
//#endif

@Mixin(GameSettings.class)
public class MixinGameOptions {

    //#if FORGE==0
    //$$ @Mutable
    //#endif
    @Shadow
    public KeyBinding[] keyBindings;

    @Unique
    private boolean essentialBindingsRegistered;

    @Inject(method = "loadOptions", at = @At("HEAD"))
    private void registerEssentialKeyBindings(CallbackInfo ci) {
        if (this.essentialBindingsRegistered) {
            return;
        }
        this.essentialBindingsRegistered = true;

        this.keyBindings = EssentialKeybindingRegistry.getInstance().registerKeyBinds(this.keyBindings);
    }
}
