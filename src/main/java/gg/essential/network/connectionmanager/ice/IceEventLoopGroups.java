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
package gg.essential.network.connectionmanager.ice;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.EventLoopGroup;
import kotlin.Lazy;
import kotlin.LazyKt;

import java.util.concurrent.ThreadFactory;

//#if FORGE && MC>=11200
//#if MC>=11700
//$$ import net.minecraftforge.fml.util.thread.SidedThreadGroups;
//#else
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
//#endif
//#endif

public class IceEventLoopGroups {
    private static EventLoopGroup makeIceEventLoopGroup(boolean server) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
            // Note: For Forge 1.8 this needs to start with "Netty Server IO" (see FMLCommonHandler.getEffectiveSide)
            .setNameFormat("Netty " + (server ? "Server" : "Client") + " IO ICE #%d")
            .setDaemon(true)
            //#if FORGE && MC>=11200
            .setThreadFactory(server ? SidedThreadGroups.SERVER : SidedThreadGroups.CLIENT)
            //#endif
            .build();
        //#if MC>=11200
        return new io.netty.channel.DefaultEventLoopGroup(0, threadFactory);
        //#else
        //$$ return new io.netty.channel.local.LocalEventLoopGroup(0, threadFactory);
        //#endif
    }

    public static final Lazy<EventLoopGroup> ICE_SERVER_EVENT_LOOP_GROUP = LazyKt.lazy(() -> makeIceEventLoopGroup(true));
    public static final Lazy<EventLoopGroup> ICE_CLIENT_EVENT_LOOP_GROUP = LazyKt.lazy(() -> makeIceEventLoopGroup(false));
}
