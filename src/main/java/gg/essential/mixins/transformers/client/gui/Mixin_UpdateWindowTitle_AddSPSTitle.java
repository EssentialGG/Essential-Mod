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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.essential.config.EssentialConfig;
import gg.essential.util.ServerType;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class Mixin_UpdateWindowTitle_AddSPSTitle {

    //#if MC>=11600
    //$$ @ModifyExpressionValue(method = "getWindowTitle", at = @At(value = "CONSTANT", args = "stringValue=title.multiplayer.other"))
    //$$ public String modifyMultiplayerWindowTitleOther(String original) {
    //$$     if (EssentialConfig.INSTANCE.getReplaceWindowTitle() && ServerType.Companion.current() instanceof ServerType.SPS) {
    //$$         return "title.multiplayer.hosted";
    //$$     }
    //$$     return original;
    //$$ }
    //$$
    //$$ @ModifyExpressionValue(method = "getWindowTitle", at = @At(value = "CONSTANT", args = "stringValue=title.multiplayer.lan"))
    //$$ public String modifyMultiplayerWindowTitleLAN(String original) {
    //$$     if (EssentialConfig.INSTANCE.getReplaceWindowTitle() && ServerType.Companion.current() instanceof ServerType.SPS) {
    //$$         return "title.multiplayer.hosted";
    //$$     }
    //$$     return original;
    //$$ }
    //#endif
}