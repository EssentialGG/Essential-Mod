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
package gg.essential.network.connectionmanager.cosmetics

import gg.essential.connectionmanager.common.packet.cosmetic.emote.ClientCosmeticEmoteWheelSelectPacket
import gg.essential.connectionmanager.common.packet.cosmetic.emote.ClientCosmeticEmoteWheelUpdatePacket
import gg.essential.connectionmanager.common.packet.cosmetic.emote.ServerCosmeticEmoteWheelPopulatePacket
import gg.essential.cosmetics.CosmeticId
import gg.essential.cosmetics.model.EmoteWheel
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.clear
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableListStateOf
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.setAll
import gg.essential.mod.cosmetics.EmoteWheelPage
import gg.essential.network.CMConnection
import gg.essential.network.connectionmanager.NetworkedManager
import gg.essential.network.cosmetics.toMod
import gg.essential.network.registerPacketHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.time.Duration.Companion.seconds

class EmoteWheelManager(
    val connectionManager: CMConnection,
    val unlockedCosmetics: State<Set<CosmeticId>>,
) : NetworkedManager {

    private val emoteWheels = mutableListStateOf<EmoteWheelPage>()
    val orderedEmoteWheels = memo {
        emoteWheels().sortedBy { it.createdAt.toEpochMilli() }
    }
    private val mutableSelectedEmoteWheelId: MutableState<String?> = mutableStateOf(null)
    val selectedEmoteWheelId: State<String?> = mutableSelectedEmoteWheelId
    val selectedEmoteWheel = memo { orderedEmoteWheels().firstOrNull { it.id == mutableSelectedEmoteWheelId() } }
    val selectedEmoteWheelIndex = memo { orderedEmoteWheels().indexOfFirst { it.id == mutableSelectedEmoteWheelId() } }
    val selectedEmoteWheelSlots = memo {
        selectedEmoteWheel()?.slots ?: List(EmoteWheelPage.SLOTS) { null }
    }

    private var flushJob: Job? = null
    private var sentEmoteWheelId: String? = null

    init {
        connectionManager.registerPacketHandler<ServerCosmeticEmoteWheelPopulatePacket> { packet ->
            populateEmoteWheels(packet.emoteWheels())
        }
    }

    override fun resetState() {
        emoteWheels.clear()
        sentEmoteWheelId = null
    }

    fun getEmoteWheel(id: String): EmoteWheelPage? {
        return emoteWheels.getUntracked().find { it.id == id }
    }

    fun selectEmoteWheel(id: String) {
        val emoteWheel = orderedEmoteWheels.getUntracked().find { it.id == id } ?: return
        mutableSelectedEmoteWheelId.set(emoteWheel.id)
        flushSelectedEmoteWheel(true)
    }

    /**
     * Selects the emote wheel at a given offset from the currently selected emote wheel, wrapping around if necessary.
     *
     * @return The index of the newly selected emote wheel or null if no emote wheels are present
     */
    fun shiftSelectedEmoteWheel(offset: Int): Int? {
        val numWheels = orderedEmoteWheels.getUntracked().size
        if (numWheels == 0) {
            return null
        }
        val curIndex = selectedEmoteWheelIndex.getUntracked()
        val newIndex = (curIndex + offset + numWheels) % numWheels
        selectEmoteWheel(orderedEmoteWheels.getUntracked()[newIndex].id)
        return newIndex
    }

    private fun populateEmoteWheels(emoteWheels: List<EmoteWheel>) {
        this.emoteWheels.setAll(emoteWheels.map { it.toMod() })

        sentEmoteWheelId = emoteWheels.find { it.selected() }?.id()
        mutableSelectedEmoteWheelId.set(sentEmoteWheelId)

        if (mutableSelectedEmoteWheelId.getUntracked() == null) {
            LOGGER.error("No emote wheel was selected, selecting the first one.")
            emoteWheels.firstOrNull()?.id()?.let { selectEmoteWheel(it) }
        }
    }

    fun flushSelectedEmoteWheel(debounce: Boolean) {
        flushJob?.cancel()
        if (debounce) {
            flushJob = connectionManager.connectionScope.launch {
                delay(3.seconds)
                flushSelectedEmoteWheel(false)
            }
            return
        }

        val selectedEmoteWheelId = mutableSelectedEmoteWheelId.getUntracked() ?: return
        if (selectedEmoteWheelId == sentEmoteWheelId) {
            return
        }
        sentEmoteWheelId = selectedEmoteWheelId
        connectionManager.call(ClientCosmeticEmoteWheelSelectPacket(selectedEmoteWheelId)).fireAndForget()
    }

    /**
     * Sets one of the saved emotes for an emote wheel.
     *
     * @param emoteWheelId The emote wheel id of the emote wheel to change
     * @param slotIndex The id of slot in the emote wheel to change
     * @param emoteId The (CosmeticId) emote id to save
     */
    fun setEmote(emoteWheelId: String, slotIndex: Int, emoteId: CosmeticId?) {
        val emoteWheel = emoteWheels.getUntracked().find { it.id == emoteWheelId } ?: return
        setEmotes(emoteWheelId, emoteWheel.slots.toMutableList().apply { this[slotIndex] = emoteId })
    }

    /**
     * Sets the saved emotes for an emote wheel
     *
     * @param emoteWheelId The emote wheel id of the emote wheel to change
     * @param emotes The (CosmeticId) list of emotes to save
     */
    fun setEmotes(emoteWheelId: String, emotes: List<CosmeticId?>) {
        val emoteWheel = emoteWheels.getUntracked().find { it.id == emoteWheelId } ?: return
        val slots = emoteWheel.slots.toMutableList()
        val unlockedCosmetics = unlockedCosmetics.getUntracked()
        for ((i, value) in emotes.withIndex()) {
            if (value != null && !unlockedCosmetics.contains(value)) {
                continue
            }

            if (value != slots.set(i, value)) {
                connectionManager.call(ClientCosmeticEmoteWheelUpdatePacket(emoteWheel.id, i, value)).fireAndForget()
            }
        }
        editEmoteWheel(emoteWheel.copy(slots = slots.toList()))
    }

    private fun editEmoteWheel(new: EmoteWheelPage) {
        emoteWheels.set { list ->
            list.set(list.indexOfFirst { it.id == new.id }, new)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(EmoteWheelManager::class.java)
    }
}