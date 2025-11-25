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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gg.essential.config.EssentialConfig;
import gg.essential.gui.multiplayer.EssentialMultiplayerGui;
import gg.essential.mixins.ext.client.gui.GuiMultiplayerExt;
import gg.essential.mixins.impl.client.gui.EssentialPostScreenDrawHook;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.UMinecraft;
import gg.essential.util.UDrawContext;
import kotlin.collections.CollectionsKt;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.LanServerInfo;
import net.minecraft.client.renderer.GlStateManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

//#if MC>=12109
//$$ import net.minecraft.client.gui.widget.Positioner;
//$$ import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
//#endif

//#if MC>=12000
//$$ import net.minecraft.client.gui.DrawContext;
//#endif

//#if MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//#endif

//#if MC>=12004
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
//#endif

@Mixin(GuiMultiplayer.class)
public abstract class MixinGuiMultiplayer extends GuiScreen implements GuiMultiplayerExt, EssentialPostScreenDrawHook {

    //#if MC>=12004
    //$$ private static final String LIST_WIDGET_INIT = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerServerListWidget;";
    //#else
    private static final String LIST_WIDGET_INIT = "Lnet/minecraft/client/gui/ServerSelectionList;<init>(Lnet/minecraft/client/gui/GuiMultiplayer;Lnet/minecraft/client/Minecraft;IIIII)V";
    //#endif

    //#if MC>=12004
    //$$ private static final String SET_DIMENSIONS = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerServerListWidget;setDimensionsAndPosition(IIII)V";
    //#elseif MC>=11600
    //$$ private static final String SET_DIMENSIONS = "Lnet/minecraft/client/gui/screen/ServerSelectionList;updateSize(IIII)V";
    //#else
    private static final String SET_DIMENSIONS = "Lnet/minecraft/client/gui/ServerSelectionList;setDimensions(IIII)V";
    //#endif

    //#if MC>=11600
    //$$ protected MixinGuiMultiplayer() {
    //$$     super(null);
    //$$ }
    //#endif

    @Shadow
    @Final
    private GuiScreen parentScreen;

    @Shadow
    protected abstract void refreshServerList();

    @Unique
    private final EssentialMultiplayerGui essentialGui = new EssentialMultiplayerGui();

    @NotNull
    @Override
    public EssentialMultiplayerGui essential$getEssentialGui() {
        return essentialGui;
    }

    @Override
    public void essential$refresh() {
        this.refreshServerList();
    }

    @Override
    public void essential$close() {
        UMinecraft.getMinecraft().displayGuiScreen(this.parentScreen);
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initEssentialGui(CallbackInfo ci) {
        essentialGui.initGui((GuiMultiplayer) (Object) this);
        essentialGui.setupButtons(
            //#if MC>=11700
            //$$ CollectionsKt.filterIsInstance(this.children(), ButtonWidget.class),
            //$$ this::addDrawableChild,
            //#else
            CollectionsKt.filterIsInstance(this.buttonList, GuiButton.class),
            this::addButton,
            //#endif
            this::removeButton
        );
    }

    //#if MC>=12109
    //$$ @Inject(method = "refreshWidgetPositions", at = @At("RETURN"))
    //$$ private void essential$updateButtonPositions(CallbackInfo ci) {
    //$$     essentialGui.initGui((MultiplayerScreen) (Object) this);
    //$$     essentialGui.setupButtons(
    //$$             CollectionsKt.filterIsInstance(this.children(), ButtonWidget.class),
    //$$             this::addDrawableChild,
    //$$             this::removeButton
    //$$     );
    //$$ }
    //#endif

    @Inject(method = "refreshServerList", at = @At("HEAD"))
    private void essential$markRefresh(CallbackInfo ci) {
        EssentialMultiplayerGui.Companion.setRefreshing(true);
    }

    //#if MC < 11200
    //$$ private GuiButton addButton(GuiButton button) {
    //$$     this.buttonList.add(button);
    //$$     return button;
    //$$ }
    //#endif

    private GuiButton removeButton(GuiButton button) {
        //#if MC>=11700
        //$$ this.remove(button);
        //#elseif MC>=11600
        //$$ this.buttons.remove(button);
        //$$ this.children.remove(button);
        //#else
        this.buttonList.remove(button);
        //#endif
        return button;
    }

    //#if MC>=12109
    //$$ @ModifyArg(method = "<init>", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/ThreePartsLayoutWidget;<init>(Lnet/minecraft/client/gui/screen/Screen;II)V"))
    //$$ private int shiftListDown$threePartsLayout(int headerHeight) {
    //$$     if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
    //$$         headerHeight += 32;
    //$$     }
    //$$     return headerHeight;
    //$$ }
    //$$ @Shadow @Final private ThreePartsLayoutWidget field_62178;
    //$$ @Inject(method = "<init>", at = @At("RETURN"))
    //$$ private void shiftListDown$title(CallbackInfo ci) {
    //$$     if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
    //$$         Positioner positioner = ((ThreePartsLayoutWidgetAccessor) this.field_62178)
    //$$             .getHeader()
    //$$             .getMainPositioner();
    //$$         // Old versions had the title at 20 from the top of the screen. Now it is centered within the header.
    //$$         // This math results in the new title position being consistent with those versions.
    //$$         int titleOffset = 20 + 9 + 20 - 32;
    //$$         positioner.marginBottom(positioner.toImpl().marginBottom + 32 - titleOffset);
    //$$     }
    //$$ }
    //#elseif MC>=12004
    //$$ @WrapOperation(method = "init", at = @At(value = "NEW", target = LIST_WIDGET_INIT))
    //$$ private MultiplayerServerListWidget shiftListDown$new(MultiplayerScreen screen, MinecraftClient client, int width, int height, int y, int itemHeight, Operation<MultiplayerServerListWidget> original) {
    //$$     if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
    //$$         height -= 32;
    //$$         y += 32;
    //$$     }
    //$$     return original.call(screen, client, width, height, y, itemHeight);
    //$$ }
    //$$
    //$$ @WrapOperation(method = "init", at = @At(value = "INVOKE", target = SET_DIMENSIONS))
    //$$ private void shiftListDown$update(MultiplayerServerListWidget widget, int width, int height, int x, int y, Operation<Void> original) {
    //$$     if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
    //$$         height -= 32;
    //$$         y += 32;
    //$$     }
    //$$     original.call(widget, width, height, x, y);
    //$$ }
    //#else
    @ModifyArg(method = "initGui", at = @At(value = "INVOKE", target = LIST_WIDGET_INIT), index = 4)
    private int shiftListDown$new(int topSpace) {
        if (!EssentialConfig.INSTANCE.getEssentialFull()) return topSpace;
        return topSpace + 32;
    }

    @ModifyArg(method = "initGui", at = @At(value = "INVOKE", target = SET_DIMENSIONS), index = 2)
    private int shiftListDown$update(int topSpace) {
        if (!EssentialConfig.INSTANCE.getEssentialFull()) return topSpace;
        return topSpace + 32;
    }
    //#endif

    @WrapWithCondition(method = "updateScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ServerSelectionList;updateNetworkServers(Ljava/util/List;)V"))
    private boolean suppressLanServersOnEssentialTabs(ServerSelectionList self, List<LanServerInfo> lanServers) {
        if (!EssentialConfig.INSTANCE.getEssentialFull()) return true;
        return EssentialConfig.INSTANCE.getCurrentMultiplayerTab() == 0;
    }

    //#if MC>=12109
    //$$ @Inject(method = "updateButtonActivationStates", at = @At("RETURN"))
    //#elseif MC>=11600
    //$$ @Inject(method = "func_214287_a", at = @At("RETURN"))
    //#else
    @Inject(method = "selectServer", at = @At("RETURN"))
    //#endif
    private void updateButtonState(CallbackInfo ci) {
        essentialGui.updateButtonState();
    }

    //#if MC<11600
    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void onButtonClicked(GuiButton button, CallbackInfo ci) {
        essentialGui.onButtonClicked(button);
    }
    //#endif

    //#if MC>=12109
    //$$ @Override
    //$$ public void essential$afterDraw(UDrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    //#else
    @Inject(method = "drawScreen", at = @At("RETURN"))
    //#if MC>=12000
    //$$ private void drawEssentialGui(DrawContext context, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
    //$$     UDrawContext drawContext = new UDrawContext(context, new UMatrixStack(context.getMatrices()));
    //#elseif MC>=11600
    //$$ private void drawEssentialGui(MatrixStack vMatrixStack, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
    //$$     UDrawContext drawContext = new UDrawContext(new UMatrixStack(vMatrixStack));
    //#else
    private void drawEssentialGui(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        UDrawContext drawContext = new UDrawContext(new UMatrixStack());
    //#endif
    //#endif

        //#if MC<11600
        // Tooltip rendering enables these even though they should be disabled.
        // But since MC renders tooltips last, Mojang never noticed.
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        //#endif

        essentialGui.draw(drawContext);
    }

    @Inject(method = "connectToServer", at = @At("HEAD"), cancellable = true)
    private void essential$onConnectToServer(ServerData server, CallbackInfo ci) {
        essentialGui.onConnectToServer(server, ci);
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void essential$onGuiClosed(CallbackInfo ci) {
        essentialGui.onClosed();
    }

    @Inject(
        //#if MC>=11600
        //$$ method = "func_214284_c",
        //#else
        method = "confirmClicked",
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GuiMultiplayer;addingServer:Z"),
            to = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GuiMultiplayer;editingServer:Z")
        ),
        //#endif
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ServerList;saveServerList()V", shift = At.Shift.AFTER),
        cancellable = true
    )
    private void switchTabOnServerAdded(CallbackInfo ci) {
        if (EssentialConfig.INSTANCE.getCurrentMultiplayerTab() != 0) {
            ci.cancel();
            essentialGui.switchTab(0);
        }
    }
}
