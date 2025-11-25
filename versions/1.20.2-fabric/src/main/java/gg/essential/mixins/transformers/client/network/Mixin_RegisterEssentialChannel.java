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

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;

//#if FORGE
//$$ import io.netty.buffer.Unpooled;
//$$ import net.minecraft.network.protocol.common.custom.DiscardedPayload;
//#endif

@Mixin(ClientPlayNetworkHandler.class)
public abstract class Mixin_RegisterEssentialChannel extends ClientCommonNetworkHandler {
    protected Mixin_RegisterEssentialChannel() { super(null, null, null); }

    //#if MC>=12006
    //$$ // Handled by EssentialChannelHandler
    //#else
    // FIXME preprocessor bug: doesn't search interfaces for mappings when mapping inject target
    //#if FABRIC
    @Inject(method = "onGameJoin", at = @At("RETURN"))
    //#else
    //$$ @Inject(method = "handleLogin", at = @At("RETURN"))
    //#endif
    private void onJoinGame(GameJoinS2CPacket packetIn, CallbackInfo ci) {
        //#if FABRIC
        sendPacket(new CustomPayloadC2SPacket(new CustomPayload() {
            @Override
            public void write(PacketByteBuf buf) {
                buf.writeBytes("essential:".getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public Identifier id() {
                return new Identifier("register");
            }
        }));
        //#elseif NEOFORGE
        //$$ // NeoForge 20.4.234 rejects this. Given 1.20.6+ works differently, we'll just not bother with this one.
        //#else
        //$$ FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        //$$ buf.writeBytes("essential:".getBytes(StandardCharsets.UTF_8));
        //$$ send(new ServerboundCustomPayloadPacket(new DiscardedPayload(new ResourceLocation("register"), buf)));
        //#endif
    }
    //#endif
}
