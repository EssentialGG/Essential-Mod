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
package gg.essential.mixins.impl.client.gui;

import org.jetbrains.annotations.Nullable;

//#if MC>=11904
//$$ import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
//#elseif MC>=11600
//$$ import net.minecraft.client.gui.widget.list.AbstractList;
//#else
import net.minecraft.client.gui.GuiListExtended;
//#endif

public interface
        //#if MC>=11904
        //$$ EssentialGuiDraggableEntryScreen<E extends AlwaysSelectedEntryListWidget.Entry<E>> {
        //#elseif MC>=11600
        //$$ EssentialGuiDraggableEntryScreen<E extends AbstractList.AbstractListEntry<E>> {
        //#else
        EssentialGuiDraggableEntryScreen<E extends GuiListExtended.IGuiListEntry> {
        //#endif

    /**
     * @return should be null if Essential is disabled
     */
    @Nullable GuiDragDropEntryHandler<E> essential$getDragHandlerOrNull();

    int essential$getQuickSwapIndex();
}
