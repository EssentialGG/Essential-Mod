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

import gg.essential.Essential;
import gg.essential.event.network.chat.SendChatMessageEvent;
import gg.essential.event.network.chat.SendCommandEvent;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12002
//$$ import net.minecraft.client.network.ClientCommonNetworkHandler;
//#else
import net.minecraft.client.network.NetHandlerPlayClient;
//#endif

//#if MC>=11900
//$$ import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
//#endif

//#if MC>=12002
//$$ @Mixin(ClientCommonNetworkHandler.class)
//#else
@Mixin(NetHandlerPlayClient.class)
//#endif
public abstract class Mixin_SendChatEvents {
    // FIXME `sendPacket` doesn't remap automatically any more
    //#if NEOFORGE && MC>=12006
    //$$ @Inject(method = "send", at = @At("HEAD"), cancellable = true)
    //#else
    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    //#endif
    public void chat(Packet<?> packetIn, CallbackInfo ci) {
        if (packetIn instanceof CPacketChatMessage) {
            //#if MC>=11901
            //$$ String orgMessage = ((ChatMessageC2SPacket) packetIn).chatMessage();
            //#else
            String orgMessage = ((CPacketChatMessage) packetIn).getMessage();
            //#endif
            String newMessage = emitSendChatMessageEvent(orgMessage);
            if (newMessage != null) {
                ((CPacketChatMessageAccessor) packetIn).setMessage(newMessage);
            } else {
                ci.cancel();
            }
        }
        //#if MC>=11900
        //$$ if (packetIn instanceof CommandExecutionC2SPacket packet) {
        //$$     SendCommandEvent event = new SendCommandEvent(packet.command());
        //$$     Essential.EVENT_BUS.post(event);
        //$$     if (event.isCancelled()) {
        //$$         ci.cancel();
        //$$     }
        //$$ }
        //#endif
    }

    @Unique
    private String emitSendChatMessageEvent(String message) {
        if (message.startsWith("/")) {
            SendCommandEvent event = new SendCommandEvent(message.substring(1));
            Essential.EVENT_BUS.post(event);
            return event.isCancelled() ? null : "/" + event.getCommandLine();
        } else {
            SendChatMessageEvent event = new SendChatMessageEvent(message);
            Essential.EVENT_BUS.post(event);
            return event.isCancelled() ? null : event.getMessage();
        }
    }
}
