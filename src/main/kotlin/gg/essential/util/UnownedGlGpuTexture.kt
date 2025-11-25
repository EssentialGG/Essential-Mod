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
package gg.essential.util

import gg.essential.util.image.GpuTexture

class UnownedGlGpuTexture(
    format: GpuTexture.Format,
    override val glId: Int,
    override val width: Int,
    override val height: Int
) : GlGpuTexture(format) {
    override fun resize(width: Int, height: Int) {
        throw UnsupportedOperationException()
    }

    override fun delete() {
        throw UnsupportedOperationException()
    }
}
