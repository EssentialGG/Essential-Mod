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
package gg.essential.asm.compat;

//#if MC<11400
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This transformer removes
 * <a href="https://github.com/DimensionalDevelopment/VanillaFix/blob/34e06730cc86c09fc813a1d35db4991e906e9177/src/main/java/org/dimdev/vanillafix/bugs/mixins/client/MixinIntegratedServer.java#L19">
 * a fix added by VanillaFix
 * </a>
 * for the integrated server shutdown race condition.
 * VanillaFix's solution is to just never ever wait on the future, which sounds like a recipe for creating race
 * conditions in other places.
 * We've already got a more robust fix of our own in the `workaroundRaceCondition` mixin, so we'll just drop the one
 * from VanillaFix.
 */
public class DisableVanillaFixIntegratedServerShutdownFixTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if ("org.dimdev.vanillafix.bugs.mixins.client.MixinIntegratedServer".equals(transformedName)) {
            ClassReader reader = new ClassReader(basicClass);
            ClassWriter writer = new ClassWriter(0);
            reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    if ("getUnchecked".equals(name)) {
                        return null;
                    }
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }
            }, 0);
            return writer.toByteArray();
        }
        return basicClass;
    }
}
//#endif