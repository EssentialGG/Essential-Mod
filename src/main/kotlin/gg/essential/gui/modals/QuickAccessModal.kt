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
package gg.essential.gui.modals

import gg.essential.Essential
import gg.essential.config.McEssentialConfig
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.utils.withAlpha
import gg.essential.event.gui.GuiKeyTypedEvent
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.MenuButton
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.common.state
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.friends.SocialMenu
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedSize
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.hoverTooltip
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.menu.AccountManager
import gg.essential.gui.menu.AccountManagerModal
import gg.essential.gui.menu.RightSideBarNew.Companion.hostOrInviteButtonPressed
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.screenshot.components.ScreenshotBrowser
import gg.essential.gui.sps.WorldShareSettingsGui
import gg.essential.gui.util.pollingStateV2
import gg.essential.gui.wardrobe.Wardrobe
import gg.essential.key.EssentialKeybindingRegistry
import gg.essential.sps.SpsAddress
import gg.essential.universal.UMinecraft
import gg.essential.universal.UScreen
import gg.essential.util.CachedAvatarImage
import gg.essential.util.GuiUtil
import gg.essential.util.USession
import gg.essential.util.isMainMenu
import me.kbrewster.eventbus.Subscribe
import net.minecraft.client.gui.GuiIngameMenu
import net.minecraft.client.gui.GuiScreen
import java.awt.Color

/**
 * Some of the elements of the [PauseMenuDisplay] are only accessible there, and thus can become inaccessible when hidden
 * by third parties, such as FancyMenu, so this modal will hold alternative & quick access to these elements.
 *
 * Elements included:
 * - Wardrobe
 * - Host / Invite
 * - Social
 * - Pictures
 * - Settings
 * - Account
 */
class QuickAccessModal(modalManager: ModalManager) : Modal(modalManager) {

    override fun LayoutScope.layoutModal() {
        box(
            Modifier
                .childBasedSize(6f)
                .color(Color.BLACK.withAlpha(0.7f))
        ) {
            column {
                layoutTitle()
                spacer(height = 6f)
                layoutBody()
            }
        }
    }

    private fun LayoutScope.layoutTitle() {
        text("Essential Quick Access Menu", Modifier.shadow(Color.BLACK))
    }

    private fun LayoutScope.layoutBody() {

        // this should match up with the check in PauseMenuDisplay
        // though perhaps we may apply further limits on this appearing in future
        val hostable = !UMinecraft.getMinecraft().isSingleplayer

        val isHostingWorld = pollingStateV2 {
            Essential.getInstance().connectionManager.spsManager.localSession != null
        }

        val hasInviteButton = memo {
            val currentServer = UMinecraft.getMinecraft().currentServerData
            val isSpsServer = currentServer?.let { SpsAddress.parse(it.serverIP) } != null
            (!hostable && !isSpsServer) || isHostingWorld()
        }

        val hostableOrHasInviteButton = memo { hostable || hasInviteButton() }

        val buttonModifier = Modifier.width(20f).height(20f)

        // several icons are right aligned for consistency with the right side bar, however right aligned icons currently
        // break with no button label text, so we will use this blank text on each such button
        val notEmpty = " ".state()

        row (Arrangement.spacedBy(6f)) {

            if_({ hostableOrHasInviteButton() && isHostingWorld() }) {
                MenuButton {
                    openScreenAndCloseModal { WorldShareSettingsGui() }
                }.setIcon(EssentialPalette.HOST_5X.state())(
                    buttonModifier.hoverScope().hoverTooltip("World Host Settings"))
            }


            if_({ hostableOrHasInviteButton() && hasInviteButton() }) {
                MenuButton(notEmpty) {
                    close()
                    hostOrInviteButtonPressed()
                }.setIcon(EssentialPalette.ENVELOPE_9X7.state(), rightAligned = true, xOffset = -1f)(
                    buttonModifier.hoverScope().hoverTooltip("Invite"))
            } `else` {
                if_(hostableOrHasInviteButton) {
                    MenuButton(notEmpty) {
                        hostOrInviteButtonPressed()
                    }.setIcon(EssentialPalette.HOST_5X.state(), rightAligned = true, xOffset = -3f, yOffset = -1f)(
                        buttonModifier.hoverScope().hoverTooltip("Host"))
                }
            }


            MenuButton(notEmpty) {
                openScreenAndCloseModal { SocialMenu() }
            }.setIcon(BasicState(EssentialPalette.SOCIAL_10X), rightAligned = true, xOffset = -1f)(
                buttonModifier.hoverScope().hoverTooltip("Social"))


            MenuButton(notEmpty) {
                openScreenAndCloseModal { Wardrobe() }
            }.setIcon(BasicState(EssentialPalette.COSMETICS_10X7), rightAligned = true, xOffset = 0f)(
                buttonModifier.hoverScope().hoverTooltip("Wardrobe"))


            MenuButton(notEmpty) {
                openScreenAndCloseModal { ScreenshotBrowser() }
            }.setIcon(BasicState(EssentialPalette.PICTURES_10X10), rightAligned = true, xOffset = 0f, yOffset = 2f)(
                buttonModifier.hoverScope().hoverTooltip("Pictures"))


            MenuButton(notEmpty) {
                openScreenAndCloseModal { McEssentialConfig.gui() }
            }.setIcon(BasicState(EssentialPalette.SETTINGS_9X8), rightAligned = true, xOffset = -1f, yOffset = 1f)(
                buttonModifier.hoverScope().hoverTooltip("Settings"))


            // Only show the account button when opened from the main menu, just to guarantee we are only altering the
            // session from a place where it is safe to do so
            if (UScreen.currentScreen.isMainMenu) {
                MenuButton(notEmpty) {
                    close()
                    GuiUtil.pushModal { AccountManagerModal(it, AccountManager()) }
                }.setIcon({
                    bind(USession.active) { active ->
                        CachedAvatarImage.create(active.uuid)(Modifier.shadow(EssentialPalette.TEXT_SHADOW))
                    }
                }, rightAligned = true, xOffset = -1f)(buttonModifier.hoverScope().hoverTooltip("Account"))
            }



        }

    }

    private inline fun <reified T : GuiScreen> openScreenAndCloseModal(noinline screen: () -> T) {
        close()
        GuiUtil.openScreen(screen)
    }

    companion object {

        init {
            Essential.EVENT_BUS.register(this)
        }

        // Opens the quick access modal from a post screen key press event
        @Subscribe
        fun keyPressed(event: GuiKeyTypedEvent.Post) {
            if (!EssentialKeybindingRegistry.getInstance().openQuickAccess.isKeyCode(event.keyCode)) return

            val screen = event.screen
            if (screen != null && !screen.isMainMenu && screen !is GuiIngameMenu) return

            open()
        }

        // Opens the quick access modal when in-game
        @JvmStatic
        fun openInGame() {
            if (UScreen.currentScreen == null) open()
        }

        private fun open() {
            GuiUtil.pushModal { QuickAccessModal(it) }
        }
    }
}