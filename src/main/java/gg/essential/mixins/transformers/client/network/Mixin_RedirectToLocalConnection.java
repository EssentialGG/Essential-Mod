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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.essential.Essential;
import gg.essential.network.connectionmanager.ice.IceManager;
import gg.essential.network.pingproxy.ProxyPingServer;
import gg.essential.network.pingproxy.ProxyPingServerKt;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalChannel;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.Dispatchers;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.UUID;

import static gg.essential.network.connectionmanager.ice.IceEventLoopGroups.ICE_CLIENT_EVENT_LOOP_GROUP;
import static gg.essential.network.connectionmanager.ice.util.IWishMixinAllowedForPublicStaticFields.connectTarget;

//#if MC>=12001
//$$ import net.minecraft.network.ClientConnection;
//#endif

/**
 * Used to redirect netty connections to a LocalChannel and LocalSocketAddress, either for ICE or the ping proxy.
 * @see gg.essential.mixins.transformers.feature.ice.client.Mixin_IceAddressResolving_Connect
 * @see gg.essential.mixins.transformers.feature.ice.client.Mixin_IceAddressResolving_Ping
 * @see MixinServerPinger
 */
@Mixin(NetworkManager.class)
public abstract class Mixin_RedirectToLocalConnection {
    //#if MC>=12001
    //$$ private static final String CONNECT = "connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;";
    //#else
    private static final String CONNECT = "createNetworkManagerAndConnect";
    //#endif

    @ModifyArg(method = CONNECT, at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/AbstractBootstrap;", remap = false))
    private static EventLoopGroup useCustomEventLoopGroup(EventLoopGroup group) {
        if (connectTarget.get() != null) {
            group = ICE_CLIENT_EVENT_LOOP_GROUP.getValue();
        }
        if (ProxyPingServerKt.getTargetServerData().get() != null) {
            group = ProxyPingServerKt.getClientGroup();
        }
        return group;
    }

    @ModifyArg(method = CONNECT, at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;", remap = false))
    private static Class<? extends Channel> injectLocalChannel(Class<? extends Channel> channel) {
        if (connectTarget.get() != null || ProxyPingServerKt.getTargetServerData().get() != null) {
            channel = LocalChannel.class;
        }
        return channel;
    }

    @WrapOperation(method = CONNECT, at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;connect(Ljava/net/InetAddress;I)Lio/netty/channel/ChannelFuture;", remap = false))
    private static ChannelFuture injectLocalChannel(Bootstrap bootstrap, InetAddress address, int port, Operation<ChannelFuture> original) throws IOException {
        ServerData serverData = ProxyPingServerKt.getTargetServerData().get();
        if (serverData != null) {
            // PingProxy connection
            ProxyPingServer server = new ProxyPingServer(serverData);
            return bootstrap.connect(server.getAddress());
        }

        UUID user = connectTarget.get();
        if (user != null) {
            // ICE connection
            IceManager iceManager = Essential.getInstance().getConnectionManager().getIceManager();
            Channel channel = bootstrap.register().syncUninterruptibly().channel();
            ChannelPromise connectPromise = channel.newPromise();
            Dispatchers.getIO().dispatch(EmptyCoroutineContext.INSTANCE, () -> {
                try {
                    SocketAddress iceAddress = iceManager.createClientAgent(user);
                    channel.eventLoop().execute(() -> channel.connect(iceAddress, connectPromise));
                } catch (Throwable t) {
                    connectPromise.setFailure(t);
                }
            });
            return connectPromise;
        }

        // regular connection
        return original.call(bootstrap, address, port);
    }
}
