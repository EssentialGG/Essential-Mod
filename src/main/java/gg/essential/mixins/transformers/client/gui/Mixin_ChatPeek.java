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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiNewChat.class)
public abstract class Mixin_ChatPeek {

    //#if MC>=12111
    //$$ private static final String RENDER = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V";
    //#else
    private static final String RENDER = "drawChat";
    //#endif

    @Shadow @Final
    private Minecraft mc;

    @ModifyVariable(method = RENDER, at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int essential$modifyUpdateCounter(int updateCounter) {
        return Essential.getInstance().getKeybindingRegistry().isHoldingChatPeek() ? 0 : updateCounter;
    }

    @ModifyVariable(method = RENDER, at = @At("STORE"),
        //#if MC>=11903
        //$$ ordinal = 3
        //#else
        ordinal = 1
        //#endif
    )
    private int essential$modifyChatLineLimit(int linesToDraw) {
        //#if MC>=11900
        //$$ double chatHeightFocused = this.client.options.getChatHeightFocused().getValue();
        //#elseif MC>=11600
        //$$ double chatHeightFocused = this.mc.gameSettings.chatHeightFocused;
        //#else
        float chatHeightFocused = this.mc.gameSettings.chatHeightFocused;
        //#endif
        return Essential.getInstance().getKeybindingRegistry().isHoldingChatPeek()
            ? GuiNewChat.calculateChatboxHeight(chatHeightFocused) / 9
            : linesToDraw;
    }
}
