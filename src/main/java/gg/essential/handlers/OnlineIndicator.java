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
package gg.essential.handlers;

import gg.essential.config.EssentialConfig;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.cosmetics.EquippedCosmetic;
import gg.essential.cosmetics.IconCosmeticRenderer;
import gg.essential.data.OnboardingData;
import gg.essential.mixins.ext.client.network.NetHandlerPlayClientExt;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.UMinecraft;
import gg.essential.universal.UResolution;
import gg.essential.util.UDrawContext;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;

//#if MC>=11800 && MC<12100
//$$ import gg.essential.compat.ImmediatelyFastCompat;
//#endif

//#if MC>=11600
//$$ import net.minecraft.client.renderer.RenderType;
//$$ import net.minecraft.util.ResourceLocation;
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer;
//#else
import net.minecraft.util.ResourceLocation;
//#endif

import java.awt.*;
import java.util.Map;
import java.util.UUID;

import static gg.essential.util.HelpersKt.identifier;

public class OnlineIndicator {

    public static final ThreadLocal<Boolean> currentlyDrawingPlayerEntityName = ThreadLocal.withInitial(() -> false);
    //#if MC>=12109
    //$$ public static CosmeticsRenderState currentCosmeticsRenderState;
    //#endif
    //#if MC==11202
    public static Entity nametagEntity = null;
    //#endif


    /**
     * When called from a {@code drawNameplate} mixin, returns whether this nameplate is the primary name nameplate, as
     * opposed to e.g. the scoreboard score line.
     * @return {@code true} if this is the primary name nameplate
     */
    public static boolean currentlyDrawingPlayerEntityName() {
        return currentlyDrawingPlayerEntityName.get();
    }

    //#if MC<11600
    private static final ResourceLocation whiteTexture = new ResourceLocation("essential", "textures/white.png");
    //#endif

    public static void drawNametagIndicator(
        UMatrixStack matrixStack,
        //#if MC>=11600
        //$$ IRenderTypeBuffer vertexConsumerProvider,
        //#endif
        CosmeticsRenderState cState,
        String str,
        int light
    ) {
        IconCosmeticRenderer.INSTANCE.drawNameTagIconAndVersionConsistentPadding(
            matrixStack,
            //#if MC>=11600
            //$$ vertexConsumerProvider,
            //#endif
            cState, str, light);
    }

    // Patcher mixins into this at HEAD to cancel it if the background is disabled.
    // Do NOT move, rename or change the return type.
    public static int getTextBackgroundOpacity() {
        //#if MC>=11600
        //$$ return (int) (UMinecraft.getMinecraft().gameSettings.getTextBackgroundOpacity(0.25F) * 255);
        //#else
        return 64;
        //#endif
    }

    // Different name to avoid the OldAnimations mixin to `drawTabIndicator`.
    public static void drawTabIndicatorOuter(
        UDrawContext drawContext,
        NetworkPlayerInfo networkPlayerInfo,
        int x, int y
    ) {

        // draw the essential user indicator
        drawTabIndicator(drawContext, networkPlayerInfo, x, y);
    }

    private static final ResourceLocation TAB_LIST_ICON = identifier("essential", "textures/tab_list_icon.png");

    // this is modified in OldAnimations, don't remove or rename
    @SuppressWarnings("ConstantConditions")
    private static void drawTabIndicator(
        UDrawContext drawContext,
        NetworkPlayerInfo networkPlayerInfo,
        int x, int y
    ) {
        if (!OnboardingData.hasAcceptedTos() || !EssentialConfig.INSTANCE.getShowEssentialIndicatorOnTab() || networkPlayerInfo == null)
            return;

        EquippedCosmetic cosmetic = IconCosmeticRenderer.INSTANCE.getIconCosmetic(networkPlayerInfo);
        if (cosmetic == null) return;

        float centreX, centreY, size;
        if (UResolution.getScaleFactor() < 4) {
            // use 1:1 scale
            centreX = x - 7.5F;
            centreY = y + 1.5F;
            size = 5;
        } else {
            // use 3:4 scale
            centreX = x - 7.5F - 0.375F; // offset centre pos by 3 eighths of a pixel
            centreY = y + 1.5F - 0.375F;
            size = 5 * 0.75F;
        }

        IconCosmeticRenderer.INSTANCE.drawTextureInTabList(drawContext, centreX, centreY, TAB_LIST_ICON, size, false);
    }

    /**
     * Certain servers will use UUID v2s in the tab list to change the player's name to include prefixes, etc.
     * This means that we need to parse all the extra data out of the username in order to get their actual UUID.
     */
    public static UUID findUUIDFromDisplayName(ITextComponent displayName) {
        if (displayName == null) {
            return null;
        }

        NetHandlerPlayClient netHandler = UMinecraft.getNetHandler();
        if (netHandler == null) {
            return null;
        }

        Map<String, UUID> nameToIdCache = ((NetHandlerPlayClientExt) netHandler).essential$getNameIdCache();

        //#if MC>=11600
        //$$ String unformattedText = displayName.getString();
        //#else
        String unformattedText = displayName.getUnformattedText();
        //#endif

        // We need to replace any invalid username characters (e.g. `[`, `★`, etc.) with a space.
        // This will allow us to iterate over the parts of the name to see which one is the
        // user's actual name.
        // For example, `[18] ★★ caoimheee` will be split into `18` and `caoimheee`.
        String[] nameParts = unformattedText.replaceAll("\\W", " ").split(" ");
        for (String part : nameParts) {
            // Splitting by " " inserts some empty String elements into the array.
            if (part.isEmpty()) continue;

            if (nameToIdCache.containsKey(part)) {
                return nameToIdCache.get(part);
            }
        }

        return null;
    }

    public static void beforeTabDraw() {
        //#if MC>=11800 && MC<12100
        //$$ ImmediatelyFastCompat.beforeHudDraw();
        //#endif
    }

    public static void afterTabDraw() {
        //#if MC>=11800 && MC<12100
        //$$ ImmediatelyFastCompat.afterHudDraw();
        //#endif
    }
}
