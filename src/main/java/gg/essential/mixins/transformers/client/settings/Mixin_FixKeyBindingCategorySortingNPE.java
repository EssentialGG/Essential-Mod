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

import kotlin.collections.CollectionsKt;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Map;
import java.util.Objects;

// Note: This mixin is disabled via our mixin plugin for Forge because Forge already includes a similar patch.
//       We do not disable it via preprocessor, so it gets remapped across versions.
@Mixin(KeyBinding.class)
public abstract class Mixin_FixKeyBindingCategorySortingNPE {

    @Final
    @Shadow
    private static Map<String, Integer> CATEGORY_ORDER;

    // Vanilla sorts its keybindings using an internal map.
    // Since our category isn't in that map, its sorting value will be null which vanilla doesn't handle.
    // To fix that, we add any missing categories on demand.
    @ModifyArg(method = "compareTo", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object ensureCategoryRegistered(Object category) {
        Map<String, Integer> map = Mixin_FixKeyBindingCategorySortingNPE.CATEGORY_ORDER;
        if (category instanceof String && !map.containsKey(category)) {
            int largest = Objects.requireNonNull(CollectionsKt.maxOrNull(map.values()));
            map.put((String) category, largest + 1);
        }
        return category;
    }
}
