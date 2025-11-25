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
package gg.essential.mixins.transformers.client.renderer.entity;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import gg.essential.config.EssentialConfig;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.cosmetics.WearablesManager;
import gg.essential.gui.common.UI3DPlayer;
import gg.essential.mixins.impl.client.gui.GuiInventoryExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC<=10809
//$$ import net.minecraft.client.renderer.entity.RendererLivingEntity;
//#else
import net.minecraft.client.renderer.entity.RenderLivingBase;
//#endif

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.render.entity.state.LivingEntityRenderState;
//#endif

//#if MC>=11700
//$$ import net.minecraft.client.render.entity.EntityRendererFactory;
//#endif

//#if MC<=10809
//$$ @Mixin(value = RendererLivingEntity.class, priority = 500)
//#else
@Mixin(value = RenderLivingBase.class, priority = 500)
//#endif

//#if MC>=12102
//$$ public abstract class MixinRendererLivingEntity<T extends LivingEntity, S extends LivingEntityRenderState> extends EntityRenderer<T, S> {
//#else
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> extends Render<T> {
//#endif
    protected MixinRendererLivingEntity() {
        super(null);
    }

    //#if MC>=12102
    //$$ @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    //$$ private void canRenderNameOfEmulatedPlayer(T entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> ci) {
    //#else
    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("HEAD"), cancellable = true)
    private void canRenderNameOfEmulatedPlayer(T entity, CallbackInfoReturnable<Boolean> ci) {
    //#endif
        UI3DPlayer component = UI3DPlayer.current;
        if (component != null) {
            ci.setReturnValue(!component.getHideNameTags().get());
            return;
        }

        if (EssentialConfig.INSTANCE.getEssentialEnabled()
                && EssentialConfig.INSTANCE.getShowOwnNametag().getUntracked()
                //#if MC>=11600
                //$$ && entity == this.renderManager.info.getRenderViewEntity()
                //#else
                && entity == this.renderManager.renderViewEntity
                //#endif
                && Minecraft.isGuiEnabled()
                && !GuiInventoryExt.isInventoryEntityRendering.getUntracked()
        ) {
            // return if we have a team that hides their own nametag
            Team team = entity.getTeam();
            if (team != null && (team.getNameTagVisibility() == Team.EnumVisible.HIDE_FOR_OWN_TEAM
                    || team.getNameTagVisibility() == Team.EnumVisible.NEVER)) return;

            ci.setReturnValue(true);
        }
    }

    //#if MC>=11600
    //$$ // Does not apply. Lighting state is now passed via arguments and therefore well scoped.
    //#else
    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableTexture2D()V", shift = At.Shift.AFTER))
    private void restoreMenuLighting(CallbackInfo ci) {
        UI3DPlayer component = UI3DPlayer.current;
        if (component != null) {
            GlStateManager.disableTexture2D();
        }
    }
    //#endif

    @Inject(method = "applyRotations", at = @At(value = "HEAD"))
    private void freezeYawIfEquippedCosmeticRequires(CallbackInfo ci,
                      //#if MC>=12102
                      //$$ @Local(ordinal = 0, argsOnly = true) S state,
                      //$$ @Local(ordinal = 0, argsOnly = true) LocalFloatRef yaw) { // Beware: Yarn parameter names are wrong in 1.21.2, this is the correct target for yaw
                      //#else
                      @Local(argsOnly = true) T entity,
                      @Local(ordinal = 1, argsOnly = true) LocalFloatRef yaw) {
                      //#endif

        //#if MC>=12102
        //$$ if (!(state instanceof PlayerEntityRenderStateExt)) return;
        //$$ CosmeticsRenderState cState = ((PlayerEntityRenderStateExt) state).essential$getCosmetics();
        //#else
        if (!(entity instanceof AbstractClientPlayer)) return;
        CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayer) entity);
        //#endif

        WearablesManager wearablesManager = cState.wearablesManager();
        if (wearablesManager == null) return;

        if (UI3DPlayer.current != null) {
            // For rendering the first person emote preview, the player is temporarily rotated to face the human that's
            // sitting in front of the screen. We don't want to overwrite that.
            return;
        }

        if (wearablesManager.getState().getLocksPlayerRotation()) {
            float frozenYaw = cState.cosmeticFrozenYaw();
            // this may result in yaw de-sync between clients if one client only looks at the locked player partway through the lock
            // but that isn't resolvable without actually modifying the real player rotation sent to the server
            if (Float.isNaN(frozenYaw)) {
                // set the value to use for next (frozen) frame, yaw is unchanged this frame
                cState.setCosmeticFrozenYaw(yaw.get());
            } else {
                yaw.set(frozenYaw);
            }
            return;
        }

        // Reset yaw stored in the player
        cState.setCosmeticFrozenYaw(Float.NaN);
    }
}