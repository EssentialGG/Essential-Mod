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
package gg.essential.gui.about.components

import gg.essential.data.VersionData
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.about.AboutMenu
import gg.essential.gui.common.FullEssentialToggle
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.constraints.CenterPixelConstraint
import gg.essential.gui.common.modal.OpenLinkModal
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.common.shadow.ShadowEffect
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.universal.USound
import gg.essential.util.bindHoverEssentialTooltip
import gg.essential.util.centered
import gg.essential.gui.util.hoveredState
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color
import java.net.URI

class TopMenu(aboutMenu: AboutMenu, platformSpecific: BasicState<Boolean>, selectedPage: BasicState<Page>) : UIContainer() {

    // List of items to display on the top/right menu
    private val menuItems = mapOf(
        "Discord" to "https://discord.gg/essential",
        "Twitter" to "https://twitter.com/EssentialMod",
        "Website" to "https://essential.gg",
//        "Support" to "https://essential.gg/support", Removed until support pages/wiki are updated
    )

    init {
        // Middle Divider
        val middleDivider by UIBlock(EssentialPalette.LIGHT_DIVIDER).constrain {
            x = 0.pixels boundTo aboutMenu.middleDivider
            width = 100.percent boundTo aboutMenu.middleDivider
            height = 100.percent
        } childOf this
    }

    private val platformSwitch by FullEssentialToggle(platformSpecific.toV2()).constrain {
        x = SiblingConstraint(10f)
        y = CenterPixelConstraint()
    }.bindParent(this, selectedPage.map { it is ChangelogPage }, index = 1)

    private val pageTitleText = selectedPage.zip(platformSpecific).map { (page, platformSpecific) ->
        if (page is ChangelogPage) {
            if (platformSpecific) {
                "${VersionData.getMinecraftPlatform().name.lowercase().replaceFirstChar { it.uppercase() }} ${VersionData.getMinecraftVersion()} Changelog"
            } else {
                "Full Changelog"
            }
        } else {
            page.name.get()
        }
    }

    private val pageTitle by EssentialUIText().bindText(pageTitleText).constrain {
        x = SiblingConstraint(10f)
        y = CenterPixelConstraint()
    } childOf this

    init {
        platformSwitch.bindHoverEssentialTooltip(
            platformSpecific.map { specific ->
                if (!specific) {
                    "Show ${VersionData.getMinecraftPlatform().name.lowercase().replaceFirstChar { it.uppercase() }} ${VersionData.getMinecraftVersion()} changes only"
                } else {
                    "Show full changelog"
                }
            },
        )
    }

    // Top/Right Menu
    private val rightMenu by UIContainer().constrain {
        x = 10.pixels(alignOpposite = true)
        y = CenterPixelConstraint()
        width = ChildBasedSizeConstraint()
        height = ChildBasedMaxSizeConstraint()
    } childOf this

    init {
        menuItems.forEach { menuItem ->
            val menuItemContainer by UIBlock().constrain {
                x = SiblingConstraint(8f)
                y = CenterConstraint()
                width = ChildBasedSizeConstraint() + 12.pixels
                height = ChildBasedMaxSizeConstraint() + 8.pixels
            }.onLeftClick {
                USound.playButtonPress()
                OpenLinkModal.openUrl(URI(menuItem.value))
            } childOf rightMenu

            menuItemContainer.effect(ShadowEffect(Color.BLACK))

            val hovered = menuItemContainer.hoveredState()
            menuItemContainer.setColor(EssentialPalette.getButtonColor(hovered).toConstraint())

            val menuItemText by EssentialUIText(menuItem.key, shadowColor = EssentialPalette.TEXT_SHADOW)
                .centered().setColor(EssentialPalette.getTextColor(hovered).toConstraint()) childOf menuItemContainer
        }
    }
}
