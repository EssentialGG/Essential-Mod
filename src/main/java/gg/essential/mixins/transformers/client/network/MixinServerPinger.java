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
package gg.essential.mixins.transformers.client.network;

import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.network.pingproxy.ProxyPingServerKt;
import gg.essential.sps.SpsAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.ServerPinger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12002
//$$ import net.minecraft.client.network.ServerAddress;
//#endif

//#if MC>=11700
//$$ import java.net.InetSocketAddress;
//#endif

import static gg.essential.mixins.ext.client.multiplayer.ServerDataExtKt.getExt;

@Mixin(ServerPinger.class)
public abstract class MixinServerPinger {

    //#if MC>=12005
    //$$ private static final String CONNECT = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/util/profiler/MultiValueDebugSampleLogImpl;)Lnet/minecraft/network/ClientConnection;";
    //#elseif MC>=12002
    //$$ private static final String CONNECT = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/util/profiler/PerformanceLog;)Lnet/minecraft/network/ClientConnection;";
    //#elseif MC>=11700
    //$$ private static final String CONNECT = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetSocketAddress;Z)Lnet/minecraft/network/ClientConnection;";
    //#else
    private static final String CONNECT = "Lnet/minecraft/network/NetworkManager;createNetworkManagerAndConnect(Ljava/net/InetAddress;IZ)Lnet/minecraft/network/NetworkManager;";
    //#endif

    @Inject(method = "ping", at = @At("HEAD"))
    private void reset(CallbackInfo ci, @Local(argsOnly = true) ServerData serverData) {
        getExt(serverData).setEssential$pingRegion(null);
    }

    @ModifyVariable(method = "ping", at = @At(value = "INVOKE", target = CONNECT), ordinal = 0, argsOnly = true)
    private ServerData setPingProxyTarget(ServerData serverData) {
        if (requiresProxy(serverData)) {
            ProxyPingServerKt.getTargetServerData().set(serverData);
        }
        return serverData;
    }

    @Inject(method = "ping", at = @At(value = "INVOKE", target = CONNECT, shift = At.Shift.AFTER))
    private void unsetPingProxyTarget(CallbackInfo ci) {
        ProxyPingServerKt.getTargetServerData().remove();
    }

    @Inject(method = "tryCompatibilityPing", at = @At("HEAD"), cancellable = true)
    private void abortIfUntrusted(
        //#if MC>=11700
        //$$ InetSocketAddress address,
        //#endif
        //#if MC>=12002
        //$$ ServerAddress serverAddress,
        //#endif
        ServerData serverData,
        CallbackInfo ci
    ) {
        if (requiresProxy(serverData)) {
            ci.cancel();
        }
    }

    private boolean requiresProxy(ServerData serverData) {
        return !getExt(serverData).getEssential$isTrusted() || SpsAddress.parse(serverData.serverIP) != null;
    }
}
