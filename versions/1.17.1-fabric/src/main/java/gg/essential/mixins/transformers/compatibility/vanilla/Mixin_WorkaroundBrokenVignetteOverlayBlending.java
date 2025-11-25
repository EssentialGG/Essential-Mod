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
package gg.essential.mixins.transformers.compatibility.vanilla;

import gg.essential.universal.shader.BlendState;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * See {@link Mixin_WorkaroundBrokenFramebufferBlitBlending} for background.
 * <br>
 * This mixin fixes a similar case in the vingette overlay rendering code where blending is set up via direct
 * RenderSystem calls, but later the vertices are drawn with the position_tex shader which uses a different blending
 * via GlBlendState. In vanilla, this happens to not be applied because of the GlBlendState bug; but depending on the
 * preceding BlendState changes by mods, it can actually be applied, resulting in the vignette being draw fully opaque,
 * see EM-3127.
 * <br>
 * The applied workaround is identical to the one in the linked mixin.
 */
@Mixin(InGameHud.class)
public abstract class Mixin_WorkaroundBrokenVignetteOverlayBlending {
    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"))
    private void workaroundBrokenGlBlendState(CallbackInfo ci) {
        BlendState.DISABLED.activate();
        BlendState.NORMAL.activate();
    }
}
