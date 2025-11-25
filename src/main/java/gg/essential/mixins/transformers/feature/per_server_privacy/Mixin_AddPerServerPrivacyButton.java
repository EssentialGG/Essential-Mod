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
package gg.essential.mixins.transformers.feature.per_server_privacy;

import gg.essential.elementa.font.DefaultFonts;
import kotlin.collections.CollectionsKt;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenAddServer;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static gg.essential.elementa.utils.TextKt.getStringSplitToWidth;
import static gg.essential.mixins.ext.client.multiplayer.ServerDataExtKt.getExt;
import static gg.essential.util.HelpersKt.buttonLiteral;

//#if MC>=12000
//$$ import net.minecraft.client.gui.DrawContext;
//#endif

//#if MC>=11700
//$$ import net.minecraft.client.gui.widget.CyclingButtonWidget;
//#endif

//#if MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import java.util.stream.Collectors;
//$$ import static gg.essential.util.HelpersKt.textLiteral;
//#endif

@Mixin(GuiScreenAddServer.class)
public abstract class Mixin_AddPerServerPrivacyButton extends GuiScreen {
    @Unique
    private GuiButton privacyButton;

    @Shadow
    @Final
    private ServerData serverData;

    // FIXME remap bug: even with a mapping declared for this, it is not automatically remapped
    //#if MC>=11600
    //$$ @Inject(method = "init", at = @At("RETURN"))
    //#else
    @Inject(method = "initGui", at = @At("RETURN"))
    //#endif
    private void initPrivacyButton(CallbackInfo ci) {
        List<GuiButton> buttonList = CollectionsKt.filterIsInstance(
            //#if MC>=11600
            //$$ this.getEventListeners(),
            //#else
            this.buttonList,
            //#endif
            GuiButton.class
        );

        //#if MC>=11700
        //$$ CyclingButtonWidget<?> resourcePacks = CollectionsKt.filterIsInstance(this.children(), CyclingButtonWidget.class).get(0);
        //#elseif MC>=11600
        //$$ Button resourcePacks = buttonList.get(0);
        //#else
        GuiButton resourcePacks = buttonList.get(2);
        //#endif

        // Move the vanilla resource packs button upwards to make space for our privacy button
        //#if MC>=11903
        //$$ resourcePacks.setY(resourcePacks.getY() - 16);
        //#else
        resourcePacks.y -= 16;
        //#endif

        // Clamp all buttons to a min y position so they do not overlay with the input fields
        int minY = 106 /* server address field y */ + 20 /* server address field height */ + 4 /* padding */;
        int resourcePacksY;
        //#if MC>=11903
        //$$ int offset = Math.max(minY - resourcePacks.getY(), 0);
        //$$ buttonList.get(0).setY(buttonList.get(0).getY() + offset);
        //$$ buttonList.get(1).setY(buttonList.get(1).getY() + offset);
        //$$ resourcePacks.setY(resourcePacksY = resourcePacks.getY() + offset);
        //#else
        int offset = Math.max(minY - resourcePacks.y, 0);

        //#if MC==11602
        // Only 1.16.2 is different, it reverts to the original indices in 1.17+ as the resource-pack button is changed
        //$$ buttonList.get(1).y += offset;
        //$$ buttonList.get(2).y += offset;
        //#else
        buttonList.get(0).y += offset;
        buttonList.get(1).y += offset;
        //#endif

        resourcePacksY = resourcePacks.y += offset;
        //#endif

        // Finally, add our privacy button right below the resource packs button
        int x = this.width / 2 - 100;
        int y = resourcePacksY + 20 + 4;
        //#if MC>=11903
        //$$ privacyButton = ButtonWidget.builder(buttonLiteral(""), this::handlePrivacyButton).dimensions(x, y, 200, 20).build();
        //#elseif MC>=11600
        //$$ privacyButton = new Button(x, y, 200, 20, buttonLiteral(""), this::handlePrivacyButton);
        //#else
        privacyButton = new GuiButton(-1, x, y, buttonLiteral(""));
        //#endif
        updatePrivacyButtonLabel();
        //#if MC>=11200
        // FIXME remap bug: adding a mapping for this breaks other Screen mappings
        //#if MC>=11700
        //$$ addDrawableChild(privacyButton);
        //#else
        addButton(privacyButton);
        //#endif
        //#else
        //$$ this.buttonList.add(privacyButton);
        //#endif
    }

    //#if MC<=11400
    @Inject(method = "actionPerformed", at = @At("RETURN"))
    private void handlePrivacyButton(GuiButton button, CallbackInfo ci) {
        handlePrivacyButton(button);
    }
    //#endif

    private void handlePrivacyButton(GuiButton button) {
        if (button == privacyButton) {
            Boolean state = getExt(this.serverData).getEssential$shareWithFriends();
            Boolean nextState = state == null ? Boolean.TRUE : state ? Boolean.FALSE : null;
            getExt(this.serverData).setEssential$shareWithFriends(nextState);
            updatePrivacyButtonLabel();
        }
    }

    @Unique
    private void updatePrivacyButtonLabel() {
        Boolean state = getExt(this.serverData).getEssential$shareWithFriends();
        String stateStr = state == null ? "Default" : state ? "On" : "Off";
        String label = "Show to Friends: " + stateStr;
        //#if MC>=11400
        //$$ privacyButton.setMessage(buttonLiteral(label));
        //#else
        privacyButton.displayString = buttonLiteral(label);
        //#endif
    }

    // FIXME remap bug: we have declared a mapping for this, it should automatically be remapped
    //#if MC>=11600
    //$$ @Inject(method = "render", at = @At("RETURN"))
    //#else
    @Inject(method = "drawScreen", at = @At("RETURN"))
    //#endif
    private void drawPrivacyButtonTooltip(
        //#if MC>=12000
        //$$ DrawContext context,
        //#elseif MC>=11600
        //$$ MatrixStack matrixStack,
        //#endif
        int mouseX, int mouseY, float partialTicks,
        CallbackInfo ci
    ) {
        if (privacyButton.isMouseOver()) {
            List<String> lines = getStringSplitToWidth(
                "When enabled, your Essential friends will see this server in the friends tab while you are " +
                    "playing on it and be able to join it.\n\nThe default option uses the global setting in the " +
                    "Essential settings.",
                width / 3f,
                1f,
                false,
                true,
                DefaultFonts.getVANILLA_FONT_RENDERER()
            );
            //#if MC>=11600
            //#if MC>=12000
            //$$ context.drawOrderedTooltip(this.textRenderer,
            //#else
            //$$ renderTooltip(matrixStack,
            //#endif
            //$$     lines.stream().map(str -> textLiteral(str).func_241878_f()).collect(Collectors.toList()), mouseX, mouseY);
            //#else
            drawHoveringText(lines, mouseX, mouseY);
            //#endif
        }
    }

    //#if MC>=11600
    //$$ Mixin_AddPerServerPrivacyButton() {
    //$$     super(null);
    //$$ }
    //#endif
}
