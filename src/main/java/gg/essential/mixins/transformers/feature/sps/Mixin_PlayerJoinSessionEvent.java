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

import com.mojang.authlib.GameProfile;
import gg.essential.Essential;
import gg.essential.event.sps.PlayerJoinSessionEvent;
import gg.essential.mixins.ext.server.integrated.IntegratedServerExt;
import gg.essential.network.connectionmanager.sps.SPSManager;
import gg.essential.util.ExtensionsKt;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import static gg.essential.gui.elementa.state.v2.ListKt.add;

//#if MC>=12002
//$$ import net.minecraft.server.network.ConnectedClientData;
//#endif

//#if MC<=11202
import net.minecraft.network.NetHandlerPlayServer;
//#endif

@Mixin(PlayerList.class)
public class Mixin_PlayerJoinSessionEvent {

    @Inject(
        method = "initializeConnectionToPlayer",
        at = @At("RETURN")
        //#if MC<=11202
        , remap = false
        //#endif
    )
    private void essential$playerJoinedSession(
        NetworkManager netManager,
        EntityPlayerMP player,
        //#if MC>=12002
        //$$ ConnectedClientData clientData,
        //#elseif MC<=11202
        NetHandlerPlayServer nethandlerplayserver,
        //#endif
        CallbackInfo info) {

        MinecraftServer server = player.mcServer;
        UUID uuid = player.getUniqueID();
        ExtensionsKt.getExecutor(Minecraft.getMinecraft()).execute(() -> {
            if (server instanceof IntegratedServerExt) {
                add(((IntegratedServerExt) server).getEssential$manager().getConnectedPlayers(), uuid);
            }
        });

        final SPSManager spsManager = Essential.getInstance().getConnectionManager().getSpsManager();
        if (spsManager.getLocalSession() != null) {
            GameProfile gameProfile = player.getGameProfile();
            ExtensionsKt.getExecutor(Minecraft.getMinecraft()).execute(() -> Essential.EVENT_BUS.post(new PlayerJoinSessionEvent(gameProfile)));
        }
    }
}