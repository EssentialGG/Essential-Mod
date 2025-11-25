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
package gg.essential.mixins.transformers.compatibility.vanilla;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// On 1.8.9, sneaking entity's nametags are rendered using a copy-paste of the nametag rendering code in another class :)
@Mixin(RendererLivingEntity.class)
public abstract class Mixin_FixSelfieNameplateOrientation_Sneaking<T extends EntityLivingBase> extends Render<T> {
    @ModifyExpressionValue(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/RenderManager;playerViewX:F", opcode = Opcodes.GETFIELD))
    private float essential$translateNameplate(float playerViewX) {
        if (this.renderManager.options.thirdPersonView == 2) {
            playerViewX *= -1;
        }
        return playerViewX;
    }

    protected Mixin_FixSelfieNameplateOrientation_Sneaking() { super(null); }
}
