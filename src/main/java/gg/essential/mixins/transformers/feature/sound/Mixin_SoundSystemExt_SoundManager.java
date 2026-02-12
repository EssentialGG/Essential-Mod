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

import dev.folomeev.kotgl.matrix.vectors.Vec3;
import gg.essential.mixins.impl.client.audio.SoundSystemExt;
import gg.essential.model.util.Quaternion;
import net.minecraft.client.audio.SoundManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.folomeev.kotgl.matrix.vectors.Vectors.vec3;
import static dev.folomeev.kotgl.matrix.vectors.Vectors.vecUnitY;

//#if MC>=11600
//$$ import net.minecraft.client.renderer.ActiveRenderInfo;
//#else
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
//#endif

@Mixin(SoundManager.class)
public abstract class Mixin_SoundSystemExt_SoundManager implements SoundSystemExt {
    @Unique
    private Vec3 listenerPosition;

    @Unique
    private Quaternion listenerRotation;

    @Nullable
    @Override
    public Vec3 essential$getListenerPosition() {
        return listenerPosition;
    }

    @Nullable
    @Override
    public Quaternion essential$getListenerRotation() {
        return listenerRotation;
    }

    //#if MC>=11600
    //$$ @Inject(method = "updateListener", at = @At("HEAD"))
    //$$ private void recordListenerPosition(ActiveRenderInfo info, CallbackInfo ci) {
    //$$     if (!info.isValid()) return;
    //$$
    //$$     net.minecraft.util.math.vector.Vector3d vec = info.getProjectedView();
    //$$     this.listenerPosition = vec3((float) vec.x, (float) vec.y, (float) vec.z);
    //$$
    //$$     net.minecraft.util.math.vector.Quaternion cameraRotMc = info.getRotation();
        //#if MC>=12100
        //$$ this.listenerRotation = new Quaternion(cameraRotMc.x, cameraRotMc.y, cameraRotMc.z, cameraRotMc.w);
        //#elseif MC>=11903
        //$$ this.listenerRotation = new Quaternion(cameraRotMc.x, cameraRotMc.y, cameraRotMc.z, cameraRotMc.w).opposite();
        //#else
        //$$ this.listenerRotation = new Quaternion(cameraRotMc.getX(), cameraRotMc.getY(), cameraRotMc.getZ(), cameraRotMc.getW()).opposite();
        //#endif
    //$$ }
    //#else
    @Group(name = "setListener", min = 1)
    @Inject(method = "setListener(Lnet/minecraft/entity/player/EntityPlayer;F)V", at = @At("HEAD"))
    private void recordListenerPosition(EntityPlayer player, float partialTicks, CallbackInfo ci) {
    //#if MC >= 1.12
        recordListenerPosition((Entity) player, partialTicks, ci);
    }
    @Group(name = "setListener", min = 1)
    @Inject(method = "setListener(Lnet/minecraft/entity/Entity;F)V", at = @At("HEAD"), remap = false)
    @Dynamic("https://github.com/MinecraftForge/MinecraftForge/commit/6f642ba6ceb1978abdd5d63a5e4227f4cd1afa23")
    private void recordListenerPosition(Entity player, float partialTicks, CallbackInfo ci) {
    //#endif
        if (player == null) return;

        double x = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double y = player.prevPosY + (player.posY - player.prevPosY) * partialTicks + player.getEyeHeight();
        double z = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
        this.listenerPosition = vec3((float) x, (float) y, (float) z);

        net.minecraft.util.math.Vec3d lookAtMc = player.getLook(partialTicks);
        Vec3 lookAt = vec3((float) lookAtMc.x, (float) lookAtMc.y, (float) lookAtMc.z);
        this.listenerRotation = Quaternion.Companion.fromLookAt(lookAt, vecUnitY());
    }
    //#endif
}
