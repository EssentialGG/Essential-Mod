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
package gg.essential.mixins.transformers.client.resources;

import net.minecraft.client.texture.PlayerSkinProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.time.Duration;

/**
 * If a skin is already loaded, call the skin available callback immediately
 * to prevent the default skin from showing for a frame.
 */
@Mixin(PlayerSkinProvider.class)
public class Mixin_InstantSkinLoad {
    // With 1.20.2, MC now has a built-in cache. However for unknown reason it sets its expiry time to only 15 seconds
    // after access (and it's only queried when a player is created, not every frame it's rendered!), despite never
    // expiring any of the textures that are loaded in the process. The only thing it really saves with that strict
    // expiry time is the SkinTextures and a wrapper CompletableFuture.
    // As such, there should not be much harm in substantially increasing this value to prevent already loaded skins
    // from taking a frame (or more) to actually show up on newly created (UI3D)players.
    //#if MC>12109
    //$$ TODO verify above is still the case / if this is still necessary
    //#endif
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/cache/CacheBuilder;expireAfterAccess(Ljava/time/Duration;)Lcom/google/common/cache/CacheBuilder;"))
    private Duration extendCacheDuration(Duration duration) {
        Duration extended = Duration.ofMinutes(5);
        return duration.compareTo(extended) >= 0 ? duration : extended;
    }
}
