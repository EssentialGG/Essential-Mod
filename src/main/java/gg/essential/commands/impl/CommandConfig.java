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
package gg.essential.commands.impl;

import gg.essential.api.commands.Command;
import gg.essential.api.commands.DefaultHandler;
import gg.essential.config.McEssentialConfig;
import gg.essential.gui.vigilancev2.VigilanceV2SettingsGui;
import gg.essential.util.GuiUtil;

/**
 * Open Essential's config gui
 */
public class CommandConfig extends Command {
    public CommandConfig() {
        super("essential");
    }

    @DefaultHandler
    public void handle() {
        GuiUtil.openScreen(VigilanceV2SettingsGui.class, McEssentialConfig.INSTANCE::gui);
    }
}
