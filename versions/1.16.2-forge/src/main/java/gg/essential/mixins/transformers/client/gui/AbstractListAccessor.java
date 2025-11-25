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

import net.minecraft.client.gui.widget.list.AbstractList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

//#if MC>=11904
//$$ import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
//$$ import net.minecraft.client.gui.widget.EntryListWidget;
//#else
//#endif

import java.util.List;

@Mixin(AbstractList.class)
public interface AbstractListAccessor {
    @Accessor("y0")
    int essential$getTop();

    @Accessor("y1")
    int essential$getBottom();

    @Accessor("x0")
    int essential$getLeft();

    @Accessor("x1")
    int essential$getRight();

    @Accessor("itemHeight")
    int essential$getItemHeight();

    @Accessor("headerHeight")
    int essential$getHeaderHeight();

    @Invoker("getRowLeft")
    int essential$getRowLeft();

    @Invoker("getRowTop")
    int essential$getRowTop(int i);

    @Accessor("children")
        //#if MC>=11904
        //$$ <E extends AlwaysSelectedEntryListWidget.Entry<E>>
        //#else
        <E extends AbstractList.AbstractListEntry<E>>
        //#endif
        List<E> essential$getChildrenList();

    @Invoker("getMaxScroll")
    int essential$getMaxScroll();


    //#if MC<11904
    @Invoker("getEntryAtPosition")
    <E extends AbstractList.AbstractListEntry<E>> E essential$getEntryAtScreenPosition(double x, double y);
    //#endif

}
