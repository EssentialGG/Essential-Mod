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
package gg.essential.mixins.transformers.events;

import gg.essential.Essential;
import gg.essential.event.render.RenderTickEvent;
import gg.essential.universal.UMatrixStack;
import gg.essential.util.UDrawContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_RenderTickEvent {

    @Shadow
    @Final
    private Timer timer;

    //#if MC>=11200
    @Shadow private boolean isGamePaused;

    @Shadow private float renderPartialTicksPaused;
    //#endif

    //#if MC < 11400
    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;updateCameraAndRender(FJ)V", shift = At.Shift.BEFORE))
    //#else
    //$$ @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJZ)V", shift = At.Shift.BEFORE))
    //#endif
    private void renderTickPre(CallbackInfo callbackInfo) {
        fireTickEvent(true);
    }

    //#if MC < 11400
    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;updateCameraAndRender(FJ)V", shift = At.Shift.AFTER))
    //#else
    //$$ @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJZ)V", shift = At.Shift.AFTER))
    //#endif
    private void renderTickPost(CallbackInfo callbackInfo) {
        fireTickEvent(false);
    }

    @Unique
    private void fireTickEvent(boolean pre) {
        UMatrixStack matrixStack = new UMatrixStack();
        UDrawContext drawContext = pre ? null : new UDrawContext(matrixStack);
        //#if MC>=11200
        float partialTicksMenu = this.timer.renderPartialTicks;
        float partialTicksInGame = this.isGamePaused ? this.renderPartialTicksPaused : partialTicksMenu;
        //#else
        //$$ float partialTicksMenu = this.timer.renderPartialTicks;
        //$$ float partialTicksInGame = partialTicksMenu;
        //#endif
        Essential.EVENT_BUS.post(new RenderTickEvent(pre, false, drawContext, matrixStack, partialTicksMenu, partialTicksInGame));
    }

}
