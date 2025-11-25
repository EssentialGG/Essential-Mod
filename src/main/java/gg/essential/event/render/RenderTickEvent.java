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
package gg.essential.event.render;

import gg.essential.universal.UMatrixStack;
import gg.essential.util.UDrawContext;
import org.jetbrains.annotations.Nullable;

public final class RenderTickEvent {

    private final boolean pre;
    private final boolean loadingScreen;

    private final UMatrixStack matrixStack;
    private final UDrawContext drawContext;
    private final float partialTicksMenu;
    private final float partialTicksInGame;

    public RenderTickEvent(boolean pre, boolean loadingScreen, UDrawContext drawContext, UMatrixStack matrixStack, float partialTicksMenu, float partialTicksInGame) {
        this.pre = pre;
        this.loadingScreen = loadingScreen;
        this.matrixStack = matrixStack;
        this.drawContext = drawContext;
        this.partialTicksMenu = partialTicksMenu;
        this.partialTicksInGame = partialTicksInGame;

        // TODO maybe split into separate events? or move post event to where it doesn't require the draw context?
        if (!pre && drawContext == null) throw new IllegalArgumentException("Post event requires draw context");
    }

    public boolean isPre() {
        return pre;
    }

    public boolean isLoadingScreen() {
        return loadingScreen;
    }

    @Nullable // null for pre, non-null for post
    public UDrawContext getDrawContext() {
        return drawContext;
    }

    public UMatrixStack getMatrixStack() {
        return matrixStack;
    }

    public float getPartialTicksMenu() {
        return partialTicksMenu;
    }

    public float getPartialTicksInGame() {
        return partialTicksInGame;
    }

    public static class Final {
    }
}
