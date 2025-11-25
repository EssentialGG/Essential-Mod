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

import gg.essential.cosmetics.WearablesManager;
import gg.essential.gui.common.EmulatedUI3DPlayer;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.handlers.EssentialSoundManager;
import gg.essential.mixins.ext.client.ParticleSystemHolder;
import gg.essential.model.ModelAnimationState;
import gg.essential.model.ParticleSystem;
import kotlin.Unit;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.world.World;

import static gg.essential.gui.elementa.state.v2.StateKt.stateOf;

public class CosmeticEventDispatcher {
    public static void dispatchEvents(AbstractClientPlayer player, WearablesManager wearablesManager) {
        //#if MC>=12106
        //$$ World world = player.getWorld();
        //#else
        World world = player.getEntityWorld();
        //#endif
        ParticleSystem particleSystem;
        if (player instanceof EmulatedUI3DPlayer.EmulatedPlayer) {
            particleSystem = ((EmulatedUI3DPlayer.EmulatedPlayer) player).getParticleSystem();
        } else if (world instanceof ParticleSystemHolder) {
            particleSystem = ((ParticleSystemHolder) world).getParticleSystem();
        } else {
            particleSystem = null;
        }
        wearablesManager.collectEvents(event -> {
            if (event instanceof ModelAnimationState.ParticleEvent) {
                if (particleSystem != null) {
                    particleSystem.spawn((ModelAnimationState.ParticleEvent) event);
                }
            } else if (event instanceof ModelAnimationState.SoundEvent) {
                boolean forceGlobal;
                State<Float> volume;
                boolean enforceEmoteSoundSettings;
                if (player instanceof EmulatedUI3DPlayer.EmulatedPlayer) {
                    EmulatedUI3DPlayer component = ((EmulatedUI3DPlayer.EmulatedPlayer) player).getEmulatedUI3DPlayer();
                    if (!component.getSounds().getUntracked()) {
                        return Unit.INSTANCE;
                    }
                    forceGlobal = true;
                    volume = component.getSoundsVolume();
                    enforceEmoteSoundSettings = false;
                } else {
                    forceGlobal = false;
                    volume = stateOf(1f);
                    enforceEmoteSoundSettings = true;
                }
                EssentialSoundManager.INSTANCE.playSound((ModelAnimationState.SoundEvent) event, forceGlobal, volume, enforceEmoteSoundSettings);
            }
            return Unit.INSTANCE;
        });
    }
}
