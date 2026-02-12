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
package gg.essential.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.util.VersionNumber;

import java.lang.reflect.Field;
import java.util.Map;

public class MixinUtils {
    private static final Logger logger = LogManager.getLogger();
    private static final VersionNumber MIXIN_0_8_2 = VersionNumber.parse("0.8.2");
    private static final String ESSENTIAL_NAMESPACE = "ESSENTIAL";

    @SuppressWarnings("unchecked")
    public static void registerInjectionPoint(Class<? extends InjectionPoint> clazz) {
        InjectionPoint.AtCode atCode = clazz.getAnnotation(InjectionPoint.AtCode.class);
        if (atCode == null) {
            throw new IllegalArgumentException("Injection point class " + clazz + " is not annotated with @AtCode");
        }

        if (VersionNumber.parse(MixinEnvironment.getCurrentEnvironment().getVersion()).compareTo(MIXIN_0_8_2) > 0) {
            InjectionPoint.register(clazz, ESSENTIAL_NAMESPACE);
        } else {
            // Versions prior to Mixin 0.8.3 do not support injection point namespaces.
            try {
                // We shouldn't need to worry about the reflection breaking because this is only the path for old versions of Mixin.
                Field typesField = InjectionPoint.class.getDeclaredField("types");
                typesField.setAccessible(true);
                Map<String, Class<? extends InjectionPoint>> types = (Map<String, Class<? extends InjectionPoint>>) typesField.get(null);
                types.put(ESSENTIAL_NAMESPACE + ':' + atCode.value(), clazz);
            } catch (Throwable t) {
                logger.error("Failed to register InjectionPoint class " + clazz + ':', t);
            }
        }
    }
}
