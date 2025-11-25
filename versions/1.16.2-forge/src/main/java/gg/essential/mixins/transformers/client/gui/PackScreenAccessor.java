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

import net.minecraft.client.gui.screen.PackLoadingManager;
import net.minecraft.client.gui.screen.PackScreen;
import net.minecraft.client.gui.widget.list.ResourcePackList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PackScreen.class)
public interface PackScreenAccessor {
    @Accessor("field_238891_u_")
    ResourcePackList essential$getAvailablePackList();

    @Accessor("field_238892_v_")
    ResourcePackList essential$getSelectedPackList();

    @Accessor("field_238887_q_")
    PackLoadingManager essential$getPackLoadingManager();
}