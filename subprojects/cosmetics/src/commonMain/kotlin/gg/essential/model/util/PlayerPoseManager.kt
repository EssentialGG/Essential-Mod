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
package gg.essential.model.util

import dev.folomeev.kotgl.matrix.vectors.Vec3
import dev.folomeev.kotgl.matrix.vectors.distance
import dev.folomeev.kotgl.matrix.vectors.mutables.plus
import dev.folomeev.kotgl.matrix.vectors.vec3
import dev.folomeev.kotgl.matrix.vectors.vecZero
import gg.essential.cosmetics.WearablesManager
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.model.EnumPart
import gg.essential.model.ModelAnimationState
import gg.essential.model.ModelInstance
import gg.essential.model.ParticleSystem
import gg.essential.model.backend.PlayerPose
import gg.essential.model.molang.MolangQueryEntity
import kotlin.math.PI
import kotlin.math.absoluteValue

/**
 * A [ModelInstance] and its most recently active animation state. We need to explicitly track the
 * most recently active animation state because we want to hold that after the emote is done playing
 * while transitioning away from it.
 */
private typealias EmoteState = Pair<ModelInstance, ModelAnimationState.AnimationState?>
private typealias EmoteStatePair = Pair<EmoteState, EmoteState?>

/** Manages a player pose to smoothly transition in of, out of and between emotes. */
class PlayerPoseManager(
    private val entity: MolangQueryEntity,
) {
    private var lastTime = entity.lifeTime

    /** Previously equipped emote, with AnimationStates paused at the moment of cancellation, and 0.1 seconds post cancellation, if it hasn't ended by then */
    private var transitionFromPausedStates: EmoteStatePair? = null
    /** Progress transitioning away from the previously equipped emote. */
    private var transitionFromProgress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    /** Currently equipped emote */
    private var transitionTo: EmoteState? = null
    /** Progress transitioning to the currently equipped emote. */
    private var transitionToProgress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    fun update(wearablesManager: WearablesManager) =
        update(
            wearablesManager.models.values
                .find { it.cosmetic.type.slot == CosmeticSlot.EMOTE }
                ?.takeUnless { it.animationState.active.isEmpty() }
        )

    fun update(target: ModelInstance?) {
        val previous = transitionTo
        var transitionStarted = false
        if (target == null) {
            if (previous != null) {
                // Switch from an emote to nothing
                transitionStarted = true
                transitionFromProgress = 1f - transitionToProgress
                transitionTo = null
                transitionToProgress = 1f
            } else {
                // From nothing to nothing
            }
        } else {
            if (target != previous?.first) {
                // From one emote (or nothing) to another emote
                transitionStarted = true
                transitionFromProgress = 1f - transitionToProgress
                transitionTo = Pair(target, target.animationState.active.firstOrNull())
                transitionToProgress = 0f
            } else {
                // Target unchanged
            }
        }

        /** Stores the most recently active animation state for use after the emote has finished. */
        fun EmoteState.updateAnimationState() =
            first.animationState.active.firstOrNull()?.let { Pair(first, it) } ?: this

        transitionTo = transitionTo?.updateAnimationState()

        // when exiting an emote we need to capture the animation state with time paused to have consistent interpolation key
        // poses to work with. To make the transition less abrupt, we also capture a paused animation state 0.1 seconds
        // into the future which is used to help smooth the initial interpolation
        if (transitionStarted) {
            // will be null if there is no previous EmoteState to transition from
            transitionFromPausedStates = previous?.updateAnimationState()?.let { fromState ->
                val originalState = fromState.second
                    ?: return@let EmoteStatePair(fromState, null) // second EmoteState is null as there is no animation state to work with

                class StaticTimeMolangQueryEntity(
                    entity: MolangQueryEntity,
                    override val lifeTime: Float = entity.lifeTime,
                ) : MolangQueryEntity by entity

                // originalState copy paused in the moment of emote cancellation
                val nowStatePaused = originalState.copy(entity = StaticTimeMolangQueryEntity(originalState.entity))
                // originalState copy paused 0.1 seconds into the future, may have ended
                val futureStatePaused = originalState.copy(
                    entity = StaticTimeMolangQueryEntity(
                        originalState.entity,
                        originalState.lifeTime + transitionTime / VIA_INTERP_TIME_MODIFIER
                    )
                )

                val instance = fromState.first
                return@let EmoteStatePair(
                    EmoteState(instance, nowStatePaused),
                    // second EmoteState will be null if the future animation state has ended
                    futureStatePaused.takeUnless { it.hasEnded }?.let { EmoteState(instance, it) }
                )
            }
        }

        val now = entity.lifeTime
        val dt = now - lastTime.also { lastTime = now }
        val progress = dt / transitionTime
        transitionToProgress += progress
        transitionFromProgress += progress
    }

    /**
     * Computes the final pose for the player based on their vanilla [basePose], equipped cosmetics,
     * the current emote and, if not yet fully transitioned, the previous emote.
     */
    fun computePose(wearablesManager: WearablesManager?, basePose: PlayerPose): PlayerPose {
        var transformedPose = basePose

        // Apply interpolated emote pose
        transformedPose = computePose(transformedPose)

        // Apply pose animations from all other cosmetics (if any)
        for ((cosmetic, model) in wearablesManager?.models ?: emptyMap()) {
            if (cosmetic.type.slot == CosmeticSlot.EMOTE) {
                continue // already handled separately before the loop
            }
            transformedPose = model.computePose(transformedPose)
        }

        return transformedPose
    }

    /**
     * Computes the final pose for the player based on their vanilla [basePose], the current emote
     * and, if not yet fully transitioned, the previous emote.
     */
    private fun computePose(basePose: PlayerPose): PlayerPose {
        var transformedPose = basePose

        fun EmoteState.computePoseForAffectedParts(basePose: PlayerPose): PlayerPose {
            val (instance, latestAnimation) = this
            // Construct a fake animation state so we can hold the most recently active animation
            // even after it is over
            // (so we can smoothly interpolate away from it)
            val animationState = ModelAnimationState(instance.animationState.entity, ParticleSystem.Locator.Zero) { null }
            latestAnimation?.let { animationState.active.add(it) }
            // Compute the pose for this model based on the neutral pose (i.e. suppressing
            // input/base/vanilla pose)
            val modelPose = instance.model.computePose(PlayerPose.neutral(), animationState)
            // but keep the base pose for those parts that were not affected by the animation
            val affectedParts =
                animationState.active.flatMapTo(mutableSetOf()) { it.animation.affectsPoseParts }
            return PlayerPose.fromMap(
                basePose.keys.associateWith {
                    if (it in affectedParts) modelPose[it] else basePose[it]
                },
                basePose.child,
            )
        }

        val transitionFromPausedStates = transitionFromPausedStates
        if (transitionFromPausedStates != null) {
            // pose we will be transitioning from, this either has no animation state or has a state paused in the moment of emote cancellation
            val fromPose = transitionFromPausedStates.first.computePoseForAffectedParts(transformedPose)
            // pose paused 0.1 seconds in the fromPose animation's future, used to help smooth the transition
            // will be null if the original animation state was null or the animation state has ended that far into the future
            val viaPose = transitionFromPausedStates.second?.computePoseForAffectedParts(transformedPose)
            transformedPose = interpolatePose(fromPose, viaPose, transformedPose, transitionFromProgress, true)
        }

        val transitionTo = transitionTo
        if (transitionTo != null) {
            val toPose = transitionTo.computePoseForAffectedParts(transformedPose)
            transformedPose = interpolatePose(transformedPose, null, toPose, transitionToProgress, false)
        }

        return transformedPose
    }

    private fun interpolatePose(from: PlayerPose, via: PlayerPose?, to: PlayerPose, alpha: Float, transitioningFrom: Boolean): PlayerPose {
        return when {
            alpha == 0f -> from
            alpha == 1f -> to
            from == to -> from
            else -> interpolatePosePartsWithLimbBounds(from, via.takeIf { from != via }, to, alpha, transitioningFrom)
        }
    }

    private fun PlayerPose.optimizePartRotations(destPose: PlayerPose): PlayerPose {

        fun PlayerPose.Part.optimizePartRotation(destination: PlayerPose.Part): PlayerPose.Part {
            return EulerAngles(rotateAngleX, rotateAngleY, rotateAngleZ)
                // try to flip the angles such that they are functionally identical but closer to the destination angles
                .optimizeForDestinationOrNull(destination.rotateAngleX, destination.rotateAngleY, destination.rotateAngleZ)
                ?.let { flipped -> copy(
                    rotateAngleX = flipped.x,
                    rotateAngleY = flipped.y,
                    rotateAngleZ = flipped.z)
                } ?: this
        }

        return mapParts { enumPart, part -> part.optimizePartRotation(destPose[enumPart])}
    }

    /**
     * interpolates between 2 [PlayerPose]s with an optional "via" pose which is used to help smooth out the emote transition
     * the interpolation is based around the body which is interpolated via the shortest route after cleaning up the Z rotation
     * after which limbs will be positionally bound relative to the body, only if we are transitioning "from" an emote,
     *
     * limbs also experience a preference to rotation direction and will try not to twist in weird unnatural angles, for both "to" and "from" emote transitions
     */
    private fun interpolatePosePartsWithLimbBounds(fromPoseIn: PlayerPose, viaPoseIn: PlayerPose?, toPose: PlayerPose, alpha: Float, transitioningFrom: Boolean): PlayerPose {
        val fromPose = fromPoseIn.optimizePartRotations(toPose)
        val viaPose = viaPoseIn?.optimizePartRotations(toPose)

        // if we have a viaPose, we will interpolate twice, first the "via -> to" pose, then the "from -> (via -> to)" pose
        val startPose = viaPose ?: fromPose // otherwise only once with a "from -> to" pose

        // calculate the first interpolated pose
        // this is either "via -> to" or "from -> to"
        var interpolatedPose = startPose.mapParts { enumPart, part -> interpolatePosePart(part, toPose[enumPart], alpha, limbAndHeadPartLerpData[enumPart]?.rotMiddle) }

        val secondAlpha = alpha * VIA_INTERP_TIME_MODIFIER
        if (viaPose != null && secondAlpha < 1f) {
            // for the first 1/3 of the transition if we have a viaPose, we interpolate from the "from" pose to the "via -> to" pose which we just did
            interpolatedPose = interpolatedPose.mapParts { enumPart, part -> interpolatePosePart(fromPose[enumPart], part, secondAlpha, null) }
        }

        // we only lock pivots when transitioning "from" an emote
        if (transitioningFrom){
            // this alpha will be used to control the limbs "binding" to the body during the transition
            // it will go from 0 > 1 > 0 over the course of the total transition with these transitions occurring within the first and last fifth of the total transition
            val limbAlpha = (alpha * 5).let {
                    when { // these could be nicer with easing via a wave function, but for a 0.06 second transition it won't be noticeable
                        it < 1 -> it
                        it > 4 -> 5 - it
                        else -> 1f
                    }
                }

            fun PlayerPose.Part.lockPivotsToPoint(point: Vec3, alpha: Float): PlayerPose.Part {
                if (alpha == 1f){ return copy(pivotX = point.x, pivotY = point.y, pivotZ = point.z) }

                val oneMinusAlpha = 1 - alpha
                fun interp(a: Float, b: Float) = a * oneMinusAlpha + b * alpha
                return copy(
                    pivotX = interp(pivotX, point.x),
                    pivotY = interp(pivotY, point.y),
                    pivotZ = interp(pivotZ, point.z),
                )
            }

            // body part interpolation is unbounded, and acts as the anchor here for the rest of the limbs, and head
            val body = interpolatedPose[EnumPart.BODY]

            interpolatedPose = interpolatedPose.mapParts { enumPart, part ->
                limbAndHeadPartLerpData[enumPart]?.let {
                    val toPivot = toPose[enumPart].pivot
                    // if we already match the destination then there is no need to move the pivots
                    // this is relevant for emotes that might, for example, wiggle the body while the legs stay in place
                    if (part.pivot == toPivot) return@let part

                    // calculate a multiplier for how distant the current and final pivots are
                    // this can be multiplied against the limb alpha, we want closer pivots to result in lower values
                    // e.g. a part already closer to the target should return a lower value to reduce limb alpha further
                    val distance = part.pivot.distance(toPivot)
                    // the max distance of 4 is arbitrary, 4 is just the default limb width which seems like a reasonable value for it
                    val distMultiplier = (distance / 4f).coerceAtMost(1f) // note this cannot be 0 due to prior check & return

                    // lock the limbs to their default attachment points relative to the body's current pose
                    val pivotLockedToBody = body.pivot.plus(it.defaultPivotRelativeToBody.rotateBy(body.rotation))
                    part.lockPivotsToPoint(pivotLockedToBody, limbAlpha * distMultiplier) // apply the locked pivots
                } ?: part // return the part unchanged if it is not a limb or head part

            }
        }

        return interpolatedPose
    }

    private fun interpolatePosePart(
        from: PlayerPose.Part,
        to: PlayerPose.Part,
        alpha: Float,
        rotMiddle: Vec3?,
    ): PlayerPose.Part {
        if (from == to) {
            return to
        }
        val oneMinusAlpha = 1 - alpha
        fun interp(a: Float, b: Float) = a * oneMinusAlpha + b * alpha

        fun interpRotInDirectionOfPreferredMiddle(from: Float, to: Float, middle: Float?): Float {
            if (middle == null) {
                // wrap angles around such that we'll always interpolate the short way round
                var fromMod = from.mod(TAU)
                var toMod = to.mod(TAU)
                if (fromMod - toMod > HALF_TAU) {
                    fromMod -= TAU
                } else if (toMod - fromMod > HALF_TAU) {
                    toMod -= TAU
                }
                return interp(fromMod, toMod)
            }

            // a preferred middle point exists so we must try to interpolate in the shortest direction that will
            // not pass over the opposite of the desired middle point

            // shift the angles to be relative to the middle point and wrap them to be values within the range -PI to PI
            val shiftedFrom = (from - middle).wrapAngle()
            val shiftedTo = (to - middle).wrapAngle()

            // interpolate the angles normally, guaranteeing that the result is within the range -PI to PI centered on our preferred middle
            val interp = interp(shiftedFrom, shiftedTo)

            // shift the interpolated angle back to its correct offset
            return interp + middle
        }

        return PlayerPose.Part(
            pivotX = interp(from.pivotX, to.pivotX),
            pivotY = interp(from.pivotY, to.pivotY),
            pivotZ = interp(from.pivotZ, to.pivotZ),
            rotateAngleX = interpRotInDirectionOfPreferredMiddle(from.rotateAngleX, to.rotateAngleX, rotMiddle?.x),
            rotateAngleY = interpRotInDirectionOfPreferredMiddle(from.rotateAngleY, to.rotateAngleY, rotMiddle?.y),
            rotateAngleZ = interpRotInDirectionOfPreferredMiddle(from.rotateAngleZ, to.rotateAngleZ, rotMiddle?.z),
            // we use this only for (mostly small) non-uniform scaling and vanilla doesn't use it at
            // all, doubt anyone will notice we're cheating and I'm not keen on doing matrix
            // interpolation
            extra =
                when {
                    from.extra == null -> to.extra
                    to.extra == null -> from.extra
                    else -> if (alpha < 0.5) from.extra else to.extra
                },
        )
    }

    private class EulerAngles(var x: Float, var y: Float, var z: Float) {
        private fun wrap() : EulerAngles = apply {
            x.wrapAngle()
            y.wrapAngle()
            z.wrapAngle()
        }

        private fun distanceTo(other: EulerAngles): Float {
            return (x - other.x).wrapAngle().absoluteValue +
                    (y - other.y).wrapAngle().absoluteValue +
                    (z - other.z).wrapAngle().absoluteValue
        }

        fun optimizeForDestinationOrNull(destX: Float, destY: Float, destZ: Float): EulerAngles? {
            val destination = EulerAngles(destX, destY, destZ)

            // this is a functionally equivalent "flipped" euler angle, the resulting transform is identical, except
            // that it might be a better/closer fit for euler interpolations to our destination angles.
            // this can have an extremely noticeable impact on certain emotes, such as the "pirouette" emote, where the
            // animation system will resolve with values in all 3 axis' when the animation itself only specifies rotation around the Y axis
            val flipped = EulerAngles(
                x + HALF_TAU,
                -(y - HALF_TAU),
                z - HALF_TAU
            )

            // if the flipped angle is closer to the destination than the original, return it
            return if (flipped.wrap().distanceTo(destination) < this.wrap().distanceTo(destination)) {
                flipped
            } else {
                null
            }
        }
    }

    // The default pivot, relative to body, and x|y|z rotation middles for a particular EnumPart
    private data class PartLerpData(val defaultPivotRelativeToBody: Vec3, val rotMiddle: Vec3){
        constructor(part: EnumPart, rotMiddle: Vec3): this(PlayerPose.neutral()[part].pivot, rotMiddle)
        constructor(part: EnumPart): this(part, vecZero())
    }

    companion object {
        private const val HALF_TAU = PI.toFloat()
        private const val TAU = HALF_TAU * 2

        private fun Float.wrapAngle(): Float {
            val aMod = this.mod(TAU)
            return when {
                aMod > HALF_TAU -> aMod - TAU
                aMod < -HALF_TAU -> aMod + TAU
                else -> aMod
            }
        }

        // specifically only contains parts that, while interpolating separately, we want to remain attached relative to EnumPart.BODY
        private val limbAndHeadPartLerpData = mapOf(
            EnumPart.HEAD to PartLerpData(EnumPart.HEAD),
            EnumPart.RIGHT_ARM to PartLerpData(EnumPart.RIGHT_ARM, vec3(-1f, 0f, 0f)), // -1 x is just an arbitrary value, placing the "middle point" for rotations ~60 degrees in front of the player
            EnumPart.LEFT_ARM to PartLerpData(EnumPart.LEFT_ARM, vec3(-1f, 0f, 0f)),
            EnumPart.RIGHT_SHOULDER_ENTITY to PartLerpData(EnumPart.RIGHT_SHOULDER_ENTITY, vec3(-1f, 0f, 0f)),
            EnumPart.LEFT_SHOULDER_ENTITY to PartLerpData(EnumPart.LEFT_SHOULDER_ENTITY, vec3(-1f, 0f, 0f)),
            EnumPart.RIGHT_LEG to PartLerpData(EnumPart.RIGHT_LEG),
            EnumPart.LEFT_LEG to PartLerpData(EnumPart.LEFT_LEG),
        )

        /**
         * Time (in seconds) we'll take to transition from one emote (or none) to another emote (or
         * none).
         */
        const val transitionTime = 0.3f

        // Modifier of either alpha or transitionTime to get the time it takes for the secondary interpolation when a "viaPose" is provided,
        private const val VIA_INTERP_TIME_MODIFIER = 3f
    }
}
