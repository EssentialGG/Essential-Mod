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
package gg.essential.mixins.transformers.client.renderer.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.config.EssentialConfig;
import gg.essential.universal.wrappers.UPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//#if MC>=12102
//$$ import net.minecraft.entity.EntityAttachments;
//#endif

// 1.16+ functionality
@Mixin(EntityRenderer.class)
public abstract class Mixin_SyncLocalPlayerNameplateCrouchHeight {

    //#if MC>=12102
    //$$ // now targets the entity render state setup
    //$$ @ModifyExpressionValue(method = "updateRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getAttachments()Lnet/minecraft/entity/EntityAttachments;"))
    //#elseif MC>=12006
    //$$ @ModifyExpressionValue(method = "renderLabelIfPresent", at = @At(value = "FIELD", target = "Lnet/minecraft/util/math/Vec3d;y:D"))
    //#elseif MC>=12000
    //$$ @ModifyExpressionValue(method = "renderLabelIfPresent",at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getNameLabelHeight()F"))
    //#else
    @ModifyExpressionValue(method = "renderName",at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getHeight()F"))
    //#endif

    //#if MC>=12102
    //$$ private <T extends Entity> EntityAttachments getHeight(final EntityAttachments original, @Local(argsOnly = true) T entity) { // only return changed
    //#elseif MC>=12006
    //$$ private <T extends Entity> double getHeight(final double original, @Local(argsOnly = true) T entity) { // only return changed
    //#else
    private <T extends Entity> float getHeight(final float original, @Local(argsOnly = true) T entity) {
    //#endif
        // the local player has an override for isCrouching() that makes it independent from the actual set pose
        // this can de-sync the nameplate height, so let's return the height value of the pose we should be in
        if (entity == UPlayer.getPlayer()
                && EssentialConfig.INSTANCE.getEssentialEnabled()
                && EssentialConfig.INSTANCE.getShowOwnNametag().getUntracked()) {

            Pose forcedPose;
            // force the correct pose dimensions for the return, only if we are in the incorrect one
            if (entity.isCrouching()) {
                forcedPose = entity.getPose() == Pose.STANDING ? Pose.CROUCHING : null;
            } else {
                forcedPose = entity.getPose() == Pose.CROUCHING ? Pose.STANDING : null;
            }

            // only override incorrect STANDING | CROUCHING poses so we preserve any alternative pose types, or modified return values
            if (forcedPose == null) return original;

            //#if MC>=12102
            //$$ return entity.getDimensions(forcedPose).attachments();
            //#elseif MC>=12006
            //$$ return entity.getDimensions(forcedPose).height();
            //#elseif MC>=12001
            //$$ return entity.getDimensions(forcedPose).height + 0.5F; // the '+ 0.5' was moved inside the method that we now replace
            //#else
            return entity.getSize(forcedPose).height;
            //#endif
        }
        return original;
    }
}
