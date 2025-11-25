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
package gg.essential.mixins.transformers.compatibility.fancymenu;

import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

//#if FABRIC
//$$ @org.spongepowered.asm.mixin.Pseudo
//$$ @Mixin(targets = {"de.keksuccino.konkrete.events.client.GuiScreenEvent", "de.keksuccino.fancymenu.events.RenderScreenEvent"}, remap = false)
//#elseif NEOFORGE
//$$ @Mixin(value = net.neoforged.neoforge.client.event.ScreenEvent.class, remap = false)
//#else
//#if MC>=11800
//$$ @Mixin(value = net.minecraftforge.client.event.ScreenEvent.class, remap = false)
//#else
@Mixin(value = net.minecraftforge.client.event.GuiScreenEvent.class, remap = false)
//#endif
//#endif
public interface KonkreteGuiScreenEventAcc {
    //#if FABRIC || MC<11800
    @Invoker
    //#else
    //$$ @Invoker("getScreen")
    //#endif
    GuiScreen invokeGetGui();
}
