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

import gg.essential.config.EssentialConfig;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.ServerSelectionList;
import com.mojang.blaze3d.matrix.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12106
//$$ import com.mojang.blaze3d.pipeline.RenderPipeline;
//#endif

//#if MC>=12102
//$$ import net.minecraft.client.render.RenderLayer;
//$$ import java.util.function.Function;
//#endif

//#if MC>=12000
//$$ import net.minecraft.util.Identifier;
//#endif

// Decreased priority for compatibility with other mods:
// - @Redirect in ViaFabric: https://github.com/ViaVersion/ViaFabric/blob/df04490b5db0709003792b85c2f6366dc5d8e6a6/viafabric-mc120/src/main/java/com/viaversion/fabric/mc120/mixin/gui/client/MixinServerEntry.java#L27-L35
// - @Redirects in draggable-lists: https://github.com/MrMelon54/draggable_lists/blob/05cce2592a46a9be830e71a33e3230d1bd93f517/common/src/main/java/com/mrmelon54/DraggableLists/mixin/server/MixinOnlineServerEntry.java#L75-L97
@Mixin(value = ServerSelectionList.NormalEntry.class, priority = 500)
public abstract class Mixin_PreventMovingOfServersInCustomTabs {
    @Shadow
    @Final
    //#if MC>=12109
    //$$ private MultiplayerServerListWidget field_19117;
    //#else
    private MultiplayerScreen owner;
    //#endif

    //#if MC>=12002
    //#if MC>=12106
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"), index = 2)
    //$$ private int hideMovingButtonsInCustomTabs(RenderPipeline renderPipeline, Identifier location, int x, int y, int width, int height) {
    //#elseif MC>=12102
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V"), index = 2)
    //$$ private int hideMovingButtonsInCustomTabs(Function<Identifier, RenderLayer> renderLayers, Identifier location, int x, int y, int width, int height) {
    //#else
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"), index = 1)
    //$$ private int hideMovingButtonsInCustomTabs(Identifier location, int x, int y, int width, int height) {
    //#endif
    //$$     if (EssentialConfig.INSTANCE.getCurrentMultiplayerTab() != 0 && location.getPath().startsWith("server_list/move_")) {
    //$$         x = Integer.MIN_VALUE;
    //$$     }
    //$$     return x;
    //$$ }
    //#else
    //#if MC>=12000
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIFFIIII)V"), index = 1)
    //$$ private int hideMovingButtonsInCustomTabs(Identifier location, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
    //#else
    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/AbstractGui;blit(Lcom/mojang/blaze3d/matrix/MatrixStack;IIFFIIII)V"), index = 1)
    private int hideMovingButtonsInCustomTabs(MatrixStack matrices, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
    //#endif
        if (EssentialConfig.INSTANCE.getCurrentMultiplayerTab() != 0 && (u == 96f || u == 64f))
            return Integer.MIN_VALUE;
        return x;
    }
    //#endif

    @Inject(method = "func_228196_a_", at = @At("HEAD"), cancellable = true)
    private void preventSwappingInCustomTabs(CallbackInfo ci) {
        if (EssentialConfig.INSTANCE.getCurrentMultiplayerTab() != 0) {
            ci.cancel();
            //#if MC>=12109
            //$$ this.field_19117.setSelected((MultiplayerServerListWidget.Entry) (Object) this);
            //#else
            this.owner.func_214287_a((ServerSelectionList.NormalEntry) (Object) this);
            //#endif
        }
    }
}
