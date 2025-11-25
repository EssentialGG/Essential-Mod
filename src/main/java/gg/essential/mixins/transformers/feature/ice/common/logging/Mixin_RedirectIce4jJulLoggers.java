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
package gg.essential.mixins.transformers.feature.ice.common.logging;

import gg.essential.network.connectionmanager.ice.Log4jAsJulLogger;
import org.ice4j.pseudotcp.PseudoTCPBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.logging.Logger;

@Mixin(value = {
    PseudoTCPBase.class,
}, targets = {
    "org.ice4j.pseudotcp.PseudoTcpSocketImpl",
}, remap = false)
public class Mixin_RedirectIce4jJulLoggers {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Ljava/util/logging/Logger;getLogger(Ljava/lang/String;)Ljava/util/logging/Logger;"))
    private static Logger logAdapter(String name) {
        return new Log4jAsJulLogger(name);
    }
}
