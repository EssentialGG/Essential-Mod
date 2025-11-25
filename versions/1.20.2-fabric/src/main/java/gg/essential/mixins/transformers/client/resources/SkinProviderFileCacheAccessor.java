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

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;

//#if MC>=12109
//$$ import net.minecraft.util.AssetInfo;
//#else
import net.minecraft.util.Identifier;
//#endif

@Mixin(targets = "net.minecraft.client.texture.PlayerSkinProvider$FileCache")
public interface SkinProviderFileCacheAccessor {
    @Accessor
    MinecraftProfileTexture.Type getType();
    @Invoker
    //#if MC>=12109
    //$$ CompletableFuture<AssetInfo.TextureAsset> invokeGet(MinecraftProfileTexture texture);
    //#else
    CompletableFuture<Identifier> invokeGet(MinecraftProfileTexture texture);
    //#endif
}
