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
package gg.essential.mixins.transformers.feature.sound;

import com.google.common.collect.Multimap;
import gg.essential.mixins.impl.client.audio.ISoundExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import paulscode.sound.SoundSystem;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

//#if MC>=11200
//#else
//$$ import net.minecraft.client.audio.SoundPoolEntry;
//#endif

@Mixin(SoundManager.class)
public abstract class Mixin_UpdateWhilePaused {
    // SoundSystem makes it impossible to know if a sound really has already stopped, or if it play command is simply
    // still in the command queue..
    // MC works around this by assuming that any sound queued within the last second is hasn't stopped yet; it keeps
    // track of this via the playingSoundsStopTime map, however it does so in ticks, and those don't advance while the
    // game is paused, so they're no good for use in e.g. the Wardrobe.
    // This may serves a similar purpose but stores millis since epoch of the time it was queued instead.
    // Special value 0 indicates that it's been more than a second (so we can short-circuit some logic).
    // Using a weak map to make sure we don't leak memory because MC might clean up our sounds as well.
    @Unique private final Map<ISoundExt, Long> commandQueueTime = new WeakHashMap<>();

    // MC doesn't tick sounds when the game is paused, as such we can't cancel them or update their volume, both of
    // which we need to be able to do.
    // So instead of using MC's update method, we'll update our sounds from `setListener`, which is called every frame
    // right before rendering.
    @Group(name = "updateEssentialSounds", min = 1)
    @Inject(method = "setListener", at = @At("RETURN"))
    private void updateEssentialSounds(CallbackInfo ci) {
        if (!this.loaded) return;
        if (!Minecraft.getMinecraft().isGamePaused()) return;

        Iterator<ITickableSound> iterator = this.tickableSounds.iterator();
        while (iterator.hasNext()) {
            ITickableSound sound = iterator.next();
            if (!(sound instanceof ISoundExt)) {
                continue;
            }
            ISoundExt soundExt = (ISoundExt) sound;
            String id = this.invPlayingSounds.get(sound);

            sound.update();

            // Also have to clean up finished sounds while the game is paused, otherwise we'll relatively quickly run into
            // the channel limit
            boolean playing = this.soundSystem.playing(id);
            // Hack because SoundSystem makes it impossible to know if the sound really has already stopped, or the play
            // command is simply still in the queue..
            if (!playing) {
                Long queueTime = this.commandQueueTime.get(soundExt);
                if (queueTime == null) {
                    // First time we're seeing this sound, assume newly queued
                    this.commandQueueTime.put(soundExt, System.currentTimeMillis());
                } else if (queueTime == 0) {
                    // Known and been existing for a while, don't need to bother checking the current time
                } else {
                    // Fresh, check how old exactly
                    if (queueTime + 1000 < System.currentTimeMillis()) {
                        // still fresh, assume it's merely stuck in queue
                        playing = true;
                    } else {
                        // older than a second now, update stored value so we can short-circuit in the future
                        this.commandQueueTime.put(soundExt, 0L);
                    }
                }
            }

            if (playing && !sound.isDonePlaying()) {
                //#if MC>=11200
                this.soundSystem.setVolume(id, this.getClampedVolume(sound));
                this.soundSystem.setPitch(id, this.getClampedPitch(sound));
                //#else
                //$$ this.soundSystem.setVolume(id, this.getNormalizedVolume(sound, this.playingSoundPoolEntries.get(sound), soundExt.essential$createAccessor().getSoundCategory()));
                //$$ this.soundSystem.setPitch(id, this.getNormalizedPitch(sound, this.playingSoundPoolEntries.get(sound)));
                //#endif
                this.soundSystem.setPosition(id, sound.getXPosF(), sound.getYPosF(), sound.getZPosF());
            } else {
                this.stopSound(sound);

                // Also need to immediately clean it up, because MC will only do that while the game is not paused
                this.playingSounds.remove(id);
                this.soundSystem.removeSource(id);
                this.playingSoundsStopTime.remove(id);
                this.commandQueueTime.remove(sound);
                //#if MC<11200
                //$$ this.playingSoundPoolEntries.remove(sound);
                //#endif

                try {
                    //#if MC>=11200
                    this.categorySounds.remove(sound.getCategory(), id);
                    //#else
                    //$$ this.categorySounds.remove(soundExt.essential$createAccessor().getSoundCategory(), id);
                    //#endif
                } catch (RuntimeException e) { // idk, ask Mojang
                }

                iterator.remove();
            }
        }
    }

    //#if MC >= 1.12
    @Group(name = "updateEssentialSounds", min = 1)
    @Inject(method = "setListener(Lnet/minecraft/entity/Entity;F)V", at = @At("RETURN"), remap = false)
    @Dynamic("https://github.com/MinecraftForge/MinecraftForge/commit/6f642ba6ceb1978abdd5d63a5e4227f4cd1afa23")
    private void updateEssentialSounds(Entity player, float partialTicks, CallbackInfo ci) {
        updateEssentialSounds(ci);
    }
    //#endif

    // Hack to get access to `sndSystem` which has package private field type
    @Unique private SoundSystem soundSystem;
    @Inject(method = "access$102", at = @At("HEAD"))
    private static void captureSoundSystem(SoundManager self, @Coerce SoundSystem soundSystem, CallbackInfoReturnable<?> ci) {
        ((Mixin_UpdateWhilePaused) (Object) self).soundSystem = soundSystem;
    }

    @Shadow private boolean loaded;
    @Shadow @Final private Map<String, ISound> playingSounds;
    @Shadow @Final private Map<ISound, String> invPlayingSounds;
    @Shadow @Final private List<ITickableSound> tickableSounds;
    @Shadow @Final private Map<String, Integer> playingSoundsStopTime;
    @Shadow @Final private Multimap<SoundCategory, String> categorySounds;
    //#if MC>=11200
    //#else
    //$$ @Shadow private Map<ISound, SoundPoolEntry> playingSoundPoolEntries;
    //#endif

    @Shadow public abstract void stopSound(ISound sound);
    //#if MC>=11200
    @Shadow protected abstract float getClampedVolume(ISound soundIn);
    @Shadow protected abstract float getClampedPitch(ISound soundIn);
    //#else
    //$$ @Shadow protected abstract float getNormalizedVolume(ISound par1, SoundPoolEntry par2, SoundCategory par3);
    //$$ @Shadow protected abstract float getNormalizedPitch(ISound par1, SoundPoolEntry par2);
    //#endif
}
