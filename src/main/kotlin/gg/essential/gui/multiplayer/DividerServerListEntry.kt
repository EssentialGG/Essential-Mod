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
package gg.essential.gui.multiplayer

import gg.essential.elementa.components.UIBlock
import gg.essential.gui.EssentialPalette
import gg.essential.mixins.ext.client.gui.essential
import gg.essential.mixins.ext.client.gui.ext
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import net.minecraft.client.gui.GuiMultiplayer

//#if MC>=12000
//$$ import net.minecraft.client.gui.DrawContext
//#endif

//#if MC>=11700
//#if MC<11900
//$$ import net.minecraft.text.LiteralText
//#endif
//$$ import net.minecraft.text.Text
//#endif

//#if MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack
//$$ import net.minecraft.client.gui.screen.ServerSelectionList
//#else
import net.minecraft.client.gui.GuiListExtended
//#endif

/**
 * A divider entry the server selection list.
 *
 * Dividers are added to the server selection list with [SelectionListWithDividers]. Because dividers
 * have a smaller height ([DIVIDER_ENTRY_HEIGHT]) than server entries, the positioning of entries
 * must be adjusted.
 * @see gg.essential.mixins.transformers.client.gui.Mixin_SelectionListDividers_GuiList
 * @see gg.essential.mixins.transformers.client.gui.Mixin_SelectionListDividers_GuiListExtended
 * @see gg.essential.mixins.transformers.client.gui.Mixin_SelectionListDividers_ServerSelectionList
 */
class DividerServerListEntry(
    private val owner: GuiMultiplayer,
    private val title: String,
    private val adIndicator: Boolean = false,
)
    //#if MC>=11600
    //$$ : ServerSelectionList.Entry()
    //#else
    : GuiListExtended.IGuiListEntry
    //#endif
{
    //#if MC>=12109
    //$$ override fun setHeight(height: Int) {
    //$$     super.setHeight(DIVIDER_ENTRY_HEIGHT)
    //$$ }
    //#else
    //$$ // Custom height is handled by Mixin_SelectionListDividers_GuiList(Extended) mixins
    //$$ // see also [ENTRY_HEIGHT_DIFFERENCE]
    //#endif

    override fun drawEntry(
        //#if MC>=12000
        //$$ drawContext: DrawContext,
        //#elseif MC>=11600
        //$$ mcMatrixStack: MatrixStack,
        //#endif
        //#if MC<12109
        slotIndex: Int,
        //#if MC>=11600
        //$$ y: Int,
        //$$ x: Int,
        //#else
        x: Int,
        y: Int,
        //#endif
        entryWidth: Int,
        entryHeight: Int,
        //#endif
        mouseX: Int,
        mouseY: Int,
        isSelected: Boolean,
        //#if MC>=11200
        partialTicks: Float
        //#endif
    ) {
        //#if MC>=12109
        //$$ val x = this.x
        //$$ val y = this.contentY
        //$$ val entryWidth = this.width
        //#endif
        //#if MC>=12106
        //$$ // FIXME This is a bit wasteful, but there should only be at most two of these at any one time.
        //$$ //       But once we have a more general DrawContext in UC/Elementa, we should get rid of this.
        //$$ gg.essential.util.AdvancedDrawContext.drawImmediate(drawContext) { matrixStack ->
        //#elseif MC>=12000
        //$$ val matrixStack = UMatrixStack(drawContext.matrices)
        //#elseif MC>=11600
        //$$ val matrixStack = UMatrixStack(mcMatrixStack)
        //#else
        val matrixStack = UMatrixStack.UNIT
        //#endif

        val textY = y + 4
        UGraphics.drawString(
            matrixStack,
            title,
            x.toFloat(),
            textY.toFloat(),
            EssentialPalette.TEXT_DISABLED.rgb,
            EssentialPalette.COMPONENT_BACKGROUND.rgb
        )

        val titleWidth = UGraphics.getStringWidth(title)
        val adTextWidth = UGraphics.getStringWidth(AD_TEXT)

        if (adIndicator) {
            val adTextX = x + entryWidth - adTextWidth - 5

            UGraphics.drawString(
                matrixStack, AD_TEXT,
                adTextX.toFloat(), textY.toFloat(),
                EssentialPalette.TEXT_DISABLED.rgb, EssentialPalette.COMPONENT_BACKGROUND.rgb
            )

            if (mouseX >= adTextX && mouseX <= adTextX + adTextWidth && mouseY >= textY && mouseY <= textY + 8) {
                owner.ext.essential.showTooltipString(
                    adTextX,
                    textY,
                    adTextWidth,
                    8,
                    "This placement has been paid for"
                )
            }
        }

        val rightPadding = if (adIndicator) adTextWidth + 6 + 5 else 5

        UIBlock.drawBlock(
            matrixStack, EssentialPalette.COMPONENT_BACKGROUND, (x + titleWidth + 6).toDouble(),
            (textY + 4).toDouble(), (x + entryWidth - rightPadding).toDouble(), (textY + 5).toDouble()
        )
        UIBlock.drawBlock(
            matrixStack, EssentialPalette.TEXT_DISABLED, (x + titleWidth + 5).toDouble(),
            (textY + 3).toDouble(), (x + entryWidth - rightPadding - 1).toDouble(), (textY + 4).toDouble()
        )
        //#if MC>=12106
        //$$ }
        //#endif
    }

    //#if MC>=11900
    //$$ override fun getNarration(): Text = Text.empty()
    //#elseif MC>=11700
    //$$ override fun getNarration(): Text = LiteralText.EMPTY
    //#endif

    //#if MC>=11600
    //$$ // Prevent selecting the divider entries
    //#if MC>=12109
    //$$ override fun mouseClicked(click: net.minecraft.client.gui.Click, doubled: Boolean): Boolean = false
    //#else
    //$$ override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = false
    //#endif
    //#else
    //#if MC>=11200
    override fun func_192633_a(i: Int, j: Int, k: Int, f: Float) {}
    //#else
    //$$ override fun setSelected(i: Int, j: Int, k: Int) {}
    //#endif

    override fun mousePressed(
        slotIndex: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: Int,
        relativeX: Int,
        relativeY: Int
    ): Boolean {
        return false
    }

    override fun mouseReleased(slotIndex: Int, x: Int, y: Int, mouseEvent: Int, relativeX: Int, relativeY: Int) {}
    //#endif

    //#if MC>=12109
    //$$ override fun connect() {}
    //$$ // this appears to be an "equals"-kind of function, used to keep selection on the same entry on refresh
    //$$ @Suppress("unused") // method, which is required, is package private, so we can't actually `override` it
    //$$ fun isOfSameType(entry: MultiplayerServerListWidget.Entry) = false
    //#endif

    companion object {
        const val SERVER_ENTRY_HEIGHT = 36
        const val DIVIDER_ENTRY_HEIGHT = 20
        const val ENTRY_HEIGHT_DIFFERENCE = SERVER_ENTRY_HEIGHT - DIVIDER_ENTRY_HEIGHT

        const val AD_TEXT = "[Ad]"
    }
}
