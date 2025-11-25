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
@file:Suppress("UNUSED_PARAMETER")

package gg.essential.gui.common

import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution

class Mc12106ScissorHandler {
    var viewportWidth: Int = 0
        private set
    var viewportHeight: Int = 0
        private set

    fun beforeDraw(matrixStack: UMatrixStack) {
        viewportWidth = UResolution.viewportWidth
        viewportHeight = UResolution.viewportHeight
    }

    fun afterDraw(matrixStack: UMatrixStack) {
    }
}