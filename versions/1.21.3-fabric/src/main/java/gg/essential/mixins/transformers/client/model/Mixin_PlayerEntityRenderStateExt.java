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
package gg.essential.mixins.transformers.client.model;

import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public abstract class Mixin_PlayerEntityRenderStateExt implements PlayerEntityRenderStateExt {
    @Unique
    private final CosmeticsRenderState.Snapshot cosmeticsRenderState = new CosmeticsRenderState.Snapshot();

    @Override
    public CosmeticsRenderState.Snapshot essential$getCosmetics() {
        return cosmeticsRenderState;
    }
}
