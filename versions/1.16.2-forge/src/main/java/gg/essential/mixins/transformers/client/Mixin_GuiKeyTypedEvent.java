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
package gg.essential.mixins.transformers.client;

import gg.essential.Essential;
import gg.essential.event.gui.GuiKeyTypedEvent;
import gg.essential.universal.UMinecraft;
import net.minecraft.client.KeyboardListener;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.INestedGuiEventHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11700 || FABRIC
//$$ import net.minecraft.client.gui.screen.Screen;
//$$ import org.spongepowered.asm.mixin.Dynamic;
//$$ import org.spongepowered.asm.mixin.injection.Surrogate;
//#endif

@Mixin(value = KeyboardListener.class, priority = 500)
public class Mixin_GuiKeyTypedEvent {

    @Unique
    private static void keyTyped(char typedChar, int keyCode, boolean[] result, CallbackInfo ci) {
        GuiKeyTypedEvent event = new GuiKeyTypedEvent(UMinecraft.getMinecraft().currentScreen, typedChar, keyCode);
        Essential.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            if (result != null) {
                result[0] = true; // mark as handled, so it doesn't get passed on to keybindings
            }
            ci.cancel();
        }
    }

    @Inject(method = {
        //#if MC>=11904
        //$$ "lambda$onKeyEvent$5",
        //$$ "lambda$keyPress$5",
        //#else
        "lambda$onKeyEvent$4",
        "lambda$keyPress$4",
        //#endif
        "method_1454"
    }, at = @At("HEAD"), cancellable = true, remap = false)
    //#if MC>=11903
    //$$ private static void onKeyTyped(
    //#else
    private void onKeyTyped(
    //#endif
        int action,
        //#if MC>=11700
        //#if FORGE && MC>=11800 && MC<12006
        //$$ // Forge 1.18+ moves this to after `result`
        //#else
        //$$ Screen screen,
        //#endif
        //#endif
        boolean[] result,
        //#if FORGE && MC>=11800 && MC<12006
        //$$ Screen screen,
        //#endif
        //#if FORGE
        int key, int scanCode, int modifier,
        //#endif
        //#if MC<11700
        INestedGuiEventHandler handler,
        //#endif
        //#if FORGE==0
        //$$ int key, int scanCode, int modifier,
        //#endif
        CallbackInfo ci
    ) {
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            keyTyped('\0', key, result, ci);
        }
    }

    @Inject(method = {
        //#if MC>=11904
        //$$ "lambda$onCharEvent$6",
        //$$ "lambda$charTyped$6",
        //#else
        "lambda$onCharEvent$5",
        "lambda$charTyped$5",
        //#endif
        "method_1458"
    }, at = @At("HEAD"), cancellable = true, remap = false)
    //#if MC>=11800 || FORGE==0
    //$$ static
    //#endif
    private void onCharTypedSingle(
        //#if FORGELIKE
        //#if MC>=11800
        //$$ Screen screen,
        //#endif
        int typedChar, int modifiers,
        //#endif
        //#if MC>=11800 && FORGELIKE
        //$$ // Forge 1.18+ doesn't have this argument
        //#else
        IGuiEventListener handler,
        //#endif
        //#if FORGELIKE==0
        //$$ int typedChar, int modifiers,
        //#endif
        CallbackInfo ci
    ) {
        keyTyped((char) typedChar, 0, null, ci);
    }

    @Inject(method = {
        //#if MC>=11904
        //$$ "lambda$onCharEvent$7",
        //$$ "lambda$charTyped$7",
        //#else
        "lambda$onCharEvent$6",
        "lambda$charTyped$6",
        //#endif
        "method_1473"
    }, at = @At("HEAD"), cancellable = true, remap = false)
    //#if MC>=11800 || FORGE==0
    //$$ static
    //#endif
    private void onCharTypedMulti(
        //#if FORGELIKE
        //#if MC>=11800
        //$$ Screen screen,
        //#endif
        char typedChar, int modifiers,
        //#endif
        //#if MC>=11800 && FORGELIKE
        //$$ // Forge 1.18+ doesn't have this argument
        //#else
        IGuiEventListener handler,
        //#endif
        //#if FORGELIKE==0
        //$$ char typedChar, int modifiers,
        //#endif
        CallbackInfo ci
    ) {
        keyTyped(typedChar, 0, null, ci);
    }

    // OptiFine Compat:
    //#if MC>=11700
    //#if FORGE && MC>=11800
    //$$ // Forge 1.18+ now happens to use the same signature as OF for this method
    //#else
    //$$ @Dynamic("Optifine reorders arguments")
    //$$ @Surrogate
    //#if MC>=11904 && FABRIC
    //$$ static
    //#endif
    //$$ private void onKeyTyped(int action, boolean[] result, Screen screen, int key, int scanCode, int modifier, CallbackInfo ci) {
    //$$     if (action == 1) {
    //$$         keyTyped('\0', key, result, ci);
    //$$     }
    //$$ }
    //#endif
    //$$
    //$$ @Dynamic("Optifine reorders arguments")
    //$$ @Surrogate
    //$$ private void onKeyTyped(int action, boolean[] result, int key, int scanCode, int modifier, Screen screen, CallbackInfo ci) {
    //$$     if (action == 1) {
    //$$         keyTyped('\0', key, result, ci);
    //$$     }
    //$$ }
    //#elseif FABRIC
    //$$ @Dynamic("Optifine reorders arguments")
    //$$ @Surrogate
    //$$ private void onKeyTyped(int action, boolean[] result, int key, int scanCode, int modifier, ParentElement screen, CallbackInfo ci) {
    //$$     if (action == 1) {
    //$$         keyTyped('\0', key, result, ci);
    //$$     }
    //$$ }
    //#endif

    //#if MC>=11700 || FABRIC
    //$$ @Dynamic("Optifine reorders arguments and adds static modifier")
    //$$ @Surrogate
    //$$ private static void onCharTypedSingle(Keyboard self, int typedChar, int modifiers, Element handler, CallbackInfo ci) {
    //$$     keyTyped((char) typedChar, 0, null, ci);
    //$$ }
    //$$
    //$$ @Dynamic("Optifine reorders arguments and adds static modifier")
    //$$ @Surrogate
    //$$ private static void onCharTypedMulti(Keyboard self, char typedChar, int modifiers, Element handler, CallbackInfo ci) {
    //$$     keyTyped(typedChar, 0, null, ci);
    //$$ }
    //#endif
}
