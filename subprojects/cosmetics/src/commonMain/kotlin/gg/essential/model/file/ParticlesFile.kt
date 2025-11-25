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
// Note: Most doc comments in here have been copied from https://bedrock.dev/docs/stable/Particles
//       or the Microsoft docs it is based on. These do not have any clear license. DO NOT REDISTRIBUTE!

package gg.essential.model.file

import dev.folomeev.kotgl.matrix.vectors.Vec4
import dev.folomeev.kotgl.matrix.vectors.mutables.lerp
import dev.folomeev.kotgl.matrix.vectors.vec4
import gg.essential.model.molang.MolangContext
import gg.essential.model.molang.Molang
import gg.essential.model.molang.Molang.Companion.ONE
import gg.essential.model.molang.Molang.Companion.ZERO
import gg.essential.model.molang.MolangVec3
import gg.essential.model.util.Color
import gg.essential.model.util.ListOrSingle
import gg.essential.model.util.PairAsList
import gg.essential.model.util.TreeMap
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject

@Serializable
data class ParticlesFile(
    @SerialName("format_version")
    val formatVersion: String,
    @SerialName("particle_effect")
    val particleEffect: ParticleEffect,
) {
    @Serializable
    data class ParticleEffect(
        val description: Description,
        val curves: Map<String, Curve> = emptyMap(),
        val events: Map<String, Event> = emptyMap(),
        val components: ParticleEffectComponents = ParticleEffectComponents(),
    )

    @Serializable
    data class Description(
        val identifier: String,
        @SerialName("basic_render_parameters")
        val basicRenderParameters: BasicRenderParameters = BasicRenderParameters(),
    )

    @Serializable
    data class BasicRenderParameters(
        val material: Material = Material.Cutout,
        val texture: String = "texture",
    )

    @Serializable
    enum class Material(val needsSorting: Boolean, val backfaceCulling: Boolean) {
        @SerialName("particles_add")
        Add(true, true),
        @SerialName("particles_alpha")
        Cutout(false, true),
        @SerialName("particles_blend")
        Blend(true, false),
    }

    @Serializable
    data class Curve(
        val type: Type,
        val nodes: List<Molang>,
        val input: Molang,
        @SerialName("horizontal_range")
        val range: Molang = ONE,
    ) {
        @Serializable
        enum class Type {
            @SerialName("linear")
            Linear,
            @SerialName("bezier")
            Bezier,
            @SerialName("catmull_rom")
            CatmullRom,
            @SerialName("bezier_chain")
            BezierChain,
        }
    }

    @Serializable
    data class Event(
        val sequence: List<Event>? = null,
        val randomize: List<@Serializable(with = WeightedEventSerializer::class) RandomizeOption>? = null,
        @SerialName("particle_effect")
        val particle: Particle? = null,
        @SerialName("sound_effect")
        val sound: Sound? = null,
        val expression: Molang? = null,
    ) {
        @Serializable
        data class RandomizeOption(
            val weight: Float,
            val value: Event,
        )

        @Serializable
        data class Particle(
            val effect: String,
            val type: Type,
            @SerialName("pre_effect_expression")
            val preEffectExpression: Molang = ZERO,
        ) {
            @Serializable
            enum class Type {
                @SerialName("emitter")
                Emitter,
                @SerialName("emitter_bound")
                EmitterBound,
                @SerialName("particle")
                Particle,
                @SerialName("particle_with_velocity")
                ParticleWithVelocity,
                ;

                val isParticle: Boolean
                    get() = this == Particle || this == ParticleWithVelocity

                val inheritVelocity: Boolean
                    get() = this == ParticleWithVelocity

                val isBound: Boolean
                    get() = this == EmitterBound
            }
        }

        @Serializable
        data class Sound(
            @SerialName("event_name")
            val event: String,
        )

        private class WeightedEventSerializer : JsonTransformingSerializer<RandomizeOption>(RandomizeOption.serializer()) {
            override fun transformDeserialize(element: JsonElement): JsonElement = if (element is JsonObject) {
                buildJsonObject {
                    put("weight", element["weight"]!!)
                    put("value", JsonObject(element.filterKeys { it != "weight" }))
                }
            } else element

            override fun transformSerialize(element: JsonElement): JsonElement {
                return JsonObject((element as JsonObject).filterKeys { it != "value" } + element["value"] as JsonObject)
            }
        }
    }
}

@Serializable
data class ParticleEffectComponents(
    @SerialName("minecraft:emitter_local_space")
    val emitterLocalSpace: EmitterLocalSpace? = null,
    @SerialName("minecraft:emitter_initialization")
    val emitterInitialization: EmitterInitialization? = null,
    @SerialName("minecraft:emitter_lifetime_events")
    val emitterLifetimeEvents: EmitterLifetimeEvents = EmitterLifetimeEvents(),
    @SerialName("minecraft:emitter_lifetime_looping")
    val emitterLifetimeLooping: EmitterLifetimeLooping? = null,
    @SerialName("minecraft:emitter_lifetime_once")
    val emitterLifetimeOnce: EmitterLifetimeOnce? = null,
    @SerialName("minecraft:emitter_lifetime_expression")
    val emitterLifetimeExpression: EmitterLifetimeExpression? = null,
    @SerialName("minecraft:emitter_rate_instant")
    val emitterRateInstant: EmitterRateInstant? = null,
    @SerialName("minecraft:emitter_rate_steady")
    val emitterRateSteady: EmitterRateSteady? = null,
    @SerialName("minecraft:emitter_shape_point")
    val emitterShapePoint: EmitterShapePoint? = null,
    @SerialName("minecraft:emitter_shape_sphere")
    val emitterShapeSphere: EmitterShapeSphere? = null,
    @SerialName("minecraft:emitter_shape_box")
    val emitterShapeBox: EmitterShapeBox? = null,
    @SerialName("minecraft:emitter_shape_disc")
    val emitterShapeDisc: EmitterShapeDisc? = null,
    @SerialName("minecraft:particle_lifetime_events")
    val particleLifetimeEvents: ParticleLifetimeEvents = ParticleLifetimeEvents(),
    @SerialName("minecraft:particle_appearance_billboard")
    val particleAppearanceBillboard: ParticleAppearanceBillboard? = null,
    @SerialName("minecraft:particle_appearance_tinting")
    val particleAppearanceTinting: ParticleAppearanceTinting? = null,
    /** When this component exists, particle will be tinted by local lighting conditions in-game.  */
    @SerialName("minecraft:particle_appearance_lighting")
    val particleAppearanceLighting: Unit? = null,
    @SerialName("minecraft:particle_initial_speed")
    val particleInitialSpeed: Molang = ZERO,
    @SerialName("minecraft:particle_initial_spin")
    val particleInitialSpin: ParticleInitialSpin? = null,
    @SerialName("minecraft:particle_initialization")
    val particleInitialization: ParticleInitialization? = null,
    @SerialName("minecraft:particle_motion_collision")
    val particleMotionCollision: ParticleMotionCollision? = null,
    @SerialName("minecraft:particle_motion_dynamic")
    val particleMotionDynamic: ParticleMotionDynamic? = null,
    @SerialName("minecraft:particle_motion_parametric")
    val particleMotionParametric: ParticleMotionParametric? = null,
    @SerialName("minecraft:particle_lifetime_expression")
    val particleLifetimeExpression: ParticleLifetimeExpression? = null,
    @SerialName("essential:particle_visibility")
    val particleVisibility: ParticleVisibility = ParticleVisibility(),
) {
    /**
     * This component specifies the frame of reference of the emitter.
     * Applies only when the emitter is attached to an entity.
     * When 'position' is true, the particles will simulate in entity space, otherwise they will simulate in world space.
     * Rotation works the same way for rotation.
     * Default is false for both, which makes the particles emit relative to the emitter, then simulate independently from the emitter.
     * Note that rotation = true and position = false is an invalid option.
     */
    @Serializable
    data class EmitterLocalSpace(
        val position: Boolean = false,
        val rotation: Boolean = false,
        /** When true, add the emitter's velocity to the initial particle velocity. */
        val velocity: Boolean = false,
    )

    @Serializable
    data class EmitterInitialization(
        @SerialName("creation_expression")
        val creationExpression: Molang? = null,
        @SerialName("per_update_expression")
        val perUpdateExpression: Molang? = null,
    )

    /** Allows for lifetime events on the emitter to trigger certain events. */
    @Serializable
    data class EmitterLifetimeEvents(
        /** fires when the emitter is created */
        @SerialName("creation_event")
        val creationEvents: ListOrSingle<String> = emptyList(),
        /** fires when the emitter expires (does not wait for particles to expire too) */
        @SerialName("expiration_event")
        val expirationEvents: ListOrSingle<String> = emptyList(),
        /** a series of times, that trigger the event; these get fired on every loop the emitter goes through */
        val timeline: TreeMap<Float, ListOrSingle<String>> = TreeMap(emptyMap()),
        /** a series of distances, that trigger the event; these get fired when the emitter has moved by the specified input distance */
        @SerialName("travel_distance_events")
        val travelDistanceEvents: TreeMap<Float, ListOrSingle<String>> = TreeMap(emptyMap()),
        /** these get fired every time the emitter has moved the specified input distance from the last time it was fired */
        @SerialName("looping_travel_distance_events")
        val loopingTravelDistanceEvents: List<LoopingDistance> = emptyList(),
    ) {
        @Serializable
        data class LoopingDistance(
            val distance: Float,
            val effects: List<String>,
        )
    }

    /** Emitter will loop until it is removed. */
    @Serializable
    data class EmitterLifetimeLooping(
        /** emitter will emit particles for this time per loop; evaluated once per particle emitter loop */
        @SerialName("active_time")
        val activeTime: Molang = Molang.literal(10f),
        /** emitter will pause emitting particles for this time per loop; evaluated once per particle emitter loop */
        @SerialName("sleep_time")
        val sleepTime: Molang = ZERO,
    )

    /** Emitter will execute once, and once the lifetime ends or the number of particles allowed to emit have emitted, the emitter expires. */
    @Serializable
    data class EmitterLifetimeOnce(
        /** how long the particles emit for; evaluated once */
        @SerialName("active_time")
        val activeTime: Molang = Molang.literal(10f),
    )

    /** Emitter will turn 'on' when the activation expression is non-zero, and will turn 'off' when it's zero. */
    @Serializable
    data class EmitterLifetimeExpression(
        /** When the expression is non-zero, the emitter will emit particles; Evaluated every frame */
        @SerialName("activation_expression")
        val activationExpression: Molang = ONE,

        /** Emitter will expire if the expression is non-zero; Evaluated every frame */
        @SerialName("expiration_expression")
        val expirationExpression: Molang = ZERO,
    )

    /** All particles come out at once, then no more unless the emitter loops. */
    @Serializable
    data class EmitterRateInstant(
        /** this many particles are emitted at once; evaluated once per particle emitter loop */
        /** how ofter a particle is emitted, in particles/sec; evaluated once per particle emitted */
        @SerialName("num_particles")
        val numParticles: Molang = Molang.literal(10f),
    )

    /** Particles come out at a steady or Molang rate over time. */
    @Serializable
    data class EmitterRateSteady(
        /** how ofter a particle is emitted, in particles/sec; evaluated once per particle emitted */
        @SerialName("spawn_rate")
        val spawnRate: Molang = ONE,
        /** maximum number of particles that can be active at once for this emitter; evaluated once per particles emitter loop */
        @SerialName("max_particles")
        val maxParticles: Molang = Molang.literal(50f),
    )

    /** All particles come out of a point offset from the emitter. */
    @Serializable
    data class EmitterShapePoint(
        /** specifies the offset from the emitter to emit the particles; evaluated once per particle emitted */
        val offset: MolangVec3 = MolangVec3.ZERO,
        /** specifies the direction of particles; evaluated once per particle emitted */
        val direction: Direction = Direction.Outwards,
    )

    /** specifies the direction of particles emitted from a shape which has volume */
    @Serializable(with = Direction.Serializer::class)
    sealed class Direction {
        /** particle direction towards center of shape */
        @Serializable(with = InwardsSerializer::class)
        object Inwards : Direction()
        /** particle direction away from center of shape */
        @Serializable(with = OutwardsSerializer::class)
        object Outwards : Direction()
        /** particle direction as specified by molang expression */
        @Serializable(with = CustomSerializer::class)
        data class Custom(val vec: MolangVec3) : Direction()

        internal object Serializer : JsonContentPolymorphicSerializer<Direction>(Direction::class) {
            override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Direction> = when {
                element is JsonPrimitive && element.content == "inwards" -> Inwards.serializer()
                element is JsonPrimitive && element.content == "outwards" -> Outwards.serializer()
                else -> Custom.serializer()
            }
        }

        private object InwardsSerializer : ObjectAsString<Inwards>("inwards", Inwards)
        private object OutwardsSerializer : ObjectAsString<Outwards>("outwards", Outwards)
        internal object CustomSerializer : KSerializer<Custom> {
            private val inner = MolangVec3.serializer()
            override val descriptor: SerialDescriptor = inner.descriptor
            override fun deserialize(decoder: Decoder) = Custom(inner.deserialize(decoder))
            override fun serialize(encoder: Encoder, value: Custom) = inner.serialize(encoder, value.vec)
        }

        private open class ObjectAsString<T>(private val str: String, private val value: T) : KSerializer<T> {
            private val inner = String.serializer()
            override val descriptor: SerialDescriptor = inner.descriptor
            override fun deserialize(decoder: Decoder) = inner.deserialize(decoder).let { value }
            override fun serialize(encoder: Encoder, value: T) = inner.serialize(encoder, str)
        }
    }

    /** All particles come out of a box of the specified size from the emitter. */
    @Serializable
    data class EmitterShapeBox(
        /** specifies the offset from the emitter to emit the particles; evaluated once per particle emitted */
        val offset: MolangVec3 = MolangVec3.ZERO,
        /** box dimensions; these are the half dimensions, the box is formed centered on the emitter with the box extending in the 3 principal x/y/z axes by these values */
        @SerialName("half_dimensions")
        val halfDimensions: MolangVec3,
        /** emit only from the surface of the box */
        @SerialName("surface_only")
        val surfaceOnly: Boolean = false,
        /** specifies the direction of particles; evaluated once per particle emitted */
        val direction: Direction = Direction.Outwards,
    )

    /** This component spawns particles using a disc shape, particles can be spawned inside the shape or on its outer perimeter. */
    @Serializable
    data class EmitterShapeDisc(
        /** specifies the normal of the disc plane, the disc will be perpendicular to this direction */
        @SerialName("plane_normal")
        val planeNormal: MolangVec3 = MolangVec3.UNIT_Y,
        /** specifies the offset from the emitter to emit the particles; evaluated once per particle emitted */
        val offset: MolangVec3 = MolangVec3.ZERO,
        /** disc radius; evaluated once per particle emitted */
        val radius: Molang = ONE,
        /** emit only from the edge of the disc */
        @SerialName("surface_only")
        val surfaceOnly: Boolean = false,
        /** specifies the direction of particles; evaluated once per particle emitted */
        val direction: Direction = Direction.Outwards,
    )

    /** All particles come out of a sphere offset from the emitter. */
    @Serializable
    data class EmitterShapeSphere(
        /** specifies the offset from the emitter to emit the particles; evaluated once per particle emitted */
        val offset: MolangVec3 = MolangVec3.ZERO,
        /** sphere radius; evaluated once per particle emitted */
        val radius: Molang = ONE,
        /** emit only from the surface of the sphere */
        @SerialName("surface_only")
        val surfaceOnly: Boolean = false,
        /** specifies the direction of particles; evaluated once per particle emitted */
        val direction: Direction = Direction.Outwards,
    )

    @Serializable
    data class ParticleLifetimeEvents(
        /** fires when the particle is created */
        @SerialName("creation_event")
        val creationEvents: ListOrSingle<String> = emptyList(),
        /** fires when the particle expires */
        @SerialName("expiration_event")
        val expirationEvents: ListOrSingle<String> = emptyList(),
        /** a series of times, that trigger the event */
        val timeline: TreeMap<Float, ListOrSingle<String>> = TreeMap(emptyMap()),
    )

    @Serializable
    data class ParticleAppearanceBillboard(
        /** specifies the x/y size of the billboard; evaluated every frame */
        val size: PairAsList<Molang, Molang>,
        /** used to orient the billboard */
        @SerialName("facing_camera_mode")
        @JsonNames("face_camera_mode")
        val facingCameraMode: FacingCameraMode,
        /** Specifies how to calculate the direction of a particle, this will be used by facing modes that require a direction as input (for instance: lookat_direction and direction) */
        val direction: Direction = Direction.FromVelocity(),
        /** specifies the UVs for the particle */
        val uv: UV = UV(uv = Pair(ZERO, ZERO), uvSize = Pair(ONE, ONE)),
    ) {
        @Serializable
        enum class FacingCameraMode {
            /** aligned to the camera, perpendicular to the view axis */
            @SerialName("rotate_xyz")
            RotateXYZ,
            /** aligned to camera, but rotating around world y axis */
            @SerialName("rotate_y")
            RotateY,
            /** aimed at the camera, biased towards world y up */
            @SerialName("lookat_xyz")
            LookAtXYZ,
            /** aimed at the camera, but rotating around world y axis */
            @SerialName("lookat_y")
            LookAtY,
            /** this is a thing that exists but the docs don't list it (but they do mention it in the explanation for the `direction` field) */
            @SerialName("lookat_direction")
            LookAtDirection,
            /** unrotated particle x axis is along the direction vector, unrotated y axis attempts to aim upwards */
            @SerialName("direction_x")
            DirectionX,
            /** unrotated particle y axis is along the direction vector, unrotated x axis attempts to aim upwards */
            @SerialName("direction_y")
            DirectionY,
            /** billboard face is along the direction vector, unrotated y axis attempts to aim upwards */
            @SerialName("direction_z")
            DirectionZ,
            /** orient the particles to match the emitter's transform (the billboard plane will match the transform's xy plane). */
            @SerialName("emitter_transform_xy")
            EmitterTransformXY,
            /** orient the particles to match the emitter's transform (the billboard plane will match the transform's xz plane). */
            @SerialName("emitter_transform_xz")
            EmitterTransformXZ,
            /** orient the particles to match the emitter's transform (the billboard plane will match the transform's yz plane). */
            @SerialName("emitter_transform_yz")
            EmitterTransformYZ,
        }

        @OptIn(ExperimentalSerializationApi::class)
        @Serializable
        @JsonClassDiscriminator("mode")
        sealed class Direction {
            /** The direction matches the direction of the velocity. */
            @Serializable
            @SerialName("derive_from_velocity")
            data class FromVelocity(
                /** the direction is set if the speed of the particle is above the threshold */
                val minSpeedThreshold: Float = 0.01f,
            ) : Direction() {
                @Transient
                val minSpeedThresholdSqr: Float = minSpeedThreshold * minSpeedThreshold
            }

            /** The direction is specified in the json definition using a vector of floats or molang expressions. */
            @Serializable
            @SerialName("custom")
            data class Custom(
                /** specifies the direction vector */
                @SerialName("custom_direction")
                val direction: MolangVec3,
            ) : Direction()
        }

        @Serializable
        data class UV(
            /** specifies the assumed texture width/height */
            @SerialName("texture_width")
            val textureWidth: Int = 1,
            @SerialName("texture_height")
            val textureHeight: Int = 1,
            /** Assuming the specified texture width and height, use these uv coordinates; evaluated every frame */
            val uv: PairAsList<Molang, Molang>? = null,
            @SerialName("uv_size")
            val uvSize: PairAsList<Molang, Molang>? = null,
            /** alternate way via specifying a flipbook animation */
            val flipbook: Flipbook? = null,
        ) {
            /** a flipbook animation uses pieces of the texture to animate, by stepping over time from one "frame" to another */
            @Serializable
            data class Flipbook(
                /** upper-left corner of starting UV patch */
                @SerialName("base_UV")
                val base: PairAsList<Molang, Molang>,
                /** size of UV patch */
                @SerialName("size_UV")
                val size: PairAsList<Float, Float>,
                /** how far to move the UV patch each frame */
                @SerialName("step_UV")
                val step: PairAsList<Float, Float>,
                /** default frames per second */
                @SerialName("frames_per_second")
                val framePerSecond: Float = 1f,
                /** maximum frame number, with first frame being frame 1 */
                @SerialName("max_frame")
                val maxFrame: Molang,
                /** adjust fps to match lifetime of particle */
                @SerialName("stretch_to_lifetime")
                val stretchToLifetime: Boolean = false,
                /** makes the animation loop when it reaches the end? */
                val loop: Boolean = false,
            )
        }
    }

    /** This component sets the color tinting of the particle. */
    @Serializable
    data class ParticleAppearanceTinting(
        val color: MolangColorOrGradient,
    )

    /** Starts the particle with a specified orientation and rotation rate. */
    @Serializable
    data class ParticleInitialSpin(
        /** specifies the initial rotation in degrees; evaluated once */
        val rotation: Molang = ZERO,
        /** specifies the spin rate in degrees/second; evaluated once */
        @SerialName("rotation_rate")
        val rotationRate: Molang = ZERO,
    )

    /** Starts the particle with a specified render expression. */
    @Serializable
    data class ParticleInitialization(
        @SerialName("per_render_expression")
        val perRenderExpression: Molang? = null,
    )

    /**
     * This component enables collisions between the terrain and the particle.
     * Collision detection in Minecraft consists of detecting an intersection, moving to a nearby non-intersecting point
     * for the particle (if possible), and setting its direction to not be aimed towards the collision (usually
     * perpendicular to the collision surface).
     * Note that if this component doesn't exist, there will be no collision.
     */
    @Serializable
    data class ParticleMotionCollision(
        /** enables collision when true/non-zero; evaluated every frame */
        val enabled: Molang = ONE,
        /**
         * alters the speed of the particle when it has collided
         * useful for emulating friction/drag when colliding, e.g a particle
         * that hits the ground would slow to a stop.
         * This drag slows down the particle by this amount in blocks/sec
         * when in contact
         */
        @SerialName("collision_drag")
        val collisionDrag: Float = 0f,
        /**
         * used for bouncing/not-bouncing
         * Set to 0.0 to not bounce, 1.0 to bounce back up to original hight
         * and in-between to lose speed after bouncing.  Set to >1.0 to gain energy on each bounce
         */
        @SerialName("coefficient_of_restitution")
        val coefficientOfRestitution: Float = 0f,
        /**
         * used to minimize interpenetration of particles with the environment
         * note that this must be less than or equal to 1/2 block
         */
        @SerialName("collision_radius")
        val collisionRadius: Float,
        /** triggers expiration on contact if true */
        @SerialName("expire_on_contact")
        val expireOnContact: Boolean = false,
        /** triggers an event */
        val events: ListOrSingle<Event> = emptyList(),
    ) {
        @Serializable
        data class Event(
            /** triggers the specified event if the conditions are met */
            val event: String,
            /** minimum speed for event triggering */
            @SerialName("min_speed")
            val minSpeed: Float = 2f,
        )
    }

    /** This component specifies the dynamic properties of the particle, from a simulation standpoint what forces act upon the particle? */
    @Serializable
    data class ParticleMotionDynamic(
        /** the linear acceleration applied to the particle; An example would be gravity which is [0, -9.8, 0]; evaluated every frame */
        @SerialName("linear_acceleration")
        val linearAcceleration: MolangVec3 = MolangVec3.ZERO,
        /** acceleration = -linear_drag_coefficient*velocity; evaluated every frame */
        @SerialName("linear_drag_coefficient")
        val linearDragCoefficient: Molang = ZERO,
        /** acceleration applies to the rotation speed of the particle; evaluated every frame */
        @SerialName("rotation_acceleration")
        val rotationAcceleration: Molang = ZERO,
        /** rotation_acceleration += -rotation_rate*rotation_drag_coefficient */
        @SerialName("rotation_drag_coefficient")
        val rotationDragCoefficient: Molang = ZERO,
    )

    /** This component directly controls the particle. */
    @Serializable
    data class ParticleMotionParametric(
        /** directly set the position relative to the emitter; evaluated every frame */
        @SerialName("relative_position")
        val relativePosition: MolangVec3 = MolangVec3.ZERO,
        /** directly set the 3d direction of the particle; evaluated every frame */
        val direction: MolangVec3? = null,
        /** directly set the rotation of the particle; evaluated every frame */
        val rotation: Molang = ZERO,
    )

    /** Standard lifetime component. These expressions control the lifetime of the particle. */
    @Serializable
    data class ParticleLifetimeExpression(
        /** this expression makes the particle expire when true (non-zero); evaluated every frame */
        @SerialName("expiration_expression")
        val expirationExpression: Molang = ZERO,
        /** particle will expire after this much time; evaluated once */
        @SerialName("max_lifetime")
        val maxLifetime: Molang,
    )

    /** Custom component that controls particle visibility for particles emitted by the player. Does not affect visibility for particles emitted by other players */
    @Serializable
    data class ParticleVisibility(
        /** controls whether player's own particles are seen in first-person */
        @SerialName("first_person")
        val firstPerson: Boolean = true,
        /** controls whether player's own particles are seen in third-person */
        @SerialName("third_person")
        val thirdPerson: Boolean = true,
    )
}

@Serializable(with = MolangColorOrGradientSerializer::class)
sealed interface MolangColorOrGradient {
    fun eval(context: MolangContext): Vec4
}

@Serializable(with = MolangColorSerializer::class)
sealed interface MolangColor : MolangColorOrGradient

@Serializable(with = MolangColorLiteralSerializer::class)
data class MolangColorLiteral(val color: Color) : MolangColor {
    private val asVec = vec4(color.r.toFloat() / 255f, color.g.toFloat() / 255f, color.b.toFloat() / 255f, color.a.toFloat() / 255f)
    override fun eval(context: MolangContext): Vec4 = asVec
}

@Serializable(with = MolangColorExpressionSerializer::class)
data class MolangColorExpression(
    val r: Molang,
    val g: Molang,
    val b: Molang,
    val a: Molang,
) : MolangColor {
    override fun eval(context: MolangContext): Vec4 =
        vec4(r.eval(context), g.eval(context), b.eval(context), a.eval(context))
}

@Serializable
data class MolangGradient(
    @Serializable(with = MolangGradientMapSerializer::class)
    val gradient: TreeMap<Float, MolangColor>,
    val interpolant: Molang,
) : MolangColorOrGradient {
    override fun eval(context: MolangContext): Vec4 {
        val alpha = interpolant.eval(context)
        val floor = gradient.floorEntry(alpha)
        val ceil = gradient.ceilingEntry(alpha)
        return when {
            floor == null -> ceil!!.value.eval(context)
            ceil == null -> floor.value.eval(context)
            floor == ceil -> floor.value.eval(context)
            else -> floor.value.eval(context).lerp(ceil.value.eval(context), (alpha - floor.key) / (ceil.key - floor.key))
        }
    }
}

internal object MolangColorOrGradientSerializer : JsonContentPolymorphicSerializer<MolangColorOrGradient>(MolangColorOrGradient::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out MolangColorOrGradient> =
        if (element is JsonObject) MolangGradient.serializer() else MolangColor.serializer()
}

internal object MolangColorSerializer : JsonContentPolymorphicSerializer<MolangColor>(MolangColor::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out MolangColor> =
        when (element) {
            is JsonArray -> MolangColorExpression.serializer()
            is JsonPrimitive -> MolangColorLiteral.serializer()
            else -> throw SerializationException("Expected array or string literal.")
        }
}

internal object MolangColorLiteralSerializer : KSerializer<MolangColorLiteral> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): MolangColorLiteral {
        val v = decoder.decodeString().substring(1).padStart(8, 'f').toLong(16)
        val a = ((v shr 24) and 0xff).toUByte()
        val r = ((v shr 16) and 0xff).toUByte()
        val g = ((v shr 8) and 0xff).toUByte()
        val b = (v and 0xff).toUByte()
        return MolangColorLiteral(Color(r, g, b, a))
    }

    override fun serialize(encoder: Encoder, value: MolangColorLiteral) {
        val c = value.color
        encoder.encodeString("#%02x%02x%02x%02x".format(c.a.toInt(), c.r.toInt(), c.g.toInt(), c.b.toInt()))
    }
}

internal object MolangColorExpressionSerializer : KSerializer<MolangColorExpression> {
    private val inner = ListSerializer(Molang.serializer())
    override val descriptor: SerialDescriptor = inner.descriptor

    override fun deserialize(decoder: Decoder): MolangColorExpression {
        val list = decoder.decodeSerializableValue(inner)
        return MolangColorExpression(list[0], list[1], list[2], list.getOrNull(3) ?: ONE)
    }

    override fun serialize(encoder: Encoder, value: MolangColorExpression) {
        encoder.encodeSerializableValue(inner, listOfNotNull(
            value.r, value.g, value.b, value.a.takeUnless { it == ONE },
        ))
    }
}

internal object MolangGradientMapSerializer : KSerializer<TreeMap<Float, MolangColor>> {
    private val mapSerializer = TreeMap.serializer(Float.serializer(), MolangColor.serializer())
    private val listSerializer = ListSerializer(MolangColor.serializer())

    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): TreeMap<Float, MolangColor> {
        val input = decoder as JsonDecoder
        val tree = input.decodeJsonElement()

        return if (tree is JsonArray) {
            val list = input.json.decodeFromJsonElement(listSerializer, tree)
            TreeMap(list.withIndex().associate { (index, value) -> index.toFloat() / list.lastIndex to value })
        } else {
            input.json.decodeFromJsonElement(mapSerializer, tree)
        }
    }

    override fun serialize(encoder: Encoder, value: TreeMap<Float, MolangColor>) {
        if (value.keys.withIndex().all { (i, v) -> v == i.toFloat() / (value.size - 1) }) {
            encoder.encodeSerializableValue(listSerializer, value.values.toList())
        } else {
            encoder.encodeSerializableValue(mapSerializer, value)
        }
    }
}
