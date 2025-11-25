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
package gg.essential.gui.friends.title

import gg.essential.elementa.dsl.provideDelegate
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialCollapsibleSearchbar
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.state
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.friends.Tab
import gg.essential.gui.friends.message.SocialMenuActions
import gg.essential.gui.friends.state.SocialStates
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.childBasedMaxHeight
import gg.essential.gui.layoutdsl.childBasedWidth
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.widthAspect

class SocialTitleManagementActions(
    private val currentTab: State<Tab>,
    socialStates: SocialStates,
    socialMenuActions: SocialMenuActions,
) : TitleManagementActions(
    socialStates,
    socialMenuActions,
) {

    override val search by EssentialCollapsibleSearchbar(
        placeholder = "Search...",
        placeholderColor = EssentialPalette.TEXT_HIGHLIGHT,
        activateOnType = false,
    )

    init {
        val buttonModifier = Modifier.height(17f).widthAspect(1f)

        val friendButton = IconButton(
            EssentialPalette.INVITE_10X6,
            tooltipText = "Add Friend"
        ).rebindIconColor(EssentialPalette.TEXT_HIGHLIGHT.state()).onActiveClick {
            addFriend()
        }

        val makeGroupButton = IconButton(
            EssentialPalette.MAKE_GROUP_9X8,
            tooltipText = "Make Group"
        ).onActiveClick {
            makeGroup()
        }

        val blockButton = IconButton(
            EssentialPalette.BLOCK_10X7,
            tooltipText = "Block Player",
        ).onActiveClick {
            blockPlayer()
        }

        this.layout(Modifier.childBasedWidth().childBasedMaxHeight()) {
            row(Arrangement.spacedBy(3f)) {
                friendButton(buttonModifier.color(EssentialPalette.BLUE_BUTTON).hoverColor(EssentialPalette.BLUE_BUTTON_HOVER).hoverScope())
                makeGroupButton(buttonModifier)
                if_({ currentTab() == Tab.FRIENDS }) {
                    blockButton(buttonModifier)
                }
                search()
            }
        }
    }

}