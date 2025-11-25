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
package gg.essential.gui.proxies

import gg.essential.config.EssentialConfig
import gg.essential.gui.common.MenuButton
import gg.essential.gui.common.MenuButton.Companion.DARK_GRAY
import gg.essential.gui.common.MenuButton.Companion.GRAY
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.UDrawContext
import net.minecraft.client.gui.GuiButton
import java.awt.Color

class MenuButtonProxy(essentialId: String, initialPosID: Int) :
    EssentialProxyElement<MenuButton>(essentialId, initialPosID, MenuButton::class.java) {

    private val tintEffect = TintVanillaButtonsEffect()

    private val refHolder = ReferenceHolderImpl()

    init {
        // background hiding observer
        effect(refHolder) {
            essentialComponentState()?.drawsBackground?.set(State {
                drawBehaviourState() == ProxyDrawBehaviour.ESSENTIAL_DRAWS
            })
        }
    }

    private fun requiresTinting(styleColor: Color, isHovered: Boolean): Boolean {
        if (platform.config.shouldDarkenRetexturedButtons) return true

        // also tint if the menu buttons style colour is non default, either GRAY or DARK_GRAY depending on mouse hover

        return styleColor != (if (isHovered) GRAY else DARK_GRAY).buttonColor
    }

    override fun handleRender(context: UDrawContext, vanillaRender: (UDrawContext) -> Unit) {
        if (drawBehaviour == ProxyDrawBehaviour.ESSENTIAL_DRAWS_TEXT_PROXY_DRAWS_BACKGROUND) {

            val comp = essentialComponent ?: return

            // FIXME remap bug: doesn't recognize the x/y fields without explicit type here
            val button: GuiButton = this
            val x1 = button.x
            val y1 = button.y
            val x2 = button.x + button.width
            val y2 = button.y + button.height

            fun renderWithoutText(context2: UDrawContext) {
                val current = vanillaTextAccess
                vanillaTextAccess = ""
                vanillaRender(context2)
                vanillaTextAccess = current
            }

            val color = comp.styleState.get().buttonColor

            // use the component's styleHover state rather than the vanilla hover method, to help avoid de-syncs with the
            // behaviour in [MenuButton.draw()] that is determined by styleHover
            if (requiresTinting(color, comp.styleHover.get())) {
                tintEffect.drawTinted(context, x1, y1, x2, y2, color) {
                    renderWithoutText(it)
                }
            } else {
                renderWithoutText(context)
            }

            return
        } else if (drawBehaviour == ProxyDrawBehaviour.PROXY_DRAWS) {
            // handle normally, i.e. vanilla render
            super.handleRender(context, vanillaRender)
        }
    }

    override fun MenuButton.click() {
        this.runAction()
    }

    override fun drawBehaviourFromComponentState(): ProxyDrawBehaviour {
        return when {
            essentialComponent == null
                -> ProxyDrawBehaviour.ESSENTIAL_DRAWS

            hasProxyContentBeenModified
                -> ProxyDrawBehaviour.PROXY_DRAWS

            EssentialConfig.useVanillaButtonForRetexturing.getUntracked()
                -> ProxyDrawBehaviour.ESSENTIAL_DRAWS_TEXT_PROXY_DRAWS_BACKGROUND

            else -> ProxyDrawBehaviour.ESSENTIAL_DRAWS
        }
    }
}