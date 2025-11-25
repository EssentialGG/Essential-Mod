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
package gg.essential.gui.overlay

import gg.essential.elementa.components.Window

interface Layer {
    val priority: LayerPriority
    /**
     * The [Window] powering this layer.
     */
    val window: Window

    /**
     * Controls whether this layer is visible on the screen.
     *
     * May be set to `false` for single frames to hide specific layers from screenshots.
     * This does not affect input processing and should not be used to hide layers for extended periods of time.
     */
    var rendered: Boolean

    /**
     * Whether this layer respects the vanilla Hide Gui (F1 key) setting.
     *
     * If this is true, then the layer won't be visible while the gui is hidden.
     * If this is false, then the layer will be visible regardless of the value of the vanilla setting.
     * In this context, the gui is considered hidden when Hide Gui is enabled and the current screen is null (this
     * matches the behavior of the vanilla HUD).
     *
     * Note that currently the screen will continue to be animated and will receive draw calls even while hidden (but
     * will be draw far off-screen).
     * This is due to a limitation in Elementa (no way to pause an entire Window) and may be changed in the future.
     *
     * Defaults to `true`.
     */
    var respectsHideGuiSetting: Boolean

    /**
     * Whether this layer requires (or at least recommends) the user to use the mouse.
     *
     * If true and the mouse is currently captured by the game (so they can turn around in-game), an empty screen will
     * be opened while this layer exists (so the mouse is freed and can be used to interact with the GUI).
     * If the empty screen has not been replaced by another screen by the time  this layer is destroyed, it will
     * automatically be closed, capturing the mouse again if the user is still in-game.
     *
     * The empty screen cannot be closed by the user directly. It is up to the content in the layer to correctly handle
     * Esc key events.
     *
     * Defaults to `true` for layers with [priority] == [LayerPriority.Modal], and `false` for all others.
     */
    var unlocksMouse: Boolean
}