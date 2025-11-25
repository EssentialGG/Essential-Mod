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

import gg.essential.sps.WindowTitleManager;
import net.minecraft.client.gui.GuiShareToLan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiShareToLan.class)
public class Mixin_UpdateWindowTitle_OpenToLan {

    //#if MC<11600
    @Inject(method = "actionPerformed", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/integrated/IntegratedServer;shareToLAN(Lnet/minecraft/world/GameType;Z)Ljava/lang/String;", shift = At.Shift.AFTER))
    private void onActionPerformed(CallbackInfo ci) {
        WindowTitleManager.INSTANCE.updateTitle();
    }
    //#endif
}
