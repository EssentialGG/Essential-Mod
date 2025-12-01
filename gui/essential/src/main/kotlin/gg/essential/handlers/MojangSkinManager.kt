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
package gg.essential.handlers

import gg.essential.gui.notification.Notifications
import gg.essential.mod.Skin
import gg.essential.network.mojang.ManagedMojangProfileApi
import gg.essential.util.Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

abstract class MojangSkinManager {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Client)

    private data class SkinChange(val api: ManagedMojangProfileApi, val skin: Skin)
    private var queuedSkinChange: SkinChange? = null

    fun queueSkin(user: UUID, skin: Skin) {
        this.queuedSkinChange = SkinChange(ManagedMojangProfileApi.forUser(user), skin)
    }

    fun flushChangesAsync() = coroutineScope.launch {
        flushChanges()
    }
    suspend fun flushChanges() {
        val newSkin = updateSkinNow()
        if (newSkin != null) {
            updateMinecraftGameProfileTextures()
        }
    }

    private suspend fun updateSkinNow(): Skin? {
        val queuedSkinChange = this.queuedSkinChange
        this.queuedSkinChange = null
        if (queuedSkinChange == null) return null

        val api = queuedSkinChange.api

        return try {
            api.putSkin(queuedSkinChange.skin)?.activeSkin
        } catch (e: Exception) {
            LOGGER.error("An error occurred while updating skin", e)
            Notifications.push("Skin", "Failed to upload skin, please try again")
            null
        }
    }

    protected abstract suspend fun updateMinecraftGameProfileTextures()

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MojangSkinManager::class.java)
    }
}
