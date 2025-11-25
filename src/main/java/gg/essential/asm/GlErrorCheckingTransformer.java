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
package gg.essential.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.charset.StandardCharsets;

public class GlErrorCheckingTransformer implements MixinTransformerWrapper.Transformer {
    //#if MC>=11700 || FABRIC
    //$$ private static final int ASM_VERSION = Opcodes.ASM9;
    //#elseif MC>=11600
    //$$ private static final int ASM_VERSION = Opcodes.ASM7;
    //#else
    private static final int ASM_VERSION = Opcodes.ASM5;
    //#endif

    private static final String OPENGL_PACKAGE = "org.lwjgl.opengl.";
    private static final String OPENGL_PACKAGE_JVM = OPENGL_PACKAGE.replace('.', '/');
    private static final byte[] OPENGL_PACKAGE_JVM_BYTES = OPENGL_PACKAGE_JVM.getBytes(StandardCharsets.UTF_8);

    private static final String GL11_JVM = OPENGL_PACKAGE_JVM + "GL11";

    private static final String GL_DEBUG = "gg.essential.util.GlDebug";
    private static final String GL_DEBUG_JVM = GL_DEBUG.replace('.', '/');
    private static final String IN_BEGIN_END_PAIR = "inBeginEndPair";
    private static final String CHECK = "checkGlError";
    private static final String CHECK_DESC = "(Ljava/lang/String;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        // Quickly scan the bytecode for any references to OpenGL classes, so we can skip parsing for most classes
        if (bytes == null || !contains(bytes, OPENGL_PACKAGE_JVM_BYTES)) {
            return bytes;
        }
        // Skip processing inner classes of this transformer, that would result in a chicken or egg problem
        if (name.startsWith(GlErrorCheckingTransformer.class.getName())) {
            return bytes;
        }
        // Skip processing our GlDebug class, otherwise our error checking method would just keep calling itself
        if (name.startsWith(GL_DEBUG)) {
            return bytes;
        }
        // Skip processing the OpenGL classes themselves, otherwise glGetError (and others) would be recursive
        // (this is required for Fabric; on LaunchWrapper all of LWJGL is already excluded globally)
        if (name.startsWith(OPENGL_PACKAGE)) {
            return bytes;
        }

        ClassReader classReader = new ClassReader(bytes);
        ClassWriter classWriter = new ClassWriter(0);
        classReader.accept(new ClassVisitor(ASM_VERSION, classWriter) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(ASM_VERSION, super.visitMethod(access, name, desc, signature, exceptions)) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        // We only care about calls that look like they might be GL calls
                        // (there'll be some false positives but that should be fine)
                        if (opcode != Opcodes.INVOKESTATIC || !owner.startsWith(OPENGL_PACKAGE_JVM)) {
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                            return;
                        }

                        // Check for errors before we do the GL call. We don't expect this to be hit, it's more of a
                        // fail safe in case an error was emitted by a call we for some reason weren't able to
                        // instrument.
                        super.visitLdcInsn("**BEFORE** " + name);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, GL_DEBUG_JVM, CHECK, CHECK_DESC, false);

                        // Original GL call
                        super.visitMethodInsn(opcode, owner, name, desc, itf);

                        // Need to track glBegin/glEnd because calling glGetError isn't allowed while inside those
                        if (owner.equals(GL11_JVM) && name.equals("glBegin")) {
                            super.visitLdcInsn(true);
                            super.visitFieldInsn(Opcodes.PUTSTATIC, GL_DEBUG_JVM, IN_BEGIN_END_PAIR, "Z");
                        }
                        if (owner.equals(GL11_JVM) && name.equals("glEnd")) {
                            super.visitLdcInsn(false);
                            super.visitFieldInsn(Opcodes.PUTSTATIC, GL_DEBUG_JVM, IN_BEGIN_END_PAIR, "Z");
                        }

                        // Check for errors after the GL call
                        super.visitLdcInsn(name);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, GL_DEBUG_JVM, CHECK, CHECK_DESC, false);
                    }

                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        super.visitMaxs(maxStack + 1, maxLocals);
                    }
                };
            }
        }, 0);
        return classWriter.toByteArray();
    }

    private static boolean contains(byte[] array, byte[] needle) {
        outer:
        for (int i = 0; i < array.length - needle.length + 1; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (array[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}
