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
package gg.essential.mixins.transformers.client.resources;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import gg.essential.Essential;
import gg.essential.handlers.GameProfileManager;
import gg.essential.mod.Skin;
import gg.essential.util.SkinKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Locale;

//#if MC>=11600
//$$ import com.google.common.hash.Hashing;
//#endif

/**
 * If a skin is already loaded, call the skin available callback immediately
 * to prevent the default skin from showing for a frame.
 */
@Mixin(SkinManager.class)
public class Mixin_InstantSkinLoad {

    @Inject(
        method = "loadProfileTextures",
        at = @At("HEAD")
    )
    private void essential$instantSkinLoad(GameProfile profile, SkinManager.SkinAvailableCallback callback, boolean requireSecure, CallbackInfo ci) {
        Skin skin;
        try {
            skin = SkinKt.gameProfileToSkin(profile);
        } catch (Exception e) {
            Essential.logger.warn("Failed to decode skin texture:", e);
            return;
        }

        if (skin != null) {
            //#if MC>=11600
            //$$ ResourceLocation location = new ResourceLocation("skins/" + Hashing.sha1().hashUnencodedChars(skin.getHash()));
            //#else
            ResourceLocation location = new ResourceLocation("skins/" + skin.getHash());
            //#endif

            //#if MC>=11700
            //$$ AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getOrDefault(location, null);
            //#else
            ITextureObject texture = Minecraft.getMinecraft().getTextureManager().getTexture(location);
            //#endif

            if (texture != null) {
                String url = String.format(Locale.ROOT, GameProfileManager.SKIN_URL, skin.getHash());
                // FIXME remap bug: doesn't remap, even with a mapping override (broken since it's an inner class?)
                //#if MC>=11600
                //$$ callback.onSkinTextureAvailable(
                //#else
                callback.skinAvailable(
                //#endif
                    MinecraftProfileTexture.Type.SKIN, location, new MinecraftProfileTexture(url, Collections.singletonMap("model", skin.getModel().getType())));
            }
        }
    }

}
