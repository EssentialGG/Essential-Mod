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
package gg.essential.gui.sps

import gg.essential.elementa.components.GradientComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.constraints.FillConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.boundTo
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.coerceAtMost
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.effect
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixel
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.elementa.dsl.toConstraint
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.effect.HorizontalScissorEffect
import gg.essential.gui.common.modal.SearchableConfirmDenyModal
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.common.shadow.ShadowEffect
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.outline
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.whenHovered
import gg.essential.gui.overlay.ModalManager
import gg.essential.universal.UMinecraft
import gg.essential.util.GuiUtil
import gg.essential.util.centered
import gg.essential.util.findChildrenOfType
import gg.essential.gui.util.hoveredState
import gg.essential.handlers.PauseMenuDisplay
import gg.essential.network.connectionmanager.sps.SPSSessionSource
import gg.essential.util.formatDate
import gg.essential.vigilance.utils.onLeftClick
import net.minecraft.client.gui.GuiCreateWorld
import net.minecraft.world.storage.WorldSummary
import java.awt.Color
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.*

//#if MC<=11809
//$$ import gg.essential.util.getLevelNbtValue
//#endif

//#if MC>=11900
//$$ import net.minecraft.client.MinecraftClient
//#endif

class WorldSelectionModal(modalManager: ModalManager) : SearchableConfirmDenyModal(
    modalManager,
    requiresButtonPress = false,
    searchbarPadding = 8f,
) {

    private val selectedWorld: State<WorldSummary?> = BasicState(null)

    init {
        titleText = "Select world to host"
        primaryButtonText = "Next"
        bindConfirmAvailable(selectedWorld.map { it != null })

        //#if MC>=11900
        //$$ val worldSummaries = UMinecraft.getMinecraft().levelStorage.loadSummaries(UMinecraft.getMinecraft().levelStorage.levelList).get()
        //#else
        val worldSummaries = UMinecraft.getMinecraft().saveLoader.saveList
        //#endif
            .sortedByDescending { it.lastTimePlayed }

        if (worldSummaries.isEmpty()) {
            hideSearchbar()
        }

        val searchState = searchBarTextState
        val filteredWorlds = stateBy {
            val search = searchState()
            worldSummaries.filter { it.displayName.contains(search, ignoreCase = true) }
        }.toListState()

        scroller.layout {
            column(Modifier.fillWidth()) {
                if_(filteredWorlds.map { it.isEmpty() }) {
                    spacer(height = 6f)

                    column(Modifier.fillWidth(), Arrangement.spacedBy(10f)) {
                        text("No worlds found.", modifier = Modifier.color(EssentialPalette.TEXT_DISABLED).shadow(EssentialPalette.COMPONENT_BACKGROUND))

                        createNewWorldEntry()
                    }
                } `else` {
                    column(Modifier.fillWidth(), Arrangement.spacedBy(2f)) {
                        createNewWorldEntry()

                        forEach(filteredWorlds, cache = true) { worldSummary ->
                            entry(worldSummary)
                        }
                    }
                    // Add extra padding for shadow to display correctly at bottom of scroller
                    spacer(height = 1f)
                }
            }
        }

        selectedWorld.set(worldSummaries.firstOrNull())

        // Remove original scissor effect to allow shadows to be displayed correctly
        scrollContainer.removeEffect<ScissorEffect>()
        scroller.removeEffect<ScissorEffect>()
        scrollContainer.effect(HorizontalScissorEffect())
        // Extend gradient to cover shadows on the side of the scroller
        for (gradientComponent in scroller.findChildrenOfType<GradientComponent>()) {
            gradientComponent.constrain {
                width += 1.pixel
            }
        }

        onPrimaryAction {
            selectedWorld.get()?.let { world ->
                PauseMenuDisplay.showInviteOrHostModalInternal(
                    SPSSessionSource.MAIN_MENU,
                    previousModal = this,
                    worldSummary = world,
                )
            }
        }
    }

    private fun LayoutScope.createNewWorldEntry() {
        var modifier = Modifier.fillWidth()
        modifier = modifier.shadow(Color.BLACK)

        box(modifier
            .height(24f)
            .hoverScope()
            .whenHovered(
                Modifier
                    .color(EssentialPalette.GRAY_BUTTON_HOVER)
                    .outline(EssentialPalette.GRAY_OUTLINE_BUTTON_OUTLINE_HOVER, 1f, drawInsideChildren = true),
                Modifier
                    .color(EssentialPalette.GRAY_BUTTON)
                    .outline(EssentialPalette.GRAY_BUTTON_HOVER, 1f, drawInsideChildren = true)
            )
        ) {
            text("Create New World", Modifier
                .color(EssentialPalette.TEXT_HIGHLIGHT)
                .hoverColor(EssentialPalette.TEXT_HIGHLIGHT)
                .shadow(EssentialPalette.COMPONENT_BACKGROUND)
            )
        }.onLeftClick {
            // `CreateWorldScreen` does some preparation when it's first opened, causing the game thread to
            // freeze. This means that the modal will be shown until this process is finished.
            // To remedy this, we can just close the modal before showing the create world screen.
            this@WorldSelectionModal.close()
            //#if MC>=12109
            //$$ val mc = MinecraftClient.getInstance()
            //$$ val prevScreen = mc.currentScreen
            //$$ CreateWorldScreen.show(mc) { mc.setScreen(prevScreen) }
            //#elseif MC>=11900
            //$$ CreateWorldScreen.create(MinecraftClient.getInstance(), GuiUtil.openedScreen())
            //#else
            GuiUtil.openScreen {
                //#if MC>=11600
                //$$ CreateWorldScreen.func_243425_a(GuiUtil.openedScreen())
                //#else
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // The parent screen can be nullable
                GuiCreateWorld(GuiUtil.openedScreen())
                //#endif
            }
            //#endif
        }
    }

    private fun LayoutScope.entry(worldSummary: WorldSummary, modifier: Modifier = Modifier) {
        WorldEntry(selectedWorld, worldSummary).onLeftClick {
            if (selectedWorld.get() == worldSummary) {
                primaryButtonAction?.invoke()
            } else {
                selectedWorld.set(worldSummary)
            }
        }(modifier)
    }

    class WorldEntry(selectedWorld: State<WorldSummary?>, val worldSummary: WorldSummary) : UIBlock() {

        private val selected = selectedWorld.map { it == worldSummary }

        private val content by UIContainer().centered().constrain {
            width = 100.percent - 12.pixels
            height = 100.percent - 12.pixels
        } childOf this

        private val icon =
            //#if MC>=11900
            //$$ worldSummary.iconPath.toFile()
            //#elseif MC>=11602
            //$$ worldSummary.iconFile
            //#else
            UMinecraft.getMinecraft().saveLoader.getSaveLoader(worldSummary.fileName, false).worldDirectory.resolve("icon.png")
            //#endif

        private val thumbnail by (if (icon.isFile) UIImage.ofFile(icon) else EssentialPalette.PACK_128X.create()).constrain {
            width = AspectConstraint()
            height = 100.percent
        } effect ShadowEffect() childOf content

        private val info by UIContainer().constrain {
            x = SiblingConstraint(7f)
            width = FillConstraint(false)
            height = 100.percent boundTo thumbnail
        } childOf content

        private val worldName by EssentialUIText(
            worldSummary.displayName,
            shadowColor = EssentialPalette.TEXT_SHADOW,
            truncateIfTooSmall = true,
        ).constrain {
            width = width.coerceAtMost(100.percent)
        } childOf info

        private val lastPlayed = let {
            val lastTime = Instant.ofEpochMilli(worldSummary.lastTimePlayed)
            val lastDate = lastTime.atZone(ZoneId.systemDefault()).toLocalDate()
            val currentTime = Instant.now()
            val durationBetween = Duration.between(lastTime, currentTime)
            when {
                durationBetween < Duration.ofMinutes(1) -> "${durationBetween.seconds} seconds ago"
                durationBetween < Duration.ofMinutes(2) -> "1 minute ago"
                durationBetween < Duration.ofHours(1) -> "${durationBetween.toMinutes()} minutes ago"
                durationBetween < Duration.ofHours(2) -> "1 hour ago"
                durationBetween < Duration.ofDays(1) -> "${durationBetween.toHours()} hours ago"
                durationBetween < Duration.ofDays(2) -> "1 day ago"
                durationBetween <= Duration.ofDays(7) -> "${durationBetween.toDays()} days ago"
                durationBetween <= Duration.ofDays(356) -> formatDate(lastDate, false)
                else -> formatDate(lastDate, true)
            }
        }

        //#if MC>=11202
        private val versionName = worldSummary.versionName
            //#if MC>=11602
            //$$ .string
            //#endif
        //#else
        //$$ private val versionName = worldSummary.getLevelNbtValue { nbt ->
        //$$     nbt.getCompoundTag("Data").getCompoundTag("Version").getString("Name")
        //$$ }.orEmpty().ifEmpty { "Unknown" }
        //#endif

        private val description by EssentialUIText(
            "$versionName - $lastPlayed",
            shadowColor = EssentialPalette.TEXT_SHADOW,
            truncateIfTooSmall = true,
        ).constrain {
            y = 0.pixels(alignOpposite = true)
            width = width.coerceAtMost(100.percent)
            color = EssentialPalette.TEXT.toConstraint()
        } childOf info

        init {
            constrain {
                width = 100.percent
                height = 31.pixels
                color = hoveredState().zip(selected).map { (hovered, selected) ->
                    if (selected) {
                        EssentialPalette.GRAY_OUTLINE_BUTTON_OUTLINE
                    } else if (hovered) {
                        EssentialPalette.GRAY_BUTTON_HOVER
                    } else {
                        EssentialPalette.GRAY_BUTTON
                    }
                }.toConstraint()
            } effect OutlineEffect(
                hoveredState().zip(selected).map {
                    val (hovered, selected) = it
                    if (selected) {
                        EssentialPalette.TEXT_HIGHLIGHT
                    } else if (hovered) {
                        EssentialPalette.GRAY_OUTLINE_BUTTON_OUTLINE_HOVER
                    } else {
                        EssentialPalette.GRAY_BUTTON_HOVER
                    }
                },
                BasicState(1f),
                drawInsideChildren = true,
            )

            effect(ShadowEffect(Color.BLACK))
        }
    }
}
