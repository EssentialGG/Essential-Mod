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

import gg.essential.util.HelpersKt;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

//#if FABRIC
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//#elseif NEOFORGE
//$$ import gg.essential.Essential;
//$$ import net.neoforged.bus.api.SubscribeEvent;
//$$ import net.neoforged.fml.common.EventBusSubscriber;
//$$ import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
//#if MC>=12106
//$$ import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
//#endif
//#else
//$$ import net.minecraftforge.network.ChannelBuilder;
//$$ import net.minecraftforge.network.SimpleChannel;
//#endif

//#if NEOFORGE
//#if MC>=12106
//$$ @EventBusSubscriber(modid = Essential.MODID)
//#else
//$$ @EventBusSubscriber(modid = Essential.MODID, bus = EventBusSubscriber.Bus.MOD)
//#endif
//#endif
public class EssentialChannelHandler {
    private static final Identifier CHANNEL = HelpersKt.identifier("essential:");

    public static void registerEssentialChannel() {
        //#if FABRIC
        // FAPI requires us to register a S2C packet type, since when the client sends a minecraft:register packet,
        // it is declaring what channels it can receive packets on. See ClientPlayNetworking class javadoc.
        PayloadTypeRegistry.playS2C().register(Payload.ID, Payload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(Payload.ID, (packet, ctx) -> {});
        //#elseif NEOFORGE
        //$$ // Handled by event subscription below
        //#else
        //$$ ChannelBuilder.named(CHANNEL)
        //$$     .networkProtocolVersion(0)
        //$$     .acceptedVersions((__, ___) -> true)
        //$$     .simpleChannel();
        //#endif
    }

    //#if NEOFORGE
    //$$ @SubscribeEvent
    //$$ public static void registerWithNeoForge(RegisterPayloadHandlersEvent event) {
    //$$     event.registrar("1").optional().playBidirectional(Payload.ID, Payload.CODEC, (__, ___) -> {});
    //$$ }
    //#if MC>=12106
    //$$ @SubscribeEvent
    //$$ public static void registerWithNeoForgeClient(RegisterClientPayloadHandlersEvent event) {
    //$$     event.register(Payload.ID, (__, ___) -> {});
    //$$ }
    //#endif
    //#endif

    private static class Payload implements CustomPayload {
        private static final CustomPayload.Id<Payload> ID = new Id<>(CHANNEL);
        private static final PacketCodec<PacketByteBuf, Payload> CODEC = PacketCodec.of(
            (value, buf) -> { throw new IllegalStateException("Should not be reached"); },
            buf -> { throw new IllegalStateException("Should not be reached"); }
        );

        @Override
        public Id<? extends net.minecraft.network.packet.CustomPayload> getId() {
            return ID;
        }
    }
}
