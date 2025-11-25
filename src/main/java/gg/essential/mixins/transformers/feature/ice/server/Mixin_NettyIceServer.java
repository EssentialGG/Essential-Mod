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
package gg.essential.mixins.transformers.feature.ice.server;

import gg.essential.mixins.ext.network.NetworkSystemExt;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalServerChannel;
import net.minecraft.network.NetworkSystem;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.net.SocketAddress;
import java.util.List;

import static gg.essential.network.connectionmanager.ice.IceEventLoopGroups.ICE_SERVER_EVENT_LOOP_GROUP;

@Mixin(NetworkSystem.class)
public abstract class Mixin_NettyIceServer implements NetworkSystemExt {

    @Shadow
    @Final
    private List<ChannelFuture> endpoints;

    @Unique
    private ChannelHandler networkedChannelInitializer;

    @Unique
    private ChannelFuture iceEndpoint;

    // We want to re-use the same channel initializer for ICE as we use for regular LAN.
    // But since that initializer is an anonymous inner class, the easiest way to get hold if it is by just capturing
    // an object when it is constructed for LAN. This necessitates that we open to LAN before ICE but we need to anyway
    // so the integrated server behaves like a LAN server (e.g. doesn't pause when in the menu).
    @ModifyArg(method = "addLanEndpoint", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/ServerBootstrap;childHandler(Lio/netty/channel/ChannelHandler;)Lio/netty/bootstrap/ServerBootstrap;", remap = false))
    private ChannelHandler captureNetworkedChannelInitializer(ChannelHandler initializer) {
        this.networkedChannelInitializer = initializer;
        return initializer;
    }

    @NotNull
    @Override
    public SocketAddress essential$getIceEndpoint() {
        //noinspection SynchronizeOnNonFinalField
        synchronized (this.endpoints) {
            if (this.iceEndpoint == null || !this.iceEndpoint.channel().isActive()) {
                ChannelFuture channelFuture = new ServerBootstrap()
                    .channel(LocalServerChannel.class)
                    .childHandler(this.networkedChannelInitializer)
                    .group(ICE_SERVER_EVENT_LOOP_GROUP.getValue())
                    .localAddress(LocalAddress.ANY)
                    .bind()
                    .syncUninterruptibly();
                this.endpoints.add(channelFuture);
                this.iceEndpoint = channelFuture;
            }
            return this.iceEndpoint.channel().localAddress();
        }
    }
}
