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
package gg.essential.mixins.transformers.cosmetics.skinmask;

import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerEntityRenderer.class)
public abstract class Mixin_ApplyToHandRenderer {
    @ModifyVariable(method = "renderArm", at = @At("HEAD"), argsOnly = true)
    private Identifier applyCosmeticsSkinMaskToSleeve(Identifier skin) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return skin;
        return ((AbstractClientPlayerExt) player).applyEssentialCosmeticsMask(skin);
    }
}
