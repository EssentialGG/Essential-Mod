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
package gg.essential.mixins.transformers.feature.multiplayer_tabs;

import gg.essential.config.EssentialConfig;
import net.minecraft.client.gui.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin effectively prevents other mods from calling `updateOnlineServers`
 * while we are displaying one of our custom tabs. This allows us to avoid issues
 * where we expect to be in full control of the server list.
 */
@Mixin(ServerSelectionList.class)
public class Mixin_PreventThirdPartyReloadOnCustomTab {
    @Inject(method = "updateOnlineServers", at = @At("HEAD"), cancellable = true)
    private void preventThirdPartyReloadOnCustomTab(ServerList serverList, CallbackInfo ci) {
        int currentTab = EssentialConfig.INSTANCE.getCurrentMultiplayerTab();
        if (currentTab != 0) {
            ci.cancel();
        }
    }
}
