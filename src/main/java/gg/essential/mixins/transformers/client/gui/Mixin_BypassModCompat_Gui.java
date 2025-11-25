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

//#if MC<=11202
import gg.essential.mixins.ext.client.multiplayer.ServerDataExtKt;
import gg.essential.sps.SpsAddress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiDisconnected.class)
public abstract class Mixin_BypassModCompat_Gui extends GuiScreen {
    private static final String MOD_REJECTION_MESSAGE = "Mod rejections ";
    private static final String SERVER_MOD_REJECTION_MESSAGE = "Server Mod rejections:";

    @Shadow
    private int textHeight;

    @Shadow
    @Final
    private GuiScreen parentScreen;

    @Shadow
    @Final
    @Mutable
    private ITextComponent message;

    @Unique
    private ServerData serverData;

    @Unique
    private GuiButton connectButton;

    @Unique
    private final Minecraft mc = Minecraft.getMinecraft();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        // Grab the current server data
        ServerData serverData = this.mc.getCurrentServerData();
        if (serverData == null) {
            return;
        }

        // We only want to act on SPS servers
        if (SpsAddress.parse(serverData.serverIP) == null) {
            return;
        }

        // And only if this disconnect was due to a mod compatibility issue
        String text = this.message.getUnformattedText();
        String modList;
        if (text.startsWith(MOD_REJECTION_MESSAGE)) {
            modList = text.substring(MOD_REJECTION_MESSAGE.length());
        } else if (text.startsWith(SERVER_MOD_REJECTION_MESSAGE)) {
            modList = text.substring(SERVER_MOD_REJECTION_MESSAGE.length());
        } else {
            return;
        }

        // While we are at it, let's make message more user-friendly
        this.message = new TextComponentString("FML has detected the following mods may be missing or incompatible: " + modList);

        // All good, we can act now
        this.serverData = serverData;
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initGui(CallbackInfo ci) {
        if (this.serverData == null) {
            return;
        }

        GuiButton disconnectButton = this.buttonList.get(0);
        int y = Math.min(this.height / 2 + this.textHeight / 2 + this.fontRenderer.FONT_HEIGHT, this.height - 30 - 24);
        disconnectButton.y = y + 24;

        this.connectButton = new GuiButton(1, this.width / 2 - 100, y, "Proceed with caution");
        this.buttonList.add(this.connectButton);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void actionPerformed(GuiButton button, CallbackInfo ci) {
        if (button == this.connectButton) {
            ServerDataExtKt.getExt(this.serverData).setEssential$skipModCompatCheck(true);
            this.mc.displayGuiScreen(new GuiConnecting(this.parentScreen, this.mc, this.serverData));
            ci.cancel();
        }
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class Mixin_BypassModCompat_Gui  {
//$$ }
//#endif
