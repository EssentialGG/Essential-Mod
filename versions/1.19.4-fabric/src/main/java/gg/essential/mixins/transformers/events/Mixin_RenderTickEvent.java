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
import gg.essential.universal.UMinecraft;
import gg.essential.util.UDrawContext;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12100
//$$ import gg.essential.mixins.transformers.client.renderer.DynamicRenderTickCounterAccessor;
//$$ import net.minecraft.client.render.RenderTickCounter;
//#endif

//#if MC>=12000
//$$ import com.llamalad7.mixinextras.sugar.Local;
//$$ import net.minecraft.client.gui.DrawContext;
//#endif

@Mixin(GameRenderer.class)
public class Mixin_RenderTickEvent {

    @Inject(method = "render", at = @At("HEAD"))
    //#if MC>=12100
    //$$ private void renderTickPre(RenderTickCounter tickDelta, boolean tick, CallbackInfo callbackInfo) {
    //#else
    private void renderTickPre(float tickDelta, long startTime, boolean tick, CallbackInfo callbackInfo) {
    //#endif
        fireTickEvent(true, null, tickDelta);
    }

    @Inject(method = "render", at = @At(value = "CONSTANT", args = "stringValue=toasts"))
    //#if MC>=12100
    //$$ private void renderTickPost(RenderTickCounter tickDelta, boolean tick, CallbackInfo callbackInfo, @Local DrawContext vDrawContext) {
    //$$     UDrawContext drawContext = new UDrawContext(vDrawContext, new UMatrixStack());
    //#elseif MC>=12000
    //$$ private void renderTickPost(float tickDelta, long startTime, boolean tick, CallbackInfo callbackInfo, @Local DrawContext vDrawContext) {
    //$$     UDrawContext drawContext = new UDrawContext(vDrawContext, new UMatrixStack());
    //#else
    private void renderTickPost(float tickDelta, long startTime, boolean tick, CallbackInfo callbackInfo) {
        UDrawContext drawContext = new UDrawContext(new UMatrixStack());
    //#endif
        fireTickEvent(false, drawContext, tickDelta);
    }

    @Unique
    private void fireTickEvent(
        boolean pre,
        UDrawContext drawContext,
        //#if MC>=12100
        //$$ RenderTickCounter counter
        //#else
        float tickDelta
        //#endif
    ) {
        UMatrixStack matrixStack = drawContext != null ? drawContext.getMatrixStack() : new UMatrixStack();
        //#if MC>=12100
        //$$ float partialTicksMenu = ((DynamicRenderTickCounterAccessor) UMinecraft.getMinecraft().getRenderTickCounter()).essential$getRawTickDelta();
        //$$ float partialTicksInGame = counter.getTickDelta(false);
        //#else
        float partialTicksMenu = UMinecraft.getMinecraft().getTickDelta();
        float partialTicksInGame = tickDelta;
        //#endif
        Essential.EVENT_BUS.post(new RenderTickEvent(pre, false, drawContext, matrixStack, partialTicksMenu, partialTicksInGame));
    }

}
