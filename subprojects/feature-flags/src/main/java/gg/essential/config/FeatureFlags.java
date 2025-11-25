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
package gg.essential.config;

import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Various feature flags which control the behavior of Essential.<br>
 * <br>
 * Before a public build is made, these will be propagated in the source code by feature-flags-processor and any
 * dead code will be eliminated.
 * Certain types of code (mixins, event handlers, command handler methods, etc.) are exempt from elimination because it
 * cannot know whether they are actually required. These can be manually annotated with {@link HideIfDisabled} if their
 * presence should be hidden in release builds.<br>
 * You may also apply {@link HideIfDisabled} to other targets to make sure they are actually eliminated if the
 * feature is disabled. The CI build will fail if any {@link HideIfDisabled} annotations for disabled features remain.
 * <br>
 * All feature flags may be overwritten at build time by setting corresponding environment variables (prefixed with
 * {@code ESSENTIAL_FEATURE_}, e.g. {@code ESSENTIAL_FEATURE_NEW_STUFF}) or the corresponding property in the
 * {@code features.properties} file (preferable because it can be committed to git and thereby allows for reproducible
 * builds) to either {@code true}, {@code false}, or a number between 0 and 100.<br>
 * If set to a number, the feature will be enabled for the given percentage of users based on their UUID.<br>
 * Additionally, if a feature flag is not forced to either true or false, it may be toggled at runtime by setting the
 * corresponding system property to {@code true} or {@code false} (e.g. {@code -Dessential.feature.new_stuff=true}).<br>
 */
public class FeatureFlags {

    private static final Logger LOGGER = LogManager.getLogger("Essential Logger");

    public static final String NEVER = "NEVER";


    // Add any features here that should be displayed in the FeaturesEnabledModal
    public static final Map<String, Pair<String, Boolean>> abTestingFlags = new HashMap<>();


}
