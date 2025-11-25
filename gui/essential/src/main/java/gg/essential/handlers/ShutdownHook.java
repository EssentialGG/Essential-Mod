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
package gg.essential.handlers;

import gg.essential.api.utils.ShutdownHookUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Util for running code right before game closes. This is called after Minecraft#Shutdown
 */
public class ShutdownHook implements ShutdownHookUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownHook.class);
    public static final ShutdownHook INSTANCE = new ShutdownHook();
    private final Queue<Runnable> hooks = new ConcurrentLinkedQueue<>();

    public void execute() {
        for (Runnable hook : this.hooks) {
            try {
                hook.run();
            } catch (Exception e) {
                LOGGER.error("Failed to run shutdown hook.", e);
            }
        }
    }

    public void register(@NotNull Runnable runnable) {
        this.hooks.add(runnable);
    }

    public void unregister(@NotNull Runnable runnable) {
        this.hooks.remove(runnable);
    }
}
