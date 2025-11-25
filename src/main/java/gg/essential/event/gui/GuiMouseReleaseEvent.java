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

import net.minecraft.client.gui.GuiScreen;

public class GuiMouseReleaseEvent {
    private final GuiScreen screen;
    private final int button;

    public GuiMouseReleaseEvent(GuiScreen screen, int button) {
        this.screen = screen;
        this.button = button;
    }

    public GuiScreen getScreen() {
        return screen;
    }

    public int getButton() {
        return button;
    }
}
