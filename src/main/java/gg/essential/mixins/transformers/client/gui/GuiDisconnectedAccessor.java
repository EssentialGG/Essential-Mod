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

import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC>=12100
//$$ import net.minecraft.network.DisconnectionInfo;
//#endif

@Mixin(GuiDisconnected.class)
public interface GuiDisconnectedAccessor {

    //#if MC>=12100
    //$$ @Accessor("info")
    //$$ DisconnectionInfo getInfo();
    //#else
    @Accessor("message")
    ITextComponent getMessage();
    //#endif
}