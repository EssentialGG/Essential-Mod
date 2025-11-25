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
package gg.essential.mixins.transformers.feature.nameplate_icon;

import gg.essential.handlers.OnlineIndicator;
import gg.essential.mixins.impl.LabelCommandExt;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import gg.essential.cosmetics.CosmeticsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OrderedRenderCommandQueueImpl.LabelCommand.class)
public class Mixin_NameplateIcon_LabelCommandExt implements LabelCommandExt {
    @Unique
    private CosmeticsRenderState cosmeticsRenderState;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        cosmeticsRenderState = OnlineIndicator.currentCosmeticsRenderState;
    }

    @Override
    public CosmeticsRenderState essential$getCosmeticsRenderState() {
        return cosmeticsRenderState;
    }
}
