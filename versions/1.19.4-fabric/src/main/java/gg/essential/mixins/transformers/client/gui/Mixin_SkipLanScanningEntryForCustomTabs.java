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
import gg.essential.config.EssentialConfig;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(MultiplayerServerListWidget.class)
public class Mixin_SkipLanScanningEntryForCustomTabs {

    @Shadow @Final private MultiplayerServerListWidget.Entry scanningEntry;

    //#if MC>=12109
    //$$ @WrapWithCondition(method = "updateEntries", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    //$$ private boolean shouldKeepScanEntry(java.util.List<?> instance, Object entry) {
    //#else
    @WrapWithCondition(method = "updateEntries", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerServerListWidget;addEntry(Lnet/minecraft/client/gui/widget/EntryListWidget$Entry;)I"))
    private boolean shouldKeepScanEntry(MultiplayerServerListWidget instance, @Coerce Element entry) {
    //#endif
        if (!EssentialConfig.INSTANCE.getEssentialEnabled()) return true;
        if (this.scanningEntry != entry) {
            return true;
        }
        return EssentialConfig.INSTANCE.getCurrentMultiplayerTab() == 0;
    }

}
