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
package gg.essential.mixins.impl.client.settings;

import gg.essential.key.EssentialKeybinding;
import gg.essential.key.EssentialKeybindingRegistry;
import gg.essential.universal.UKeyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Unique;

//#if MC >= 11600
//#if FORGE
//$$ import net.minecraftforge.client.settings.KeyModifier;
//#endif
//$$ import net.minecraft.client.util.InputMappings;
//#endif

public class KeybindUtils {
    //#if MC>=11200
    @Unique
    public static KeyBinding getKeyBindSaveToolbar() {
        GameSettings gameSettings = Minecraft.getMinecraft().gameSettings;
        if (gameSettings == null) return null;
        return gameSettings.keyBindSaveToolbar;
    }
    //#endif

    @Unique
    public static KeyBinding getKeyBindZoom() {
        EssentialKeybinding zoomKey = EssentialKeybindingRegistry.getInstance().getZoom();
        return zoomKey == null ? null : zoomKey.keyBinding;
    }

    @Unique
    public static boolean conflicts(KeyBinding key1, KeyBinding key2) {
        boolean areBothUnbound;
        //#if MC >= 11600
        //$$ areBothUnbound = key1.isInvalid() && key2.isInvalid();
        //#else
        areBothUnbound = key1.getKeyCode() == UKeyboard.KEY_NONE && key2.getKeyCode() == UKeyboard.KEY_NONE;
        //#endif
        //#if MC >= 11200
        return key1.conflicts(key2) && !areBothUnbound;
        //#else
        //$$ return key1.getKeyCode() == key2.getKeyCode() && !areBothUnbound;
        //#endif
    }

    @Unique
    public static void unbindKeybind(KeyBinding keyBinding) {
        //#if MC >= 11600
        //#if FORGE
        //$$ keyBinding.setKeyModifierAndCode(KeyModifier.NONE,
        //#else
        //$$ keyBinding.setBoundKey(
        //#endif
        //$$     InputMappings.INPUT_INVALID);
        //#else
        keyBinding.setKeyCode(UKeyboard.KEY_NONE);
        //#endif

        KeyBinding.resetKeyBindingArrayAndHash();
    }

    @Unique
    public static void unbindIfConflicting(KeyBinding keep, KeyBinding unbind) {
        if (keep != null & unbind != null && conflicts(keep, unbind)) {
            unbindKeybind(unbind);
        }
    }
}
