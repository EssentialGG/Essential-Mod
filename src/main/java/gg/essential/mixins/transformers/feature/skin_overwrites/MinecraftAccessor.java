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
package gg.essential.mixins.transformers.feature.skin_overwrites;

//#if MC>=12002
//$$ import com.mojang.authlib.yggdrasil.ProfileResult;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.gen.Accessor;
//$$
//$$ import java.util.concurrent.CompletableFuture;
//$$
//$$ @Mixin(MinecraftClient.class)
//$$ public interface MinecraftAccessor {
//$$     @Accessor
//$$     void setGameProfileFuture(CompletableFuture<ProfileResult> future);
//$$ }
//#else
@org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
public interface MinecraftAccessor {
}
//#endif
