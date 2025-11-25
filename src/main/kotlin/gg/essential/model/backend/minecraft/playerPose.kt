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
package gg.essential.model.backend.minecraft

import dev.folomeev.kotgl.matrix.matrices.Mat4
import dev.folomeev.kotgl.matrix.vectors.Vec3
import dev.folomeev.kotgl.matrix.vectors.mutables.mutableVec3
import dev.folomeev.kotgl.matrix.vectors.mutables.negate
import dev.folomeev.kotgl.matrix.vectors.vec4
import gg.essential.cosmetics.CosmeticsRenderState
import gg.essential.mixins.ext.client.model.geom.ExtraTransformHolder
import gg.essential.mixins.impl.client.model.CapePoseSupplier
import gg.essential.mixins.impl.client.model.ElytraPoseSupplier
import gg.essential.mixins.impl.client.renderer.entity.PlayerEntityRendererExt
import gg.essential.mixins.transformers.client.model.ModelPlayerAccessor
import gg.essential.model.backend.PlayerPose
import gg.essential.model.util.getRotationEulerZYX
import gg.essential.model.util.times
import net.minecraft.client.model.ModelBiped
import net.minecraft.client.model.ModelRenderer
import net.minecraft.client.renderer.entity.RenderPlayer

//#if MC>=12109
//$$ import dev.folomeev.kotgl.matrix.vectors.vecZero
//#endif

//#if MC>=12102
//$$ import dev.folomeev.kotgl.matrix.vectors.mutables.minus
//$$ import dev.folomeev.kotgl.matrix.vectors.mutables.plus
//$$ import dev.folomeev.kotgl.matrix.vectors.mutables.plusSelf
//$$ import gg.essential.model.util.rotateBy
//$$ import gg.essential.model.util.toMat3
//$$ import gg.essential.model.util.toMat4
//#endif

//#if MC>=12109
//$$ fun PlayerEntityRenderer<*>.toPose(
//#else
fun RenderPlayer.toPose(
//#endif
    withCape: Boolean,
    withElytra: Boolean,
): PlayerPose {
    val basePose = mainModel.toPose()
    val features = (this as PlayerEntityRendererExt).`essential$getFeatures`()
    val capePose = if (withCape) features.firstNotNullOfOrNull { (it as? CapePoseSupplier)?.capePose } else null
    val wingsPose = if (withElytra) features.firstNotNullOfOrNull { (it as? ElytraPoseSupplier)?.wingsPose } else null
    return basePose.copy(
        cape = capePose ?: PlayerPose.Part.MISSING,
        leftWing = wingsPose?.first ?: PlayerPose.Part.MISSING,
        rightWing = wingsPose?.second ?: PlayerPose.Part.MISSING,
    )
}

//#if MC>=11600
//$$ fun BipedModel<*>.toPose(): PlayerPose = PlayerPose(
//#else
fun ModelBiped.toPose(): PlayerPose = PlayerPose(
//#endif
    // FIXME preprocessor bug: for some reason it doesn't remap these
    //#if MC>=11600 && FABRIC || MC>=11700
    //$$ head.toPose(),
    //#if MC>=11700
    //$$ body.toPose(),
    //#else
    //$$ torso.toPose(),
    //#endif
    //$$ rightArm.toPose(),
    //$$ leftArm.toPose(),
    //$$ rightLeg.toPose(),
    //$$ leftLeg.toPose(),
    //#else
    bipedHead.toPose(),
    bipedBody.toPose(),
    bipedRightArm.toPose(),
    bipedLeftArm.toPose(),
    bipedRightLeg.toPose(),
    bipedLeftLeg.toPose(),
    //#endif
    // MC treats these separately, so we have to have separate mixins for each one as well and will just use default
    // values here.
    rightShoulderEntity = PlayerPose.Part(),
    leftShoulderEntity = PlayerPose.Part(),
    rightWing = PlayerPose.Part(),
    leftWing = PlayerPose.Part(),
    cape = PlayerPose.Part(),
    //#if MC>=12102
    //$$ false,
    //#elseif MC>=11600 && FABRIC
    //$$ child,
    //#elseif MC>=11700
    //$$ young,
    //#else
    isChild,
    //#endif
).let { pose ->
    //#if MC>=12109
    //$$ if (body.hasChild("cape"))  {
    //$$     // FIXME Ideally we'd pass the true `extraOffset` here, but we can't easily get it at this point.
    //$$     //       It doesn't matter particularly much since `Mixin_CapePoseSupplier` does consider it (so cosmetics
    //$$     //       and particles will be correct), and we don't have any emotes which have complex animations for the
    //$$     //       cape specifically.
    //$$     //       I'm also kind of hoping MC will get rid of it in the future anyway.
    //$$     pose.withCapePose(vecZero(), body, body.getChild("cape"))
    //$$ } else {
    //$$     pose
    //$$ }
    //#else
    pose
    //#endif
}

fun ModelRenderer.toPose() = PlayerPose.Part(
    rotationPointX,
    rotationPointY,
    rotationPointZ,
    rotateAngleX,
    rotateAngleY,
    rotateAngleZ,
    (this as ExtraTransformHolder).extra,
)

//#if MC>=11600
//$$ fun PlayerPose.applyTo(model: BipedModel<*>) {
//#else
fun PlayerPose.applyTo(model: ModelBiped) {
//#endif
    // FIXME preprocessor bug: for some reason it doesn't remap these
    //#if MC>=11600 && FABRIC || MC>=11700
    //$$ head.applyTo(model.head)
    //#if MC>=12000
    //$$ // Hat is now a proper child of head
    //#elseif MC>=11700
    //$$ head.applyTo(model.hat)
    //#else
    //$$ head.applyTo(model.helmet)
    //#endif
    //#if MC>=11700
    //$$ body.applyTo(model.body)
    //#else
    //$$ body.applyTo(model.torso)
    //#endif
    //$$ rightArm.applyTo(model.rightArm)
    //$$ leftArm.applyTo(model.leftArm)
    //$$ rightLeg.applyTo(model.rightLeg)
    //$$ leftLeg.applyTo(model.leftLeg)
    //#else
    head.applyTo(model.bipedHead)
    head.applyTo(model.bipedHeadwear)
    body.applyTo(model.bipedBody)
    rightArm.applyTo(model.bipedRightArm)
    leftArm.applyTo(model.bipedLeftArm)
    rightLeg.applyTo(model.bipedRightLeg)
    leftLeg.applyTo(model.bipedLeftLeg)
    //#endif
    //#if MC>=12109
    //$$ if (model.body.hasChild("cape")) applyCapePose(vecZero(), model.body, model.body.getChild("cape"))
    //#endif
    //#if MC<12102
    if (model is ModelPlayerAccessor) {
        head.applyTo(model.ears)
    }
    //#if MC>=11600 && FABRIC
    //$$ model.child = child
    //#elseif MC>=11700
    //$$ model.young = child
    //#else
    model.isChild = child
    //#endif
    //#endif
}

fun PlayerPose.Part.applyTo(model: ModelRenderer) {
    model.rotationPointX = pivotX
    model.rotationPointY = pivotY
    model.rotationPointZ = pivotZ
    model.rotateAngleX = rotateAngleX
    model.rotateAngleY = rotateAngleY
    model.rotateAngleZ = rotateAngleZ
    (model as ExtraTransformHolder).extra = extra
}

//#if MC>=12102
//$$ fun PlayerPose.withCapePose(
//$$     extraOffset: Vec3,
//$$     bodyModel: ModelPart,
//$$     capeModel: ModelPart,
//$$ ): PlayerPose {
//$$     // Chain body pose and cape pose (which is a child of body) into a single absolute pose
//$$     val bodyPose = bodyModel.toPose()
//$$     val bodyPos = bodyPose.pivot.plus(extraOffset)
//$$     val bodyRot = bodyPose.rotation
//$$     val capePose = capeModel.toPose()
//$$     val pos = capePose.pivot.rotateBy(bodyRot).plusSelf(bodyPos)
//$$     val rotation = bodyRot.times(capePose.rotation)
//$$     val rot = rotation.toMat3().toMat4().getRotationEulerZYX()
//$$     val absPose = PlayerPose.Part(pos.x, pos.y, pos.z, rot.x, rot.y, rot.z)
//$$     return copy(cape = absPose)
//$$ }
//$$
//$$ fun PlayerPose.applyCapePose(
//$$     extraOffset: Vec3,
//$$     bodyModel: ModelPart,
//$$     capeModel: ModelPart,
//$$ ) {
//$$     // Convert the absolute cape pose into one relative to the body pose (because capeModel is a child of bodyModel)
//$$     // i.e. inverse of withCapePose
//$$     val bodyPose = bodyModel.toPose()
//$$     val bodyPos = bodyPose.pivot.plus(extraOffset)
//$$     val bodyRot = bodyPose.rotation
//$$     val absPose = this.cape
//$$     val pos = absPose.pivot.minus(bodyPos).rotateBy(bodyRot.conjugate())
//$$     val rotation = bodyRot.invert().times(absPose.rotation)
//$$     val rot = rotation.toMat3().toMat4().getRotationEulerZYX()
//$$     val relPose = PlayerPose.Part(pos.x, pos.y, pos.z, rot.x, rot.y, rot.z)
//$$     // and finally apply it to the ModelPart
//$$     relPose.applyTo(capeModel)
//$$ }
//#else
fun PlayerPose.withCapePose(
    capeModel: ModelRenderer,
    vanillaMatrix: Mat4,
): PlayerPose {
    val modelPose = capeModel.toPose()
    // The complete stack of transformations as applied by MC is
    //   vanillaMatrix * modelPose.pivot * modelPose.rotation
    // we need to somehow get it into our regular form:
    //   result.pivot * result.rotation * result.extra
    // Luckily it's fairly easy to get our pivot by just shoving MC's pivot through MC's matrix
    val pivot = vec4(modelPose.pivotX, modelPose.pivotY, modelPose.pivotZ, 1f).times(vanillaMatrix)
    // and since `modelPose.rotation` is always the identity matrix in vanilla, we can simplify getting the rotation
    // to simply retrieving the rotational part of `vanillaMatrix`:
    val rotation = vanillaMatrix.getRotationEulerZYX()
    // MC doesn't do any scaling, so we shouldn't need the `extra` part and all that's left is to plug it all in
    val combinedPose = PlayerPose.Part(
        pivotX = pivot.x,
        pivotY = pivot.y,
        pivotZ = pivot.z,
        rotateAngleX = rotation.x,
        rotateAngleY = rotation.y,
        rotateAngleZ = rotation.z,
        extra = null,
    )
    return copy(cape = combinedPose)
}
//#endif

fun getElytraPoseOffset(cState: CosmeticsRenderState): Vec3 {
    val offset = mutableVec3()

    //#if MC<11400
    // When sneaking, the Minecraft renderer shifts the entire model down, the elytra renderer has this assumption
    // baked in, however our Pose system does not and wants absolute coordinates.
    if (cState.isSneaking()) {
        offset.y -= 0.2f /* from LayerCustomHead */ * 16 /* scale */
    }
    //#endif

    // Because of the manual translate in LayerCape
    offset.z += 2f

    return offset
}

fun PlayerPose.withElytraPose(
    leftWing: ModelRenderer,
    rightWing: ModelRenderer,
    cState: CosmeticsRenderState,
): PlayerPose {
    val offset = getElytraPoseOffset(cState)
    return copy(leftWing = leftWing.toPose().offset(offset), rightWing = rightWing.toPose().offset(offset))
}

fun PlayerPose.applyElytraPose(
    leftWing: ModelRenderer,
    rightWing: ModelRenderer,
    cState: CosmeticsRenderState,
) {
    val offset = getElytraPoseOffset(cState).negate()
    this.leftWing.offset(offset).applyTo(leftWing)
    this.rightWing.offset(offset).applyTo(rightWing)
}
