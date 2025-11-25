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
import net.minecraft.resources.ResourcePackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

//#if MC>=12109
//$$ import java.util.function.Consumer;
//#endif

@Mixin(PackLoadingManager.class)
public interface PackLoadingManagerAccessor {
    @Accessor("field_238863_d_")
    //#if MC>=12109
    //$$ Consumer<ResourcePackOrganizer.AbstractPack> essential$getChangeRunnable();
    //#else
    Runnable essential$getChangeRunnable();
    //#endif
    @Accessor("field_238860_a_")
    List<ResourcePackInfo> essential$getEnabledList();
    @Accessor("field_238861_b_")
    List<ResourcePackInfo> essential$getDisabledList();
}