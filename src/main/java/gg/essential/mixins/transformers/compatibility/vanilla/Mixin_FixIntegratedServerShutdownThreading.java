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
package gg.essential.mixins.transformers.compatibility.vanilla;

import gg.essential.mixins.transformers.server.MinecraftServerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Minecraft shuts down the integrated server from a thread scheduled via {@link Runtime#addShutdownHook(Thread)}.
 * However it does so by directly calling {@link IntegratedServer#stopServer()} which executes the shutdown procedures
 * while the server thread is still running or already partially or completely shut down.
 * At best this just throws, but if you're unlucky it can also result in data corruption.
 * <p>
 * To properly shut down the server from a shutdown hook thread we instead notify the server thread that we'd like to
 * stop, and then wait for it to be complete.
 */
@Mixin(Minecraft.class)
public class Mixin_FixIntegratedServerShutdownThreading {
    @Redirect(method = "stopIntegratedServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/integrated/IntegratedServer;stopServer()V"))
    private static void initiateShutdownAndJoin(IntegratedServer server) throws InterruptedException {
        Thread serverThread = ((MinecraftServerAccessor) server).getServerThread();
        // Need to check if the thread exists because 1.8.9 at boot always initializes an `IntegratedServer` instance
        // which doesn't have a thread (or even world).
        if (serverThread != null) {
            server.initiateShutdown();
            serverThread.join();
        }
    }
}
