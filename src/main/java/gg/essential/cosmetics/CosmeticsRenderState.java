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
package gg.essential.cosmetics;

import gg.essential.Essential;
import gg.essential.config.EssentialConfig;
import gg.essential.connectionmanager.common.enums.ProfileStatus;
import gg.essential.gui.common.EmulatedUI3DPlayer;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import gg.essential.mixins.impl.client.renderer.entity.ArmorRenderingUtil;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.model.ModelInstance;
import gg.essential.model.backend.PlayerPose;
import gg.essential.model.util.PlayerPoseManager;
import gg.essential.network.connectionmanager.profile.ProfileManager;
import gg.essential.util.UIdentifier;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static gg.essential.util.UIdentifierKt.toMC;

public interface CosmeticsRenderState {
    @Nullable WearablesManager wearablesManager(); // TODO should capture render state only
    @Nullable PlayerPoseManager poseManager(); // TODO should capture render state only

    Set<Integer> blockedArmorSlots();

    /** The player skin texture WITHOUT our skin mask applied (used by cosmetics/emotes to e.g. create fake limbs). */
    ResourceLocation skinTexture();

    ResourceLocation emissiveCapeTexture();

    boolean onlineIndicator();
    ModelInstance nametagIcon();

    boolean isSneaking();

    void setRenderedPose(PlayerPose pose);
    void setPoseModified(boolean poseModified);
    void setSuppressedArmor(boolean[] slots);

    float cosmeticFrozenYaw();
    void setCosmeticFrozenYaw(float frozenYaw);

    /**
     * Implementation which delegates to the underlying entity.
     * Cheap to construct but not thread safe.
     * Used primarily on older MC versions and on non-standard rendering paths.
     */
    final class Live implements CosmeticsRenderState {
        private final AbstractClientPlayer player;

        public Live(AbstractClientPlayer player) {
            this.player = player;
        }

        private AbstractClientPlayerExt playerExt() {
            return (AbstractClientPlayerExt) player;
        }

        @Override
        public WearablesManager wearablesManager() {
            return EssentialModelRenderer.shouldRender(player) ? playerExt().getWearablesManager() : null;
        }

        @Override
        public PlayerPoseManager poseManager() {
            if (EssentialConfig.INSTANCE.getDisableEmotes() && !(player instanceof EmulatedUI3DPlayer.EmulatedPlayer)) {
                return null;
            }
            return playerExt().getPoseManager();
        }

        @Override
        public Set<Integer> blockedArmorSlots() {
            if (!EssentialModelRenderer.shouldRender(player)) {
                return Collections.emptySet();
            }
            EssentialConfig.CosmeticOrArmor armorHidingSetting = ArmorRenderingUtil.getCosmeticArmorSetting(player);
            if (armorHidingSetting != EssentialConfig.CosmeticOrArmor.ONLY_COSMETICS) {
                return Collections.emptySet();
            }
            return playerExt().getCosmeticsState().getPartsEquipped();
        }

        @Override
        public ResourceLocation skinTexture() {
            //#if MC>=12109
            //$$ return player.getSkin().body().texturePath();
            //#elseif MC>=12002
            //$$ return player.getSkinTextures().texture();
            //#else
            return player.getLocationSkin();
            //#endif
        }

        @Override
        public ResourceLocation emissiveCapeTexture() {
            if (!EssentialModelRenderer.shouldRender(player)) {
                return null;
            }
            UIdentifier identifier = playerExt().getEmissiveCapeTexture();
            if (identifier == null) {
                return null;
            }
            return toMC(identifier);
        }

        @Override
        public boolean onlineIndicator() {
            if (!EssentialConfig.INSTANCE.getShowEssentialIndicatorOnNametag()) return false;
            ProfileManager profileManager = Essential.getInstance().getConnectionManager().getProfileManager();
            UUID uuid = player.getGameProfile().getId();
            ProfileStatus status = profileManager.getStatus(uuid);
            return status != ProfileStatus.OFFLINE;
        }

        @Override
        public ModelInstance nametagIcon() {
            if (!EssentialConfig.INSTANCE.getShowEssentialIndicatorOnNametag()) return null;
            EquippedCosmetic cosmetic = playerExt().getCosmeticsState().getCosmetics().get(CosmeticSlot.ICON);
            if (cosmetic == null) return null;
            return playerExt().getWearablesManager().getModels().get(cosmetic.getCosmetic());
        }

        @Override
        public boolean isSneaking() {
            return player.isSneaking();
        }

        @Override
        public void setRenderedPose(PlayerPose pose) {
            playerExt().setRenderedPose(pose);
        }

        @Override
        public void setPoseModified(boolean poseModified) {
            playerExt().setPoseModified(poseModified);
        }

        @Override
        public void setSuppressedArmor(boolean[] slots) {
            System.arraycopy(slots, 0, playerExt().wasArmorRenderingSuppressed(), 0, slots.length);
        }

        @Override
        public float cosmeticFrozenYaw() {
            return playerExt().essential$getCosmeticFrozenYaw();
        }

        @Override
        public void setCosmeticFrozenYaw(final float frozenYaw) {
            playerExt().essential$setCosmeticFrozenYaw(frozenYaw);
        }
    }

    /**
     * Implementation which snapshots the state of the entity on {@link #update}.
     * Relatively expensive to construct but safe to use from any thread afterwards.
     */
    final class Snapshot implements CosmeticsRenderState {
        private Live live; // only for smuggling the rendered player pose out of the renderer!
        private WearablesManager wearablesManager;
        private PlayerPoseManager poseManager;
        private Set<Integer> blockedArmorSlots = Collections.emptySet();
        private ResourceLocation skinTexture;
        private ResourceLocation emissiveCapeTexture;
        private boolean onlineIndicator;
        private ModelInstance nametagIcon;
        private boolean isSneaking;
        private float cosmeticFrozenYaw;

        @Override
        public WearablesManager wearablesManager() {
            return wearablesManager;
        }

        @Override
        public PlayerPoseManager poseManager() {
            return poseManager;
        }

        @Override
        public Set<Integer> blockedArmorSlots() {
            return blockedArmorSlots;
        }

        @Override
        public ResourceLocation skinTexture() {
            return skinTexture;
        }

        @Override
        public ResourceLocation emissiveCapeTexture() {
            return emissiveCapeTexture;
        }

        @Override
        public boolean onlineIndicator() {
            return onlineIndicator;
        }

        @Override
        public ModelInstance nametagIcon() {
            return nametagIcon;
        }

        @Override
        public float cosmeticFrozenYaw() {
            return cosmeticFrozenYaw;
        }

        @Override
        public boolean isSneaking() {
            return isSneaking;
        }

        public void update(AbstractClientPlayer entity) {
            Live live = new Live(entity);
            this.live = live;
            wearablesManager = live.wearablesManager();
            poseManager = live.poseManager();
            blockedArmorSlots = live.blockedArmorSlots();
            skinTexture = live.skinTexture();
            emissiveCapeTexture = live.emissiveCapeTexture();
            onlineIndicator = live.onlineIndicator();
            nametagIcon = live.nametagIcon();
            isSneaking = live.isSneaking();
            cosmeticFrozenYaw = live.cosmeticFrozenYaw();
        }

        @Override
        public void setRenderedPose(PlayerPose pose) {
            Live live = this.live;
            if (live != null) {
                live.setRenderedPose(pose);
            }
        }

        @Override
        public void setPoseModified(boolean poseModified) {
            Live live = this.live;
            if (live != null) {
                live.setPoseModified(poseModified);
            }
        }

        @Override
        public void setSuppressedArmor(boolean[] slots) {
            Live live = this.live;
            if (live != null) {
                live.setSuppressedArmor(slots);
            }
        }

        @Override
        public void setCosmeticFrozenYaw(final float frozenYaw) {
            Live live = this.live;
            if (live != null) {
                live.setCosmeticFrozenYaw(frozenYaw);
            }
        }
    }
}
