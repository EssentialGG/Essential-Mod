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
package gg.essential.mixins.transformers.client.multiplayer;

import gg.essential.mixins.ext.client.multiplayer.ServerDataExt;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerData.class)
public abstract class MixinServerData implements ServerDataExt {
    private static final String KEY_SHARE_WITH_FRIENDS = "essential:shareWithFriends";

    @Unique
    private boolean isTrusted = true;

    @Unique
    private String pingRegion;

    @Unique
    private Long pingOverride;

    @Unique
    private boolean skipModCompatCheck;

    @Unique
    private Boolean shareWithFriends;

    @Unique
    private boolean showDownloadIcon;

    @Unique
    private String recommendedVersion;

    @Override
    public boolean getEssential$isTrusted() {
        return this.isTrusted;
    }

    @Override
    public void setEssential$isTrusted(boolean isTrusted) {
        this.isTrusted = isTrusted;
    }

    @Nullable
    @Override
    public String getEssential$pingRegion() {
        return this.pingRegion;
    }

    @Override
    public void setEssential$pingRegion(@Nullable String pingRegion) {
        this.pingRegion = pingRegion;
    }

    @Override
    public @Nullable Long getEssential$pingOverride() {
        return this.pingOverride;
    }

    @Override
    public void setEssential$pingOverride(@Nullable Long pingOverride) {
        this.pingOverride = pingOverride;
    }

    @Override
    public boolean getEssential$skipModCompatCheck() {
        return this.skipModCompatCheck;
    }

    @Override
    public void setEssential$skipModCompatCheck(boolean skipModCompatCheck) {
        this.skipModCompatCheck = skipModCompatCheck;
    }

    @Nullable
    @Override
    public Boolean getEssential$shareWithFriends() {
        return shareWithFriends;
    }

    @Override
    public void setEssential$shareWithFriends(@Nullable Boolean shareWithFriends) {
        this.shareWithFriends = shareWithFriends;
    }

    @Override
    public boolean getEssential$showDownloadIcon() {
        return this.showDownloadIcon;
    }

    @Override
    public void setEssential$showDownloadIcon(boolean showDownloadIcon) {
        this.showDownloadIcon = showDownloadIcon;
    }

    @Override
    public @Nullable String getEssential$recommendedVersion() {
        return this.recommendedVersion;
    }

    @Override
    public void setEssential$recommendedVersion(String recommendedVersion) {
        this.recommendedVersion = recommendedVersion;
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "copyFrom", at = @At("RETURN"))
    private void copyEssentialExt(ServerData from, CallbackInfo ci) {
        MixinServerData fromExt = (MixinServerData) (Object) from;
        this.isTrusted = fromExt.isTrusted;
        this.pingRegion = fromExt.pingRegion;
        this.pingOverride = fromExt.pingOverride;
        this.skipModCompatCheck = fromExt.skipModCompatCheck;
        this.shareWithFriends = fromExt.shareWithFriends;
        this.showDownloadIcon = fromExt.showDownloadIcon;
        this.recommendedVersion = fromExt.recommendedVersion;
    }

    @Inject(method = "getNBTCompound", at = @At("RETURN"))
    private void writeEssentialExt(CallbackInfoReturnable<NBTTagCompound> ci) {
        NBTTagCompound tag = ci.getReturnValue();
        if (shareWithFriends != null) {
            tag.setBoolean(KEY_SHARE_WITH_FRIENDS, shareWithFriends);
        }
    }

    @Inject(method = "getServerDataFromNBTCompound", at = @At("RETURN"))
    private static void readEssentialExt(NBTTagCompound tag, CallbackInfoReturnable<MixinServerData> ci) {
        MixinServerData serverData = ci.getReturnValue();
        if (tag.hasKey(KEY_SHARE_WITH_FRIENDS)) {
            //#if MC>=12105
            //$$ serverData.shareWithFriends = tag.getBoolean(KEY_SHARE_WITH_FRIENDS).orElse(false);
            //#else
            serverData.shareWithFriends = tag.getBoolean(KEY_SHARE_WITH_FRIENDS);
            //#endif
        }
    }
}
