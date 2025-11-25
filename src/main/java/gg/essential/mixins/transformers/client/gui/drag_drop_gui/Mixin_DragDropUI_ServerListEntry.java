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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import gg.essential.config.EssentialConfig;
import gg.essential.mixins.impl.client.gui.EssentialGuiDraggableEntryScreen;
import gg.essential.mixins.impl.client.gui.GuiDragDropEntryHandler;
import gg.essential.mixins.transformers.client.gui.GuiMultiplayerAccessor;
import gg.essential.mixins.transformers.client.gui.ServerSelectionListAccessor;
import gg.essential.universal.USound;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.gui.ServerSelectionList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=12109
//$$ import net.minecraft.client.gui.Click;
//#endif

//#if MC>=11600
//$$ import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
//$$ import com.llamalad7.mixinextras.sugar.Local;
//$$ import gg.essential.mixins.transformers.client.gui.AbstractListAccessor;
//#else
//#endif

import java.util.List;

@Mixin(ServerListEntryNormal.class)
public abstract class Mixin_DragDropUI_ServerListEntry {

    // method name set here for consistency, including the instances where it is already within a preprocessed block
    //#if MC>=11600
    //$$ @Unique private static final String MOUSE_CLICKED = "mouseClicked";
    //#else
    @Unique private static final String MOUSE_CLICKED = "mousePressed";
    //#endif

    @Shadow @Final private GuiMultiplayer owner;

    @Inject(method = MOUSE_CLICKED, at = @At(value = "INVOKE"
            //#if MC>=12109
            //$$ , target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerServerListWidget$ServerEntry;connect()V"
            //#else
            , target = "Lnet/minecraft/client/gui/GuiMultiplayer;connectToSelected()V"
            //#endif
            , shift = At.Shift.AFTER))
    private void playSoundAfterConnect(final CallbackInfoReturnable<Boolean> cir, @Share("serverConnection") final LocalBooleanRef serverConnection) {
        // playing this sound after will add a slight delay to the sound due to connection logic
        // however if run before, the sound will get cut out due to the impacts of calling connectToSelected()
        if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
            USound.INSTANCE.playButtonPress();
            serverConnection.set(true);
        }
    }

    //#if MC>=11600
    //$$ @Inject(method = {MOUSE_CLICKED}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ServerSelectionList$NormalEntry;func_228196_a_(II)V", shift = At.Shift.AFTER))
    //$$    private void playSoundAfterMove(final CallbackInfoReturnable<Boolean> cir) {
    //$$        if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
    //$$            USound.INSTANCE.playButtonPress();
    //$$        }
    //$$    }
    //#else
    @Inject(method = {MOUSE_CLICKED}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiMultiplayer;moveServerUp(Lnet/minecraft/client/gui/ServerListEntryNormal;IZ)V", shift = At.Shift.AFTER))
    private void playSoundAfterMoveUp(final CallbackInfoReturnable<Boolean> cir) {
        if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
            USound.INSTANCE.playButtonPress();
        }
    }

    @Inject(method = {MOUSE_CLICKED}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiMultiplayer;moveServerDown(Lnet/minecraft/client/gui/ServerListEntryNormal;IZ)V", shift = At.Shift.AFTER))
    private void playSoundAfterMoveDown(final CallbackInfoReturnable<Boolean> cir) {
        if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
            USound.INSTANCE.playButtonPress();
        }
    }
    //#endif


    //#if MC>=11600
    //$$ // these are used to capture the relative mouse coordinates local variables, which are not held up to the TAIL inject below
    //#if MC>=12109
    //$$ @ModifyVariable(method = MOUSE_CLICKED, at = @At(value = "STORE"), ordinal = 0)
    //#else
    //$$ @ModifyVariable(method = MOUSE_CLICKED, at = @At(value = "STORE"), ordinal = 2)
    //#endif
    //$$ private double captureRelativeMouseCoordinatesX(final double value, @Share("relativeX") LocalDoubleRef relativeXSet) {
    //$$     if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
    //$$         // do nothing, just to capture the relative mouse coordinates
    //$$         relativeXSet.set(value);
    //$$     }
    //$$     return value;
    //$$ }
    //$$
    //#if MC>=12109
    //$$ @ModifyVariable(method = MOUSE_CLICKED, at = @At(value = "STORE"), ordinal = 1)
    //#else
    //$$ @ModifyVariable(method = MOUSE_CLICKED, at = @At(value = "STORE"), ordinal = 3)
    //#endif
    //$$ private double captureRelativeMouseCoordinatesY(final double value, @Share("relativeY") LocalDoubleRef relativeYSet) {
    //$$     if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
    //$$         // do nothing, just to capture the relative mouse coordinates
    //$$         relativeYSet.set(value);
    //$$     }
    //$$     return value;
    //$$ }
    //#endif

    // mixin targets the mousePressed method after vanilla buttons have been checked and would have returned from the method
    @Inject(method = {MOUSE_CLICKED},
            //#if MC>=12109
            //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerServerListWidget$Entry;mouseClicked(Lnet/minecraft/client/gui/Click;Z)Z"
            //#elseif MC>=11600
            //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;milliTime()J", ordinal = 1
            //#else
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J", ordinal = 1
            //#endif
            ))
    private void grabHoverEntry(
            //#if MC>=11600
            //#if MC>=12109
            //$$ Click click, boolean doubled,
            //#else
            //$$ double mouseX, double mouseY, int button,
            //#endif
            //$$ final CallbackInfoReturnable<Boolean> cir,
            //$$ @Share("serverConnection") final LocalBooleanRef serverConnection, @Share("relativeX") LocalDoubleRef relativeXGet, @Share("relativeY") LocalDoubleRef relativeYGet
            //#else
            final int slotIndex, final int mouseX, final int mouseY, final int mouseEvent, final int relativeX, final int relativeY, final CallbackInfoReturnable<Boolean> cir,
            @Share("serverConnection") final LocalBooleanRef serverConnection
            //#endif
    ) {
        if (EssentialConfig.INSTANCE.getCurrentMultiplayerTab() != 0) return;

        // a double click server connection is the only way this would be true at this injection point so exit out
        if (serverConnection.get()) return;

        //#if MC>=11600
        //$$ GuiDragDropEntryHandler<ServerSelectionList.Entry> dragHandler = ((EssentialGuiDraggableEntryScreen<ServerSelectionList.Entry>) this.owner).essential$getDragHandlerOrNull();
        //#else
        GuiDragDropEntryHandler<ServerListEntryNormal> dragHandler = ((EssentialGuiDraggableEntryScreen<ServerListEntryNormal>) this.owner).essential$getDragHandlerOrNull();
        //#endif

        if (dragHandler != null && !dragHandler.isDraggingEntry()) {
            ServerListEntryNormal selectedEntry = (ServerListEntryNormal) (Object) this;
            ServerSelectionList serverListSelector = ((GuiMultiplayerAccessor) this.owner).getServerListSelector();

            //#if MC>=11600
            //$$ List<ServerSelectionList.Entry> container = (List<ServerSelectionList.Entry>) (List<?>) ((ServerSelectionListAccessor) serverListSelector).getServerListInternet();
            //#else
            List<ServerListEntryNormal> container = ((ServerSelectionListAccessor) serverListSelector).getServerListInternet();
            //#endif

            // bail if the entry is not in the list, possibly due to a mod
            if (!container.contains(selectedEntry)) return;

            //#if MC>=12109
            //$$ double mouseX = click.x();
            //$$ double mouseY = click.y();
            //#endif

            //#if MC>=11600
            //$$ int slotIndex = container.indexOf(selectedEntry);
            //$$ double relativeX = relativeXGet.get();
            //$$ double relativeY = relativeYGet.get();
            //#endif

            dragHandler.setPendingDraggedEntryState(selectedEntry, container, relativeX, relativeY,
                    slotIndex, mouseX, mouseY, true);
        }
    }

    /**
     * disables the hover visuals while dragging entries
     */
    @ModifyVariable(method = "drawEntry", at = @At(value = "HEAD"), ordinal = 0, argsOnly = true)
    private boolean hideSelectionVisuals(boolean isSelected) {
        GuiDragDropEntryHandler<?> dragHandler = ((EssentialGuiDraggableEntryScreen<?>) this.owner).essential$getDragHandlerOrNull();
        if (dragHandler != null && dragHandler.isDraggingEntry()) {
            return false;
        }
        return isSelected;
    }

    /**
     * disables the hover visuals while dragging entries
     */
    //#if MC>=11900
    //$$ @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    //$$ private Object hideSelectionVisualsTouchscreen(final Object touchscreen) {
    //#else
    @ModifyExpressionValue(method = "drawEntry", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;touchscreen:Z"))
    private boolean hideSelectionVisualsTouchscreen(boolean touchscreen) {
    //#endif
        GuiDragDropEntryHandler<?> dragHandler = ((EssentialGuiDraggableEntryScreen<?>) this.owner).essential$getDragHandlerOrNull();
        if (dragHandler != null && dragHandler.isDraggingEntry()) {
            return false;
        }
        return touchscreen;
    }

}
