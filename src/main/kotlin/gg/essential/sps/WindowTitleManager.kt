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
package gg.essential.sps

import gg.essential.Essential
import gg.essential.config.EssentialConfig
import gg.essential.data.VersionData.getMinecraftVersion
import gg.essential.event.render.RenderTickEvent
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.universal.UI18n
import gg.essential.universal.UMinecraft
import gg.essential.util.ServerType
import me.kbrewster.eventbus.Subscribe

//#if MC>=11600
//#else
import org.lwjgl.opengl.Display
//#endif

object WindowTitleManager {

    val referenceHolder = ReferenceHolderImpl()

    // Calling `Display.setTitle` results in random xcb threading errors on 1.8.9, likely because Forge's loading
    // screen is running in another thread and something in this LWJGL version isn't synchronized properly.
    // To work around this, we'll suppress title updates until the first real frame is rendered, by which point
    // Forge's loading screen should be fully shut down.
    // Since we haven't identified the underlying race condition, we don't know for sure that it's exclusive to the
    // LWJGL version of 1.8.9, so out of an abundance of caution we'll apply it on all versions.
    private var beforeFirstFrame = true

    fun register() {

        effect(referenceHolder) {
            // Run effect when config value changes
            EssentialConfig.replaceWindowTitleState()

            updateTitle()
        }

        Essential.EVENT_BUS.register(object {
            @Subscribe
            fun onRenderTick(event: RenderTickEvent) {
                beforeFirstFrame = false
                Essential.EVENT_BUS.unregister(this)
                updateTitle()
            }
        })
    }

    fun updateTitle() {
        if (beforeFirstFrame) return

        //#if MC>=11600
        //$$ UMinecraft.getMinecraft().setDefaultMinecraftTitle()
        //#else
        if (EssentialConfig.replaceWindowTitle) {
            Display.setTitle(this.createTitle())
        }
        //#endif
    }

    private fun createTitle(): String {
        val mc = UMinecraft.getMinecraft()

        val builder = StringBuilder("Minecraft* ").append(getMinecraftVersion())
        val netHandler = UMinecraft.getNetHandler()

        if (netHandler != null && netHandler.networkManager.isChannelOpen) {
            builder.append(" - ")

            val currentServer = mc.currentServerData

            builder.append(UI18n.i18n(when (ServerType.current()) {
                is ServerType.Singleplayer -> "title.singleplayer"
                is ServerType.SPS -> "title.multiplayer.hosted"
                is ServerType.Multiplayer -> if(currentServer != null && currentServer.isOnLAN)
                    "title.multiplayer.lan" else "title.multiplayer.other"
                is ServerType.Realms -> "title.multiplayer.realms"
                else -> "title.multiplayer.other"
            }))
        }
        return builder.toString()
    }
}