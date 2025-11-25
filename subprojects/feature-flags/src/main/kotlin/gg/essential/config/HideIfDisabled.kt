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
package gg.essential.config

/**
 * Ensures that the annotated element is removed from the class file at build time if the given feature flag is
 * disabled at build time. Fails the build if this is not possible.
 *
 *
 * Note that this cannot (and MUST NOT) be used to control behavior because it unconditionally removes the element
 * at build time only. It has no effect at runtime (it is in fact removed at build time) and the element will not be
 * removed if the feature flag is set to any value other than `false` (e.g. A/B testing) at build time.
 * The primary purpose of this annotation is to hide code which should not yet be visible to the general public.
 *
 * The secondary purpose of this annotation is to ensure that some new code is never by accident accessed without
 * checking the corresponding feature flag. It is common practice to apply it to some of your new data types / methods,
 * even if they do not need to be hidden explicitly, just to make sure the build fails if they are unexpectedly
 * retained.
 * Note that for this purpose it is not necessary to put this annotation on absolutely everything you add, especially
 * if that thing already inherently refers to another class/method/property which is already annotated; only the core
 * classes/methods are usually sufficient.
 *
 * Of special note is also usage of this annotation in combination with changes to packets:
 * It is highly recommended to apply this annotation to any new fields in existing packets (and to new packets).
 * This is because GSON will look at all fields of a Packet at runtime and try to deserialize them even if the feature
 * flag isn't enabled yet (it doesn't know about feature flags), so if the fields are not explicitly hidden from
 * production, one would need to already consider backwards compatibility with production clients for protocol changes
 * even before the proper release of the feature.
 */
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.EXPRESSION, // function/constructor call arguments only
    AnnotationTarget.TYPEALIAS,
)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class HideIfDisabled(
    /**
     * The name of a flag from [FeatureFlags].
     */
    val value: String
)
