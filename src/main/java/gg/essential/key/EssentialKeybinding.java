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
package gg.essential.key;

import com.google.common.collect.ImmutableMap;
import gg.essential.api.utils.GuiUtil;
import gg.essential.config.EssentialConfig;
import gg.essential.mixins.transformers.client.options.GameOptionsAccessor;
import gg.essential.mixins.transformers.client.options.KeyBindingAccessor;
import gg.essential.universal.UKeyboard;
import gg.essential.universal.UMinecraft;
import gg.essential.util.GuiEssentialPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

//#if MC >= 11400
//#if FORGE
//$$ import net.minecraftforge.client.settings.KeyModifier;
//#endif
//$$ import net.minecraft.client.util.InputMappings;
//#endif

/**
 * Wrapper class so we can add functionality easily
 */
public class EssentialKeybinding implements GuiEssentialPlatform.Keybind {
    public static final List<EssentialKeybinding> ALL_BINDS = new ArrayList<>();
    public static boolean cancelKeybinds = false;
    public final KeyBinding keyBinding;
    private final String keyId;
    private final boolean alwaysTick;
    private Runnable onInitialPress;
    private Runnable onRepeatedHold;
    private Runnable onRelease;
    private boolean registeredWithMinecraft = false;
    private boolean pressed = false;
    private boolean requiresEssentialFull = false;

    //#if MC>=12109
    //$$ public EssentialKeybinding(String keyId, KeyBinding.Category category, int keyCode) {
    //#else
    public EssentialKeybinding(String keyId, String category, int keyCode) {
    //#endif
        this(keyId, category, keyCode, false);
    }

    //#if MC>=12109
    //$$ public EssentialKeybinding(String keyId, KeyBinding.Category category, int keyCode, boolean alwaysTick) {
    //#else
    public EssentialKeybinding(String keyId, String category, int keyCode, boolean alwaysTick) {
    //#endif
        this.keyId = keyId;
        this.keyBinding = new KeyBinding(LEGACY_IDS.getOrDefault(keyId, "keybind.name." + keyId), keyCode, category);
        this.alwaysTick = alwaysTick;
        ALL_BINDS.add(this);
    }

    private int getKeyCode() {
        //#if MC < 11400
        return keyBinding.getKeyCode();
        //#else
        //$$return ((KeyBindingAccessor) keyBinding).getBoundKey().getKeyCode();
        //#endif
    }

    public void register() {
        GameSettings settings = Minecraft.getMinecraft().gameSettings;
        ((GameOptionsAccessor) settings).setKeyBindings(register(settings.keyBindings));

        //#if MC>=11202
        KeyBindingAccessor.getKeybinds().put(keyBinding.getKeyDescription(), keyBinding);
        //#else
        //$$ KeyBindingAccessor.getKeybinds().add(keyBinding);
        //#endif
        KeyBinding.resetKeyBindingArrayAndHash();
    }

    // For registering when mc.gameSettings might not yet be initialized
    KeyBinding[] register(KeyBinding[] allBindings) {
        if (registeredWithMinecraft) return allBindings;
        registeredWithMinecraft = true;
        return ArrayUtils.add(allBindings, keyBinding);
    }

    public EssentialKeybinding withInitialPress(Runnable runnable) {
        this.onInitialPress = runnable;
        return this;
    }

    public EssentialKeybinding requiresEssentialFull() {
        requiresEssentialFull = true;
        return this;
    }

    public EssentialKeybinding withRelease(Runnable runnable) {
        this.onRelease = runnable;
        return this;
    }

    public EssentialKeybinding withRepeatedHold(Runnable runnable) {
        this.onRepeatedHold = runnable;
        return this;
    }

    public void tickEvents() {
        if (alwaysTick) {
            tickMainMenu();
        } else if (GuiUtil.getOpenedScreen() instanceof GuiMainMenu ^ cancelKeybinds) {
            cancelKeybinds = false;
            tickMainMenu();
        } else if (UMinecraft.getWorld() != null) {
            tickWorld();
        }
    }

    private void tickMainMenu() {
        if (getRequiresEssentialFull() && !EssentialConfig.INSTANCE.getEssentialFull()) return;
        int keyCode = getKeyCode();
        boolean keyDown = keyCode != UKeyboard.KEY_NONE && UKeyboard.isKeyDown(keyCode);

        if (!pressed && keyDown) {
            pressed = true;
            if (onInitialPress != null)
                onInitialPress.run();
        } else if (pressed && keyDown) {
            if (onRepeatedHold != null)
                onRepeatedHold.run();
        } else if (pressed) {
            pressed = false;
            if (onRelease != null)
                onRelease.run();
        }
    }

    private void tickWorld() {
        if (getRequiresEssentialFull() && !EssentialConfig.INSTANCE.getEssentialFull()) return;
        if (keyBinding.isPressed() && onInitialPress != null) {
            onInitialPress.run();
        } else if (keyBinding.isKeyDown() && onRepeatedHold != null) {
            onRepeatedHold.run();
        } else if (pressed && !keyBinding.isKeyDown() && onRelease != null) {
            onRelease.run();
        }
        pressed = keyBinding.isKeyDown();
    }

    public boolean isRegisteredWithMinecraft() {
        return registeredWithMinecraft;
    }

    public void setKeyCode(int keyCode) {
        //#if MC < 11400
        keyBinding.setKeyCode(keyCode);
        //#else
        //#if FORGE
        //$$ keyBinding.setKeyModifierAndCode(KeyModifier.NONE,
        //#else
        //$$ keyBinding.setBoundKey(
        //#endif
        //#if MC>=12109
        //$$     InputUtil.fromKeyCode(new net.minecraft.client.input.KeyInput(keyCode, -1, 0)));
        //#else
        //$$     InputMappings.getInputByCode(keyCode, -1));
        //#endif
        //#endif
    }

    @Override
    public boolean isBound() {
        //#if MC>=11400
        //$$ return !keyBinding.isInvalid();
        //#else
        return keyBinding.getKeyCode() != 0;
        //#endif
    }

    @Override
    public @Nullable String getBoundKeyName() {
        if (!isBound()) return null;
        return UKeyboard.getKeyName(keyBinding);
    }

    @Override
    public boolean isConflicting() {
        if (!this.isBound()) {
            return false;
        }

        for (KeyBinding binding : Minecraft.getMinecraft().gameSettings.keyBindings) {
            if (this.keyBinding == binding) continue;

            //#if MC>=11701
            //$$ if (this.keyBinding.equals(binding)) {
            //#elseif MC==10809
            //$$ if (this.keyBinding.getKeyCode() == binding.getKeyCode()) {
            //#else
            if (this.keyBinding.conflicts(binding)) {
            //#endif
                return true;
            }
        }

        return false;
    }

    public boolean isKeyCode(int keyCode) {
        return isBound() && getKeyCode() == keyCode;
    }

    /**
     * Unregisters the keybinding by removing it from the MC keybinding list
     */
    public void unregister() {
        if (!registeredWithMinecraft) return;
        GameSettings settings = Minecraft.getMinecraft().gameSettings;
        //There should only ever be one instance
        int i = ArrayUtils.indexOf(settings.keyBindings, keyBinding);
        if (i > 0)
            ((GameOptionsAccessor) settings).setKeyBindings(ArrayUtils.removeAll(settings.keyBindings, i));

        //#if MC>=11202
        KeyBindingAccessor.getKeybinds().remove(keyBinding.getKeyDescription());
        //#else
        //$$ KeyBindingAccessor.getKeybinds().remove(keyBinding);
        //#endif
        KeyBinding.resetKeyBindingArrayAndHash();

        registeredWithMinecraft = false;
    }

    public boolean getRequiresEssentialFull() {
        return requiresEssentialFull;
    }

    /**
     * We used to translate certain keybinding ids (which was a bad idea because they will then get lost if someone
     * changes their language or if we decide to change the translation, and I18n is not even loaded at the point where
     * we need to register keybindings since 1.17) but we no longer do. To not wipe everyone's bindings, we keep the
     * legacy ids for those bindings which have seen use in production.
     */
    private static final Map<String, String> LEGACY_IDS = ImmutableMap.<String, String>builder()
        // Does not apply to 1.17+ cause I18n is not loaded when we registered the bindings, so they were never
        // translated in the first place.
        //#if MC<11700
        .put("ESSENTIAL_FRIENDS", "Open Friends Gui")
        .put("COSMETIC_STUDIO", "Open Cosmetic Studio")
        .put("ZOOM", "Zoom")
        .put("COSMETICS_VISIBILITY_TOGGLE", "Cosmetic Visibility Toggle")
        .put("CHAT_PEEK", "Chat Peek")
        .put("INVITE_FRIENDS", "Invite Friends")
        //#endif
        .build();
}
