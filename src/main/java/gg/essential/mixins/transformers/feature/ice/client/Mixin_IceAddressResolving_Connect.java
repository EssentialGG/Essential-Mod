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
package gg.essential.mixins.transformers.feature.ice.client;

import gg.essential.Essential;
import gg.essential.sps.SpsAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import static gg.essential.network.connectionmanager.ice.util.IWishMixinAllowedForPublicStaticFields.connectTarget;

//#if MC>=11700
//$$ import net.minecraft.client.network.ServerAddress;
//#endif

@Mixin(targets = "net.minecraft.client.multiplayer.GuiConnecting$1")
public abstract class Mixin_IceAddressResolving_Connect {

    //#if MC>=11700
    //$$ @ModifyArg(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AllowedAddressResolver;resolve(Lnet/minecraft/client/network/ServerAddress;)Ljava/util/Optional;", remap = true), remap = false)
    //$$ private ServerAddress setIceTarget(ServerAddress address) throws Exception {
    //$$     String host = setIceTarget(address.getAddress());
    //$$     if (!host.equals(address.getAddress())) {
    //$$         address = new ServerAddress(host, address.getPort());
    //$$     }
    //$$     return address;
    //$$ }
    //#else
    @ModifyArg(method = "run", at = @At(value = "INVOKE", target = "Ljava/net/InetAddress;getByName(Ljava/lang/String;)Ljava/net/InetAddress;"), remap = false)
    //#endif
    private String setIceTarget(String address) throws Exception {
        Essential essential = Essential.getInstance();
        UUID user;
        SpsAddress spsAddress = SpsAddress.parse(address);
        user = spsAddress != null ? spsAddress.getHost() : null;
        if (user != null) {
            connectTarget.set(user);
            address = "127.0.0.1"; // just needs to resolve
        }
        return address;
    }

    @Inject(method = "run", at = @At("RETURN"), remap = false)
    private void unsetIceTarget(CallbackInfo ci) {
        connectTarget.remove();
    }
}
