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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.essential.gui.multiplayer.DividerServerListEntry;
import gg.essential.mixins.ext.client.gui.SelectionListWithDividers;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.gui.ServerSelectionList;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//#if MC>=12109
//$$ import com.llamalad7.mixinextras.sugar.Local;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

//#if MC>=11600
//$$ import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.gui.widget.list.ExtendedList;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import java.util.ArrayList;
//$$ import java.util.function.Consumer;
//#else
import net.minecraft.client.gui.GuiListExtended;
//#endif

@Mixin(ServerSelectionList.class)
public class Mixin_SelectionListDividers_ServerSelectionList
    //#if MC>=11600
    //$$ extends ExtendedList<ServerSelectionList.Entry>
    //#endif
    implements SelectionListWithDividers<DividerServerListEntry>
{

    /**
     * A map of entry indices to dividers. A TreeMap is used to allow easily determining the number
     * of dividers that occur before a given index. The keys of this map reflect the final index
     * of the dividers in the list of server entries and divider entries. This adjustment is done
     * in {@link #essential$setDividers(Map)}
     */
    @Unique
    private TreeMap<Integer, DividerServerListEntry> essential$dividers = new TreeMap<>();

    @Override
    public @NotNull TreeMap<Integer, DividerServerListEntry> getEssential$offsetDividers() {
        return this.essential$dividers;
    }

    @Override
    public void setEssential$offsetDividers(@NotNull TreeMap<Integer, DividerServerListEntry> dividers) {
        this.essential$dividers = dividers;
    }

    @Shadow @Final private List<ServerListEntryNormal> serverListInternet;

    //#if MC>=11600
    //$$ public Mixin_SelectionListDividers_ServerSelectionList() {
        //#if MC>=12004
        //$$ super(null, 0, 0, 0, 0);
        //#else
        //$$ super(null, 0, 0, 0, 0, 0);
        //#endif
    //$$ }
    //#endif

    //#if MC>=12109
    //$$ @Inject(method = "updateEntries", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerServerListWidget;replaceEntries(Ljava/util/Collection;)V"))
    //$$ public void modifyListEntries(CallbackInfo ci, @Local List<MultiplayerServerListWidget.Entry> entries) {
    //$$     if (!essential$dividers.isEmpty()) {
    //$$         for (Map.Entry<Integer, DividerServerListEntry> entry : essential$dividers.entrySet()) {
    //$$             entries.add(entry.getKey(), entry.getValue());
    //$$         }
    //$$     }
    //$$ }
    //#elseif MC>=11600
    //$$ @Inject(method = "setList", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", ordinal = 0), cancellable = true)
    //$$ public void modifyListEntries(CallbackInfo ci) {
    //$$     if (!essential$dividers.isEmpty()) {
    //$$         List<ServerSelectionList.Entry> entries = new ArrayList<>(this.serverListInternet);
    //$$         for (Map.Entry<Integer, DividerServerListEntry> entry : essential$dividers.entrySet()) {
    //$$             entries.add(entry.getKey(), entry.getValue());
    //$$         }
    //$$         entries.forEach(this::addEntry);
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //#else
    @Inject(method = "getListEntry", at = @At("HEAD"), cancellable = true)
    private void modifyListEntries(int index, CallbackInfoReturnable<GuiListExtended.IGuiListEntry> cir) {
        if (!essential$dividers.isEmpty()) {
            if (essential$dividers.containsKey(index)) {
                cir.setReturnValue(essential$dividers.get(index));
            } else {
                int adjustedIndex = index - essential$dividers.headMap(index).size();
                cir.setReturnValue(this.serverListInternet.get(adjustedIndex));
            }
        }
    }

    @ModifyReturnValue(method = "getSize", at = @At("TAIL"))
    private int modifyListSize(int original) {
        if (!essential$dividers.isEmpty()) {
            return this.serverListInternet.size() + essential$dividers.size();
        }
        return original;
    }
    //#endif
}
