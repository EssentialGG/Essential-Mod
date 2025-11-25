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

import gg.essential.Essential;
import gg.essential.event.gui.*;
import gg.essential.mixins.impl.ClassHook;
import gg.essential.util.UDrawContext;
import net.minecraft.client.gui.GuiScreen;
import gg.essential.mixins.transformers.client.gui.GuiScreenAccessor;

//#if MC>=11700
//$$ import net.minecraft.client.gui.Drawable;
//$$ import net.minecraft.client.gui.Selectable;
//$$ import java.util.ArrayList;
//$$ import kotlin.collections.CollectionsKt;
//#endif

//#if MC>=11600
//$$ import net.minecraft.client.gui.IGuiEventListener;
//$$ import net.minecraft.client.gui.widget.Widget;
//$$ import java.util.AbstractList;
//$$ import java.util.List;
//#endif

public class GuiScreenHook extends ClassHook<GuiScreen> {
    public GuiScreenHook(GuiScreen instance) {
        super(instance);
    }

    public void setWorldAndResolution() {
        //#if MC>=11600
        //#if MC>=11700
        //$$ List<Drawable> drawableList = ((GuiScreenAccessor) instance).getDrawables();
        //$$ List<Selectable> selectableList = ((GuiScreenAccessor) instance).getSelectables();
        //$$ List<ClickableWidget> buttonList = CollectionsKt.filterIsInstanceTo(selectableList, new ArrayList<>(), ClickableWidget.class);
        //#else
        //$$ List<Widget> buttonList = ((GuiScreenAccessor) instance).getButtonList();
        //#endif
        //$$ List<IGuiEventListener> children = ((GuiScreenAccessor) instance).essential$getChildren();
        //$$ InitGuiEvent event = new InitGuiEvent(instance, new AbstractList<Widget>() {
        //$$     @Override
        //$$     public Widget get(int i) {
        //$$         return buttonList.get(i);
        //$$     }
        //$$
        //$$     @Override
        //$$     public int size() {
        //$$         return buttonList.size();
        //$$     }
        //$$
        //$$     @Override
        //$$     public Widget set(int index, Widget element) {
        //$$         Widget removed = remove(index);
        //$$         add(index, element);
        //$$         return removed;
        //$$     }
        //$$
        //$$     @Override
        //$$     public void add(int index, Widget element) {
        //$$         // Try to hit the corresponding spot in the children list as it determines the tab order.
        //$$         int childrenIndex = index < size() ? children.indexOf(buttonList.get(index)) : 0;
        //$$         if (childrenIndex == -1) childrenIndex = 0;
                //#if MC>=11700
                //$$ int drawableIndex = index < size() ? drawableList.indexOf(buttonList.get(index)) : 0;
                //$$ if (drawableIndex == -1) drawableIndex = 0;
                //$$ int selectableIndex = index < size() ? selectableList.indexOf(buttonList.get(index)) : 0;
                //$$ if (selectableIndex == -1) selectableIndex = 0;
                //#endif
        //$$         buttonList.add(index, element);
        //$$         children.add(childrenIndex, element);
                //#if MC>=11700
                //$$ drawableList.add(drawableIndex, element);
                //$$ selectableList.add(selectableIndex, element);
                //#endif
        //$$     }
        //$$
        //$$     @Override
        //$$     public Widget remove(int index) {
        //$$         Widget removed = buttonList.remove(index);
        //$$         if (removed != null) {
        //$$             children.remove(removed);
                    //#if MC>=11700
                    //$$ drawableList.remove(removed);
                    //$$ selectableList.remove(removed);
                    //#endif
        //$$         }
        //$$         return removed;
        //$$     }
        //$$ });
        //#else
        InitGuiEvent event = new InitGuiEvent(instance, ((GuiScreenAccessor) instance).getButtonList());
        //#endif
        Essential.EVENT_BUS.post(event);
    }

    public GuiDrawScreenEvent drawScreen(UDrawContext drawContext, int mouseX, int mouseY, float partialTicks, boolean post) {
        GuiDrawScreenEvent event = new GuiDrawScreenEvent(instance, drawContext, mouseX, mouseY, partialTicks, post);
        Essential.EVENT_BUS.post(event);
        return event;
    }
}