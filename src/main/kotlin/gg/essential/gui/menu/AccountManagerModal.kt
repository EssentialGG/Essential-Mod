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
package gg.essential.gui.menu

import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.Window
import gg.essential.elementa.dsl.effect
import gg.essential.elementa.dsl.pixel
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.events.UIClickEvent
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.ContextOptionMenu
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.common.effect.HorizontalScissorEffect
import gg.essential.gui.common.modal.EssentialModal2
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.FloatPosition
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedMaxHeight
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillHeight
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.layoutdsl.fillRemainingWidth
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.heightAspect
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.hoverTooltip
import gg.essential.gui.layoutdsl.icon
import gg.essential.gui.layoutdsl.maxHeight
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.scrollable
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.then
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.modals.AddAccountModal
import gg.essential.gui.overlay.ModalManager
import gg.essential.universal.USound
import gg.essential.util.CachedAvatarImage
import gg.essential.util.GuiUtil
import gg.essential.util.USession
import gg.essential.util.onRightClick
import gg.essential.util.scrollGradient
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color

class AccountManagerModal(
    manager: ModalManager,
    private val accountManager: AccountManager,
    private val accounts: ListState<AccountManager.AccountInfo> = accountManager.allAccounts
) : EssentialModal2(manager) {

    private lateinit var scroller: ScrollComponent

    override fun LayoutScope.layoutTitle() {
        text("Your Accounts", Modifier.shadow(Color.BLACK))
    }

    override fun LayoutScope.layoutBody() {
        scroller = scrollable(
            Modifier.fillWidth().childBasedMaxHeight().maxHeight(166f),
            vertical = true
        ) {
            column(Modifier.fillWidth()) {
                column(Modifier.fillWidth(), Arrangement.spacedBy(2f)) {
                    forEach(memo { accounts().sortedByDescending { USession.active().uuid == it.uuid } }.toListState()) { account ->
                        entry(account)
                    }
                }
            }
        }
        scroller.removeEffect<ScissorEffect>()
        scroller.effect(HorizontalScissorEffect(bottomMargin = 1.pixel))
        scroller.scrollGradient(20.pixels)
    }

    override fun LayoutScope.layoutBodyScrollBar() = layoutBodyScrollBarImpl(scroller)

    override fun LayoutScope.layoutButtons() {
        primaryButton("Add Account", Modifier.width(122f)) {
            GuiUtil.pushModal { AddAccountModal(it) }
        }
    }

    private fun LayoutScope.entry(accountInfo: AccountManager.AccountInfo) {
        val uuid = accountInfo.uuid
        val name = accountInfo.name
        val isOriginal = memo { accountInfo in accountManager.originalAccounts() }
        val isActive = State { USession.active().uuid == uuid }

        fun openContextMenu(event: UIClickEvent) {
            if (isActive.getUntracked() || isOriginal.getUntracked()) {
                return
            }
            ContextOptionMenu.create(ContextOptionMenu.Position(event), Window.of(this@AccountManagerModal),
                ContextOptionMenu.Option(
                    "Sign Out",
                    EssentialPalette.SIGN_OUT_8X7,
                    textColor = EssentialPalette.TEXT_WARNING,
                    hoveredColor = EssentialPalette.TEXT_WARNING
                ) {
                    accountManager.promptRemove(uuid, name)
                }
            )
        }

        box(
            Modifier.height(19f).fillWidth().color(EssentialPalette.BUTTON_HIGHLIGHT)
                .hoverColor(EssentialPalette.GRAY_OUTLINE_BUTTON_OUTLINE).shadow().hoverScope()
        ) {
            box(
                Modifier.fillParent(padding = 1f).color(EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT)
                    .hoverColor(EssentialPalette.BUTTON_HIGHLIGHT)
            ) {
                row(Modifier.fillWidth(padding = 4f).fillHeight(padding = 3f)) {
                    row(Modifier.fillRemainingWidth(), Arrangement.spacedBy(7f, FloatPosition.START)) {
                        CachedAvatarImage.create(uuid)(Modifier.shadow())
                        text(name, Modifier.alignVertical(Alignment.Start(1f)).shadow(Color.BLACK))
                    }

                    if_(isActive) {
                        icon(
                            EssentialPalette.CHECKMARK_7X5,
                            Modifier.color(EssentialPalette.FEATURED_BLUE)
                                .shadow(Color.BLACK)
                        )
                        spacer(width = 3f)
                    } `else` {
                        box(Modifier.width(12f).heightAspect(1f).alignVertical(Alignment.Start).hoverScope()) {
                            val iconModifier = memo {
                                if (isOriginal()) {
                                    Modifier.color(EssentialPalette.TEXT_DARK_DISABLED).hoverTooltip(
                                        "Cannot sign out of account\nused to launch Minecraft",
                                        position = EssentialTooltip.Position.ABOVE
                                    )
                                } else {
                                    Modifier.color(EssentialPalette.TEXT)
                                        .hoverColor(EssentialPalette.TEXT_HIGHLIGHT)
                                }
                            }
                            icon(EssentialPalette.OPTIONS_8X2, Modifier.then(iconModifier))
                        }.onLeftClick { event ->
                            event.stopPropagation()
                            openContextMenu(event)
                        }
                    }
                }
            }
        }.onLeftClick { event ->
            event.stopPropagation()
            if (isActive.getUntracked()) {
                return@onLeftClick
            }
            USound.playButtonPress()
            accountManager.login(uuid)
            close()
        }.onRightClick { event ->
            event.stopPropagation()
            openContextMenu(event)
        }
    }

}