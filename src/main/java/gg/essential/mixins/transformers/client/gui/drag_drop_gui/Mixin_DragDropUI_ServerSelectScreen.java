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

import gg.essential.config.EssentialConfig;
import gg.essential.event.gui.GuiDrawScreenEvent;
import gg.essential.mixins.impl.client.gui.EssentialGuiDraggableEntryScreen;
import gg.essential.mixins.impl.client.gui.EssentialPostScreenDrawHook;
import gg.essential.mixins.impl.client.gui.GuiDragDropEntryHandler;
import gg.essential.mixins.transformers.client.gui.ServerSelectionListAccessor;
import gg.essential.mixins.transformers.client.multiplayer.ServerListAccessor;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 12004
//$$ import gg.essential.mixins.transformers.client.gui.EntryListWidgetAccessor;
//#endif

//#if MC >= 11904
//$$ import gg.essential.mixins.impl.client.gui.EssentialEntryAtScreenPosAccess;
//#endif

//#if MC>=11600
//$$ import net.minecraft.util.text.ITextComponent;
//$$ import gg.essential.mixins.transformers.client.gui.AbstractListAccessor;
//#else
import net.minecraft.client.gui.ServerListEntryNormal;
//#endif

import java.util.List;

import static gg.essential.mixins.impl.client.gui.GuiDragDropEntryHandler.initServerIndicator;

@Mixin(GuiMultiplayer.class)
public abstract class Mixin_DragDropUI_ServerSelectScreen extends GuiScreen implements EssentialPostScreenDrawHook,
        //#if MC>=11600
        //$$ EssentialGuiDraggableEntryScreen<ServerSelectionList.Entry> {
        //#else
        EssentialGuiDraggableEntryScreen<ServerListEntryNormal> {
        //#endif


    //#if MC>=11600
    //$$ @Shadow protected ServerSelectionList serverListSelector;
    //$$ @Unique private @Nullable GuiDragDropEntryHandler<ServerSelectionList.Entry> guiDragHandler = null;
    //#else
    @Shadow private ServerSelectionList serverListSelector;
    @Unique private @Nullable GuiDragDropEntryHandler<ServerListEntryNormal> guiDragHandler = null;
    //#endif

    @Shadow private ServerList savedServerList;

    //#if MC>=11600
    //$$  public GuiDragDropEntryHandler<ServerSelectionList.Entry> essential$getDragHandlerOrNull() {
    //#else
    public GuiDragDropEntryHandler<ServerListEntryNormal> essential$getDragHandlerOrNull() {
    //#endif
        return guiDragHandler;
    }

    @Unique
    private void saveChanges() {
        this.savedServerList.saveServerList();
    }

    @Unique
    private void drawDraggedEntry(GuiDrawScreenEvent event) {
        // is called by this instance of the drag handler, is not null
        assert guiDragHandler != null;

        // the dragged entry is always inside the server list so no need to check if the mouse is within the bounds during a drag like the pack screen
        //#if MC>=12004
        //$$ if (this.serverListWidget.getMaxScroll() != 0) {
        //#elseif MC>=11600
        //$$ if (((AbstractListAccessor)this.serverListSelector).essential$getMaxScroll() != 0) {
        //#else
        if (serverListSelector.getMaxScroll() != 0) {
            //#endif
            // use drag center position, rather than mouse, for consistent scrolling behaviour as these entries are bound within the list area
            guiDragHandler.scrollIfDraggingNearTopOrBottom(guiDragHandler.getDragCenterPos().y(), this.serverListSelector);
        }

        int entryWidth = this.serverListSelector.getListWidth();
        int entryLeft = width / 2 - entryWidth / 2 + 2;
        //#if MC>=12004
        //$$ int top = this.serverListWidget.getY();
        //$$ int bottom = this.serverListWidget.getBottom();
        //$$ int slotHeight = ((EntryListWidgetAccessor)this.serverListWidget).essential$getItemHeight();
        //#elseif MC>=11600
        //$$ int top = ((AbstractListAccessor) this.serverListSelector).essential$getTop();
        //$$ int bottom = ((AbstractListAccessor) this.serverListSelector).essential$getBottom();
        //$$ int slotHeight = ((AbstractListAccessor)this.serverListSelector).essential$getItemHeight();
        //#else
        int top = this.serverListSelector.top;
        int slotHeight = this.serverListSelector.slotHeight;
        int bottom = this.serverListSelector.bottom;
        //#endif

        guiDragHandler.drawDraggedEntryWithinBounds(event, entryWidth, slotHeight - 4, 5, 0, entryLeft, top + 2, entryLeft + entryWidth - 3, bottom);
    }

    @Inject(method = "initGui", at = @At(value = "HEAD"))
    private void clearDragHandler(CallbackInfo ci) {
        if (guiDragHandler != null) {
            guiDragHandler.close(null, null);
            guiDragHandler = null;
        }
    }

    @Inject(method = "initGui", at = @At(value = "TAIL"))
    private void initDragHandler(CallbackInfo ci) {
        if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
            guiDragHandler = new GuiDragDropEntryHandler<
                    //#if MC>=11600
                    //$$ServerSelectionList.Entry
                    //#else
                    ServerListEntryNormal
                    //#endif
                    >(
                    (GuiMultiplayer) (Object) this,
                    this::saveChanges,
                    //#if MC>=11600
                    //$$ ((ServerSelectionListAccessor)this.serverListSelector)::updateList,
                    //#else
                    () -> {
                    },
                    //#endif
                    this::dropDraggedEntry,
                    this::updateIndicatorsForChangedDragPos,
                    this::drawDraggedEntry,
                    this::onRevertedDrag,
                    //#if MC>=11600
                    //$$ ()-> this.serverListSelector.setSelected(null),
                    //#else
                    ()-> this.serverListSelector.setSelectedSlotIndex(-1),
                    //#endif
                    initServerIndicator((GuiMultiplayer) (Object) this, this.serverListSelector));
        }
    }

    @Unique
    private void onRevertedDrag(
            //#if MC>=11600
            //$$ ServerSelectionList.Entry entry
            //#else
            ServerListEntryNormal entry
            //#endif
            , int index) {
        //#if MC>=11600
        //$$ this.serverListSelector.setSelected(entry);
        //#else
        this.serverListSelector.setSelectedSlotIndex(index);
        //#endif
    }


    /**
     * drop the dragged entry into the list at the position of the empty indicator, or release the entry to its origin
     */
    @Unique
    private void dropDraggedEntry() {
        // the visual server list
        //#if MC>=11600
        //$$ List<ServerSelectionList.Entry> serverListVisual = (List<ServerSelectionList.Entry>) (List<?>)  ((ServerSelectionListAccessor) this.serverListSelector).getServerListInternet();
        //#else
        List<ServerListEntryNormal> serverListVisual = ((ServerSelectionListAccessor) serverListSelector).getServerListInternet();
        //#endif

        // is called by this instance of the drag handler, is not null
        assert guiDragHandler != null;

        guiDragHandler.placeDraggedEntryAtIndicatorOrReleaseToOrigin(serverListVisual, null,
                (entry, fromIndex, toIndex, fromLeftList, toLeftList) -> {
                    // if the final indexes are different, and not invalid -1
                    if (fromIndex != toIndex && toIndex >= 0 && fromIndex >= 0) {
                        // modify the saved server list to reflect the change
                        List<ServerData> serverListSaved = ((ServerListAccessor) this.savedServerList).getServers();
                        ServerData movingEntry = serverListSaved.remove(fromIndex);
                        serverListSaved.add(toIndex, movingEntry);
                    }
                }, (toLeftList, entry, index) -> {
                    //#if MC>=11600
                    //$$ this.serverListSelector.setSelected(entry);
                    //#else
                    this.serverListSelector.setSelectedSlotIndex(index);
                    //#endif
                });
    }

    /**
     * check if the dragged objects center is over a list and if so, place an empty indicator entry at the position
     */
    @Unique
    private void updateIndicatorsForChangedDragPos(GuiDragDropEntryHandler.ScreenPosition dragPos) {
        // is called by this instance of the drag handler, is not null
        assert guiDragHandler != null;

        //#if MC>=11600
        //$$ // the visual server list
        //$$ List<ServerSelectionList.Entry> serverList = (List<ServerSelectionList.Entry>) (List<?>)  ((ServerSelectionListAccessor) this.serverListSelector).getServerListInternet();
        //$$
        //$$ if (this.serverListSelector.isMouseOver(dragPos.x(), dragPos.y())) {
        //#else
        // the visual server list
        List<ServerListEntryNormal> serverList = ((ServerSelectionListAccessor) this.serverListSelector).getServerListInternet();

        if (this.serverListSelector.isMouseYWithinSlotBounds((int) dragPos.y())) {
            //#endif

            //#if MC>=12004
            //$$ int slotIndex = this.serverListWidget.children().indexOf(((EssentialEntryAtScreenPosAccess<MultiplayerServerListWidget.Entry>)this.serverListWidget).essential$getEntryAtScreenPosition(dragPos.x(), dragPos.y()));
            //#elseif MC>=11904
            //$$ int slotIndex = ((AbstractListAccessor)this.serverListWidget).essential$getChildrenList().indexOf(((EssentialEntryAtScreenPosAccess<MultiplayerServerListWidget.Entry>)this.serverListWidget).essential$getEntryAtScreenPosition(dragPos.x(), dragPos.y()));
            //#elseif MC>=11600
            //$$ int slotIndex = ((AbstractListAccessor)this.serverListSelector).essential$getChildrenList().indexOf(((AbstractListAccessor)this.serverListSelector).essential$getEntryAtScreenPosition(dragPos.x(), dragPos.y()));
            //#else
            int slotIndex = this.serverListSelector.getSlotIndexFromScreenCoords((int) dragPos.x(), (int) dragPos.y());
            //#endif

            // might be within the non-normal server slots
            if (slotIndex >= serverList.size()) slotIndex = -1;

            guiDragHandler.placeIndicatorInListAtIndex(serverList, slotIndex, null, 0, 0);
        } else {
            // ensure no empty indicator is present
            guiDragHandler.placeIndicatorWhenOutsideOfLists(serverList, null);
        }
    }

    @Inject(method = "onGuiClosed", at = @At(value = "HEAD"))
    private void onClose(CallbackInfo ci) {
        if (guiDragHandler != null) {
            guiDragHandler.close(null, null);
            guiDragHandler = null;
        }
    }

    //#if MC>=12109
    //$$ @Inject(method = "connect", at = @At(value = "HEAD"))
    //#else
    @Inject(method = "connectToSelected", at = @At(value = "HEAD"))
    //#endif
    private void onConnect(CallbackInfo ci) {
        // should be covered by the close method, but just in case
        if (guiDragHandler != null && guiDragHandler.isDraggingEntry()) {
            guiDragHandler.revertDraggedEntryToOriginalContainer(null, null);
        }
    }

    @Override
    public int essential$getQuickSwapIndex() {
        return -1;
    }

    //#if MC<11300
    // feature is not present in 1.16+
    @Inject(method = "handleMouseInput", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiScreen;handleMouseInput()V"
            , shift = At.Shift.AFTER), cancellable = true)
    private void handleMouseInput(CallbackInfo ci) {
        // prevent 1.12 "drag mouse to scroll" effect from occurring while dragging an entry
        // this also prevents mouse wheel scrolling while dragging, but we provide an alternative
        if (guiDragHandler != null && guiDragHandler.isDraggingEntry()) {
            ci.cancel();
        }
    }
    //#endif

    //#if MC>=11600
    //$$ protected Mixin_DragDropUI_ServerSelectScreen() {super(null);}
    //#endif
}
