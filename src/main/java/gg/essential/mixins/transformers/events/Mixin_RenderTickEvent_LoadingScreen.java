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
// Newer versions treat loading screens as regular screens and use the regular render method
//#if MC<11600
package gg.essential.mixins.transformers.events;

import gg.essential.Essential;
import gg.essential.event.render.RenderTickEvent;
import gg.essential.universal.UMatrixStack;
import gg.essential.util.UDrawContext;
import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingScreenRenderer.class)
public class Mixin_RenderTickEvent_LoadingScreen {
    @Shadow @Final private Minecraft mc;

    @Inject(method = "setLoadingProgress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;bindFramebuffer(Z)V"))
    private void renderTickPre(CallbackInfo callbackInfo) {
        //#if MC>=11200
        float partialTicks = this.mc.getRenderPartialTicks();
        //#else
        //$$ float partialTicks = 0; // FIXME could get this via Accessor, but we don't really need it atm anyway
        //#endif
        Essential.EVENT_BUS.post(new RenderTickEvent(true, true, null, new UMatrixStack(), partialTicks, partialTicks));
    }

    @Inject(method = "setLoadingProgress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;unbindFramebuffer()V"))
    private void renderTickPost(CallbackInfo callbackInfo) {
        //#if MC>=11200
        float partialTicks = this.mc.getRenderPartialTicks();
        //#else
        //$$ float partialTicks = 0; // FIXME could get this via Accessor, but we don't really need it atm anyway
        //#endif
        UMatrixStack stack = new UMatrixStack();
        Essential.EVENT_BUS.post(new RenderTickEvent(false, true, new UDrawContext(stack), stack, partialTicks, partialTicks));
    }

    @Inject(method = "setLoadingProgress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateDisplay()V"))
    private void renderTickFinal(CallbackInfo callbackInfo) {
        Essential.EVENT_BUS.post(new RenderTickEvent.Final());
    }
}
//#else
//$$ package gg.essential.mixins.transformers.events;
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class Mixin_RenderTickEvent_LoadingScreen {}
//#endif
