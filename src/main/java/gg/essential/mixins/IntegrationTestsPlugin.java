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
package gg.essential.mixins;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static gg.essential.mixins.Plugin.hasClass;

public class IntegrationTestsPlugin implements IMixinConfigPlugin {
    public static final boolean ENABLED = System.getProperty("essential.integrationTest") != null;

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!ENABLED) {
            return false;
        }

        if (mixinClassName.endsWith("_Emojiful")) {
            if (!hasClass(targetClassName)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        enableInjectionCounting(mixinInfo);
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static final Set<IMixinConfig> injectionCountingEnabled = Collections.newSetFromMap(new IdentityHashMap<>());

    public static void enableInjectionCounting(IMixinInfo mixinInfo) {
        if (!ENABLED) {
            return;
        }

        IMixinConfig config = mixinInfo.getConfig();
        if (!injectionCountingEnabled.add(config)) {
            return;
        }
        try {
            Field injectorOptionsField = config.getClass().getDeclaredField("injectorOptions");
            injectorOptionsField.setAccessible(true);
            Object injectorOptions = injectorOptionsField.get(config);
            Field defaultRequireValueField = injectorOptions.getClass().getDeclaredField("defaultRequireValue");
            defaultRequireValueField.setAccessible(true);
            defaultRequireValueField.set(injectorOptions, 1);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void registerUncaughtExceptionHandler() {
        if (!ENABLED) {
            return;
        }

        if (Thread.getDefaultUncaughtExceptionHandler() != null) {
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                LoggerFactory.getLogger("UncaughtExceptionHandler")
                    .error("Uncaught exception in thread \"{}\"", thread.getName(), throwable);
            } catch (Throwable t) {
                throwable.addSuppressed(t);
                //noinspection CallToPrintStackTrace
                throwable.printStackTrace();
            }
        });
    }
}
