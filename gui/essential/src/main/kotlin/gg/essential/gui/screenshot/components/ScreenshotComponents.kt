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
package gg.essential.gui.screenshot.components

import gg.essential.elementa.components.*
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.sps.SpsAddress
import gg.essential.util.UuidNameLookup

//metadata is null when it does not exist. It is automatically created at the first time its needed (such as to set favorite or upload)
data class ScreenshotProperties(val id: ScreenshotId, var metadata: ClientScreenshotMetadata?) {

    fun matchesSearch(textSearch: String): Boolean {
        val matchAgainst = mutableListOf(id.name)
        val metadata = metadata
        if (metadata != null) {
            matchAgainst.add(UuidNameLookup.getName(metadata.authorId).getNow(null) ?: "")
            val identifier = metadata.locationMetadata.identifier
            if (identifier != null) {
                val host = SpsAddress.parse(identifier)?.host
                if (host != null) {
                    matchAgainst.add(UuidNameLookup.getName(host).getNow(null) ?: "")
                }
                matchAgainst.add(identifier)
            }
        }
        return matchAgainst.any {
            it.contains(textSearch, ignoreCase = true)
        }
    }
}

fun ClientScreenshotMetadata.cloneWithNewChecksum(checksum: String): ClientScreenshotMetadata {
    return copy(checksum = checksum)
}

abstract class ScreenshotView(val active: State<Boolean>) : UIContainer() {
    val titleBar: UIContainer = UIContainer()

    companion object {
        val buttonSize = 17f
    }
}

sealed interface View {
    val back: View

    data object List : View {
        override val back: View
            get() = this
    }

    data class Focus(val screenshot: ScreenshotId) : View {
        override val back: View
            get() = List
    }

    data class Edit(val screenshot: ScreenshotId, override val back: View) : View
}

// Magic constants are put here and named instead of being inlined
const val screenshotPadding = 10f
const val screenshotGroupHeaderHeight = 30f
const val hoverOutlineWidth = 2f
const val focusImageWidthPercent = 67
const val focusImageVerticalPadding = 20f
