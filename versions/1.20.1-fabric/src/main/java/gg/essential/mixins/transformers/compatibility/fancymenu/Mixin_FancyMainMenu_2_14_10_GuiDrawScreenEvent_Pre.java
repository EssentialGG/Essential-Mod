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

import gg.essential.Essential;
import gg.essential.event.gui.GuiDrawScreenEvent;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.UMouse;
import gg.essential.util.UDrawContext;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FancyMenu cancels the regular rendering of the Main Menu when customizations are enabled.
 * We still want to draw our main menu stuff though, so this mixin fires our pre event from Fancy Menu's handler.
 * This mixin handles FancyMenu versions 2.14.10 and above on Fabric.
 *
 * @see Mixin_FancyMainMenu_GuiDrawScreenEvent_Pre
 */
// FIXME can we improve this with FancyMenu 3.0
//#if FABRIC
@Pseudo
@Mixin(targets = "de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.MainMenuHandler", remap = false)
public class Mixin_FancyMainMenu_2_14_10_GuiDrawScreenEvent_Pre extends Mixin_FancyMainMenu_GuiDrawScreenEvent_Post {
    @Inject(method = "drawToBackground", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableBlend()V", ordinal = 0))
    private void emitEssentialPreEvent(@Coerce FancyMenuScreenBackgroundRenderedEventAcc event, CallbackInfo ci) {
        emitPostEvent = true;

        Essential.EVENT_BUS.post(new GuiDrawScreenEvent(
            event.invokeGetGui(),
            new UDrawContext(event.invokeGetDrawContext(), new UMatrixStack(event.invokeGetDrawContext().getMatrices())),
            (int) UMouse.Scaled.getX(),
            (int) UMouse.Scaled.getY(),
            MinecraftClient.getInstance().getLastFrameDuration(),
            false
        ));
    }

}
//#else
//$$ @Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public class Mixin_FancyMainMenu_2_14_10_GuiDrawScreenEvent_Pre {}
//#endif
