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
package gg.essential.mixins.transformers.client.settings;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;

// Keybind categories are now properly registered, so this is no longer necessary
@Mixin(KeyBinding.class)
public abstract class Mixin_FixKeyBindingCategorySortingNPE {
}
