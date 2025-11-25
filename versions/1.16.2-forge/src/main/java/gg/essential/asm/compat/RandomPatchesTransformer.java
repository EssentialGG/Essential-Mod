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

import gg.essential.asm.EssentialTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.util.Annotations;

import java.util.HashSet;
import java.util.Set;

/**
 * RandomPatches has multiple keybinding features which mix into the `equals/conflicts` method
 * to check if other keybinds use the same key. This tries to load vanilla keybinds from the
 * GameOptions/GameSettings class which does not exist when Essential loads its keybinds.
 * To solve this, we add a null check so it skips conflict checking when Essential registers
 * its keybinds.
 * <a href="https://github.com/TheRandomLabs/RandomPatches/blob/a2a9f19caab38f3f4078bf5acadcca7e300d4788/src/main/java/com/therandomlabs/randompatches/mixin/client/keybindings/KeyBindingMixin.java#L82">Link to Mixin</a>
 */
public class RandomPatchesTransformer implements EssentialTransformer {
    private static final boolean isDev = isDevelopment();
    //#if FABRIC
    //$$ private static final String minecraftClient = isDev ? "net/minecraft/client/MinecraftClient" : "net/minecraft/class_310";
    //$$ private static final String getInstanceName = isDev ? "getInstance" : "method_1551";
    //$$ private static final String gameOptionsDescriptor = isDev ? "Lnet/minecraft/client/options/GameOptions;" : "Lnet/minecraft/class_315;";
    //$$ private static final String gameOptionsFieldName = isDev ? "options" : "field_1690";
    //#else
    private static final String minecraftClient = "net/minecraft/client/Minecraft";
    private static final String getInstanceName = isDev ? "getInstance" : "func_71410_x";
    private static final String gameOptionsDescriptor = "Lnet/minecraft/client/GameSettings;";
    private static final String gameOptionsFieldName = isDev ? "gameSettings" : "field_71474_y";
    //#endif

    private static final String mixinMergedDescriptor = "Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;";
    private static final String randomPatchesMixinName = "com.therandomlabs.randompatches.mixin.client.keybindings.KeyBindingMixin";

    @Override
    public Set<String> getTargets() {
        return new HashSet<String>() {{
            //#if FABRIC
            //$$ add("net.minecraft.client.options.KeyBinding");
            //$$ add("net.minecraft.class_304");
            //#else
            add("net.minecraft.client.settings.KeyBinding");
            //#endif
        }};
    }

    @Override
    public void preApply(ClassNode classNode) {}

    @Override
    public void postApply(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            if (!isRandomPatchesConflictMixin(methodNode)) continue;
            methodNode.instructions.insert(checkOptionsNotNull());
        }
    }

    private static boolean isRandomPatchesConflictMixin(MethodNode methodNode) {
        if (methodNode.visibleAnnotations == null || !methodNode.name.contains("conflicts")) return false;
        for (AnnotationNode annotation : methodNode.visibleAnnotations) {
            if (!annotation.desc.equals(mixinMergedDescriptor)) continue;
            if (randomPatchesMixinName.equals(Annotations.getValue(annotation, "mixin"))) {
                return true;
            }
        }
        return false;
    }

    private InsnList checkOptionsNotNull() {
        InsnList list = new InsnList();
        LabelNode label = new LabelNode();
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, minecraftClient, getInstanceName, "()L" + minecraftClient  + ";"));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, minecraftClient, gameOptionsFieldName, gameOptionsDescriptor));
        list.add(new JumpInsnNode(Opcodes.IFNONNULL, label));
        list.add(new InsnNode(Opcodes.RETURN));
        list.add(label);
        return list;
    }

    private static boolean isDevelopment() {
        //#if FABRIC
        //$$ return net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
        //#else
        String target = System.getenv("target");
        if (target == null) return false;
        return target.equalsIgnoreCase("fmluserdevclient");
        //#endif
    }
}
