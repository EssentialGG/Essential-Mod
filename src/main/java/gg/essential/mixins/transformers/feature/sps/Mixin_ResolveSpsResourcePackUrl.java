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
package gg.essential.mixins.transformers.feature.sps;

import gg.essential.Essential;
import gg.essential.network.connectionmanager.ice.IceManager;
import gg.essential.sps.SpsAddress;
import gg.essential.sps.quic.jvm.UtilKt;
import net.minecraft.util.HttpUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.net.MalformedURLException;
import java.net.URL;

@Mixin(HttpUtil.class)
public class Mixin_ResolveSpsResourcePackUrl {
    @ModifyVariable(method = "downloadResourcePack", at = @At("HEAD"), argsOnly = true)
    //#if MC<11900
    private static String resolveSpsUrl(String str) throws MalformedURLException {
        URL url = new URL(str);
        URL resolvedUrl = resolveSpsUrl(url);
        if (resolvedUrl == url) return str;
        return resolvedUrl.toString();
    }
    @Unique
    //#endif
    private static URL resolveSpsUrl(URL url) throws MalformedURLException {
        if (SpsAddress.parse(url.getHost()) == null) {
            return url;
        }

        IceManager iceManager = Essential.getInstance().getConnectionManager().getIceManager();
        Integer proxyHttpPort = iceManager.getProxyHttpPort();
        if (proxyHttpPort == null) {
            Essential.logger.warn("Received resource pack url with SPS target but http proxy is not available: {}", url);
            return url;
        }

        return new URL(url.getProtocol(), UtilKt.getLOCALHOST().getHostAddress(), proxyHttpPort, url.getFile());
    }
}
