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
package gg.essential.gui.friends

import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.util.*
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.common.shadow.ShadowEffect
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.color.toConstraint
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.studio.Tag
import gg.essential.gui.util.hoveredState
import gg.essential.universal.USound
import gg.essential.vigilance.utils.onLeftClick

class TabsSelector(
    selectedTab: MutableState<Tab>,
    unseenFriendRequests: State<Int>,
) : UIContainer() {

    private val tabContainer by UIContainer().constrain {
        x = 4.pixels
        width = ChildBasedSizeConstraint()
        height = FillConstraint()
    } childOf this

    init {

        Tab.values().forEach { tab ->
            val wrapper by UIContainer().constrain {
                x = SiblingConstraint(1f)
                width = ChildBasedMaxSizeConstraint() + 12.pixels
                height = 100.percent
            }.onLeftClick {
                USound.playButtonPress()
                selectedTab.set(tab)
            } childOf tabContainer
            val content by UIContainer().centered().constrain {
                width  = ChildBasedSizeConstraint()
                height = ChildBasedMaxSizeConstraint()
            } childOf wrapper

            val text = EssentialUIText(tab.display).constrain {
                y = CenterConstraint()
                val hoveredState = wrapper.hoveredState().toV2()
                color = memo {
                    if (selectedTab() == tab) {
                        EssentialPalette.ACCENT_BLUE
                    } else {
                        if (hoveredState()) {
                            EssentialPalette.TEXT_HIGHLIGHT
                        } else {
                            EssentialPalette.TEXT
                        }
                    }
                }.toConstraint()
            }.bindShadowColor(selectedTab.map { if (it == tab) EssentialPalette.BLUE_SHADOW else EssentialPalette.COMPONENT_BACKGROUND }.toV1(this)) childOf content

            if (tab == Tab.FRIENDS) {
                val count = unseenFriendRequests

                val unseenFriendRequestIndicator by Tag(
                    stateOf(EssentialPalette.RED),
                    stateOf(EssentialPalette.TEXT_HIGHLIGHT),
                    text = { count().toString() },
                ).constrain {
                    x = SiblingConstraint(5f)
                    y = CenterConstraint()
                    width = ChildBasedSizeConstraint() + 4.pixels
                    height = ChildBasedSizeConstraint() + 4.pixels
                }.bindParent(content, count.map { it > 0 }) effect ShadowEffect(EssentialPalette.BLACK)
            }
        }
    }

}

enum class Tab(val display: String) {
    CHAT("Chat"),
    FRIENDS("Friends"),
}