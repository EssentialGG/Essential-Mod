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

import gg.essential.sps.SpsAddress;
import net.minecraft.client.network.ServerPinger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static gg.essential.network.connectionmanager.ice.util.IWishMixinAllowedForPublicStaticFields.connectTarget;

//#if MC>=11700
//$$ import net.minecraft.client.network.ServerAddress;
//#endif

@Mixin(ServerPinger.class)
public abstract class Mixin_IceAddressResolving_Ping {

    //#if MC>=11700
    //$$ @ModifyArg(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AllowedAddressResolver;resolve(Lnet/minecraft/client/network/ServerAddress;)Ljava/util/Optional;"))
    //$$ private ServerAddress setIceTarget(ServerAddress address) {
    //$$     String host = setIceTarget(address.getAddress());
    //$$     if (!host.equals(address.getAddress())) {
    //$$         address = new ServerAddress(host, address.getPort());
    //$$     }
    //$$     return address;
    //$$ }
    //#else
    @ModifyArg(method = "ping", at = @At(value = "INVOKE", target = "Ljava/net/InetAddress;getByName(Ljava/lang/String;)Ljava/net/InetAddress;"))
    //#endif
    private String setIceTarget(String address) {
        SpsAddress spsAddress = SpsAddress.parse(address);
        if (spsAddress != null) {
            connectTarget.set(spsAddress.getHost());
            address = "127.0.0.1"; // just needs to resolve
        }
        return address;
    }

    @Inject(method = "ping", at = @At("RETURN"))
    private void unsetIceTarget(CallbackInfo ci) {
        connectTarget.remove();
    }
}
