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
package gg.essential.gui.layoutdsl

import gg.essential.elementa.components.UIImage
import gg.essential.elementa.components.inspector.Inspector
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.AutoImageSize
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.image.ImageFactory

fun LayoutScope.image(icon: ImageFactory, modifier: Modifier = Modifier): UIImage {
    val image = icon.create()
    image.supply(AutoImageSize(image))
    return image(modifier)
}

fun LayoutScope.image(icon: State<ImageFactory>, modifier: Modifier = Modifier) =
    bind(icon) { image(it, modifier) }

/** Like [image] but with a default [EssentialPalette.TEXT_SHADOW] shadow. */
fun LayoutScope.icon(icon: ImageFactory, modifier: Modifier = Modifier) =
    image(icon, Modifier.shadow(EssentialPalette.TEXT_SHADOW).then(modifier))

fun LayoutScope.icon(icon: State<ImageFactory>, modifier: Modifier = Modifier) =
    bind(icon) { icon(it, modifier) }

@Deprecated("Using StateV1 is discouraged, use StateV2 instead")
fun LayoutScope.icon(icon: gg.essential.elementa.state.State<ImageFactory>, modifier: Modifier = Modifier) =
    bind(icon) { icon(it, modifier) }

@Suppress("unused")
private val init = run {
    Inspector.registerComponentFactory(null)
}
