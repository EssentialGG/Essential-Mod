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
package gg.essential.mixins.transformers.compatibility.sba;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gg.essential.handlers.OnlineIndicator;
import gg.essential.universal.UMatrixStack;
import gg.essential.util.UDrawContext;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// https://github.com/BiscuitDevelopment/SkyblockAddons/blob/main/src/main/java/codes/biscuit/skyblockaddons/features/tablist/TabListRenderer.java
@Pseudo
@Mixin(targets = "codes.biscuit.skyblockaddons.features.tablist.TabListRenderer")
@SuppressWarnings({"UnresolvedMixinReference", "DefaultAnnotationParam"})
public class MixinTabListRenderer {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcodes/biscuit/skyblockaddons/features/tablist/TabLine;getType()Lcodes/biscuit/skyblockaddons/features/tablist/TabStringType;", ordinal = 0), remap = false)
    private static void resetNetworkPlayerInfo(CallbackInfo ci, @Share("info") LocalRef<NetworkPlayerInfo> infoRef) {
        infoRef.set(null);
    }

    @ModifyVariable(method = "render", at = @At("STORE"), remap = false)
    private static NetworkPlayerInfo recordNetworkPlayerInfo(NetworkPlayerInfo info, @Share("info") LocalRef<NetworkPlayerInfo> infoRef) {
        infoRef.set(info);
        return info;
    }

    @ModifyArg(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I", ordinal = 2, remap = true),
        index = 1,
        remap = false
    )
    private static float shiftTextAndRenderEssentialIndicator(String text, float x, float y, int color, @Share("info") LocalRef<NetworkPlayerInfo> infoRef) {
        NetworkPlayerInfo networkPlayerInfo = infoRef.get();
        if (networkPlayerInfo != null) {
            UDrawContext drawContext = new UDrawContext(new UMatrixStack());
            OnlineIndicator.drawTabIndicatorOuter(drawContext, networkPlayerInfo, (int) x, (int) y);

        }

        return x;
    }
}
