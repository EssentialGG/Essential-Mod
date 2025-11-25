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
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.essential.config.EssentialConfig;
import gg.essential.mixins.impl.client.gui.EssentialGuiDraggableEntryScreen;
import gg.essential.mixins.impl.client.gui.GuiDragDropEntryHandler;
import gg.essential.universal.USound;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.resources.ResourcePackListEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=12109
//$$ import gg.essential.mixins.impl.client.gui.ResourcePackListWrapper;
//$$ import net.minecraft.client.gui.Click;
//$$ import net.minecraft.client.gui.screen.pack.PackListWidget.Entry;
//#endif

//#if MC>=11904
//$$ import gg.essential.mixins.transformers.client.gui.PackListWidgetAccessor;
//$$ import net.minecraft.client.MinecraftClient;
//#endif

//#if MC>=11600
//$$ import org.spongepowered.asm.mixin.injection.ModifyVariable;
//$$ import com.llamalad7.mixinextras.sugar.Share;
//$$ import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
//$$ import gg.essential.mixins.transformers.client.gui.PackLoadingManagerAccessor;
//$$ import net.minecraft.resources.ResourcePackInfo;
//$$ import gg.essential.mixins.transformers.client.gui.AbstractListAccessor;
//$$ import gg.essential.mixins.transformers.client.gui.PackScreenAccessor;
//$$ import net.minecraft.client.gui.widget.list.ResourcePackList;
//#endif

import java.util.List;

@Mixin(ResourcePackListEntry.class)
public abstract class Mixin_DragDropUI_ResourcePackListEntry {

    // method name set here for consistency, including the instances where it is already within a preprocessed block
    //#if MC>=11600
    //$$ @Unique private static final String MOUSE_CLICKED = "mouseClicked";
    //#else
    @Unique private static final String MOUSE_CLICKED = "mousePressed";
    //#endif

    //#if MC>=11904
    //$$ @Shadow @Final private PackListWidget widget; // indirect Screen access
    //#elseif MC>=11600
    //$$ @Shadow @Final protected Screen field_214429_b;
    //#else
    @Shadow @Final protected GuiScreenResourcePacks resourcePacksGUI;
    //#endif

    @Unique
    private GuiScreenResourcePacks getResourcePacksGUI() {
        //#if MC>=11904
        //$$ return ((PackListWidgetAccessor) this.widget).essential$getScreen();
        //#elseif MC>=11600
        //$$ return (PackScreen) this.field_214429_b;
        //#else
        return resourcePacksGUI;
        //#endif
    }

    @Shadow protected abstract boolean showHoverOverlay();

    //#if MC>=11600
    //$$ @Inject(method = {MOUSE_CLICKED}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/PackLoadingManager$IPack;func_230468_k_()V", shift = At.Shift.AFTER))
    //$$ private void playSoundAfterButtonMoveDown(final CallbackInfoReturnable<Boolean> cir) {
    //$$     if (EssentialConfig.INSTANCE.getEssentialEnabled()) USound.INSTANCE.playButtonPress();
    //$$ }
    //$$ @Inject(method = {MOUSE_CLICKED}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/PackLoadingManager$IPack;func_230467_j_()V", shift = At.Shift.AFTER))
    //$$ private void playSoundAfterButtonMoveUp(final CallbackInfoReturnable<Boolean> cir) {
    //$$     if (EssentialConfig.INSTANCE.getEssentialEnabled()) USound.INSTANCE.playButtonPress();
    //$$ }
    //$$ @Inject(method = {MOUSE_CLICKED}, at = @At(value = "INVOKE", target =
    //#if MC>=12109
    //$$    "Lnet/minecraft/client/gui/screen/pack/PackListWidget$ResourcePackEntry;enable()V"
    //#elseif MC>=11904
    //$$    "Lnet/minecraft/client/gui/screen/pack/PackListWidget$ResourcePackEntry;enable()Z"
    //#else
    //$$    "Lnet/minecraft/client/gui/screen/PackLoadingManager$IPack;func_230471_h_()V"
    //#endif
    //$$ , shift = At.Shift.AFTER))
    //$$ private void playSoundAfterButtonEnable(final CallbackInfoReturnable<Boolean> cir) {
    //$$     if (EssentialConfig.INSTANCE.getEssentialEnabled()) USound.INSTANCE.playButtonPress();
    //$$ }
    //$$ @Inject(method = {MOUSE_CLICKED}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/PackLoadingManager$IPack;func_230472_i_()V", shift = At.Shift.AFTER))
    //$$ private void playSoundAfterButtonDisable(final CallbackInfoReturnable<Boolean> cir) {
    //$$     if (EssentialConfig.INSTANCE.getEssentialEnabled()) USound.INSTANCE.playButtonPress();
    //$$ }
    //#else
    // only single mixin for 1.12 as each button handled the list moving logic directly
    @Inject(method = {MOUSE_CLICKED}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreenResourcePacks;markChanged()V", shift = At.Shift.AFTER))
    private void playSoundAfterButtons(final CallbackInfoReturnable<Boolean> cir) {
        if (EssentialConfig.INSTANCE.getEssentialEnabled()) USound.INSTANCE.playButtonPress();
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

    @ModifyReturnValue(method = {MOUSE_CLICKED}, at = @At(value = "TAIL"))
    private boolean grabHoverEntry(final boolean handled,
                                   //#if MC>=11600
                                   //#if MC>=12109
                                   //$$ Click click,
                                   //#else
                                   //$$ double mouseX, double mouseY, int button,
                                   //#endif
                                   //$$ @Share("relativeX") LocalDoubleRef relativeXGet, @Share("relativeY") LocalDoubleRef relativeYGet
                                   //#else
                                   final int slotIndex, final int mouseX, final int mouseY, final int mouseEvent, final int relativeX, final int relativeY
                                   //#endif
    ) {
        //#if MC>=12109
        //$$ double mouseX = click.x();
        //$$ double mouseY = click.y();
        //#endif

        //#if MC>=12006
        //$$ if (this.widget.getMaxScroll() > 0 && mouseX > this.widget.getRight() - 8) {
        //$$     // 1.20.6+ allows clicking the entries under the scrollbar this will break dragging behaviour
        //$$     return handled;
        //$$ }
        //#endif

        if (
                //#if MC<12006
                !handled && // just in case some other mod did something, this is always true in vanilla past 1.20.6
                //#endif
                showHoverOverlay()) { // is movable entry
            @SuppressWarnings("unchecked")
            //#if MC>=12109
            //$$ GuiDragDropEntryHandler<Entry> dragHandler = ((EssentialGuiDraggableEntryScreen<Entry>) getResourcePacksGUI()).essential$getDragHandlerOrNull();
            //#else
            GuiDragDropEntryHandler<ResourcePackListEntry> dragHandler = ((EssentialGuiDraggableEntryScreen<ResourcePackListEntry>) getResourcePacksGUI()).essential$getDragHandlerOrNull();
            //#endif
            if (dragHandler != null && !dragHandler.isDraggingEntry()) {
                ResourcePackListEntry selectedEntry = (ResourcePackListEntry) (Object) this;

                //#if MC>=11600
                //$$ ResourcePackList availableWidget = ((PackScreenAccessor)getResourcePacksGUI()).essential$getAvailablePackList();
                //$$ ResourcePackList selectedWidget =  ((PackScreenAccessor)getResourcePacksGUI()).essential$getSelectedPackList();
                //#endif

                //#if MC>=12109
                //$$ @SuppressWarnings("unchecked") List<Entry> availableList = ResourcePackListWrapper.of(((AbstractListAccessor<Entry>) availableWidget).essential$getChildrenList());
                //$$ @SuppressWarnings("unchecked") List<Entry> selectedList = ResourcePackListWrapper.of(((AbstractListAccessor<Entry>) selectedWidget).essential$getChildrenList());
                //#elseif MC>=12004
                //$$ List<ResourcePackEntry> availableList = availableWidget.children();
                //$$ List<ResourcePackEntry> selectedList = selectedWidget.children();
                //#elseif MC>=11600
                //$$ List<ResourcePackEntry> availableList = ((AbstractListAccessor) availableWidget).essential$getChildrenList();
                //$$ List<ResourcePackEntry> selectedList = ((AbstractListAccessor) selectedWidget).essential$getChildrenList();
                //#else
                List<ResourcePackListEntry> availableList = getResourcePacksGUI().getAvailableResourcePacks();
                List<ResourcePackListEntry> selectedList = getResourcePacksGUI().getSelectedResourcePacks();
                //#endif

                boolean availableListIsContainer = availableList.contains(selectedEntry);
                //#if MC>=12109
                //$$ List<Entry> container = availableListIsContainer ? availableList : selectedList;
                //#else
                List<ResourcePackListEntry> container = availableListIsContainer ? availableList : selectedList;
                //#endif

                // bail if the entry is not in the list, possibly due to a mod
                if (!container.contains(selectedEntry)) return handled;

                boolean mustStayInOriginalList = false;

                //#if MC>=11600
                //$$ int slotIndex = container.indexOf(selectedEntry);
                //$$
                //$$ List<ResourcePackInfo> dataEnabledList = ((PackLoadingManagerAccessor)((PackScreenAccessor) getResourcePacksGUI()).essential$getPackLoadingManager()).essential$getEnabledList();
                //$$
                //$$ if(!availableListIsContainer){
                //$$     // additional cancel check if this is a server pack or otherwise unmovable entry
                //$$     ResourcePackInfo packInfo = dataEnabledList.get(slotIndex);
                //$$     if (packInfo.isOrderLocked()) return handled;
                //$$
                //$$     // check if we cannot move this to the other list
                //$$     mustStayInOriginalList = packInfo.isAlwaysEnabled();
                //$$ }
                //#endif

                // shift click to instantly swap
                //#if MC>=12109
                //$$ boolean hasShift = click.hasShift();
                //#else
                boolean hasShift = GuiScreen.isShiftKeyDown();
                //#endif
                if (hasShift && !mustStayInOriginalList) {
                    //#if MC>=12109
                    //$$ List<Entry> otherContainer = (availableListIsContainer ? selectedList : availableList);
                    //#else
                    List<ResourcePackListEntry> otherContainer = (availableListIsContainer ? selectedList : availableList);
                    //#endif
                    // check for any top priority / server packs in the selected list
                    int destIndex = availableListIsContainer
                            ? ((EssentialGuiDraggableEntryScreen<?>) getResourcePacksGUI()).essential$getQuickSwapIndex()
                            : 0;

                    otherContainer.add(destIndex, selectedEntry);
                    container.remove(selectedEntry);
                    //#if MC>=11600
                    //$$ List<ResourcePackInfo> dataDisabledList = ((PackLoadingManagerAccessor)((PackScreenAccessor) getResourcePacksGUI()).essential$getPackLoadingManager()).essential$getDisabledList();
                    //$$
                    //$$ // reflect changes in data lists of the pack loading manager
                    //$$ if (availableListIsContainer) {
                    //$$     ResourcePackInfo entryData = dataDisabledList.remove(slotIndex);
                    //$$     dataEnabledList.add(destIndex, entryData);
                    //$$ } else {
                    //$$     ResourcePackInfo entryData = dataEnabledList.remove(slotIndex);
                    //$$     dataDisabledList.add(destIndex, entryData);
                    //$$ }
                    //#endif
                    dragHandler.runForDataChange.run();
                } else {
                    //#if MC>=11600
                    //$$ double relativeX = relativeXGet.get();
                    //$$ double relativeY = relativeYGet.get();
                    //#endif

                    dragHandler.setPendingDraggedEntryState(selectedEntry, container, relativeX, relativeY,
                            slotIndex, mouseX, mouseY, mustStayInOriginalList);
                }

                // signify that the click was handled
                return true;
            }
        }
        return handled;
    }

    /**
     * disables the hover visuals while dragging entries
     */
    @ModifyExpressionValue(method = "drawEntry", at = @At(value = "INVOKE", target =
            //#if MC>=11600
            //$$ "Lnet/minecraft/client/gui/widget/list/ResourcePackList$ResourcePackEntry;func_238920_a_()Z"
            //#else
            "Lnet/minecraft/client/resources/ResourcePackListEntry;showHoverOverlay()Z"
            //#endif
    ))
    private boolean hideSelectionVisuals(boolean isSelected) {
        GuiDragDropEntryHandler<?> dragHandler = ((EssentialGuiDraggableEntryScreen<?>) getResourcePacksGUI()).essential$getDragHandlerOrNull();
        if (dragHandler != null && dragHandler.isDraggingEntry()) {
            return false;
        }
        return isSelected;
    }

}
