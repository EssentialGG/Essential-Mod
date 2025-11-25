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
package gg.essential.mixins.transformers.client.gui.inventory;

import gg.essential.gui.common.UI3DPlayer;
import gg.essential.mixins.impl.client.gui.GuiInventoryExt;
import net.minecraft.client.gui.inventory.GuiInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12106
//$$ @Mixin(net.minecraft.client.gui.render.EntityGuiElementRenderer.class)
//#else
@Mixin(GuiInventory.class)
//#endif
public class Mixin_TrackInventoryPlayerRendering {

    private static final String DRAW_ENTITY =
        //#if MC>=12106
        //$$ "render(Lnet/minecraft/client/gui/render/state/special/EntityGuiElementRenderState;Lnet/minecraft/client/util/math/MatrixStack;)V";
        //#elseif MC>=12005
        //$$ "drawEntity(Lnet/minecraft/client/gui/DrawContext;FFFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/entity/LivingEntity;)V";
        //#elseif MC>=12002
        //$$ "drawEntity(Lnet/minecraft/client/gui/DrawContext;FFILorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/entity/LivingEntity;)V";
        //#elseif MC>=12000
        //$$ "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIILorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/entity/LivingEntity;)V";
        //#elseif MC>=11904
        //$$ "drawEntity(Lnet/minecraft/client/util/math/MatrixStack;IIILorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/entity/LivingEntity;)V";
        //#else
        "drawEntityOnScreen";
        //#endif

    @Inject(method = DRAW_ENTITY, at = @At("HEAD"))
    //#if MC<12106
    static
    //#endif
    private void essential$disableCosmeticsInInventoryStart(CallbackInfo info) {
        // If UI3DPlayer.current is null then the method was not called while rendering a player display
        GuiInventoryExt.isInventoryEntityRendering.set(UI3DPlayer.current == null);
    }

    @Inject(method = DRAW_ENTITY, at = @At("RETURN"))
    //#if MC<12106
    static
    //#endif
    private void essential$disableCosmeticsInInventoryCleanup(CallbackInfo info) {
        GuiInventoryExt.isInventoryEntityRendering.set(false);
    }
}
