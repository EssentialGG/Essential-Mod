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
package gg.essential.gui.screenshot.editor.change

import gg.essential.gui.elementa.state.v2.add
import gg.essential.gui.elementa.state.v2.clear
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.mutableListStateOf

class EditHistory {
    val history = mutableListStateOf<Change>()
    val future = mutableListStateOf<Change>()

    val hasChanges = history.map { it.isNotEmpty() }

    val undoEnabled = history.map { it.isNotEmpty() }
    val redoEnabled = future.map { it.isNotEmpty() }

    fun reset() {
        history.clear()
        future.clear()
    }

    /**
     * Adds a new change to the history stack
     *
     * This removes all "future" changes!
     * @param change Change to add
     * @return this
     */
    fun pushChange(change: Change) = apply {
        future.clear()
        history.add(change)
    }

    /**
     * Tries to undo the last stroke
     * @return this
     */
    fun undo() = apply {
        val action = history.getUntracked().lastOrNull() ?: return@apply
        history.set { it.removeAt(it.lastIndex) }
        future.add(action)
    }

    /**
     * Tries to redo the "next" stroke
     * @return this
     */
    fun redo() = apply {
        val action = future.getUntracked().lastOrNull() ?: return@apply
        future.set { it.removeAt(it.lastIndex) }
        history.add(action)
    }
}
