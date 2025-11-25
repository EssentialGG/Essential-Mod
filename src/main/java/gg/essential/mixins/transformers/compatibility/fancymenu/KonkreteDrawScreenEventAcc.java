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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

//#if MC>=12000
//$$ import net.minecraft.client.gui.DrawContext;
//#elseif MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//#endif

//#if FABRIC
//$$ @org.spongepowered.asm.mixin.Pseudo
//$$ @Mixin(targets = {"de.keksuccino.konkrete.events.client.GuiScreenEvent$DrawScreenEvent", "de.keksuccino.fancymenu.events.RenderScreenEvent"}, remap = false)
//#elseif NEOFORGE
//$$ @Mixin(value = net.neoforged.neoforge.client.event.ScreenEvent.Render.class, remap = false)
//#else
//#if MC>=11900
//$$ @Mixin(value = net.minecraftforge.client.event.ScreenEvent.Render.class, remap = false)
//#elseif MC>=11800
//$$ @Mixin(value = net.minecraftforge.client.event.ScreenEvent.DrawScreenEvent.class, remap = false)
//#else
@Mixin(value = net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.class, remap = false)
//#endif
//#endif
public interface KonkreteDrawScreenEventAcc extends KonkreteGuiScreenEventAcc {
    @Invoker
    int invokeGetMouseX();

    @Invoker
    int invokeGetMouseY();

    //#if FABRIC || MC<11800
    @Invoker
    //#elseif MC>=11900
    //$$ @Invoker("getPartialTick")
    //#else
    //$$ @Invoker("getPartialTicks")
    //#endif
    float invokeGetRenderPartialTicks();

    //#if MC>=12000
    //$$ @Invoker("getGuiGraphics")
    //$$ DrawContext invokeGetDrawContext();
    //#elseif MC>=11600
    //#if FABRIC || MC<11800
    //$$ @Invoker
    //#else
    //$$ @Invoker("getPoseStack")
    //#endif
    //$$ MatrixStack invokeGetMatrixStack();
    //#endif
}
