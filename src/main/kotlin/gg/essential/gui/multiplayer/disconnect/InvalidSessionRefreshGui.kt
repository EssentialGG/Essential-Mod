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
package gg.essential.gui.multiplayer.disconnect

import gg.essential.Essential
import gg.essential.config.EssentialConfig
import gg.essential.elementa.font.DefaultFonts
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.utils.getStringSplitToWidth
import gg.essential.gui.account.factory.ManagedSessionFactory
import gg.essential.gui.menu.AccountManager
import gg.essential.gui.modals.AddAccountModal
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMinecraft
import gg.essential.util.*
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiDisconnected
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiMainMenu

//#if MC==11602 && FORGE
//$$ import net.minecraft.client.gui.IGuiEventListener
//$$ import gg.essential.util.textLiteral
//#elseif MC>=11602
//$$ import gg.essential.util.textLiteral
//#if MC>=11701
//$$ import net.minecraft.client.network.ServerAddress
//#endif
//#endif

import java.awt.Color
import java.util.concurrent.TimeUnit

class InvalidSessionRefreshGui(
    private val screen: GuiDisconnected,
    private val parentScreen: GuiScreen,
    private val serverDataInfo: ServerDataInfo,
) {

    private var refreshButtonState = BasicState(true)
    private var errorMessage: String = ""

    private lateinit var returnButton: GuiButton
    private lateinit var refreshSessionButton: GuiButton
    private lateinit var alwaysRefreshButton: GuiButton
    private lateinit var addAccountButton: GuiButton

    private var isAccountManaged = Essential.getInstance().sessionFactories.filterIsInstance<ManagedSessionFactory>()
        .any { UUIDUtil.getClientUUID() in it.sessions }

    init {
        if (EssentialConfig.autoRefreshSession) {
            if (!ServerConnectionUtil.hasRefreshed) {
                refreshButtonState.set(false)
                Multithreading.scheduleOnMainThread({
                    refreshAndRejoin()
                }, 1L, TimeUnit.SECONDS)
            }
        }

        refreshButtonState.onSetValue {
            refreshSessionButton.enabled = it
        }
    }

    private fun makeButton(id: Int, x: Int, y: Int, width: Int, height: Int, text: String): GuiButton {
        //#if MC<=11202
        return GuiButton(id, x, y, width, height, text)
        //#elseif MC>=11903
        //$$ return ButtonWidget.builder(textLiteral(text)) { onButtonClicked(it) }.dimensions(x, y, width, height).build()
        //#else
        //$$ return Button(x, y, width, height, textLiteral(text)) { onButtonClicked(it) }
        //#endif
    }

    fun setupButtons(
        buttons: List<GuiButton>,
        addButton: (GuiButton) -> GuiButton,
    ) {

        returnButton = buttons[0]
        val margin = 8

        refreshSessionButton = makeButton(
            100,
            returnButton.x,
            returnButton.y + returnButton.height + margin,
            returnButton.width,
            returnButton.height,
            "Refresh session and rejoin",
        )
        refreshSessionButton.enabled = refreshButtonState.get()

        val alwaysRefreshWidth = refreshSessionButton.width / 4
        alwaysRefreshButton = makeButton(
            101,
            refreshSessionButton.x + refreshSessionButton.width - alwaysRefreshWidth,
            refreshSessionButton.y + refreshSessionButton.height + margin,
            alwaysRefreshWidth,
            refreshSessionButton.height,
            if (EssentialConfig.autoRefreshSession) "Yes" else "No",
        )

        addAccountButton = makeButton(
            102,
            returnButton.x,
            returnButton.y + returnButton.height + margin,
            returnButton.width,
            returnButton.height,
            "Add Account",
        )

        if (isAccountManaged) {
            addButton(refreshSessionButton)
            addButton(alwaysRefreshButton)
        } else {
            addButton(addAccountButton)
            errorMessage =
                "If you add your Minecraft account to the account switcher in the main menu, you can refresh your session without restarting."
        }
    }

    fun draw(drawContext: UDrawContext) {
        if (!::refreshSessionButton.isInitialized) return

        //#if MC>=12106
        //$$ // FIXME This is a bit wasteful, but there should only be at most one of these at any one time.
        //$$ //       But once we have a more general DrawContext in UC/Elementa, we should get rid of this.
        //$$ gg.essential.util.AdvancedDrawContext.drawImmediate(drawContext.mc) { matrixStack ->
        //$$     doDraw(matrixStack)
        //$$ }
        //#else
        doDraw(drawContext.matrixStack)
        //#endif
    }

    private fun doDraw(matrixStack: UMatrixStack) {
        if (errorMessage.isNotEmpty()) {
            //#if MC>=12000
            //$$ var y = 80f
            //#else
            var y = 65f
            //#endif

            getStringSplitToWidth(errorMessage, screen.width - 50f, 1.0f).reversed().forEach {
                DefaultFonts.VANILLA_FONT_RENDERER.drawString(
                    matrixStack,
                    it,
                    Color.WHITE,
                    (screen.width / 2) - (DefaultFonts.VANILLA_FONT_RENDERER.getStringWidth(it, 10.0f) / 2),
                    returnButton.y - y,
                    10f,
                    1f,
                )
                y += UMinecraft.getFontRenderer().FONT_HEIGHT + 1
            }
        }

        if (isAccountManaged) {
            DefaultFonts.VANILLA_FONT_RENDERER.drawString(
                matrixStack,
                "Always refresh session:",
                Color.WHITE,
                refreshSessionButton.x.toFloat(),
                alwaysRefreshButton.y + 6f,
                10f,
                1f,
            )
        }
    }

    fun onButtonClicked(button: GuiButton) {
        if (!button.enabled) return

        when (button) {
            refreshSessionButton -> { refreshAndRejoin() }
            alwaysRefreshButton -> {
                val autoRefresh = !EssentialConfig.autoRefreshSession
                EssentialConfig.autoRefreshSession = autoRefresh
                //#if MC<=11202
                alwaysRefreshButton.displayString = if (autoRefresh) "Yes" else "No"
                //#else
                //$$ alwaysRefreshButton.message = if (autoRefresh) textLiteral("Yes") else textLiteral("No")
                //#endif
            }
            addAccountButton -> {
                GuiUtil.openScreen { GuiMainMenu() }
                GuiUtil.pushModal { AddAccountModal(it) }
            }
        }
    }

    private fun refreshAndRejoin() {
        refreshButtonState.set(false)

        AccountManager.refreshCurrentSession(true) { _, error ->
            if (error != null) {
                // If we're here, the session token could not be refreshed and the account probably needs to be re-added.
                isAccountManaged = false
                GuiUtil.openScreen { screen }
                ServerConnectionUtil.hasRefreshed = true
                Essential.logger.warn("Session token no longer valid: {}", error)
            } else {
                // The session was successfully refreshed - let's connect to the server again!
                if (serverDataInfo.serverData != null) {
                    MinecraftUtils.connectToServer(serverDataInfo.serverData, parentScreen)
                } else {
                    val hostAndPort = "${serverDataInfo.ip}:${serverDataInfo.port}"
                    MinecraftUtils.connectToServer(hostAndPort, hostAndPort, previousScreen = parentScreen)
                }
                ServerConnectionUtil.hasRefreshed = true
            }
        }
    }
}
