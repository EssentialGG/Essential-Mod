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
import gg.essential.mixins.impl.client.gui.EssentialGuiScreenBeforeClose;
import gg.essential.mixins.impl.client.gui.EssentialPostScreenDrawHook;
import gg.essential.mixins.impl.client.gui.GuiDragDropEntryHandler;
import gg.essential.mixins.transformers.client.gui.ResourcePackListEntryAccessor;
import net.minecraft.client.gui.GuiResourcePackList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.resources.ResourcePackListEntry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12109
//$$ import gg.essential.mixins.impl.client.gui.ResourcePackListWrapper;
//$$ import net.minecraft.client.gui.screen.pack.PackListWidget.Entry;
//#endif

//#if MC >= 12004
//$$ import gg.essential.mixins.transformers.client.gui.EntryListWidgetAccessor;
//#endif

//#if MC >= 11904
//$$ import gg.essential.mixins.impl.client.gui.EssentialEntryAtScreenPosAccess;
//#endif

//#if MC>=11600
//$$ import net.minecraft.client.gui.screen.PackLoadingManager;
//$$ import net.minecraft.resources.ResourcePackInfo;
//$$ import gg.essential.mixins.transformers.client.gui.PackLoadingManagerAccessor;
//$$ import gg.essential.mixins.transformers.client.gui.AbstractListAccessor;
//$$ import gg.essential.mixins.transformers.client.gui.PackScreenAccessor;
//$$ import net.minecraft.util.text.ITextComponent;
//$$ import org.spongepowered.asm.mixin.Final;
//#else
import net.minecraft.client.gui.GuiResourcePackAvailable;
import net.minecraft.client.gui.GuiResourcePackSelected;
//#endif

//#if MC<11200
//$$ import net.minecraft.client.resources.ResourcePackListEntryDefault;
//#endif

import java.util.List;

import static gg.essential.mixins.impl.client.gui.GuiDragDropEntryHandler.initResourcePackIndicator;

@Mixin(GuiScreenResourcePacks.class)
public abstract class Mixin_DragDropUI_ResourcePacksScreen
    extends GuiScreen
    implements EssentialPostScreenDrawHook
    //#if MC>=12109
    //$$ , EssentialGuiDraggableEntryScreen<Entry>
    //#else
    , EssentialGuiDraggableEntryScreen<ResourcePackListEntry>
    //#endif
    , EssentialGuiScreenBeforeClose
{
    @Unique
    //#if MC>=12109
    //$$ private @Nullable GuiDragDropEntryHandler<Entry> guiDragHandler = null;
    //#else
    private @Nullable GuiDragDropEntryHandler<ResourcePackListEntry> guiDragHandler = null;
    //#endif


    //#if MC>=12109
    //$$ @SuppressWarnings("unchecked") @Unique private List<Entry> getAvailablePacksList() {return ResourcePackListWrapper.of(((AbstractListAccessor<Entry>) this.availablePackList).essential$getChildrenList());}
    //$$ @SuppressWarnings("unchecked") @Unique private List<Entry> getSelectedPacksList() {return ResourcePackListWrapper.of(((AbstractListAccessor<Entry>) this.selectedPackList).essential$getChildrenList());}
    //#elseif MC>=12004
    //$$ @Unique private List<ResourcePackEntry> getAvailablePacksList() {return this.availablePackList.children();}
    //$$ @Unique private List<ResourcePackEntry> getSelectedPacksList() {return this.selectedPackList.children();}
    //#elseif MC>=11600
    //$$ @Unique private List<ResourcePackEntry> getAvailablePacksList() {return ((AbstractListAccessor)this.field_238891_u_).essential$getChildrenList();}
    //$$ @Unique private List<ResourcePackEntry> getSelectedPacksList() {return ((AbstractListAccessor)this.field_238892_v_).essential$getChildrenList();}
    //#else
    @Shadow private List<ResourcePackListEntry> availableResourcePacks;
    @Shadow private List<ResourcePackListEntry> selectedResourcePacks;
    @Unique private List<ResourcePackListEntry> getAvailablePacksList() {return availableResourcePacks;}
    @Unique private List<ResourcePackListEntry> getSelectedPacksList() {return selectedResourcePacks;}
    //#endif

    //#if MC>=11600
    //$$ @Shadow private ResourcePackList field_238891_u_;
    //$$ @Shadow private ResourcePackList field_238892_v_;
    //$$
    //$$ @Unique private ResourcePackList getAvailableResourcePacksWidget() {return this.field_238891_u_;}
    //$$ @Unique private ResourcePackList getSelectedResourcePacksWidget() {return this.field_238892_v_;}
    //$$
    //$$ @Final @Shadow private PackLoadingManager field_238887_q_;
    //$$
    //$$ @Unique private void markChanged() {
        //#if MC>=12109
        //$$ // The argument specifies which entry to select after refreshing, but we handle selection ourselves
        //$$ // already, so we can just pass `null` here.
        //$$ ((PackLoadingManagerAccessor)this.organizer).essential$getChangeRunnable().accept(null);
        //#else
        //$$ ((PackLoadingManagerAccessor)this.field_238887_q_).essential$getChangeRunnable().run();
        //#endif
    //$$ }
    //#else
    @Shadow private GuiResourcePackAvailable availableResourcePacksList;
    @Shadow private GuiResourcePackSelected selectedResourcePacksList;

    // getters to have better multi-version consistency
    @Unique private GuiResourcePackAvailable getAvailableResourcePacksWidget() {return availableResourcePacksList;}
    @Unique private GuiResourcePackSelected getSelectedResourcePacksWidget() {return selectedResourcePacksList;}

    @Shadow public abstract void markChanged();
    //#endif

    @Override
    //#if MC>=12109
    //$$ public @Nullable GuiDragDropEntryHandler<Entry> essential$getDragHandlerOrNull() {
    //#else
    public @Nullable GuiDragDropEntryHandler<ResourcePackListEntry> essential$getDragHandlerOrNull() {
    //#endif
        return guiDragHandler;
    }

    @Unique
    private void drawDraggedEntry(GuiDrawScreenEvent event) {
        // called by drag handler instance, is not null
        assert guiDragHandler != null;

        // use mouse position for scroll calculations
        GuiDragDropEntryHandler.ScreenPosition mousePos = new GuiDragDropEntryHandler.ScreenPosition(event.getMouseX(), event.getMouseY());

        if (isPosWithinBoundsOf(getAvailableResourcePacksWidget(), mousePos) &&
                //#if MC>=12004
                //$$ getAvailableResourcePacksWidget().getMaxScroll() != 0
                //#elseif MC>=11600
                //$$ ((AbstractListAccessor)getAvailableResourcePacksWidget()).essential$getMaxScroll() != 0
                //#else
                getAvailableResourcePacksWidget().getMaxScroll() != 0
                //#endif
        ) {
            guiDragHandler.scrollIfDraggingNearTopOrBottom(mousePos.y(), getAvailableResourcePacksWidget());
        } else if (isPosWithinBoundsOf(getSelectedResourcePacksWidget(), mousePos) &&
                //#if MC>=12004
                //$$ getSelectedResourcePacksWidget().getMaxScroll() != 0
                //#elseif MC>=11600
                //$$ ((AbstractListAccessor)getSelectedResourcePacksWidget()).essential$getMaxScroll() != 0
                //#else
                getSelectedResourcePacksWidget().getMaxScroll() != 0
                //#endif
        ) {
            guiDragHandler.scrollIfDraggingNearTopOrBottom(mousePos.y(), getSelectedResourcePacksWidget());
        }

        guiDragHandler.drawDraggedEntryWithinBounds(event,
                getAvailableResourcePacksWidget().getListWidth(),
                //#if MC>=12004
                //$$ ((EntryListWidgetAccessor)getAvailableResourcePacksWidget()).essential$getItemHeight() - 4
                //#elseif MC>=11600
                //$$ ((AbstractListAccessor)getAvailableResourcePacksWidget()).essential$getItemHeight() - 4
                //#else
                getAvailableResourcePacksWidget().slotHeight - 4
                //#endif
                , 0
                // in 1.20.2+ the red "incompatible pack" background starts to render with different widths to the UIBlocks
                //#if MC>=12006
                //$$ , (guiDragHandler.isThisListTheDragOrigin(getAvailablePacksList()) ? getAvailableResourcePacksWidget() : getSelectedResourcePacksWidget())
                //$$            .getMaxScroll() > 0 ? 1 : -6 // difference here is due to vanilla changes that now consider the scrollbar in the width of the red background
                //#elseif MC>=12002
                //$$ , -6
                //#else
                , 0
                //#endif
                , 2, 1, width, height);

        // bounds of pack lists if needed
        // availableResourcePacksList.left+2,availableResourcePacksList.top+1,
        // selectedResourcePacksList.right,availableResourcePacksList.bottom);
    }

    @Inject(method = "initGui", at = @At(value = "HEAD"))
    private void clearDragHandler(CallbackInfo ci) {
        if (guiDragHandler != null) {
            // lists are always initialized if drag handler is already present
            guiDragHandler.close(getAvailablePacksList(), getSelectedPacksList());
            guiDragHandler = null;
        }
    }

    @Inject(method = "initGui", at = @At(value = "TAIL"))
    private void initDragHandler(CallbackInfo ci) {
        if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
            guiDragHandler = new GuiDragDropEntryHandler<>(
                    (GuiScreenResourcePacks) (Object) this,
                    this::markChanged,
                    () -> {
                        //#if MC>=12109
                        //$$ //noinspection unchecked
                        //$$ ((AbstractListAccessor<Entry>) getAvailableResourcePacksWidget()).essential$recalculateAllChildrenPositions();
                        //$$ //noinspection unchecked
                        //$$ ((AbstractListAccessor<Entry>) getSelectedResourcePacksWidget()).essential$recalculateAllChildrenPositions();
                        //#endif
                    },
                    this::dropDraggedEntry,
                    this::updateIndicatorsForChangedDragPos,
                    this::drawDraggedEntry,
                    (a, b) -> {},
                    ()->{
                        // to clear the list selections after a pending drag starts
                        //#if MC>=11600
                        //$$ getAvailableResourcePacksWidget().setSelected(null);
                        //$$ getSelectedResourcePacksWidget().setSelected(null);
                        //#endif
                        // no 1.12 or 1.8 action needed
                    },
                    initResourcePackIndicator((GuiScreenResourcePacks) (Object) this)
                    );
        }
    }

    /**
     * drop the dragged entry into the list at the position of the empty indicator, or release the entry to its origin
     */
    @Unique
    private void dropDraggedEntry() {
        // is called by drag handler instance, is not null
        assert guiDragHandler != null;

        guiDragHandler.placeDraggedEntryAtIndicatorOrReleaseToOrigin(getAvailablePacksList(), getSelectedPacksList(),
                (entry, fromIndex, toIndex, fromLeftList, toLeftList) -> {
                    //#if MC>=11600
                    //$$ List<ResourcePackInfo> right_enabledList = ((PackLoadingManagerAccessor)this.field_238887_q_).essential$getEnabledList();
                    //$$ List<ResourcePackInfo> left_disabledList = ((PackLoadingManagerAccessor)this.field_238887_q_).essential$getDisabledList();
                    //$$
                    //$$ ResourcePackInfo entryData = (fromLeftList ? left_disabledList : right_enabledList).remove(fromIndex);
                    //$$ if (toLeftList) {
                    //$$     left_disabledList.add(toIndex, entryData);
                    //$$ } else {
                    //$$     right_enabledList.add(toIndex, entryData);
                    //$$ }
                    //#endif
                }, (toLeftList, entry, index) -> {
                    //#if MC>=11600
                    //$$ if (toLeftList) {
                    //$$     getAvailableResourcePacksWidget().setSelected(entry);
                    //$$ } else {
                    //$$     getSelectedResourcePacksWidget().setSelected(entry);
                    //$$ }
                    //#endif
                    // no action needed in <1.16 as the resource-pack screen does not have selection behaviour then
                }
        );

        //#if MC<=11300
        // these flags need to be set after dragging, they are required for scrollbar functionality
        getSelectedResourcePacksWidget().setEnabled(true);
        getAvailableResourcePacksWidget().setEnabled(true);
        //#endif
    }

    @Unique
    private boolean isPosWithinBoundsOf(GuiResourcePackList list, GuiDragDropEntryHandler.ScreenPosition pos) {
        //#if MC>=12004
        //#if MC>=12109
        //$$ int header = 0;
        //#else
        //$$ int header = ((EntryListWidgetAccessor)list).essential$getHeaderHeight();
        //#endif
        //$$ double top = list.getY();
        //$$ int bottom = list.getBottom();
        //$$ int left = list.getX();
        //$$ int right = list.getRight();
        //#elseif MC>=11600
        //$$ int header = ((AbstractListAccessor)list).essential$getHeaderHeight();
        //$$ double top = ((AbstractListAccessor) list).essential$getTop();
        //$$ int bottom = ((AbstractListAccessor) list).essential$getBottom();
        //$$ int left = ((AbstractListAccessor) list).essential$getLeft();
        //$$ int right = ((AbstractListAccessor) list).essential$getRight();
        //#else
        int header = list.headerPadding;
        double top = list.top;
        int bottom = list.bottom;
        int left = list.left;
        int right = list.right;
        //#endif

        // an additional padding of 4 is applied, on all versions, to the top content padding for the small black gradient overlay at the top
        double visualHeaderSpace = header + 4 - list.getAmountScrolled();
        if (visualHeaderSpace > 0) {
            top += visualHeaderSpace;
        }
        // padding on the bottom is irrelevant as we already consider the mouse being there as "hovering the bottom slot"

        return pos.y() >= top && pos.y() <= bottom && pos.x() >= left && pos.x() <= right;
    }

    /**
     * check if the dragged objects center is over a list and if so, place an empty indicator entry at the position
     */
    @Unique
    private void updateIndicatorsForChangedDragPos(GuiDragDropEntryHandler.ScreenPosition dragPos) {
        // called by drag handler instance, is not null
        assert guiDragHandler != null;

        if (isPosWithinBoundsOf(getAvailableResourcePacksWidget(), dragPos)) {
            // replicate the vanilla behaviour by not supporting reordering of the available pack list
            guiDragHandler.handleIndicatorForNonReorderingList(getAvailablePacksList(),getSelectedPacksList());
        } else if (isPosWithinBoundsOf(getSelectedResourcePacksWidget(), dragPos)) {
            guiDragHandler.placeIndicatorInListAtIndex(getSelectedPacksList(),
                    //#if MC>=12109
                    //$$ getSelectedPacksList().indexOf(((EssentialEntryAtScreenPosAccess<Entry>)getSelectedResourcePacksWidget()).essential$getEntryAtScreenPosition(dragPos.x(), dragPos.y())),
                    //#elseif MC>=11904
                    //$$ getSelectedPacksList().indexOf(((EssentialEntryAtScreenPosAccess<ResourcePackEntry>)getSelectedResourcePacksWidget()).essential$getEntryAtScreenPosition(dragPos.x(), dragPos.y())),
                    //#elseif MC>=11600
                    //$$ getSelectedPacksList().indexOf(((AbstractListAccessor)getSelectedResourcePacksWidget()).essential$getEntryAtScreenPosition(dragPos.x(), dragPos.y())),
                    //#else
                    getSelectedResourcePacksWidget().getSlotIndexFromScreenCoords((int) dragPos.x(), (int) dragPos.y()),
                    //#endif
                    getAvailablePacksList(), getCountOfImmovableTopEntries(), getCountOfImmovableBottomEntries());
        } else {
            // ensure no empty indicator is present
            guiDragHandler.placeIndicatorWhenOutsideOfLists(getAvailablePacksList(), getSelectedPacksList());
        }
    }

    @Unique
    private int getCountOfImmovableBottomEntries() {
        // should only return 0 or 1 in 1.12 & 1.8, but check anyway for possible mod interference

        //#if MC>=11600
        //$$ List<ResourcePackInfo> selectedList = ((PackLoadingManagerAccessor) this.field_238887_q_).essential$getEnabledList();
        //#else
        List<ResourcePackListEntry> selectedList = getSelectedPacksList();
        //#endif

        if (selectedList.isEmpty()) return 0;

        // loop from the bottom and return the count upon finding the first non bottom-locked entry
        for (int i = 0; i < selectedList.size(); i++) {
            //#if MC>=11600
            //$$     ResourcePackInfo packInfo = selectedList.get(selectedList.size() - 1 - i);
            //$$     if (!packInfo.isOrderLocked() || packInfo.getPriority() != ResourcePackInfo.Priority.BOTTOM) {
            //$$         return i;
            //$$     }
            //#else
            // should only ever be 1 or 0 in 1.12, but check anyway for possible mod interference
            ResourcePackListEntry entry = selectedList.get(selectedList.size() - 1 - i);

            // is called by drag handler instance, is not null
            assert guiDragHandler != null;
            if (guiDragHandler.isIndicatorEntry(entry) // empty entry extends immovable instance for simpler constructor, so check this first
                    || ((ResourcePackListEntryAccessor) entry).essential$isMovable() // if the entry is movable
                    //#if MC>=11200
                    || entry.isServerPack()// at this point should only be blocked by the default pack, or any modded entry mimicing it
                    //#else
                    //$$ || !(entry instanceof ResourcePackListEntryDefault) // if the entry is not the default pack
                    //#endif
            ) {

                return i;
            }
            //#endif
        }
        // no unlocked entries
        return selectedList.size();

    }

    @Unique
    private int getCountOfImmovableTopEntries() {
        // should only return 0 or 1 in 1.12, but check anyway for possible mod interference
        // should only return 0 in 1.8, but check anyway for possible mod interference

        //#if MC>=11600
        //$$ List<ResourcePackInfo> selectedList = ((PackLoadingManagerAccessor) this.field_238887_q_).essential$getEnabledList();
        //#else
        List<ResourcePackListEntry> selectedList = getSelectedPacksList();
        //#endif

        if (selectedList.isEmpty()) return 0;

        // loop from the top and return the count upon finding the first non top-locked entry
        for (int i = 0; i < selectedList.size(); i++) {
            //#if MC>=11600
            //$$     ResourcePackInfo packInfo = selectedList.get(i);
            //$$     if (!(packInfo.isOrderLocked() && packInfo.getPriority() == ResourcePackInfo.Priority.TOP)) {
            //$$         return i;
            //$$     }
            //#else
            // should only ever be 1 or 0 in 1.12, but check anyway for possible mod interference
            ResourcePackListEntry entry = selectedList.get(i);

            assert guiDragHandler != null;
            if (guiDragHandler.isIndicatorEntry(entry) // empty entry extends immovable instance for simpler constructor, so check this first
                    || ((ResourcePackListEntryAccessor) entry).essential$isMovable() // if the entry is movable
                    //#if MC>=11200
                    || !entry.isServerPack() // if the entry is not a server pack, as we do not want the default pack at the bottom to trigger this
                    //#else
                    //$$ || entry instanceof ResourcePackListEntryDefault // if the entry is the default pack
                    //#endif
            ) {
                return i;
            }
            //#endif
        }
        // no unlocked entries
        return selectedList.size();
    }

    @Override
    public void essential$beforeClose() {
        if (guiDragHandler != null) {
            guiDragHandler.close(getAvailablePacksList(), getSelectedPacksList());
            guiDragHandler = null;
        }
    }

    @Override
    public int essential$getQuickSwapIndex() {
        return getCountOfImmovableTopEntries();
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

    //constructor
    //#if MC>=11600
    //$$  protected Mixin_DragDropUI_ResourcePacksScreen() {super(null);}
    //#endif
}

