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
package gg.essential.mixins.transformers.client.network;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import gg.essential.Essential;
import gg.essential.handlers.GameProfileManager;
import gg.essential.mixins.impl.client.network.NetworkPlayerInfoExt;
import gg.essential.mod.Skin;
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

//#if MC>=12002
//$$ import net.minecraft.client.util.SkinTextures;
//$$ import java.util.function.Supplier;
//#endif

@Mixin(NetworkPlayerInfo.class)
public class Mixin_ApplyGameProfileOverrides {


    @Shadow
    @Final
    @Mutable
    private GameProfile gameProfile;

    //#if MC>=12002
    //$$ @Shadow
    //$$ @Final
    //$$ @Mutable
    //$$ private Supplier<SkinTextures> texturesSupplier;
    //$$
    //$$ @Shadow
    //$$ private static Supplier<SkinTextures> texturesSupplier(GameProfile par1) { throw new AssertionError(); }
    //#else
    @Shadow
    private boolean playerTexturesLoaded;

    //#if MC>=11200
    @Shadow
    Map<MinecraftProfileTexture.Type, ResourceLocation> playerTextures;
    //#else
    //$$ @Shadow
    //$$ private ResourceLocation locationSkin;
    //$$ @Shadow
    //$$ private ResourceLocation locationCape;
    //#endif
    //#endif

    //#if MC>=12002
    //$$ @Inject(method = "getSkinTextures", at = @At("HEAD"))
    //$$ public void getSkinTextures(CallbackInfoReturnable<SkinTextures> info) {
    //#else
    @Inject(method = {"getLocationSkin", "getLocationCape"}, at = @At("HEAD"))
    public void updateGameProfileTextures(CallbackInfoReturnable<ResourceLocation> info) {
    //#endif
        EquippedOutfitsManager manager = ((NetworkPlayerInfoExt) this).getEssential$equippedOutfitsManager();
        if (manager == null) manager = Essential.getInstance().getConnectionManager().getCosmeticsManager().getInfraEquippedOutfitsManager();
        Skin skin = manager.getSkin(this.gameProfile.getId());
        String equippedCapeHash = manager.getCapeHash(this.gameProfile.getId());
        GameProfile updatedProfile = GameProfileManager.handleGameProfile(this.gameProfile, skin, equippedCapeHash);
        if (updatedProfile != null) {
            this.gameProfile = updatedProfile;
            //#if MC>=12002
            //$$ this.texturesSupplier = texturesSupplier(profile);
            //#else
            //#if MC>=11200
            this.playerTextures.clear();
            //#else
            //$$ this.locationSkin = null;
            //$$ this.locationCape = null;
            //#endif
            this.playerTexturesLoaded = false;
            //#endif
        }
    }
}
