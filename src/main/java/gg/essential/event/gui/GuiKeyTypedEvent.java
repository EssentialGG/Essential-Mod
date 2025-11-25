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
package gg.essential.event.gui;

import gg.essential.event.CancellableEvent;
import net.minecraft.client.gui.GuiScreen;

public class GuiKeyTypedEvent extends CancellableEvent {

    private final GuiScreen screen;
    private final char typedChar;
    private final int keyCode;

    /**
     * Fired whenever a key is typed
     *
     * @param screen    Current GuiScreen of where the key was typed
     * @param typedChar character that was typed
     * @param keyCode   keycode for the character that was typed
     */
    public GuiKeyTypedEvent(GuiScreen screen, char typedChar, int keyCode) {
        this.screen = screen;
        this.typedChar = typedChar;
        this.keyCode = keyCode;
    }

    public GuiScreen getScreen() {
        return screen;
    }

    public char getTypedChar() {
        return typedChar;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public static class Post extends GuiKeyTypedEvent {

        /**
         * Fired after the vanilla Screen key typed handling has run, in later versions this will also not be fired if the
         * key event was handled, early returns out of the vanilla method can also be considered "handled" and not trigger this.
         * NOTE: By default, this will not be fired from a screen that fully overrides the Screen keyTyped method without calling super, check usages!
         *
         * @param screen    Current GuiScreen of where the key was typed
         * @param typedChar character that was typed
         * @param keyCode   keycode for the character that was typed
         */
        public Post(final GuiScreen screen, final char typedChar, final int keyCode) {
            super(screen, typedChar, keyCode);
        }
    }
}