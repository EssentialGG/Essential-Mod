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
package gg.essential.mixins.transformers.client.entity;

import com.mojang.authlib.properties.Property;
import gg.essential.Essential;
import gg.essential.api.utils.JsonHolder;
import gg.essential.config.EssentialConfig;
import gg.essential.connectionmanager.common.packet.cosmetic.ClientCosmeticAnimationTriggerPacket;
import gg.essential.cosmetics.*;
import gg.essential.cosmetics.events.AnimationTarget;
import gg.essential.cosmetics.skinmask.MaskedSkinProvider;
import gg.essential.gui.common.EmulatedUI3DPlayer;
import gg.essential.gui.elementa.state.v2.MutableState;
import gg.essential.gui.elementa.state.v2.Observer;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.handlers.GameProfileManager;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.model.BedrockModel;
import gg.essential.model.PlayerMolangQuery;
import gg.essential.model.backend.PlayerPose;
import gg.essential.model.backend.minecraft.MinecraftRenderBackend;
import gg.essential.model.util.PlayerPoseManager;
import gg.essential.util.UIdentifier;
import gg.essential.util.UUIDUtil;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.collections.MapsKt;
import kotlin.jvm.functions.Function1;
import net.minecraft.client.entity.AbstractClientPlayer;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

import static gg.essential.cosmetics.EquippedOutfitsManagerMcKt.getEquippedOutfitsManager;
import static gg.essential.gui.elementa.state.v2.FlattenKt.flatten;
import static gg.essential.gui.elementa.state.v2.StateKt.memo;
import static gg.essential.gui.elementa.state.v2.StateKt.mutableStateOf;
import static gg.essential.network.cosmetics.ConversionsKt.toInfra;
import static gg.essential.util.Let.let;
import static gg.essential.util.UIdentifierKt.toMC;

//#if MC>=12109
//$$ import net.minecraft.util.AssetInfo;
//#endif

//#if MC>=12002
//$$ import com.llamalad7.mixinextras.injector.ModifyReturnValue;
//$$ import net.minecraft.client.util.SkinTextures;
//#endif

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer implements AbstractClientPlayerExt {

    @Unique
    private final PlayerMolangQuery molangQuery = new PlayerMolangQuery((AbstractClientPlayer) (Object) this);
    @Unique
    private final UUID cosmeticsSourceUuid = computeCosmeticsSourceUuid();
    @Unique
    private final boolean skinOverriddenByServer = let(((EntityPlayer) (Object) this).getGameProfile(), gameProfile -> {
        String values = GameProfileManager.getSafeTexturesValue(gameProfile);
        final JsonHolder root = new JsonHolder(new String(Base64.getDecoder().decode(values)));
        return !root.optString("profileId").isEmpty() && !gameProfile.getId().equals(UUIDUtil.formatWithDashes(root.optString("profileId")));
    });
    @Unique
    private final MutableState<State<Map<CosmeticSlot, EquippedCosmetic>>> cosmeticsSourceState = mutableStateOf(
        let(getEquippedOutfitsManager(this).getVisibleCosmeticsState(cosmeticsSourceUuid), cosmeticsSource -> {
            if (!skinOverriddenByServer) {
                return cosmeticsSource;
            } else {
                return memo((Function1<? super Observer, ? extends Map<CosmeticSlot, EquippedCosmetic>>) obs -> {
                    Map<CosmeticSlot, EquippedCosmetic> cosmetics = cosmeticsSource.get(obs);
                    if (skinOverriddenByServer && EssentialConfig.INSTANCE.getHideCosmeticsWhenServerOverridesSkinState().get(obs)) {
                        cosmetics = MapsKt.filterKeys(cosmetics, it -> it == CosmeticSlot.EMOTE || it == CosmeticSlot.ICON);
                    }
                    return cosmetics;
                });
            }
        })
    );
    @Unique
    private final State<Map<CosmeticSlot, EquippedCosmetic>> cosmeticsSource = flatten(cosmeticsSourceState);
    @Unique
    private WearablesManager wearablesManager;
    @Unique
    private final MaskedSkinProvider maskedSkinProvider = new MaskedSkinProvider();
    @Unique
    private String essentialCosmeticsCape;
    @Unique
    private Pair<List<UIdentifier>, @Nullable List<UIdentifier>> essentialCosmeticsCapeResources;

    @Unique
    private final boolean[] armorRenderingSuppressed = new boolean[4];

    @Unique
    private final PlayerPoseManager poseManager = new PlayerPoseManager(molangQuery);

    @Unique
    private boolean poseModified;

    @Unique
    private PlayerPose renderedPose;

    // mixin needs this to be able to correctly identify field initializers
    public MixinAbstractClientPlayer() {
    }

    //#if MC>=12002
    //$$ @Unique
    //$$ private String getModel() {
        //#if MC>=12109
        //$$ return this.getSkin().model().asString();
        //#else
        //$$ return this.getSkinTextures().model().getName();
        //#endif
    //$$ }
    //$$ @Shadow public abstract SkinTextures getSkinTextures();
    //#else
    @Shadow
    public abstract String getSkinType();
    //#endif

    @Override
    public UUID getCosmeticsSourceUuid() {
        return cosmeticsSourceUuid;
    }

    private UUID computeCosmeticsSourceUuid() {
            AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
            UUID uuid = self.getUniqueID();
            Collection<Property> properties = self.getGameProfile().getProperties().get("essential:real_uuid");
            for (Property property : properties) {
                String value = property.getValue();
                try {
                    uuid = UUIDUtil.formatWithDashes(value);
                } catch (IllegalArgumentException e) {
                    Essential.logger.warn("Failed to parse fake_player uuid \"" + value + "\" for " + self.getUniqueID(), e);
                }
            }
            return uuid;
    }

    @Override
    public State<Map<CosmeticSlot, EquippedCosmetic>> getCosmeticsSource() {
        return this.cosmeticsSource;
    }

    @Override
    public void setCosmeticsSource(State<Map<CosmeticSlot, EquippedCosmetic>> cosmeticsSource) {
        this.cosmeticsSourceState.set(cosmeticsSource);
    }

    @Override
    public @NotNull WearablesManager getWearablesManager() {
        if (this.wearablesManager == null) {
            AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

            Set<AnimationTarget> animationTargets;
            if (player instanceof EmulatedUI3DPlayer.EmulatedPlayer) {
                animationTargets = EnumSet.of(AnimationTarget.SELF, AnimationTarget.OTHERS);
            } else if (player instanceof EntityPlayerSP) {
                animationTargets = EnumSet.of(AnimationTarget.SELF);
            } else {
                animationTargets = EnumSet.of(AnimationTarget.OTHERS);
            }

            boolean sendsAnimationPackets =
                player.getUniqueID().equals(UUIDUtil.getClientUUID()) && !(player instanceof EmulatedUI3DPlayer.EmulatedPlayer);

            this.wearablesManager = new WearablesManager(MinecraftRenderBackend.INSTANCE, molangQuery, animationTargets, (cosmetic, event) -> {
                if (sendsAnimationPackets) {
                    CosmeticSlot slot = cosmetic.getType().getSlot();
                    Essential.getInstance().getConnectionManager()
                        .send(new ClientCosmeticAnimationTriggerPacket(toInfra(slot), event));
                }
                return Unit.INSTANCE;
            });
        }
        return this.wearablesManager;
    }

    @Override
    public @NotNull CosmeticsState getCosmeticsState() {
        return this.getWearablesManager().getState();
    }

    @Override
    public void setEssentialCosmeticsCape(@Nullable String cape, @Nullable Pair<List<UIdentifier>, @Nullable List<UIdentifier>> textures) {
        this.essentialCosmeticsCape = cape;
        this.essentialCosmeticsCapeResources = textures;
    }

    @Override
    public ResourceLocation applyEssentialCosmeticsMask(ResourceLocation skin) {
        if (EssentialModelRenderer.shouldRender((AbstractClientPlayer) (Object) this)) {
            ResourceLocation maskedSkin = maskedSkinProvider.provide(skin, getCosmeticsState().getSkinMask());
            if (maskedSkin != null) {
                return maskedSkin;
            }
        }
        return skin;
    }

    //#if MC>=12002
    //$$ @ModifyReturnValue(method = "getSkinTextures", at = @At("RETURN"))
    //$$ private SkinTextures overrideCapeIfSelectedInEssential(SkinTextures skinTextures) {
    //#if MC>=12109
    //$$     var asset = skinTextures.cape();
    //$$     var texture = asset != null ? asset.texturePath() : null;
    //$$     CallbackInfoReturnable<Identifier> ci = new CallbackInfoReturnable<>("", true, texture);
    //$$     overrideCapeIfSelectedInEssential(ci);
    //$$     if (ci.getReturnValue() != texture) {
    //$$         var newAsset = new AssetInfo.SkinAssetInfo(ci.getReturnValue(), null);
    //$$         skinTextures = new SkinTextures(skinTextures.body(), newAsset, skinTextures.elytra(), skinTextures.model(), skinTextures.secure());
    //$$     }
    //#else
    //$$     CallbackInfoReturnable<Identifier> ci = new CallbackInfoReturnable<>("", true, skinTextures.capeTexture());
    //$$     overrideCapeIfSelectedInEssential(ci);
    //$$     if (ci.getReturnValue() != skinTextures.capeTexture()) {
    //$$         skinTextures = new SkinTextures(skinTextures.texture(), skinTextures.textureUrl(), ci.getReturnValue(), skinTextures.elytraTexture(), skinTextures.model(), skinTextures.secure());
    //$$     }
    //#endif
    //$$     return skinTextures;
    //$$ }
    //$$ @Unique
    //#else
    @Inject(method = "getLocationCape", at = @At("HEAD"), cancellable = true)
    //#endif
    private void overrideCapeIfSelectedInEssential(CallbackInfoReturnable<ResourceLocation> ci) {
        if (!EssentialConfig.INSTANCE.getEssentialEnabled()) {
            // Don't override the cape's texture if Essential is disabled.
            return;
        }

        if (!EssentialModelRenderer.shouldRender((AbstractClientPlayer) (Object) this)) {
            return;
        }

        if (essentialCosmeticsCape != null) {
            Pair<List<UIdentifier>, @Nullable List<UIdentifier>> frames = this.essentialCosmeticsCapeResources;

            // Frames will be null if "Cape Disabled" is equipped
            if (frames == null) {
                ci.setReturnValue(null);
                return;
            }

            ci.setReturnValue(toMC(frames.getFirst().get(getCapeAnimationFrame(frames.getFirst().size()))));
        }
    }

    @Override
    public @Nullable UIdentifier getEmissiveCapeTexture() {
        if (essentialCosmeticsCape == null) {
            return null;
        }

        Pair<List<UIdentifier>, @Nullable List<UIdentifier>> frames = this.essentialCosmeticsCapeResources;
        if (frames == null) {
            return null;
        }
        List<UIdentifier> emissiveFrames = frames.getSecond();
        if (emissiveFrames == null) {
            return null;
        }

        return emissiveFrames.get(getCapeAnimationFrame(emissiveFrames.size()));
    }

    @Unique
    private int getCapeAnimationFrame(int frameCount) {
        if (frameCount == 1) {
            // fast path, no animation
            return 0;
        } else {
            // slow path, need to compute the current frame index
            float lifetime = new PlayerMolangQuery((AbstractClientPlayer) (Object) this).getLifeTime();
            int frame = (int) (lifetime * BedrockModel.TEXTURE_ANIMATION_FPS);
            return frame % frameCount;
        }
    }

    @Override
    public boolean[] wasArmorRenderingSuppressed() {
        return armorRenderingSuppressed;
    }

    @NotNull
    @Override
    public PlayerPoseManager getPoseManager() {
        return poseManager;
    }

    @Override
    public boolean isPoseModified() {
        return poseModified;
    }

    @Override
    public void setPoseModified(boolean poseModified) {
        this.poseModified = poseModified;
    }

    @Override
    @Nullable
    public PlayerPose getRenderedPose() {
        return renderedPose;
    }

    @Override
    public void setRenderedPose(PlayerPose renderedPose) {
        this.renderedPose = renderedPose;
    }

    @Unique
    private float cosmeticFrozenYaw = Float.NaN;

    @Override
    public float essential$getCosmeticFrozenYaw(){
        return cosmeticFrozenYaw;
    }

    @Override
    public void essential$setCosmeticFrozenYaw(float frozenYaw){
        cosmeticFrozenYaw = frozenYaw;
    }
}
