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
package gg.essential.mixins.transformers.client.renderer;

import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.cosmetics.WearablesManager;
import gg.essential.handlers.ZoomHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class Mixin_HideFirstPersonHandAndItems {
    //#if MC>=12109
    //$$ private static final String RENDER_ITEM_IN_FIRST_PERSON = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V";
    //#elseif MC>=11600
    //$$ private static final String RENDER_ITEM_IN_FIRST_PERSON = "renderItemInFirstPerson(FLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer$Impl;Lnet/minecraft/client/entity/player/ClientPlayerEntity;I)V";
    //#elseif MC==10809
    //$$ private static final String RENDER_ITEM_IN_FIRST_PERSON = "renderItemInFirstPerson(F)V";
    //#else
    private static final String RENDER_ITEM_IN_FIRST_PERSON = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V";
    //#endif

    // Despite the method's name, it does indeed render the hand too.
    @Inject(method = RENDER_ITEM_IN_FIRST_PERSON, at = @At("HEAD"), cancellable = true)
    private void essential$cancelHandIfZoomed(CallbackInfo ci) {
        if (ZoomHandler.getInstance().isZoomActive // hide hand during zooming
                || doesHideArmDuringRotationLock()) ci.cancel();
    }


    @Unique
    private boolean doesHideArmDuringRotationLock() {
        // hide hand during emote with a locked rotation
        if (Minecraft.getMinecraft().player == null) return false;
        CosmeticsRenderState cState = new CosmeticsRenderState.Live(Minecraft.getMinecraft().player);
        WearablesManager wearablesManager = cState.wearablesManager();
        return wearablesManager != null && wearablesManager.getState().getLocksPlayerRotation();
    }
}
