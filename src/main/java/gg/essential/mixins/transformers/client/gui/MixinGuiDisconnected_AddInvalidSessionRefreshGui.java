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
import gg.essential.gui.multiplayer.disconnect.InvalidSessionRefreshGui;
import gg.essential.mixins.impl.client.gui.EssentialPostScreenDrawHook;
import gg.essential.universal.UMatrixStack;
import gg.essential.util.ServerConnectionUtil;
import gg.essential.util.ServerDataInfo;
import gg.essential.util.UDrawContext;
import kotlin.collections.CollectionsKt;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12000
//$$ import net.minecraft.client.gui.DrawContext;
//#endif

//#if MC<=11202
import net.minecraft.client.renderer.GlStateManager;
//#else
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//#endif

import static gg.essential.util.HelpersKt.textTranslatable;

@Mixin(GuiDisconnected.class)
public abstract class MixinGuiDisconnected_AddInvalidSessionRefreshGui extends GuiScreen implements EssentialPostScreenDrawHook {

    @Shadow @Final private GuiScreen parentScreen;

    @Unique
    private InvalidSessionRefreshGui essentialGui = null;

    //#if MC>=11602
    //$$ protected MixinGuiDisconnected_AddInvalidSessionRefreshGui(ITextComponent titleIn) {
    //$$     super(titleIn);
    //$$ }
    //#endif

    @Inject(method = "<init>", at = @At("RETURN"))
    //#if MC>=11602
    //$$ public void onConstructor(Screen parent, ITextComponent reason, ITextComponent message, CallbackInfo ci) {
    //#else
    public void onConstructor(GuiScreen parent, String reason, ITextComponent message, CallbackInfo ci) {
    //#endif
        // Detect whether this screen shows the "invalid session" message
        boolean isInvalidSessionGui = message.equals(
                textTranslatable("disconnect.loginFailedInfo", textTranslatable("disconnect.loginFailedInfo.invalidSession"))
        );

        if (isInvalidSessionGui && EssentialConfig.INSTANCE.getEssentialFull()) {
            ServerDataInfo serverDataInfo = ServerConnectionUtil.getMostRecentServerInfo();

            if (serverDataInfo != null) {
                essentialGui = new InvalidSessionRefreshGui((GuiDisconnected) (Object) this, this.parentScreen, serverDataInfo);
            }
        }
    }

    //#if MC<=11202
    @Inject(method = "initGui", at = @At("RETURN"))
    //#else
    //$$ @Inject(method = "init", at = @At("RETURN"))
    //#endif
    private void initEssentialGui(CallbackInfo ci) {
        if (essentialGui == null) return;

        essentialGui.setupButtons(
                //#if MC<=11602
                CollectionsKt.filterIsInstance(this.buttonList, GuiButton.class),
                this::addButton
                //#else
                //$$CollectionsKt.filterIsInstance(this.children(), ButtonWidget.class),
                //$$this::addDrawableChild
                //#endif
        );
    }

    //#if MC==10809
    //$$ private GuiButton addButton(GuiButton guiButton) {
    //$$     this.buttonList.add(guiButton);
    //$$     return guiButton;
    //$$ }
    //#endif

    //#if MC>=12002
    //$$ @Override
    //$$ public void essential$afterDraw(UDrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    //$$     if (essentialGui == null) return;
    //$$     essentialGui.draw(drawContext);
    //$$ }
    //#else
    //#if MC<=11202
    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void drawEssentialGui(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (essentialGui == null) return;

        // Tooltip rendering enables these even though they should be disabled.
        // But since MC renders tooltips last, Mojang never noticed.
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        essentialGui.draw(new UDrawContext(new UMatrixStack()));
    }
    //#else
    //$$ @Inject(method = "render", at = @At("RETURN"))
    //#if MC>=12000
    //$$ private void renderEssentialGui(DrawContext context, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
    //$$     MatrixStack matrixStack = context.getMatrices();
    //$$     UDrawContext drawContext = new UDrawContext(context, new UMatrixStack(matrixStack));
    //#else
    //$$ private void renderEssentialGui(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
    //$$     UDrawContext drawContext = new UDrawContext(new UMatrixStack(matrixStack));
    //#endif
    //$$     if (essentialGui == null) return;
    //$$
    //$$     essentialGui.draw(drawContext);
    //$$ }
    //#endif
    //#endif

    //#if MC<=11202
    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void onButtonClicked(GuiButton button, CallbackInfo ci) {
        if (essentialGui == null) return;

        essentialGui.onButtonClicked(button);
    }
    //#endif
}
