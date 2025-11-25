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

import gg.essential.gui.proxies.EssentialProxyElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Priority ensures our HEAD inject will be early in the final execution order, ahead of any other default priority mixins,
// which is needed to mimic the pre 1.20.4 overridden method behaviour.
// We want this so we can run our logic before other mixins that may cancel render() at HEAD,
// the only specifically known/relevant case is Fancy Menu v3+ as it cancels the render method via a mixin here:
// https://github.com/Keksuccino/FancyMenu/blob/8e27c931935eaaf67e5ea47b922ab5896418c2ef/common/src/main/java/de/keksuccino/fancymenu/mixin/mixins/common/client/MixinAbstractWidget.java#L117
@Mixin(value = ClickableWidget.class, priority = 500)
public class MixinGuiButtonRenderOverrider {

    @Unique
    private final boolean isEssentialProxyElement = (Object) this instanceof EssentialProxyElement;
    @Unique
    private boolean isMidOverride = false;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderButton(final DrawContext context, final int mouseX, final int mouseY, final float delta, final CallbackInfo ci) {
        if (isEssentialProxyElement && !isMidOverride) {
            ci.cancel();

            isMidOverride = true; // allows this renderOverride to call back to this method via super freely
            //noinspection DataFlowIssue
            ((EssentialProxyElement<?>) (Object) this).renderOverride(context, mouseX, mouseY, delta);
            isMidOverride = false;
        }
    }
}
