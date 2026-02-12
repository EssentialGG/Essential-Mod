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
package gg.essential.network.connectionmanager.handler.cosmetics;

import gg.essential.connectionmanager.common.packet.wardrobe.ServerWardrobeSettingsPacket;
import gg.essential.model.EssentialAsset;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.cosmetics.CosmeticsManager;
import gg.essential.network.connectionmanager.cosmetics.WardrobeSettings;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import gg.essential.network.cosmetics.ConversionsKt;
import org.jetbrains.annotations.NotNull;

public class ServerWardrobeSettingsPacketHandler extends PacketHandler<ServerWardrobeSettingsPacket> {

    @Override
    protected void onHandle(@NotNull final ConnectionManager connectionManager, @NotNull final ServerWardrobeSettingsPacket packet) {
        CosmeticsManager cosmeticsManager = connectionManager.getCosmeticsManager();
        EssentialAsset currentFeaturedPageConfig = packet.getCurrentFeaturedPageConfig();
        if (currentFeaturedPageConfig != null) {
            cosmeticsManager.getInfraCosmeticsData().addFeaturedPageCollection(ConversionsKt.toMod(currentFeaturedPageConfig));
        }
        cosmeticsManager.getInfraCosmeticsData().addFeaturedPageCollection(ConversionsKt.toMod(packet.getFallbackFeaturedPageConfig()));
        WardrobeSettings settings = cosmeticsManager.getWardrobeSettings();
        settings.populateOutfitsLimit(packet.getOutfitsLimit());
        settings.populateSkinsLimit(packet.getSkinsLimit());
        settings.populateGiftingCoinSpendRequirement(packet.getGiftingCoinSpendRequirement());
        settings.populateYouNeedMinimumAmount(packet.getYouNeedMinimumAmount());
    }

}
