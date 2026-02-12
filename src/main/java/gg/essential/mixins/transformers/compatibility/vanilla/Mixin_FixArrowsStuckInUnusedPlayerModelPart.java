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

import gg.essential.model.backend.PlayerPose;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Arrows are rendered on the player by picking a random model part from the list of all model parts ever constructed
 * for that model and then rendering the arrow somewhere on it.
 * The player model however overwrites some model parts of its parent class, thereby "leaking" the originals. Meaning
 * they will still be in the list of all parts even though they're no longer used. This can cause arrows to float in
 * places where visually there's nothing for them to stick to.
 * This is a vanilla bug but it's particularly obvious with emotes, so we'll fix it.
 *
 * Similar issue happens with the cape: MC doesn't actually position it using the regular model part offset/rotation
 * fields, it - for some unknown reason - uses raw GL. As such, arrows stuck in the cape will similarly float in places
 * where they shouldn't be.
 *
 * Similarly, the ears part is only positioned if it is also rendered, so once again, arrows stuck in it will be
 * floating where they shouldn't be if the ears are never rendered. This one however is fixed by setting the ears to
 * match the head in {@link gg.essential.model.backend.minecraft.PlayerPoseKt#applyTo(PlayerPose, ModelBiped)}
 */
@Mixin(ModelPlayer.class)
public abstract class Mixin_FixArrowsStuckInUnusedPlayerModelPart extends ModelBiped {
    @Shadow
    @Final
    private boolean smallArms;

    @Shadow
    @Final
    private ModelRenderer bipedCape;

    //#if MC>=11600
    //$$ @Shadow
    //$$ private List<ModelRenderer> modelRenderers;
    //#endif

    @Inject(method = "<init>", at = @At(
        value = "ESSENTIAL:FIELD_IN_INIT",
        target = "Lnet/minecraft/client/model/ModelPlayer;bipedLeftArm:Lnet/minecraft/client/model/ModelRenderer;",
        opcode = Opcodes.PUTFIELD
    ))
    private void removeOverwrittenModelPartsFromPartsList(CallbackInfo ci) {
        //#if MC>=11600
        //$$ List<ModelRenderer> parts = this.modelRenderers;
        //#else
        List<ModelRenderer> parts = this.boxList;
        //#endif
        if (this.smallArms) {
            parts.remove(this.bipedLeftArm);
            parts.remove(this.bipedRightArm);
        } else {
            parts.remove(this.bipedLeftArm);
        }
        parts.remove(this.bipedLeftLeg);

        parts.remove(this.bipedCape);
    }

    //#if MC>=11600
    //$$ Mixin_FixArrowsStuckInUnusedPlayerModelPart() { super(0f); }
    //#endif
}
