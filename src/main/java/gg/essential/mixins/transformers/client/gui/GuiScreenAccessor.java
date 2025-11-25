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

//#if MC < 11400
import net.minecraft.client.gui.GuiButton;
//#else
//#if MC>=11700
//$$ import net.minecraft.client.gui.Drawable;
//$$ import net.minecraft.client.gui.Selectable;
//$$ import org.spongepowered.asm.mixin.gen.Invoker;
//#else
//$$ import net.minecraft.client.gui.widget.Widget;
//#endif
//$$ import net.minecraft.client.gui.IGuiEventListener;
//#endif
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(GuiScreen.class)
public interface GuiScreenAccessor {

    //#if MC < 11400
    @Accessor
    List<GuiButton> getButtonList();
    //#else
    //#if MC>=11700
    //$$ @Accessor
    //$$ List<Drawable> getDrawables();
    //$$ @Accessor
    //$$ List<Selectable> getSelectables();
    //$$ @Invoker("addDrawableChild")
    //$$ <T extends Element & Drawable & Selectable> T essential$addDrawableChild(T drawable);
    //#else
    //$$ @Accessor("buttons")
    //$$ List<Widget> getButtonList();
    //#endif
    //$$ // EM-932: prefixed to resolve conflict with AntiqueAtlas
    //$$ @Accessor("children")
    //$$ List<IGuiEventListener> essential$getChildren();
    //#endif

    //#if MC>=11400
    //#else
    @Invoker
    void invokeMouseReleased(int mouseX, int mouseY, int button);
    //#endif
}