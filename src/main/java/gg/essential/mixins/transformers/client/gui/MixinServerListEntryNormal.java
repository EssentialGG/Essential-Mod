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
import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.elementa.components.UIImage;
import gg.essential.gui.EssentialPalette;
import gg.essential.gui.multiplayer.DividerServerListEntry;
import gg.essential.gui.multiplayer.EssentialMultiplayerGui;
import gg.essential.gui.multiplayer.FriendsIndicator;
import gg.essential.mixins.ext.client.gui.GuiMultiplayerExt;
import gg.essential.mixins.ext.client.gui.ServerListEntryNormalExt;
import gg.essential.network.connectionmanager.serverdiscovery.NewServerDiscoveryManager;
import gg.essential.sps.SpsAddress;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.UMinecraft;
import gg.essential.util.UUIDUtil;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.gui.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

import static gg.essential.mixins.ext.client.multiplayer.ServerDataExtKt.getExt;

//#if MC>=12106
//$$ import com.mojang.blaze3d.pipeline.RenderPipeline;
//$$ import static gg.essential.util.UIdentifierKt.toMC;
//#endif

//#if MC>=12102
//$$ import java.util.function.Function;
//#endif

//#if MC>=12005
//$$ import net.minecraft.text.Text;
//#endif

//#if MC>=12000
//$$ import net.minecraft.client.gui.DrawContext;
//$$ import net.minecraft.util.Identifier;
//#endif

//#if MC>=11602
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import gg.essential.mixins.transformers.client.gui.AbstractListAccessor;
//$$ import gg.essential.util.HelpersKt;
//$$ import net.minecraft.util.text.ITextComponent;
//$$ import net.minecraft.util.text.ITextProperties;
//$$ import net.minecraft.util.text.TranslationTextComponent;
//$$ import java.util.Arrays;
//$$ import java.util.Collections;
//$$ import java.util.List;
//$$ import java.util.stream.Collectors;
//$$ import static gg.essential.util.HelpersKt.textLiteral;
//#else
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
//#endif

@Mixin(ServerListEntryNormal.class)
public abstract class MixinServerListEntryNormal
    //#if MC>=12109
    //$$ extends MultiplayerServerListWidget.Entry
    //#endif
    implements ServerListEntryNormalExt {

    //#if MC>=12106
    //$$ private static final String DRAW_TEXTURE = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V";
    //#elseif MC>=12102
    //$$ private static final String DRAW_TEXTURE = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V";
    //#elseif MC>=12002
    //$$ private static final String DRAW_TEXTURE = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V";
    //#elseif MC>=12000
    //$$ private static final String DRAW_TEXTURE = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIFFIIII)V";
    //#elseif MC>=11600
    //$$ private static final String DRAW_TEXTURE = "Lnet/minecraft/client/gui/AbstractGui;blit(Lcom/mojang/blaze3d/matrix/MatrixStack;IIFFIIII)V";
    //#else
    private static final String DRAW_TEXTURE = "Lnet/minecraft/client/gui/Gui;drawModalRectWithCustomSizedTexture(IIFFIIFF)V";
    //#endif

    @Shadow @Final private ServerData server;
    @Shadow @Final private GuiMultiplayer owner;

    @Unique
    private FriendsIndicator friends;

    @Unique
    private int populationOrVersionTextWidth;

    @Unique
    private UIImage downloadIcon;

    @Unique
    private NewServerDiscoveryManager.ImpressionConsumer impressionConsumer = null;

    @Unique
    private boolean trackedImpression = false;

    @Override
    public void essential$setImpressionConsumer(@NotNull NewServerDiscoveryManager.ImpressionConsumer consumer) {
        this.impressionConsumer = consumer;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initFriends(CallbackInfo ci) {
        this.friends = new FriendsIndicator(this.server);
    }

    @NotNull
    @Override
    public FriendsIndicator essential$getFriends() {
        return this.friends;
    }

    //#if MC>=11600
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringPropertyWidth(Lnet/minecraft/util/text/ITextProperties;)I"))
    //$$ private ITextProperties recordPopulationOrVersionTextWidth(ITextProperties text) {
    //$$     this.populationOrVersionTextWidth = UMinecraft.getFontRenderer().getStringPropertyWidth(text);
    //#else
    @ModifyArg(method = "drawEntry", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"))
    private String recordPopulationOrVersionTextWidth(String text) {
        this.populationOrVersionTextWidth = UMinecraft.getFontRenderer().getStringWidth(text);
    //#endif
        return text;
    }

    @Inject(method = "drawEntry", at = @At("RETURN"))
    private void drawFriendsIndicator(
        //#if MC>=12000
        //$$ DrawContext context,
        //#elseif MC>=11602
        //$$ MatrixStack vMatrixStack,
        //#endif
        //#if MC<12109
        int slotIndex,
        //#if MC>=11602
        //$$ int y,
        //$$ int x,
        //#else
        int x,
        int y,
        //#endif
        int listWidth,
        int slotHeight,
        //#endif
        int mouseX,
        int mouseY,
        boolean isSelected,
        //#if MC>=11202
        float partialTicks,
        //#endif
        CallbackInfo ci
    ) {
        //#if MC>=12109
        //$$ int x = this.getContentX();
        //$$ int y = this.getContentY();
        //$$ int listWidth = this.getContentWidth();
        //#endif
        //#if MC>=12000
        //$$ UMatrixStack matrixStack = new UMatrixStack(context.getMatrices());
        //#elseif MC>=11602
        //$$ UMatrixStack matrixStack = new UMatrixStack(vMatrixStack);
        //#else
        UMatrixStack matrixStack = new UMatrixStack();
        //#endif
        String tooltip = this.friends.draw(matrixStack, x, y, listWidth, mouseX, mouseY, populationOrVersionTextWidth);
        if (tooltip != null) {
            //#if MC>=12106
            //$$ context.drawTooltip(Arrays.stream(tooltip.split("\n")).map(HelpersKt::textLiteral).map(Text::asOrderedText).collect(Collectors.toList()), mouseX, mouseY);
            //#elseif MC>=12005
            //$$ this.screen.setTooltip(Arrays.stream(tooltip.split("\n")).map(HelpersKt::textLiteral).map(Text::asOrderedText).collect(Collectors.toList()));
            //#elseif MC>=11600
            //$$ this.owner.func_238854_b_(Arrays.stream(tooltip.split("\n")).map(HelpersKt::textLiteral).collect(Collectors.toList()));
            //#else
            this.owner.setHoveringText(tooltip);
            //#endif
        }
    }

    //#if MC>=11600
    //#if MC>=12005
    //#if MC>=12106
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/text/Text;II)V"))
    //#else
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerScreen;setTooltip(Lnet/minecraft/text/Text;)V"))
    //#endif
    //$$ private Text addServerRegionToPing(Text str) {
    //#else
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/Collections;singletonList(Ljava/lang/Object;)Ljava/util/List;"))
    //$$ private Object addServerRegionToPing(Object str) {
    //#endif
        //#if MC>=11900
        //$$ Object content = ((Text) str).getContent();
        //#else
        //$$ Object content = str;
        //#endif
    //$$     if (!(content instanceof TranslationTextComponent)) return str;
    //$$     if (!"multiplayer.status.ping".equals(((TranslationTextComponent) content).getKey())) return str;
    //#else
    @ModifyConstant(method = "drawEntry", constant = @Constant(stringValue = "ms"))
    private String addServerRegionToPing(String str) {
    //#endif
        String region = getExt(this.server).getEssential$pingRegion();
        if (region != null) {
            // Uppercase cause it looks nicer, no trailing numbers cause those do not really add anything
            region = region.toUpperCase(Locale.ROOT);
            while (!region.isEmpty() && Character.isDigit(region.charAt(region.length() - 1))) {
                region = region.substring(0, region.length() - 1);
            }

            if ("SPS".equals(region)) {
                SpsAddress spsAddress = SpsAddress.parse(this.server.serverIP);
                if (spsAddress != null) {
                    region = UUIDUtil.getName(spsAddress.getHost()).join();
                }
            }

            if (!region.isEmpty()) {
                //#if MC>=11600
                //$$ str = ((ITextComponent) str).deepCopy().append(textLiteral(" from " + region));
                //#else
                str += " from " + region;
                //#endif
            }
        }
        return str;
    }

    @WrapWithCondition(method = "drawEntry",at = @At(value = "INVOKE", target = DRAW_TEXTURE, ordinal = 0))
    private boolean drawDownloadIcon(
        //#if MC>=12000
        //$$ DrawContext context,
        //#if MC>=12106
        //$$ RenderPipeline renderPipeline,
        //#elseif MC>=12102
        //$$ Function<?, ?> renderLayers,
        //#endif
        //$$ Identifier texture,
        //#elseif MC>=11602
        //$$ MatrixStack vMatrixStack,
        //#endif
        int x,
        int y,
        //#if MC<12002
        float u,
        float v,
        //#endif
        int width,
        int height
        //#if MC>=12002
        //#elseif MC>=11600
        //$$ , int textureWidth,
        //$$ int textureHeight
        //#else
        , float textureWidth,
        float textureHeight
        //#endif
    ) {
        //#if MC>=12000
        //$$ UMatrixStack matrixStack = new UMatrixStack(context.getMatrices());
        //#elseif MC>=11602
        //$$ UMatrixStack matrixStack = new UMatrixStack(vMatrixStack);
        //#else
        UMatrixStack matrixStack = new UMatrixStack();
        //#endif
        if (getExt(this.server).getEssential$showDownloadIcon()) {
            //#if MC>=12106
            //$$ context.drawTexture(renderPipeline, toMC(EssentialPalette.DOWNLOAD_7X8_ID),
            //$$     x + 2, y + 1,
            //$$     0f, 0f, // u, v
            //$$     7, 8, // width, height
            //$$     7, 8, // textureWidth, textureHeight
            //$$     EssentialPalette.BLUE_SHADOW.getRGB());
            //$$ context.drawTexture(renderPipeline, toMC(EssentialPalette.DOWNLOAD_7X8_ID),
            //$$     x + 1, y,
            //$$     0f, 0f, // u, v
            //$$     7, 8, // width, height
            //$$     7, 8, // textureWidth, textureHeight
            //$$     EssentialPalette.SERVER_DOWNLOAD_ICON.getRGB());
            //#else
            if (downloadIcon == null) {
                downloadIcon = EssentialPalette.DOWNLOAD_7X8.create();
            }
            downloadIcon.drawImage(matrixStack, x + 2, y + 1, 7, 8, EssentialPalette.BLUE_SHADOW);
            downloadIcon.drawImage(matrixStack, x + 1, y, 7, 8, EssentialPalette.SERVER_DOWNLOAD_ICON);
            //#endif
            return false;
        }
        return true;
    }

    @WrapWithCondition(
        method = "drawEntry",
        at = @At(
            value = "INVOKE",
            //#if MC>=12106
            //$$ target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/text/Text;II)V",
            //$$ ordinal = 0
            //#elseif MC>=12006
            //$$ target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerScreen;setTooltip(Lnet/minecraft/text/Text;)V",
            //$$ ordinal = 0
            //#elseif MC>=11600
            //$$ target = "Lnet/minecraft/client/gui/screen/MultiplayerScreen;func_238854_b_(Ljava/util/List;)V",
            //$$ ordinal = 0
            //#else
            target = "Lnet/minecraft/client/gui/GuiMultiplayer;setHoveringText(Ljava/lang/String;)V",
            ordinal = 1
            //#endif
        )
    )
    private boolean showCustomTooltip(
        //#if MC>=12106
        //$$ DrawContext drawContext,
        //#else
        GuiMultiplayer instance,
        //#endif
        //#if MC>=12006
        //$$ Text text,
        //#elseif MC>=11600
        //$$ List<ITextComponent> texts,
        //#else
        String text,
        //#endif
        //#if MC>=12106
        //$$ int tooltipX,
        //$$ int tooltipY,
        //#endif
        //#if MC>=12109
        //$$ @Local(argsOnly = true) DrawContext drawContext_ // don't actually need this, just need something for the trailing comma
        //#else
        //#if MC>=11600
        //$$ @Local(ordinal = 1, argsOnly = true) int y,
        //$$ @Local(ordinal = 2, argsOnly = true) int x,
        //#else
        @Local(ordinal = 1, argsOnly = true) int x,
        @Local(ordinal = 2, argsOnly = true) int y,
        //#endif
        @Local(ordinal = 3, argsOnly = true) int listWidth
        //#endif
    ) {
        //#if MC>=12109
        //$$ int x = this.getContentX();
        //$$ int y = this.getContentY();
        //$$ int listWidth = this.getContentWidth();
        //#endif
        if (getExt(this.server).getEssential$showDownloadIcon()) {
            EssentialMultiplayerGui gui = ((GuiMultiplayerExt) this.owner).essential$getEssentialGui();
            gui.showTooltipString(x + listWidth - 15 + 1, y, 7, 8, "Download compatible version");
            return false;
        }
        return true;
    }

    //#if MC>=11600
    //$$ @Inject(method = "mouseClicked", at = @At(value = "CONSTANT", args = "doubleValue=32.0"), cancellable = true)
    //#if MC>=12109
    //$$ private void onMousePressed(CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) double relativeX, @Local(ordinal = 1) double relativeY) {
    //#else
    //$$ private void onMousePressed(CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 2) double relativeX, @Local(ordinal = 3) double relativeY) {
    //#endif
    //#else
    @Inject(method = "mousePressed", at = @At("HEAD"), cancellable = true)
    private void handleMousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY, CallbackInfoReturnable<Boolean> cir) {
    //#endif
        if (getExt(this.server).getEssential$showDownloadIcon()) {
            int listWidth = ((GuiMultiplayerAccessor) this.owner).getServerListSelector().getListWidth();
            if (relativeX >= listWidth - 15 && relativeX <= listWidth - 5 && relativeY >= 0 && relativeY <= 8) {
                ((GuiMultiplayerExt) this.owner).essential$getEssentialGui().showDownloadModal(this.server);
                cir.setReturnValue(true);
            }
        }
    }

    @ModifyVariable(
            method = "drawEntry",
            at = @At(value = "STORE"),
            slice = @Slice(from = @At(
                value = "FIELD",
                //#if MC>=11600
                //$$ target = "Lnet/minecraft/client/multiplayer/ServerData;populationInfo:Lnet/minecraft/util/text/ITextComponent;",
                //#else
                target = "Lnet/minecraft/client/multiplayer/ServerData;populationInfo:Ljava/lang/String;",
                //#endif
                opcode = Opcodes.GETFIELD
            )),
            ordinal = 0
    )
    //#if MC>=11600
    //$$ private ITextComponent showPopulationInfo(ITextComponent s) {
    //#else
    private String showPopulationInfo(String s) {
    //#endif
        if (getExt(this.server).getEssential$showDownloadIcon()) {
            return this.server.populationInfo;
        }
        return s;
    }

    @Inject(method = "drawEntry", at = @At("HEAD"))
    private void trackImpression(
        CallbackInfo ci
        //#if MC<12109
        //#if MC>=11600
        //$$ , @Local(ordinal = 1, argsOnly = true) int y
        //#else
        , @Local(ordinal = 2, argsOnly = true) int y
        //#endif
        //#endif
    ) {
        //#if MC>=12109
        //$$ int y = this.getContentY();
        //#endif
        if (!this.trackedImpression && this.impressionConsumer != null) {
            ServerSelectionList list = ((GuiMultiplayerAccessor) this.owner).getServerListSelector();
            //#if MC>=12004
            //$$ int top = list.getY();
            //$$ int bottom = list.getBottom();
            //#elseif MC>=11600
            //$$ int top = ((AbstractListAccessor) list).essential$getTop();
            //$$ int bottom = ((AbstractListAccessor) list).essential$getBottom();
            //#else
            int top = list.top;
            int bottom = list.bottom;
            //#endif
            if (y >= top && y + DividerServerListEntry.SERVER_ENTRY_HEIGHT <= bottom) {
                this.impressionConsumer.accept(this.server.serverIP);
                this.trackedImpression = true;
            }
        }
    }
}
