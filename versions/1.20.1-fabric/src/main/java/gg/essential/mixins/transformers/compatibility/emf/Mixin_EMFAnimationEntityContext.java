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
package gg.essential.mixins.transformers.compatibility.emf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Pseudo
@Mixin(targets = "traben.entity_model_features.models.animation.EMFAnimationEntityContext")
public class Mixin_EMFAnimationEntityContext {

    @WrapOperation(method = "Ltraben/entity_model_features/models/animation/EMFAnimationEntityContext;isEntityAnimPaused()Z", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectSet;contains(Ljava/lang/Object;)Z"), remap = false)
    private static boolean pauseAnimationsWhenEmoting(ObjectSet<?> instance, Object uuid, Operation<Boolean> original) {
        return original.call(instance, uuid) ||
                // this method may still be run for entities when MinecraftClient.getInstance().world == null depending on the behaviour of other mods
                // specifically this gets run for special inventory chest entities that meet this condition in the Quark mod's "q" menu
                (MinecraftClient.getInstance().world != null && essential$isEntityEmoting(MinecraftClient.getInstance().world.getPlayerByUuid((UUID) uuid)));
    }

    @Unique
    private static boolean essential$isEntityEmoting(Entity entity) {
        if (entity == null) return false;
        if (!(entity instanceof AbstractClientPlayerEntity)) return false;
        AbstractClientPlayerExt abstractClientPlayerEntity = (AbstractClientPlayerExt) entity;
        return abstractClientPlayerEntity.isPoseModified();
    }
}
