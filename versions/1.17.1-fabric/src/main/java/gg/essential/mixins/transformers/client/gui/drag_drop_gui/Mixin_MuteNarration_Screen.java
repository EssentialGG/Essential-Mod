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
package gg.essential.mixins.transformers.client.gui.drag_drop_gui;

import gg.essential.mixins.impl.client.gui.EssentialGuiDraggableEntryScreen;
import gg.essential.mixins.impl.client.gui.GuiDragDropEntryHandler;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class Mixin_MuteNarration_Screen {

    // Before 1.17 the narrator only reads out the screen title once opened, so we don't need to cancel the narrations there.
    // But in 1.17+ the narrator reads details about the lists and also any updates to these details, so we must prevent
    // it during dragging as the state of the lists is frequently changing and the narrator would be excessive.
    //
    // This only silences new narrations, the previous one will still finish reading out, which is good as that will
    // still hold relevant information about the screen and the entry & list that was last hovered.
    @Inject(method = "narrateScreen", at = @At("HEAD"), cancellable = true)
    private void preventNarrationOnDrag(CallbackInfo ci) {
        // if we are in a screen that has a drag handler, and it is currently dragging an entry, cancel the narration
        if (this instanceof EssentialGuiDraggableEntryScreen<?> handlerAccess) {
            GuiDragDropEntryHandler<?> dragHandler = handlerAccess.essential$getDragHandlerOrNull();
            if (dragHandler != null && dragHandler.isDraggingEntry()) {
                ci.cancel();
            }
        }
    }
}