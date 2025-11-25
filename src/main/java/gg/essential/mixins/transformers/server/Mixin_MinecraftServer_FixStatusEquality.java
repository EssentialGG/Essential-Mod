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
package gg.essential.mixins.transformers.server;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import gg.essential.Essential;
import gg.essential.network.connectionmanager.sps.SPSManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

//#if MC>=11900
//$$ import java.util.Collections;
//$$ import java.util.List;
//$$ import net.minecraft.util.math.random.Random;
//#else
import java.util.Collections;
import java.util.List;
import java.util.Random;
//#endif

@Mixin(MinecraftServer.class)
public abstract class Mixin_MinecraftServer_FixStatusEquality {

    @ModifyArg(method =
                //#if MC>=11904
                //$$ "createMetadataPlayers"
                //#else
                "tick"
                //#endif
            , at = @At(value = "INVOKE", target =
                //#if MC>=11900
                //$$ "Lnet/minecraft/util/math/MathHelper;nextInt(Lnet/minecraft/util/math/random/Random;II)I"
                //#else
                "Lnet/minecraft/util/math/MathHelper;getInt(Ljava/util/Random;II)I"
                //#endif
            ))
    private Random modifyPlayerSampleIndexRandom(Random original, @Share("hosting") LocalBooleanRef hostingRef) {

        // establish if we are hosting via essential
        SPSManager spsManager = Essential.getInstance().getConnectionManager().getSpsManager();
        hostingRef.set(spsManager.getLocalSession() != null);

        if (!hostingRef.get()) return original;

        // always return the same seeded random when hosting to avoid status equality issues
        //#if MC>=11900
        //$$ return Random.create(0);
        //#else
        return new Random(0);
        //#endif
    }

    //#if MC>=11904
    //$$ @ModifyArg(method = "createMetadataPlayers", at = @At(value = "INVOKE",
    //$$         target =
        //#if MC>=12003
        //$$ "Lnet/minecraft/util/Util;shuffle(Ljava/util/List;Lnet/minecraft/util/math/random/Random;)V",
        //#else
        //$$ "Lnet/minecraft/util/Util;shuffle(Lit/unimi/dsi/fastutil/objects/ObjectArrayList;Lnet/minecraft/util/math/random/Random;)V",
        //#endif
    //$$         ordinal = 0))
    //$$ private Random modifyShuffleRandom(Random original, @Share("hosting") LocalBooleanRef hostingRef) {
    //$$     // always return the same seeded random when hosting to avoid status equality issues
    //$$     return hostingRef.get() ? Random.create(0) : original;
    //$$ }
    //#else
    // pre 1.19.4 vanilla uses a shuffle method without a random parameter
    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Collections;shuffle(Ljava/util/List;)V"))
    private boolean modifyShuffleRandom(final List<?> list, @Share("hosting") LocalBooleanRef hostingRef) {
        if (hostingRef.get()) {
            // shuffle consistently when hosting to avoid status equality issues
            Collections.shuffle(list, new java.util.Random(0));
            return false;
        }
        return true;
    }
    //#endif

}
