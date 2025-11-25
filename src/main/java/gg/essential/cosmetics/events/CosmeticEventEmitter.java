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
package gg.essential.cosmetics.events;

import gg.essential.cosmetics.IngameEquippedOutfitsManager;
import gg.essential.cosmetics.IngameEquippedOutfitsUpdateDispatcher;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.model.ModelInstance;
import gg.essential.network.cosmetics.Cosmetic;
import kotlin.Pair;
import me.kbrewster.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import gg.essential.event.entity.PlayerTickEvent;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class CosmeticEventEmitter {

    public CosmeticEventEmitter() {

    }

    public void triggerEvent(UUID playerUuid, CosmeticSlot slot, String event) {
        IngameEquippedOutfitsUpdateDispatcher.Companion.sendUpdates(Collections.singletonList(new Pair<>(
            playerUuid,
            Collections.singletonList(new IngameEquippedOutfitsManager.Update.AnimationEvent(slot, event))
        )));
    }

    public static void doTriggerEvent(UUID playerUuid, CosmeticSlot slot, String event) {
        WorldClient world = Minecraft.getMinecraft().world;
        if (world == null) {
            return;
        }

        AbstractClientPlayerExt player = findPlayerByCosmeticsSourceUuid(world, playerUuid);
        if (player == null) {
            return;
        }

        if (event.equals("reset")) {
            player.getWearablesManager().resetModel(slot);
            return;
        }

        final Map<Cosmetic, ModelInstance> essentialCosmeticModels =
            player.getWearablesManager().getModels();
        for (ModelInstance value : essentialCosmeticModels.values()) {
            if (slot == value.getCosmetic().getType().getSlot()) {
                value.getEssentialAnimationSystem().fireTriggerFromAnimation(event, null);
            }
        }
    }

    private static AbstractClientPlayerExt findPlayerByCosmeticsSourceUuid(WorldClient world, UUID uuid) {
        //#if MC>=11400
        //$$ for (PlayerEntity player : world.getPlayers()) {
        //#else
        for (EntityPlayer player : world.playerEntities) {
        //#endif
            if (player instanceof AbstractClientPlayerExt) {
                AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) player;
                if (playerExt.getCosmeticsSourceUuid().equals(uuid)) {
                    return playerExt;
                }
            }
        }
        return null;
    }

    public void fireEvent(AbstractClientPlayer player, AnimationEventType type) {
        final Map<Cosmetic, ModelInstance> essentialCosmeticModels =
            ((AbstractClientPlayerExt) player).getWearablesManager().getModels();
        for (ModelInstance value : essentialCosmeticModels.values()) {
            value.getEssentialAnimationSystem().processEvent(type);
        }
    }


    @Subscribe
    public void tick(PlayerTickEvent tickEvent) {
        if (tickEvent.isPre()) return;

        EntityPlayer playerEntity = tickEvent.getPlayer();
        if (!(playerEntity instanceof AbstractClientPlayer)) {
            return;
        }

        fireEvent((AbstractClientPlayer) playerEntity, AnimationEventType.TICK);
    }
}
